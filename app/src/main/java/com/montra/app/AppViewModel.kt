package com.montra.app

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.montra.app.data.AppDatabase
import com.montra.app.data.BackupFormatException
import com.montra.app.data.BackupManager
import com.montra.app.data.BudgetEntity
import com.montra.app.data.CategoryEntity
import com.montra.app.data.GoalEntity
import com.montra.app.data.Repository
import com.montra.app.data.TransactionEntity
import com.montra.app.domain.Period
import com.montra.app.domain.PeriodMode
import com.montra.app.security.LockoutPolicy
import com.montra.app.security.PinPolicy
import com.montra.app.security.SecurePrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth

/** A one-off message for the snackbar, optionally with an action such as Undo. */
class UiMessage(val text: String, val actionLabel: String? = null, val onAction: (() -> Unit)? = null)

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repository(AppDatabase.get(app))
    private val prefs = SecurePrefs(app)

    val categories: StateFlow<List<CategoryEntity>> =
        repo.categories.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val transactions: StateFlow<List<TransactionEntity>> =
        repo.transactions.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val budgets: StateFlow<List<BudgetEntity>> =
        repo.budgets.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val goals: StateFlow<List<GoalEntity>> =
        repo.goals.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** The day / month / year shown on Home, History and Insights. */
    val period = MutableStateFlow(Period(PeriodMode.MONTH, LocalDate.now()))

    /** Budgets are always monthly, so they keep their own month. */
    val budgetMonth = MutableStateFlow(YearMonth.now())

    // Security state (Compose snapshot state so the UI reacts directly)
    var lockEnabled by mutableStateOf(prefs.lockEnabled)
        private set
    var locked by mutableStateOf(prefs.lockEnabled)
        private set
    var secureScreen by mutableStateOf(prefs.secureScreen)
        private set
    var lockoutSecondsLeft by mutableStateOf(0)
        private set

    /** True for a PIN saved by an older version (4 digits); the user must switch to a 6-digit PIN. */
    var pinNeedsUpgrade by mutableStateOf(prefs.hasPin && !prefs.pinIsStrong)
        private set
    var attemptsLeft by mutableStateOf(LockoutPolicy.attemptsLeft(prefs.failures))
        private set

    val pinIsStrong: Boolean get() = prefs.pinIsStrong

    private val _messages = MutableSharedFlow<UiMessage>(extraBufferCapacity = 8)
    val messages = _messages.asSharedFlow()

    private fun say(text: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
        _messages.tryEmit(UiMessage(text, actionLabel, onAction))
    }

    init {
        viewModelScope.launch { repo.seedIfEmpty() }
        refreshLockout()
    }

    // ---- Period navigation ----
    fun setMode(mode: PeriodMode) {
        period.value = period.value.copy(mode = mode)
    }

    fun shiftPeriod(delta: Int) {
        period.value = period.value.shift(delta)
    }

    fun periodToToday() {
        period.value = period.value.copy(anchor = LocalDate.now())
    }

    fun shiftBudgetMonth(delta: Long) {
        budgetMonth.value = budgetMonth.value.plusMonths(delta)
    }

    fun budgetMonthToToday() {
        budgetMonth.value = YearMonth.now()
    }

    // ---- Transactions ----
    fun saveTransaction(t: TransactionEntity) {
        viewModelScope.launch {
            repo.saveTransaction(t)
            say(if (t.id == 0L) "Transaction added" else "Transaction updated")
        }
    }

    fun deleteTransaction(t: TransactionEntity) {
        viewModelScope.launch {
            repo.deleteTransaction(t)
            say("Transaction deleted", "Undo") {
                viewModelScope.launch { repo.restoreTransaction(t) }
            }
        }
    }

    // ---- Categories ----
    fun saveCategory(c: CategoryEntity) {
        viewModelScope.launch {
            repo.saveCategory(c)
            say(if (c.id == 0L) "Category added" else "Category updated")
        }
    }

    fun deleteCategory(c: CategoryEntity) {
        viewModelScope.launch {
            if (repo.deleteCategory(c)) {
                say("Category deleted; its transactions moved to the fallback category")
            } else {
                say("The fallback category can't be deleted")
            }
        }
    }

    // ---- Budgets ----
    fun saveBudget(b: BudgetEntity) {
        viewModelScope.launch {
            repo.saveBudget(b)
            say("Budget saved")
        }
    }

    fun deleteBudget(b: BudgetEntity) {
        viewModelScope.launch {
            repo.deleteBudget(b)
            say("Budget removed")
        }
    }

    // ---- Goals ----
    fun saveGoal(g: GoalEntity) {
        viewModelScope.launch {
            repo.saveGoal(g)
            say("Goal saved")
        }
    }

    fun deleteGoal(g: GoalEntity) {
        viewModelScope.launch {
            repo.deleteGoal(g)
            say("Goal deleted")
        }
    }

    /** Adds (positive) or withdraws (negative) money; the saved amount never drops below zero. */
    fun adjustGoal(g: GoalEntity, deltaPaise: Long) {
        viewModelScope.launch {
            val updated = (g.savedPaise + deltaPaise).coerceAtLeast(0L)
            repo.saveGoal(g.copy(savedPaise = updated))
            say(if (updated >= g.targetPaise) "Goal reached! 🎉" else "Savings updated")
        }
    }

    // ---- Backup / restore / export ----
    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            try {
                val json = BackupManager.toJson(repo.snapshot())
                withContext(Dispatchers.IO) { writeText(uri, json) }
                say("Backup saved")
            } catch (e: Exception) {
                say("Backup failed: ${e.message ?: "unknown error"}")
            }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            try {
                val text = withContext(Dispatchers.IO) { readText(uri) }
                val data = BackupManager.parse(text)
                repo.restore(data)
                say("Restored ${data.transactions.size} transactions")
            } catch (e: BackupFormatException) {
                say(e.message ?: "Invalid backup file")
            } catch (e: Exception) {
                say("Restore failed: ${e.message ?: "unknown error"}")
            }
        }
    }

    fun exportCsv(uri: Uri) {
        viewModelScope.launch {
            try {
                val csv = BackupManager.toCsv(repo.snapshot())
                withContext(Dispatchers.IO) { writeText(uri, csv) }
                say("CSV exported")
            } catch (e: Exception) {
                say("Export failed: ${e.message ?: "unknown error"}")
            }
        }
    }

    fun resetAll() {
        viewModelScope.launch {
            repo.resetAll()
            say("All data erased")
        }
    }

    private fun writeText(uri: Uri, text: String) {
        val out = getApplication<Application>().contentResolver.openOutputStream(uri, "wt")
            ?: throw IllegalStateException("Can't open the destination file")
        out.use { it.write(text.toByteArray(Charsets.UTF_8)) }
    }

    private fun readText(uri: Uri): String {
        val input = getApplication<Application>().contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Can't open the selected file")
        input.use { stream ->
            val bytes = stream.readBytes()
            if (bytes.size > MAX_BACKUP_BYTES) throw BackupFormatException("File is too large to be a backup.")
            return bytes.toString(Charsets.UTF_8)
        }
    }

    // ---- App lock ----
    /** Turns the lock on (or replaces the PIN). Returns an error message, or null on success. */
    fun enablePin(pin: String): String? {
        PinPolicy.validate(pin)?.let { return it }
        prefs.setPin(pin)
        lockEnabled = true
        pinNeedsUpgrade = false
        attemptsLeft = LockoutPolicy.attemptsLeft(0)
        say("App lock enabled")
        return null
    }

    /** Changes the PIN after checking the current one. Returns an error message, or null on success. */
    fun changePin(current: String, newPin: String): String? {
        if (!prefs.verifyPin(current)) return "Current PIN is incorrect"
        if (current == newPin) return "Choose a different PIN from the current one"
        PinPolicy.validate(newPin)?.let { return it }
        prefs.setPin(newPin)
        lockEnabled = true
        pinNeedsUpgrade = false
        say("PIN changed")
        return null
    }

    /** Disables the lock if [currentPin] is right. */
    fun disablePin(currentPin: String): Boolean {
        if (!prefs.verifyPin(currentPin)) return false
        prefs.clearPin()
        lockEnabled = false
        locked = false
        pinNeedsUpgrade = false
        say("App lock disabled")
        return true
    }

    fun tryUnlock(pin: String): Boolean {
        if (prefs.lockoutRemainingMillis() > 0) {
            refreshLockout()
            return false
        }
        if (prefs.verifyPin(pin)) {
            prefs.recordSuccess()
            lockoutSecondsLeft = 0
            attemptsLeft = LockoutPolicy.attemptsLeft(0)
            locked = false
            return true
        }
        prefs.recordFailure()
        attemptsLeft = LockoutPolicy.attemptsLeft(prefs.failures)
        refreshLockout()
        return false
    }

    fun refreshLockout() {
        val remaining = prefs.lockoutRemainingMillis()
        lockoutSecondsLeft = if (remaining > 0) ((remaining + 999) / 1000).toInt() else 0
    }

    private var skipNextLock = false

    /** Call right before opening a system file picker so returning from it doesn't demand the PIN again. */
    fun expectExternalScreen() {
        skipNextLock = true
    }

    fun onForeground() {
        skipNextLock = false
    }

    fun onBackground() {
        if (skipNextLock) {
            skipNextLock = false
            return
        }
        if (lockEnabled) locked = true
    }

    fun updateSecureScreen(enabled: Boolean) {
        prefs.secureScreen = enabled
        secureScreen = enabled
    }

    private companion object {
        const val MAX_BACKUP_BYTES = 50 * 1024 * 1024
    }
}
