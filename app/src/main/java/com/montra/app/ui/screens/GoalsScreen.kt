package com.montra.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.montra.app.data.CATEGORY_COLORS
import com.montra.app.data.GoalEntity
import com.montra.app.domain.FinanceCalc
import com.montra.app.ui.components.ChuField
import com.montra.app.ui.components.ColorPicker
import com.montra.app.ui.components.EmptyState
import com.montra.app.ui.components.GlassCard
import com.montra.app.ui.components.Muted
import com.montra.app.ui.components.NeonButton
import com.montra.app.ui.components.NeonProgress
import com.montra.app.ui.components.RingProgress
import com.montra.app.ui.theme.CColors
import com.montra.app.util.Money
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val dateFormat = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)

@Composable
fun GoalsScreen(vm: AppViewModel, modifier: Modifier = Modifier) {
    val goals by vm.goals.collectAsStateWithLifecycle()
    val today = LocalDate.now()
    var editing by remember { mutableStateOf<GoalEntity?>(null) }
    var creating by remember { mutableStateOf(false) }
    var adjusting by remember { mutableStateOf<GoalEntity?>(null) }

    val totalSaved = goals.sumOf { it.savedPaise }
    val totalTarget = goals.sumOf { it.targetPaise }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Savings goals", color = CColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Muted("Track what you're saving for")
                }
                NeonButton("+ New goal", onClick = { creating = true })
            }
        }
        if (goals.isNotEmpty()) {
            item {
                GlassCard(Modifier.fillMaxWidth(), glow = CColors.Lime) {
                    Muted("Total saved")
                    Text(Money.format(totalSaved), color = CColors.TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(8.dp))
                    NeonProgress(if (totalTarget > 0) totalSaved.toFloat() / totalTarget else 0f, CColors.Lime)
                    Spacer(Modifier.height(6.dp))
                    Muted("of ${Money.formatWhole(totalTarget)} across ${goals.size} goal${if (goals.size == 1) "" else "s"}")
                }
            }
        }
        if (goals.isEmpty()) {
            item { EmptyState("🏆", "No goals yet", "Create a goal — a trip, a gadget, an emergency fund — and watch it fill up.") }
        } else {
            items(goals, key = { it.id }) { g ->
                val color = Color(g.color)
                val progress = FinanceCalc.goalProgress(g)
                val need = FinanceCalc.goalMonthlyNeed(g, today)
                GlassCard(Modifier.fillMaxWidth(), glow = color, onClick = { editing = g }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RingProgress(progress, color, "${(progress * 100).roundToInt()}%")
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("${g.emoji}  ${g.name}", color = CColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Muted("${Money.formatWhole(g.savedPaise)} of ${Money.formatWhole(g.targetPaise)}", size = 13)
                            val deadline = g.deadline
                            if (g.savedPaise >= g.targetPaise) {
                                Text("🎉 Goal reached!", color = CColors.Lime, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            } else if (deadline != null) {
                                Muted("By ${LocalDate.ofEpochDay(deadline).format(dateFormat)}" + (need?.let { " · save ${Money.formatWhole(it)}/month" } ?: ""))
                            } else {
                                Muted("${Money.formatWhole(g.targetPaise - g.savedPaise)} to go")
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    NeonButton("Add / withdraw", onClick = { adjusting = g }, modifier = Modifier.fillMaxWidth(), colors = listOf(color, CColors.Violet))
                }
            }
        }
    }

    if (creating || editing != null) {
        GoalSheet(
            existing = editing,
            today = today,
            onDismiss = { creating = false; editing = null },
            onSave = { vm.saveGoal(it); creating = false; editing = null },
            onDelete = { vm.deleteGoal(it); creating = false; editing = null }
        )
    }
    adjusting?.let { g ->
        AdjustSheet(
            goal = g,
            onDismiss = { adjusting = null },
            onApply = { delta -> vm.adjustGoal(g, delta); adjusting = null }
        )
    }
}

@Composable
private fun GoalSheet(
    existing: GoalEntity?,
    today: LocalDate,
    onDismiss: () -> Unit,
    onSave: (GoalEntity) -> Unit,
    onDelete: (GoalEntity) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var emoji by remember { mutableStateOf(existing?.emoji ?: "🎯") }
    var targetText by remember { mutableStateOf(existing?.let { Money.toInput(it.targetPaise) } ?: "") }
    var monthsText by remember {
        mutableStateOf(existing?.deadline?.let { FinanceCalc.monthsLeft(today, it).toString() } ?: "")
    }
    var color by remember { mutableStateOf(existing?.color ?: CATEGORY_COLORS[0]) }
    var confirmDelete by remember { mutableStateOf(false) }
    var tried by remember { mutableStateOf(false) }

    val target = Money.parsePaise(targetText)
    val months = monthsText.trim().let { if (it.isEmpty()) null else it.toIntOrNull() }
    val monthsInvalid = monthsText.isNotBlank() && (months == null || months !in 1..600)

    FormSheet(if (existing == null) "New goal" else "Edit goal", onDismiss) {
        ChuField(name, { if (it.length <= 40) name = it }, "Goal name", error = if (tried && name.isBlank()) "Give your goal a name" else null)
        ChuField(
            targetText, { targetText = it }, "Target amount", numeric = true, prefix = "₹",
            error = if (tried && target == null) "Enter an amount greater than 0" else null
        )
        QuickAmountChips(listOf(5000L, 10000L, 25000L, 50000L, 100000L, 500000L)) { targetText = it.toString() }
        ChuField(
            monthsText, { monthsText = it.filter(Char::isDigit).take(3) }, "Deadline in months from today (optional)",
            error = if (monthsInvalid) "Use 1 to 600 months, or leave empty" else null
        )
        ChuField(emoji, { if (it.length <= 8) emoji = it }, "Emoji")
        Text("Colour", color = CColors.TextSecondary, fontSize = 13.sp)
        ColorPicker(CATEGORY_COLORS, color) { color = it }

        NeonButton(
            "Save goal",
            onClick = {
                tried = true
                if (name.isNotBlank() && target != null && !monthsInvalid) {
                    onSave(
                        GoalEntity(
                            id = existing?.id ?: 0L,
                            name = name.trim(),
                            emoji = emoji.trim().ifEmpty { "🎯" },
                            color = color,
                            targetPaise = target,
                            savedPaise = existing?.savedPaise ?: 0L,
                            deadline = months?.let { today.plusMonths(it.toLong()).toEpochDay() },
                            createdAt = existing?.createdAt ?: System.currentTimeMillis()
                        )
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        if (existing != null) {
            GhostButton(
                if (confirmDelete) "Tap again to confirm delete" else "Delete goal",
                onClick = { if (confirmDelete) onDelete(existing) else confirmDelete = true },
                modifier = Modifier.fillMaxWidth(),
                color = CColors.Red
            )
        }
    }
}

@Composable
private fun AdjustSheet(goal: GoalEntity, onDismiss: () -> Unit, onApply: (Long) -> Unit) {
    var amountText by remember { mutableStateOf("") }
    var tried by remember { mutableStateOf(false) }
    val amount = Money.parsePaise(amountText)
    val tooMuch = amount != null && amount > goal.savedPaise

    FormSheet("${goal.emoji} ${goal.name}", onDismiss) {
        Muted("Saved so far: ${Money.format(goal.savedPaise)} of ${Money.format(goal.targetPaise)}", size = 13)
        ChuField(
            amountText, { amountText = it }, "Amount", numeric = true, prefix = "₹",
            error = when {
                tried && amount == null -> "Enter an amount greater than 0"
                tried && tooMuch -> "You only have ${Money.format(goal.savedPaise)} saved in this goal"
                else -> null
            }
        )
        QuickAmountChips(listOf(500L, 1000L, 2000L, 5000L, 10000L)) { amountText = it.toString() }
        NeonButton(
            "Add to goal",
            onClick = {
                tried = true
                if (amount != null) onApply(amount)
            },
            modifier = Modifier.fillMaxWidth()
        )
        GhostButton(
            "Withdraw from goal",
            onClick = {
                tried = true
                if (amount != null && !tooMuch) onApply(-amount)
            },
            modifier = Modifier.fillMaxWidth(),
            color = CColors.Amber
        )
    }
}
