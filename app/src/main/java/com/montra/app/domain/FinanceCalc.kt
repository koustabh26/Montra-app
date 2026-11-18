package com.montra.app.domain

import com.montra.app.data.BudgetEntity
import com.montra.app.data.GoalEntity
import com.montra.app.data.TransactionEntity
import com.montra.app.data.TxType
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

data class Summary(val income: Long, val expense: Long) {
    val net: Long get() = income - expense

    /** Share of income that was kept, -100%..100% (as -1..1). 0 when there is no income. */
    val savingsRate: Double
        get() = if (income > 0) (net.toDouble() / income).coerceIn(-1.0, 1.0) else 0.0
}

data class MonthPoint(val month: YearMonth, val income: Long, val expense: Long)

data class DayPoint(val date: LocalDate, val income: Long, val expense: Long)

data class BudgetStatus(val budget: BudgetEntity, val spent: Long) {
    val limit: Long get() = budget.limitPaise
    val remaining: Long get() = limit - spent
    val ratio: Float get() = if (limit > 0) spent.toFloat() / limit.toFloat() else 0f
    val isOver: Boolean get() = spent > limit
}

object FinanceCalc {
    fun dateOf(t: TransactionEntity): LocalDate = LocalDate.ofEpochDay(t.date)

    fun monthOf(t: TransactionEntity): YearMonth = YearMonth.from(dateOf(t))

    fun inMonth(list: List<TransactionEntity>, ym: YearMonth): List<TransactionEntity> =
        list.filter { monthOf(it) == ym }

    fun inRange(list: List<TransactionEntity>, from: YearMonth, to: YearMonth): List<TransactionEntity> =
        list.filter { val m = monthOf(it); !m.isBefore(from) && !m.isAfter(to) }

    fun inPeriod(list: List<TransactionEntity>, p: Period): List<TransactionEntity> {
        val s = p.start.toEpochDay()
        val e = p.end.toEpochDay()
        return list.filter { it.date in s..e }
    }

    /** All 12 months of [year], January first. */
    fun monthlyTotals(all: List<TransactionEntity>, year: Int): List<MonthPoint> =
        (1..12).map { m ->
            val ym = YearMonth.of(year, m)
            val s = summary(inMonth(all, ym))
            MonthPoint(ym, s.income, s.expense)
        }

    /** The last [n] days ending at [end], oldest first (days without activity are included as zeros). */
    fun dayTrend(all: List<TransactionEntity>, end: LocalDate, n: Int): List<DayPoint> {
        val byDay = all.groupBy { it.date }
        return (n - 1 downTo 0).map { back ->
            val d = end.minusDays(back.toLong())
            val items = byDay[d.toEpochDay()].orEmpty()
            DayPoint(d, total(items, TxType.INCOME), total(items, TxType.EXPENSE))
        }
    }

    /** Spending at the current pace for the whole [p]; the plain total once the period is over. */
    fun projectedForPeriod(expenseSoFar: Long, p: Period, today: LocalDate): Long {
        if (!p.contains(today)) return expenseSoFar
        return expenseSoFar * p.totalDays / p.elapsedDays(today)
    }

    fun total(list: List<TransactionEntity>, type: String): Long =
        list.filter { it.type == type }.sumOf { it.amountPaise }

    fun summary(list: List<TransactionEntity>): Summary =
        Summary(total(list, TxType.INCOME), total(list, TxType.EXPENSE))

    /** (categoryId, total) sorted by total, largest first. */
    fun categoryTotals(list: List<TransactionEntity>, type: String): List<Pair<Long, Long>> =
        list.filter { it.type == type }
            .groupBy { it.categoryId }
            .map { (id, items) -> id to items.sumOf { it.amountPaise } }
            .sortedByDescending { it.second }

    /** One entry per day of the month (index 0 = day 1). */
    fun dailyTotals(list: List<TransactionEntity>, ym: YearMonth, type: String): LongArray {
        val out = LongArray(ym.lengthOfMonth())
        list.forEach {
            if (it.type == type) {
                val d = dateOf(it)
                if (YearMonth.from(d) == ym) out[d.dayOfMonth - 1] += it.amountPaise
            }
        }
        return out
    }

    /** The last [n] months ending at [end], oldest first. */
    fun trend(all: List<TransactionEntity>, end: YearMonth, n: Int): List<MonthPoint> =
        (n - 1 downTo 0).map { back ->
            val m = end.minusMonths(back.toLong())
            val s = summary(inMonth(all, m))
            MonthPoint(m, s.income, s.expense)
        }

    /** Days that count towards "per day" figures for [ym], as of [today]. */
    fun daysElapsed(ym: YearMonth, today: LocalDate): Int {
        val current = YearMonth.from(today)
        return when {
            ym == current -> today.dayOfMonth
            ym.isBefore(current) -> ym.lengthOfMonth()
            else -> 1
        }
    }

    fun avgDailySpend(expense: Long, ym: YearMonth, today: LocalDate): Long =
        expense / daysElapsed(ym, today)

    /** Month-end spending at the current pace. Past/future months return the actual total. */
    fun projectedExpense(expenseSoFar: Long, ym: YearMonth, today: LocalDate): Long {
        if (ym != YearMonth.from(today)) return expenseSoFar
        return expenseSoFar * ym.lengthOfMonth() / today.dayOfMonth
    }

    fun budgetStatuses(budgets: List<BudgetEntity>, monthTx: List<TransactionEntity>): List<BudgetStatus> =
        budgets.map { b ->
            val spent = if (b.categoryId == 0L) {
                total(monthTx, TxType.EXPENSE)
            } else {
                monthTx.filter { it.type == TxType.EXPENSE && it.categoryId == b.categoryId }
                    .sumOf { it.amountPaise }
            }
            BudgetStatus(b, spent)
        }.sortedByDescending { it.ratio }

    /** Whole months left until [deadlineEpochDay], at least 1. */
    fun monthsLeft(today: LocalDate, deadlineEpochDay: Long): Int {
        val days = ChronoUnit.DAYS.between(today, LocalDate.ofEpochDay(deadlineEpochDay))
        if (days <= 0) return 1
        return ((days + 29) / 30).toInt().coerceAtLeast(1)
    }

    /** Amount to set aside each month to reach the goal on time; null without a deadline, 0 when reached. */
    fun goalMonthlyNeed(goal: GoalEntity, today: LocalDate): Long? {
        val deadline = goal.deadline ?: return null
        val remaining = goal.targetPaise - goal.savedPaise
        if (remaining <= 0) return 0
        val months = monthsLeft(today, deadline).toLong()
        return (remaining + months - 1) / months
    }

    fun goalProgress(goal: GoalEntity): Float =
        if (goal.targetPaise > 0) (goal.savedPaise.toFloat() / goal.targetPaise.toFloat()).coerceIn(0f, 1f) else 0f

    /** Percentage change from [previous] to [current]; null when there is no baseline. */
    fun percentChange(current: Long, previous: Long): Double? =
        if (previous <= 0) null else (current - previous).toDouble() / previous.toDouble() * 100.0
}
