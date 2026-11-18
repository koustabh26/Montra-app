package com.montra.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.montra.app.AppViewModel
import com.montra.app.data.TxType
import com.montra.app.domain.FinanceCalc
import com.montra.app.domain.PeriodMode
import com.montra.app.ui.components.DonutChart
import com.montra.app.ui.components.EmptyState
import com.montra.app.ui.components.GlassCard
import com.montra.app.ui.components.LegendRow
import com.montra.app.ui.components.LineChart
import com.montra.app.ui.components.Muted
import com.montra.app.ui.components.NeonProgress
import com.montra.app.ui.components.PeriodSwitcher
import com.montra.app.ui.components.SectionTitle
import com.montra.app.ui.components.Slice
import com.montra.app.ui.components.TrendBars
import com.montra.app.ui.theme.CColors
import com.montra.app.util.Money
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private val rowDateFormat = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH)

private class Bars(val title: String, val labels: List<String>, val income: List<Long>, val expense: List<Long>)

@Composable
fun AnalyticsScreen(vm: AppViewModel, modifier: Modifier = Modifier) {
    val txs by vm.transactions.collectAsStateWithLifecycle()
    val cats by vm.categories.collectAsStateWithLifecycle()
    val period by vm.period.collectAsStateWithLifecycle()
    val today = LocalDate.now()

    val catMap = remember(cats) { cats.associateBy { it.id } }
    val periodTx = remember(txs, period) { FinanceCalc.inPeriod(txs, period) }
    val summary = remember(periodTx) { FinanceCalc.summary(periodTx) }
    val prevSummary = remember(txs, period) { FinanceCalc.summary(FinanceCalc.inPeriod(txs, period.shift(-1))) }
    val expenseByCat = remember(periodTx) { FinanceCalc.categoryTotals(periodTx, TxType.EXPENSE) }
    val incomeByCat = remember(periodTx) { FinanceCalc.categoryTotals(periodTx, TxType.INCOME) }
    val biggest = remember(periodTx) { periodTx.filter { it.type == TxType.EXPENSE }.maxByOrNull { it.amountPaise } }
    val busiestWeekday = remember(periodTx) {
        periodTx.filter { it.type == TxType.EXPENSE }
            .groupBy { FinanceCalc.dateOf(it).dayOfWeek }
            .mapValues { (_, v) -> v.sumOf { it.amountPaise } }
            .maxByOrNull { it.value }
    }

    // Bars: last 7 days / last 6 months / the 12 months of the year.
    val bars = remember(txs, period) {
        when (period.mode) {
            PeriodMode.DAY -> {
                val pts = FinanceCalc.dayTrend(txs, period.anchor, 7)
                Bars(
                    "Last 7 days",
                    pts.map { it.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH) },
                    pts.map { it.income }, pts.map { it.expense }
                )
            }
            PeriodMode.MONTH -> {
                val pts = FinanceCalc.trend(txs, period.yearMonth, 6)
                Bars(
                    "Last 6 months",
                    pts.map { it.month.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH) },
                    pts.map { it.income }, pts.map { it.expense }
                )
            }
            PeriodMode.YEAR -> {
                val pts = FinanceCalc.monthlyTotals(txs, period.anchor.year)
                Bars(
                    "Month by month · ${period.anchor.year}",
                    pts.map { it.month.month.getDisplayName(TextStyle.NARROW, Locale.ENGLISH) },
                    pts.map { it.income }, pts.map { it.expense }
                )
            }
        }
    }
    val monthPoints = remember(txs, period) {
        if (period.mode == PeriodMode.YEAR) FinanceCalc.monthlyTotals(txs, period.anchor.year) else emptyList()
    }
    val dailyExpense = remember(txs, period) {
        if (period.mode == PeriodMode.MONTH) FinanceCalc.dailyTotals(txs, period.yearMonth, TxType.EXPENSE) else LongArray(0)
    }

    val palette = listOf(
        CColors.Cyan, CColors.Violet, CColors.Magenta, CColors.Lime, CColors.Amber,
        Color(0xFF4D9BFF), Color(0xFFFF6B4A), Color(0xFF2DD4BF)
    )
    val slices = remember(expenseByCat, catMap) {
        val top = expenseByCat.take(7).mapIndexed { i, (id, v) ->
            Slice(catMap[id]?.name ?: "Unknown", v, catMap[id]?.let { Color(it.color) } ?: palette[i % palette.size])
        }
        val rest = expenseByCat.drop(7).sumOf { it.second }
        if (rest > 0) top + Slice("Other", rest, Color(0xFF6B7280)) else top
    }

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

        item {
            GlassCard(Modifier.fillMaxWidth(), glow = CColors.Cyan) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Muted("Income")
                        Text(Money.formatWhole(summary.income), color = CColors.Lime, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Muted("Expenses")
                        Text(Money.formatWhole(summary.expense), color = CColors.Magenta, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Muted("Saved")
                        Text(
                            (if (summary.net < 0) "−" else "") + Money.formatWhole(abs(summary.net)),
                            color = if (summary.net < 0) CColors.Red else CColors.Cyan,
                            fontWeight = FontWeight.Bold, fontSize = 17.sp
                        )
                    }
                }
            }
        }

        item { SectionTitle("Spending by category") }
        item {
            GlassCard(Modifier.fillMaxWidth(), glow = CColors.Magenta) {
                if (slices.isEmpty()) {
                    EmptyState("📊", "No expenses in this ${period.mode.label.lowercase()}", "Add transactions to see your breakdown.")
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        DonutChart(slices, "Total spent", Money.compact(summary.expense))
                    }
                    Spacer(Modifier.height(10.dp))
                    val total = summary.expense.coerceAtLeast(1L)
                    slices.forEach { s ->
                        LegendRow(s.color, s.label, Money.formatWhole(s.value), "${(s.value * 100.0 / total).roundToInt()}%")
                    }
                }
            }
        }

        if (incomeByCat.isNotEmpty()) {
            item { SectionTitle("Income sources") }
            item {
                GlassCard(Modifier.fillMaxWidth(), glow = CColors.Lime) {
                    val total = summary.income.coerceAtLeast(1L)
                    incomeByCat.forEach { (id, v) ->
                        val c = catMap[id]
                        LegendRow(Color(c?.color ?: 0xFF39FFB0), c?.name ?: "Unknown", Money.formatWhole(v), "${(v * 100.0 / total).roundToInt()}%")
                    }
                }
            }
        }

        item { SectionTitle("Income vs expenses · ${bars.title}") }
        item {
            GlassCard(Modifier.fillMaxWidth(), glow = CColors.Violet) {
                TrendBars(labels = bars.labels, income = bars.income, expense = bars.expense)
                Spacer(Modifier.height(8.dp))
                Row {
                    Text("■ Income", color = CColors.Lime, fontSize = 12.sp)
                    Spacer(Modifier.width(14.dp))
                    Text("■ Expenses", color = CColors.Magenta, fontSize = 12.sp)
                }
            }
        }

        // MONTH: every day of the month, date-wise
        if (period.mode == PeriodMode.MONTH) {
            item { SectionTitle("Day by day") }
            item {
                GlassCard(Modifier.fillMaxWidth(), glow = CColors.Cyan) {
                    if (dailyExpense.all { it == 0L }) {
                        EmptyState("📈", "No spending this month", "Your daily breakdown will appear here.")
                    } else {
                        LineChart(dailyExpense.toList(), CColors.Cyan)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Muted("1")
                            Muted("${dailyExpense.size}")
                        }
                        Spacer(Modifier.height(10.dp))
                        val max = dailyExpense.max().coerceAtLeast(1L)
                        for (i in dailyExpense.indices.reversed()) {
                            val v = dailyExpense[i]
                            if (v > 0) {
                                BarRow(
                                    label = period.yearMonth.atDay(i + 1).format(rowDateFormat),
                                    fraction = v.toFloat() / max,
                                    amount = Money.formatWhole(v),
                                    color = if (period.yearMonth.atDay(i + 1) == today) CColors.Cyan else CColors.Magenta
                                )
                            }
                        }
                    }
                }
            }
        }

        // YEAR: each month with its spending and savings
        if (period.mode == PeriodMode.YEAR) {
            item { SectionTitle("Month by month") }
            item {
                GlassCard(Modifier.fillMaxWidth(), glow = CColors.Cyan) {
                    val max = monthPoints.maxOfOrNull { it.expense }?.coerceAtLeast(1L) ?: 1L
                    if (monthPoints.all { it.expense == 0L && it.income == 0L }) {
                        EmptyState("🗓️", "Nothing recorded in ${period.anchor.year}", "Add transactions to see each month here.")
                    } else {
                        monthPoints.forEach { m ->
                            if (m.expense > 0L || m.income > 0L) {
                                BarRow(
                                    label = m.month.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH),
                                    fraction = m.expense.toFloat() / max,
                                    amount = Money.formatWhole(m.expense),
                                    color = CColors.Magenta,
                                    sub = "Earned ${Money.formatWhole(m.income)}  ·  Saved ${(if (m.income - m.expense < 0) "−" else "") + Money.formatWhole(abs(m.income - m.expense))}"
                                )
                            }
                        }
                    }
                }
            }
        }

        // DAY: the day's own transactions
        if (period.mode == PeriodMode.DAY) {
            item { SectionTitle("Transactions on this day") }
            item {
                GlassCard(Modifier.fillMaxWidth(), glow = CColors.Cyan) {
                    if (periodTx.isEmpty()) {
                        EmptyState("🗒️", "Nothing recorded", "Nothing was added for this date.")
                    } else {
                        periodTx.forEach { t -> TransactionRow(t, catMap[t.categoryId]) { } }
                    }
                }
            }
        }

        item { SectionTitle("Insights") }
        item {
            GlassCard(Modifier.fillMaxWidth(), glow = CColors.Amber) {
                val insights = buildList {
                    val change = FinanceCalc.percentChange(summary.expense, prevSummary.expense)
                    if (change != null) {
                        val dir = if (change >= 0) "up" else "down"
                        add("Spending is $dir ${abs(change).roundToInt()}% compared with ${period.previousName}.")
                    }
                    if (summary.income > 0) {
                        add("You kept ${(summary.savingsRate * 100).roundToInt()}% of your income in this ${period.mode.label.lowercase()}.")
                    }
                    expenseByCat.firstOrNull()?.let { (id, v) ->
                        add("Top category: ${catMap[id]?.name ?: "Unknown"} (${Money.formatWhole(v)}).")
                    }
                    if (biggest != null) {
                        add("Biggest expense: ${Money.formatWhole(biggest.amountPaise)} on ${catMap[biggest.categoryId]?.name ?: "Unknown"}, ${dayLabel(FinanceCalc.dateOf(biggest), today)}.")
                    }
                    if (period.mode != PeriodMode.DAY) {
                        busiestWeekday?.let { add("You spend most on ${it.key.getDisplayName(TextStyle.FULL, Locale.ENGLISH)}s.") }
                        add("Average spend per day: ${Money.formatWhole(summary.expense / period.elapsedDays(today))}.")
                    }
                    if (period.mode == PeriodMode.YEAR) {
                        val monthsCounted = if (period.contains(today)) today.monthValue else 12
                        add("Average spend per month: ${Money.formatWhole(summary.expense / monthsCounted)}.")
                    }
                }
                if (periodTx.isEmpty()) {
                    EmptyState("💡", "No insights yet", "Add transactions for this ${period.mode.label.lowercase()} to unlock insights.")
                } else {
                    insights.forEach {
                        Text("•  $it", color = CColors.TextPrimary, fontSize = 14.sp, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
                    }
                }
            }
        }
    }
}

/** A label, a proportional bar and an amount; used for the day-by-day and month-by-month lists. */
@Composable
private fun BarRow(label: String, fraction: Float, amount: String, color: Color, sub: String? = null) {
    Column(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = CColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(112.dp))
            Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                NeonProgress(fraction, color, height = 8)
            }
            Text(amount, color = CColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        if (sub != null) Muted(sub, size = 11)
    }
}
