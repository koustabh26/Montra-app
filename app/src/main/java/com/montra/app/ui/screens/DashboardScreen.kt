package com.montra.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.montra.app.AppViewModel
import com.montra.app.data.TransactionEntity
import com.montra.app.data.TxType
import com.montra.app.domain.FinanceCalc
import com.montra.app.domain.Period
import com.montra.app.domain.PeriodMode
import com.montra.app.ui.components.CategoryBadge
import com.montra.app.ui.components.EmptyState
import com.montra.app.ui.components.GlassCard
import com.montra.app.ui.components.Muted
import com.montra.app.ui.components.NeonProgress
import com.montra.app.ui.components.PeriodSwitcher
import com.montra.app.ui.components.SectionTitle
import com.montra.app.ui.theme.CColors
import com.montra.app.util.Money
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun DashboardScreen(
    vm: AppViewModel,
    onEdit: (TransactionEntity) -> Unit,
    onSeeAll: () -> Unit,
    onOpenBudgets: () -> Unit,
    modifier: Modifier = Modifier
) {
    val txs by vm.transactions.collectAsStateWithLifecycle()
    val cats by vm.categories.collectAsStateWithLifecycle()
    val budgets by vm.budgets.collectAsStateWithLifecycle()
    val period by vm.period.collectAsStateWithLifecycle()
    val today = LocalDate.now()

    val catMap = remember(cats) { cats.associateBy { it.id } }
    val periodTx = remember(txs, period) { FinanceCalc.inPeriod(txs, period) }
    val summary = remember(periodTx) { FinanceCalc.summary(periodTx) }
    val topCats = remember(periodTx) { FinanceCalc.categoryTotals(periodTx, TxType.EXPENSE).take(4) }

    // Budget alerts always describe the current month, whichever period is on screen.
    val thisMonthTx = remember(txs, today) { FinanceCalc.inMonth(txs, YearMonth.from(today)) }
    val alerts = remember(budgets, thisMonthTx) {
        FinanceCalc.budgetStatuses(budgets, thisMonthTx).filter { it.ratio >= 0.8f }
    }
    val recent = remember(txs) { txs.take(5) }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            PeriodSwitcher(
                period, today,
                onMode = { vm.setMode(it) },
                onPrev = { vm.shiftPeriod(-1) },
                onNext = { vm.shiftPeriod(1) },
                onToday = { vm.periodToToday() }
            )
        }

        // Hero balance card
        item {
            GlassCard(Modifier.fillMaxWidth(), glow = CColors.Cyan) {
                Muted("Net balance · ${period.label(today)}")
                Spacer(Modifier.height(4.dp))
                Text(
                    (if (summary.net < 0) "−" else "") + Money.format(abs(summary.net)),
                    color = if (summary.net < 0) CColors.Magenta else CColors.TextPrimary,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FlowTile("⬇ Income", summary.income, CColors.Lime, Modifier.weight(1f))
                    FlowTile("⬆ Expenses", summary.expense, CColors.Magenta, Modifier.weight(1f))
                }
                Spacer(Modifier.height(14.dp))
                if (summary.income > 0) {
                    val rate = summary.savingsRate
                    Row(Modifier.fillMaxWidth()) {
                        Muted("Kept from income", Modifier.weight(1f))
                        Text(
                            "${(rate * 100).roundToInt()}%",
                            color = if (rate >= 0.2) CColors.Lime else if (rate >= 0) CColors.Amber else CColors.Red,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    NeonProgress(rate.toFloat().coerceAtLeast(0f), if (rate >= 0.2) CColors.Lime else CColors.Amber)
                } else {
                    Muted("No income recorded for this ${period.mode.label.lowercase()}.")
                }
            }
        }

        // Plain-language pace card that matches the chosen period
        item {
            when (period.mode) {
                PeriodMode.DAY -> {
                    val monthTx = remember(txs, period) { FinanceCalc.inMonth(txs, period.yearMonth) }
                    DayPaceCard(period.anchor, today, summary.expense, FinanceCalc.total(monthTx, TxType.EXPENSE))
                }
                PeriodMode.MONTH -> {
                    val ym = period.yearMonth
                    val daily = remember(txs, ym) { FinanceCalc.dailyTotals(txs, ym, TxType.EXPENSE) }
                    val todaySpend = remember(txs, today) {
                        txs.filter { it.type == TxType.EXPENSE && it.date == today.toEpochDay() }.sumOf { it.amountPaise }
                    }
                    PaceCard(
                        month = ym,
                        today = today,
                        todaySpend = todaySpend,
                        avgDaily = FinanceCalc.avgDailySpend(summary.expense, ym, today),
                        projected = FinanceCalc.projectedExpense(summary.expense, ym, today),
                        monthExpense = summary.expense,
                        daily = daily,
                        overallBudget = budgets.firstOrNull { it.categoryId == 0L }
                    )
                }
                PeriodMode.YEAR -> YearPaceCard(period, today, txs)
            }
        }

        if (alerts.isNotEmpty()) {
            item {
                GlassCard(Modifier.fillMaxWidth(), glow = CColors.Red, onClick = onOpenBudgets) {
                    Text("⚠ Budget alerts · this month", color = CColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(Modifier.height(6.dp))
                    alerts.take(3).forEach { a ->
                        val name = if (a.budget.categoryId == 0L) "Overall budget" else (catMap[a.budget.categoryId]?.name ?: "Category")
                        val pct = (a.ratio * 100).roundToInt()
                        Text(
                            if (a.isOver) "$name is over by ${Money.formatWhole(-a.remaining)} ($pct%)" else "$name is at $pct%",
                            color = if (a.isOver) CColors.Red else CColors.Amber,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }

        item { SectionTitle("Where it went") }
        item {
            GlassCard(Modifier.fillMaxWidth(), glow = CColors.Magenta) {
                if (topCats.isEmpty()) {
                    EmptyState("🌌", "No spending in this ${period.mode.label.lowercase()}", "Add an expense to see your top categories.")
                } else {
                    val top = topCats.first().second.coerceAtLeast(1L)
                    topCats.forEach { (id, total) ->
                        val c = catMap[id]
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            CategoryBadge(c, 34)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Row {
                                    Text(c?.name ?: "Unknown", color = CColors.TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                    Text(Money.formatWhole(total), color = CColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Spacer(Modifier.height(5.dp))
                                NeonProgress(total.toFloat() / top, Color(c?.color ?: 0xFF9AA0C3), height = 6)
                            }
                        }
                    }
                }
            }
        }

        item {
            SectionTitle("Recent activity") {
                Text(
                    "See all",
                    color = CColors.Cyan,
                    fontSize = 13.sp,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onSeeAll).padding(6.dp)
                )
            }
        }
        item {
            GlassCard(Modifier.fillMaxWidth(), glow = CColors.Violet) {
                if (recent.isEmpty()) {
                    EmptyState("✨", "Welcome to Montra", "Tap the + button to add your first transaction.")
                } else {
                    recent.forEach { t -> TransactionRow(t, catMap[t.categoryId]) { onEdit(t) } }
                }
            }
        }
    }
}

@Composable
private fun FlowTile(label: String, paise: Long, color: Color, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.10f))
            .padding(12.dp)
    ) {
        Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(2.dp))
        Text(Money.formatWhole(paise), color = CColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PaceRow(
    title: String,
    value: String,
    explain: String,
    valueColor: Color = CColors.TextPrimary,
    note: String? = null,
    noteColor: Color = CColors.TextSecondary
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = CColors.TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(value, color = valueColor, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.height(2.dp))
        Text(explain, color = CColors.TextSecondary, fontSize = 12.sp)
        if (note != null) {
            Spacer(Modifier.height(2.dp))
            Text(note, color = noteColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun Divider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(CColors.Divider))
}

/** DAY view: what was spent that day, compared with a typical day of that month. */
@Composable
private fun DayPaceCard(date: LocalDate, today: LocalDate, spent: Long, monthExpense: Long) {
    val ym = YearMonth.from(date)
    val monthName = ym.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
    val typicalDay = FinanceCalc.avgDailySpend(monthExpense, ym, today)

    GlassCard(Modifier.fillMaxWidth(), glow = CColors.Amber) {
        Text("This day", color = CColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(4.dp))
        val diff = spent - typicalDay
        val note: String
        val noteColor: Color
        if (typicalDay == 0L) {
            note = "No other spending in $monthName to compare with."
            noteColor = CColors.TextSecondary
        } else if (diff > 0) {
            note = "${Money.formatWhole(diff)} more than a typical day in $monthName."
            noteColor = CColors.Amber
        } else if (diff < 0) {
            note = "${Money.formatWhole(-diff)} less than a typical day in $monthName."
            noteColor = CColors.Lime
        } else {
            note = "Exactly a typical day in $monthName."
            noteColor = CColors.TextSecondary
        }
        PaceRow(
            title = if (date == today) "Spent today" else "Spent on this day",
            value = Money.formatWhole(spent),
            explain = if (spent == 0L) "No expenses recorded for this day." else "Total of all expenses on this date.",
            valueColor = CColors.Amber,
            note = note,
            noteColor = noteColor
        )
        Divider()
        PaceRow(
            title = "A typical day in $monthName",
            value = Money.formatWhole(typicalDay),
            explain = "${Money.formatWhole(monthExpense)} spent in the month ÷ ${FinanceCalc.daysElapsed(ym, today)} days."
        )
    }
}

/** MONTH view: today's spend, the daily average, and the month-end forecast, each explained. */
@Composable
private fun PaceCard(
    month: YearMonth,
    today: LocalDate,
    todaySpend: Long,
    avgDaily: Long,
    projected: Long,
    monthExpense: Long,
    daily: LongArray,
    overallBudget: com.montra.app.data.BudgetEntity?
) {
    val isCurrent = month == YearMonth.from(today)
    val daysCounted = FinanceCalc.daysElapsed(month, today)
    val dayWord = if (daysCounted == 1) "day" else "days"
    val monthShort = month.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
    val lastDay = month.lengthOfMonth()

    GlassCard(Modifier.fillMaxWidth(), glow = CColors.Amber) {
        Text("Spending pace", color = CColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(4.dp))

        if (isCurrent) {
            PaceRow(
                title = "Spent today",
                value = Money.formatWhole(todaySpend),
                explain = if (todaySpend == 0L) "No expenses recorded yet today." else "Everything you've spent since midnight."
            )
        } else {
            val peak = daily.withIndex().maxByOrNull { it.value }
            PaceRow(
                title = "Biggest spending day",
                value = Money.formatWhole(peak?.value ?: 0L),
                explain = if (peak == null || peak.value == 0L) "No expenses in this month." else "${peak.index + 1} $monthShort was your highest-spending day."
            )
        }

        Divider()

        PaceRow(
            title = "Daily average",
            value = Money.formatWhole(avgDaily),
            explain = "${Money.formatWhole(monthExpense)} spent ÷ $daysCounted $dayWord = what you spend on a typical day."
        )

        Divider()

        if (isCurrent) {
            val early = today.dayOfMonth <= 3
            val note: String?
            val noteColor: Color
            if (overallBudget != null) {
                val diff = projected - overallBudget.limitPaise
                if (diff > 0) {
                    note = "That's ${Money.formatWhole(diff)} over your ${Money.formatWhole(overallBudget.limitPaise)} monthly budget."
                    noteColor = CColors.Red
                } else {
                    note = "That's ${Money.formatWhole(-diff)} under your ${Money.formatWhole(overallBudget.limitPaise)} monthly budget."
                    noteColor = CColors.Lime
                }
            } else {
                note = if (early) "Early estimate — it gets more accurate as the month goes on." else null
                noteColor = CColors.TextSecondary
            }
            PaceRow(
                title = "Month-end forecast",
                value = Money.formatWhole(projected),
                explain = "If you keep spending about ${Money.formatWhole(avgDaily)} a day until $lastDay $monthShort, this is your total for the month.",
                valueColor = CColors.Amber,
                note = note,
                noteColor = noteColor
            )
        } else {
            PaceRow(
                title = "Month total",
                value = Money.formatWhole(monthExpense),
                explain = "Total spent in the whole month.",
                valueColor = CColors.Amber
            )
        }
    }
}

/** YEAR view: monthly average, the heaviest month and the year-end forecast. */
@Composable
private fun YearPaceCard(period: Period, today: LocalDate, txs: List<TransactionEntity>) {
    val year = period.anchor.year
    val months = remember(txs, year) { FinanceCalc.monthlyTotals(txs, year) }
    val totalExpense = months.sumOf { it.expense }
    val isCurrent = period.contains(today)
    val monthsCounted = if (isCurrent) today.monthValue else 12
    val avgMonthly = totalExpense / monthsCounted
    val peak = months.maxByOrNull { it.expense }
    val forecast = FinanceCalc.projectedForPeriod(totalExpense, period, today)

    GlassCard(Modifier.fillMaxWidth(), glow = CColors.Amber) {
        Text("Year at a glance · $year", color = CColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(4.dp))
        PaceRow(
            title = "Monthly average",
            value = Money.formatWhole(avgMonthly),
            explain = "${Money.formatWhole(totalExpense)} spent ÷ $monthsCounted month${if (monthsCounted == 1) "" else "s"}."
        )
        Divider()
        PaceRow(
            title = "Heaviest month",
            value = Money.formatWhole(peak?.expense ?: 0L),
            explain = if (peak == null || peak.expense == 0L) "No expenses recorded this year."
            else "${peak.month.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} was your highest-spending month."
        )
        Divider()
        PaceRow(
            title = if (isCurrent) "Year-end forecast" else "Year total",
            value = Money.formatWhole(forecast),
            explain = if (isCurrent) "If you keep spending at this year's pace, this is where $year will end." else "Total spent in $year.",
            valueColor = CColors.Amber
        )
    }
}
