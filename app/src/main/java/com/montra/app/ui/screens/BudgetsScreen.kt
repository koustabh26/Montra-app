@file:OptIn(ExperimentalLayoutApi::class)

package com.montra.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import com.montra.app.ui.components.QuickAmountChips
import com.montra.app.ui.components.GhostButton
import com.montra.app.ui.components.FormSheet
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.montra.app.AppViewModel
import com.montra.app.data.BudgetEntity
import com.montra.app.data.CategoryEntity
import com.montra.app.data.TxType
import com.montra.app.domain.BudgetStatus
import com.montra.app.domain.FinanceCalc
import com.montra.app.ui.components.CategoryBadge
import com.montra.app.ui.components.CategoryPicker
import com.montra.app.ui.components.ChuField
import com.montra.app.ui.components.EmptyState
import com.montra.app.ui.components.GlassCard
import com.montra.app.ui.components.MonthSwitcher
import com.montra.app.ui.components.Muted
import com.montra.app.ui.components.NeonButton
import com.montra.app.ui.components.NeonProgress
import com.montra.app.ui.theme.CColors
import com.montra.app.util.Money
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.roundToInt

/** Pseudo-category used for the overall monthly budget (id 0). */
private val OVERALL = CategoryEntity(id = 0, name = "Overall", emoji = "🎯", color = 0xFF00E5FF, type = TxType.EXPENSE)

private fun statusColor(ratio: Float): Color = when {
    ratio >= 1f -> CColors.Red
    ratio >= 0.7f -> CColors.Amber
    else -> CColors.Lime
}

@Composable
fun BudgetsScreen(vm: AppViewModel, modifier: Modifier = Modifier) {
    val txs by vm.transactions.collectAsStateWithLifecycle()
    val cats by vm.categories.collectAsStateWithLifecycle()
    val budgets by vm.budgets.collectAsStateWithLifecycle()
    val month by vm.budgetMonth.collectAsStateWithLifecycle()
    val today = LocalDate.now()

    val catMap = remember(cats) { cats.associateBy { it.id } + (0L to OVERALL) }
    val monthTx = remember(txs, month) { FinanceCalc.inMonth(txs, month) }
    val statuses = remember(budgets, monthTx) { FinanceCalc.budgetStatuses(budgets, monthTx) }

    var editing by remember { mutableStateOf<BudgetEntity?>(null) }
    var creating by remember { mutableStateOf(false) }

    val daysLeft = if (month == YearMonth.from(today)) month.lengthOfMonth() - today.dayOfMonth + 1 else 0

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { MonthSwitcher(month, { vm.shiftBudgetMonth(-1) }, { vm.shiftBudgetMonth(1) }, { vm.budgetMonthToToday() }) }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Monthly budgets", color = CColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Muted("Limits repeat every month")
                }
                NeonButton("+ New", onClick = { creating = true })
            }
        }
        if (statuses.isEmpty()) {
            item {
                EmptyState("🎯", "No budgets yet", "Set an overall monthly limit or one per category to stay on track.")
            }
        } else {
            items(statuses, key = { it.budget.id }) { s ->
                BudgetCard(s, catMap[s.budget.categoryId], daysLeft) { editing = s.budget }
            }
        }
    }

    if (creating || editing != null) {
        val existing = editing
        BudgetSheet(
            existing = existing,
            expenseCategories = cats.filter { it.type == TxType.EXPENSE },
            takenIds = budgets.map { it.categoryId }.toSet(),
            onDismiss = { creating = false; editing = null },
            onSave = { vm.saveBudget(it); creating = false; editing = null },
            onDelete = { vm.deleteBudget(it); creating = false; editing = null }
        )
    }
}

@Composable
private fun BudgetCard(s: BudgetStatus, category: CategoryEntity?, daysLeft: Int, onClick: () -> Unit) {
    val color = statusColor(s.ratio)
    GlassCard(Modifier.fillMaxWidth(), glow = color, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryBadge(category)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(category?.name ?: "Category", color = CColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Muted("${Money.formatWhole(s.spent)} of ${Money.formatWhole(s.limit)}")
            }
            Text("${(s.ratio * 100).roundToInt()}%", color = color, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        Spacer(Modifier.height(10.dp))
        NeonProgress(s.ratio, color)
        Spacer(Modifier.height(8.dp))
        if (s.isOver) {
            Text("Over budget by ${Money.format(-s.remaining)}", color = CColors.Red, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        } else {
            val perDay = if (daysLeft > 0) " · ${Money.formatWhole(s.remaining / daysLeft)}/day left" else ""
            Text("${Money.format(s.remaining)} remaining$perDay", color = CColors.TextSecondary, fontSize = 13.sp)
        }
    }
}

@Composable
private fun BudgetSheet(
    existing: BudgetEntity?,
    expenseCategories: List<CategoryEntity>,
    takenIds: Set<Long>,
    onDismiss: () -> Unit,
    onSave: (BudgetEntity) -> Unit,
    onDelete: (BudgetEntity) -> Unit
) {
    val options = remember(expenseCategories, takenIds, existing) {
        (listOf(OVERALL) + expenseCategories).filter { it.id == existing?.categoryId || it.id !in takenIds }
    }
    var categoryId by remember { mutableStateOf(existing?.categoryId ?: options.firstOrNull()?.id) }
    var limitText by remember { mutableStateOf(existing?.let { Money.toInput(it.limitPaise) } ?: "") }
    var tried by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val limit = Money.parsePaise(limitText)

    FormSheet(if (existing == null) "New budget" else "Edit budget", onDismiss) {
        if (options.isEmpty()) {
            Text("Every category already has a budget.", color = CColors.TextSecondary)
        } else {
            // Amount first so it is always visible, even with the keyboard open.
            ChuField(
                limitText, { limitText = it }, "Monthly limit",
                numeric = true, prefix = "₹",
                error = if (tried && limit == null) "Enter an amount greater than 0" else null
            )
            QuickAmountChips(listOf(1000L, 2500L, 5000L, 10000L, 25000L, 50000L)) { limitText = it.toString() }

            if (existing == null) {
                Text("Applies to", color = CColors.TextSecondary, fontSize = 13.sp)
                CategoryPicker(options, categoryId, { categoryId = it.id })
            } else {
                val label = if (existing.categoryId == 0L) {
                    "🎯 Overall monthly budget"
                } else {
                    expenseCategories.firstOrNull { it.id == existing.categoryId }
                        ?.let { "${it.emoji} ${it.name}" } ?: "Category"
                }
                Text(label, color = CColors.TextPrimary, fontWeight = FontWeight.SemiBold)
            }

            NeonButton(
                "Save budget",
                onClick = {
                    tried = true
                    val cid = categoryId
                    if (limit != null && cid != null) {
                        onSave(BudgetEntity(id = existing?.id ?: 0L, categoryId = cid, limitPaise = limit))
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (existing != null) {
            GhostButton(
                if (confirmDelete) "Tap again to confirm delete" else "Delete budget",
                onClick = { if (confirmDelete) onDelete(existing) else confirmDelete = true },
                modifier = Modifier.fillMaxWidth(),
                color = CColors.Red
            )
        }
    }
}
