package com.montra.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.montra.app.AppViewModel
import com.montra.app.security.PinPolicy
import com.montra.app.ui.components.GlassCard
import com.montra.app.ui.components.Muted
import com.montra.app.ui.components.SectionTitle
import com.montra.app.ui.theme.CColors

private enum class SettingsDialog { NONE, SET_PIN, CHANGE_PIN, DISABLE_PIN, CONFIRM_RESTORE, CONFIRM_RESET }

@Composable
fun SettingsScreen(
    vm: AppViewModel,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onExportCsv: () -> Unit,
    modifier: Modifier = Modifier
) {
    var dialog by remember { mutableStateOf(SettingsDialog.NONE) }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { SectionTitle("Security") }
        item {
            GlassCard(Modifier.fillMaxWidth(), glow = CColors.Cyan) {
                SwitchRow(
                    title = "App lock",
                    subtitle = if (vm.lockEnabled) "6-digit PIN required every time the app opens" else "Protect your finances with a 6-digit PIN",
                    checked = vm.lockEnabled,
                    onChange = { on -> dialog = if (on) SettingsDialog.SET_PIN else SettingsDialog.DISABLE_PIN }
                )
                if (vm.lockEnabled) {
                    Spacer(Modifier.height(4.dp))
                    ActionRow("Change PIN", "Needs your current PIN") { dialog = SettingsDialog.CHANGE_PIN }
                }
                Spacer(Modifier.height(4.dp))
                SwitchRow(
                    title = "Screen privacy",
                    subtitle = "Hide the app from screenshots and the recent-apps view",
                    checked = vm.secureScreen,
                    onChange = { vm.updateSecureScreen(it) }
                )
            }
        }

        item { SectionTitle("Backup & data") }
        item {
            GlassCard(Modifier.fillMaxWidth(), glow = CColors.Violet) {
                ActionRow("Back up to file", "Save everything as a .json file you can keep anywhere", onClick = onBackup)
                ActionRow("Restore from backup", "Replaces all current data with a backup file") {
                    dialog = SettingsDialog.CONFIRM_RESTORE
                }
                ActionRow("Export transactions (CSV)", "Open your history in Excel or Sheets", onClick = onExportCsv)
            }
        }
        item {
            GlassCard(Modifier.fillMaxWidth(), glow = CColors.Red) {
                ActionRow("Erase all data", "Deletes transactions, budgets and goals; categories reset to defaults", color = CColors.Red) {
                    dialog = SettingsDialog.CONFIRM_RESET
                }
            }
        }

        item { SectionTitle("About") }
        item {
            GlassCard(Modifier.fillMaxWidth(), glow = CColors.Magenta) {
                Text("Montra", color = CColors.TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                Muted("Version 1.0.0 · Currency: Indian Rupee (₹)", size = 13)
                Spacer(Modifier.height(6.dp))
                Muted("Works fully offline. Your data lives only on this device unless you export a backup. Backup files are not encrypted — store them somewhere private.", size = 13)
            }
        }
    }

    when (dialog) {
        SettingsDialog.SET_PIN -> SetPinDialog(
            onDismiss = { dialog = SettingsDialog.NONE },
            onConfirm = { pin ->
                val error = vm.enablePin(pin)
                if (error == null) dialog = SettingsDialog.NONE
                error
            }
        )
        SettingsDialog.CHANGE_PIN -> ChangePinDialog(
            onDismiss = { dialog = SettingsDialog.NONE },
            onConfirm = { current, newPin ->
                val error = vm.changePin(current, newPin)
                if (error == null) dialog = SettingsDialog.NONE
                error
            }
        )
        SettingsDialog.DISABLE_PIN -> DisablePinDialog(
            verify = { vm.disablePin(it) },
            onDismiss = { dialog = SettingsDialog.NONE },
            onDone = { dialog = SettingsDialog.NONE }
        )
        SettingsDialog.CONFIRM_RESTORE -> ConfirmDialog(
            title = "Restore backup?",
            message = "This replaces ALL current transactions, budgets, goals and categories with the contents of the backup file.",
            confirmLabel = "Choose file",
            onDismiss = { dialog = SettingsDialog.NONE },
            onConfirm = { dialog = SettingsDialog.NONE; onRestore() }
        )
        SettingsDialog.CONFIRM_RESET -> ConfirmDialog(
            title = "Erase everything?",
            message = "All transactions, budgets and goals will be permanently deleted. This can't be undone — back up first if unsure.",
            confirmLabel = "Erase",
            danger = true,
            onDismiss = { dialog = SettingsDialog.NONE },
            onConfirm = { vm.resetAll(); dialog = SettingsDialog.NONE }
        )
        SettingsDialog.NONE -> Unit
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = CColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Muted(subtitle, size = 12)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ActionRow(
    title: String,
    subtitle: String,
    color: androidx.compose.ui.graphics.Color = CColors.TextPrimary,
    onClick: () -> Unit
) {
    Column(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp)
    ) {
        Text(title, color = color, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        Muted(subtitle, size = 12)
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    danger: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CColors.CardBottom,
        title = { Text(title, color = CColors.TextPrimary) },
        text = { Text(message, color = CColors.TextSecondary) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = if (danger) CColors.Red else CColors.Cyan)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
internal fun PinField(value: String, onChange: (String) -> Unit, label: String, error: String? = null) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.length <= PinPolicy.LENGTH && it.all(Char::isDigit)) onChange(it) },
        label = { Text(label) },
        singleLine = true,
        isError = error != null,
        supportingText = error?.let { msg -> { Text(msg) } },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier.fillMaxWidth()
    )
}

/**
 * Choose a new 6-digit PIN. [onConfirm] returns an error message to show, or null when the PIN was saved.
 * With [forced] the dialog can't be dismissed (used to replace an old 4-digit PIN).
 */
@Composable
internal fun SetPinDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> String?,
    title: String = "Set a 6-digit PIN",
    forced: Boolean = false
) {
    var pin by remember { mutableStateOf("") }
    var again by remember { mutableStateOf("") }
    var tried by remember { mutableStateOf(false) }
    var serverError by remember { mutableStateOf<String?>(null) }

    val policyError = if (pin.length == PinPolicy.LENGTH) PinPolicy.validate(pin) else null
    val lengthError = if (tried && pin.length != PinPolicy.LENGTH) "PIN must be exactly 6 digits" else null
    val mismatch = tried && pin.length == PinPolicy.LENGTH && pin != again

    AlertDialog(
        onDismissRequest = { if (!forced) onDismiss() },
        containerColor = CColors.CardBottom,
        title = { Text(title, color = CColors.TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Muted(
                    if (forced) "Your old PIN is too short. Choose a new 6-digit PIN to keep using the app lock."
                    else "Exactly 6 digits. Avoid dates, repeats and sequences. If you forget it you'll have to reinstall the app and lose local data, so keep a backup file.",
                    size = 12
                )
                PinField(pin, { pin = it; serverError = null }, "New PIN", lengthError ?: policyError)
                PinField(again, { again = it; serverError = null }, "Confirm PIN", if (mismatch) "PINs don't match" else serverError)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                tried = true
                if (pin.length == PinPolicy.LENGTH && policyError == null && pin == again) {
                    serverError = onConfirm(pin)
                }
            }) { Text("Save PIN") }
        },
        dismissButton = if (forced) null else ({ TextButton(onClick = onDismiss) { Text("Cancel") } })
    )
}

/** Change the PIN: the current PIN must be entered first. */
@Composable
internal fun ChangePinDialog(onDismiss: () -> Unit, onConfirm: (current: String, newPin: String) -> String?) {
    var current by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var again by remember { mutableStateOf("") }
    var tried by remember { mutableStateOf(false) }
    var serverError by remember { mutableStateOf<String?>(null) }

    val policyError = if (pin.length == PinPolicy.LENGTH) PinPolicy.validate(pin) else null
    val mismatch = tried && pin.length == PinPolicy.LENGTH && pin != again

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CColors.CardBottom,
        title = { Text("Change PIN", color = CColors.TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PinField(current, { current = it; serverError = null }, "Current PIN", serverError)
                PinField(pin, { pin = it }, "New 6-digit PIN", policyError ?: if (tried && pin.length != PinPolicy.LENGTH) "PIN must be exactly 6 digits" else null)
                PinField(again, { again = it }, "Confirm new PIN", if (mismatch) "PINs don't match" else null)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                tried = true
                if (current.isNotEmpty() && pin.length == PinPolicy.LENGTH && policyError == null && pin == again) {
                    serverError = onConfirm(current, pin)
                }
            }) { Text("Change PIN") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun DisablePinDialog(verify: (String) -> Boolean, onDismiss: () -> Unit, onDone: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CColors.CardBottom,
        title = { Text("Turn off app lock", color = CColors.TextPrimary) },
        text = {
            PinField(pin, { pin = it; wrong = false }, "Current PIN", if (wrong) "Incorrect PIN" else null)
        },
        confirmButton = {
            TextButton(onClick = { if (verify(pin)) onDone() else wrong = true }) { Text("Turn off") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
