package com.montra.app

import com.montra.app.data.BudgetEntity
import com.montra.app.data.GoalEntity
import com.montra.app.data.TransactionEntity
import com.montra.app.data.TxType
import com.montra.app.domain.FinanceCalc
import com.montra.app.ui.components.sanitizeAmount
import com.montra.app.util.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class FinanceCalcTest {
    private fun tx(type: String, rupees: Long, cat: Long, day: LocalDate) =
        TransactionEntity(amountPaise = rupees * 100, type = type, categoryId = cat, date = day.toEpochDay())

    private val nov = YearMonth.of(2026, 11)
    private val list = listOf(
        tx(TxType.INCOME, 50_000, 10, LocalDate.of(2026, 11, 1)),
        tx(TxType.EXPENSE, 12_000, 1, LocalDate.of(2026, 11, 2)),
        tx(TxType.EXPENSE, 3_000, 2, LocalDate.of(2026, 11, 10)),
        tx(TxType.EXPENSE, 4_000, 1, LocalDate.of(2026, 10, 20))
    )

    @Test
    fun summaryAndSavingsRate() {
        val s = FinanceCalc.summary(FinanceCalc.inMonth(list, nov))
        assertEquals(50_000_00L, s.income)
        assertEquals(15_000_00L, s.expense)
        assertEquals(35_000_00L, s.net)
        assertEquals(0.7, s.savingsRate, 1e-9)
    }

    @Test
    fun categoryTotalsSortedDescending() {
        val totals = FinanceCalc.categoryTotals(FinanceCalc.inMonth(list, nov), TxType.EXPENSE)
        assertEquals(listOf(1L to 12_000_00L, 2L to 3_000_00L), totals)
    }

    @Test
    fun dailyTotalsHaveOneEntryPerDay() {
        val d = FinanceCalc.dailyTotals(list, nov, TxType.EXPENSE)
        assertEquals(30, d.size)
        assertEquals(12_000_00L, d[1])
        assertEquals(3_000_00L, d[9])
    }

    @Test
    fun projectionScalesToMonthLength() {
        val today = LocalDate.of(2026, 11, 10)
        assertEquals(15_000_00L * 30 / 10, FinanceCalc.projectedExpense(15_000_00L, nov, today))
        assertEquals(15_000_00L, FinanceCalc.projectedExpense(15_000_00L, YearMonth.of(2026, 10), today))
    }

    @Test
    fun budgetStatusFlagsOverspend() {
        val statuses = FinanceCalc.budgetStatuses(
            listOf(BudgetEntity(categoryId = 1, limitPaise = 10_000_00), BudgetEntity(categoryId = 0, limitPaise = 40_000_00)),
            FinanceCalc.inMonth(list, nov)
        )
        val food = statuses.first { it.budget.categoryId == 1L }
        assertEquals(12_000_00L, food.spent)
        assertEquals(true, food.isOver)
        assertEquals(-2_000_00L, food.remaining)
        assertEquals(15_000_00L, statuses.first { it.budget.categoryId == 0L }.spent)
    }

    @Test
    fun goalMonthlyNeedRoundsUp() {
        val today = LocalDate.of(2026, 11, 15)
        val goal = GoalEntity(
            name = "Bike", emoji = "🏍️", color = 0xFF00E5FF, targetPaise = 100_000_00,
            savedPaise = 10_000_00, deadline = today.plusDays(90).toEpochDay()
        )
        assertEquals(30_000_00L, FinanceCalc.goalMonthlyNeed(goal, today))
        assertNull(FinanceCalc.goalMonthlyNeed(goal.copy(deadline = null), today))
        assertEquals(0L, FinanceCalc.goalMonthlyNeed(goal.copy(savedPaise = goal.targetPaise), today))
    }

    @Test
    fun percentChangeNeedsBaseline() {
        assertNull(FinanceCalc.percentChange(100, 0))
        assertEquals(50.0, FinanceCalc.percentChange(150, 100)!!, 1e-9)
    }

    @Test
    fun amountInputIsSanitised() {
        assertEquals("1250.50", sanitizeAmount("1250.50"))
        assertEquals("12.5", sanitizeAmount("12,5"))        // comma keyboards
        assertEquals("1.23", sanitizeAmount("1.234"))       // max 2 decimals
        assertEquals("1.57", sanitizeAmount("1.5.7"))       // second "." is dropped
        assertEquals("125", sanitizeAmount("₹ 1 2-5"))      // junk removed
        assertEquals("1234567890", sanitizeAmount("12345678901")) // 10 integer digits max
        assertEquals("", sanitizeAmount("abc"))
    }

    @Test
    fun compactMoneyHasNoStrayDecimals() {
        assertEquals("₹950", Money.compact(95_000L))
        assertEquals("₹12.5K", Money.compact(12_50_000L))
        assertEquals("₹10K", Money.compact(10_00_000L))
        assertEquals("₹1.2L", Money.compact(1_20_00_000L))
        assertEquals("₹3.4Cr", Money.compact(3_40_00_00_000L))
        assertEquals("-₹5K", Money.compact(-5_00_000L))
    }

    @Test
    fun moneyParsing() {
        assertEquals(125050L, Money.parsePaise("1,250.50"))
        assertEquals(100L, Money.parsePaise("1"))
        assertNull(Money.parsePaise(""))
        assertNull(Money.parsePaise("0"))
        assertNull(Money.parsePaise("-5"))
        assertNull(Money.parsePaise("1.234"))
        assertNull(Money.parsePaise("abc"))
        assertEquals("1250.50", Money.toInput(125050L))
        assertEquals("1250", Money.toInput(125000L))
    }
}
