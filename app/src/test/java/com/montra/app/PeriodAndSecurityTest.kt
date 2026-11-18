package com.montra.app

import com.montra.app.data.TransactionEntity
import com.montra.app.data.TxType
import com.montra.app.domain.FinanceCalc
import com.montra.app.domain.Period
import com.montra.app.domain.PeriodMode
import com.montra.app.security.LockoutPolicy
import com.montra.app.security.PinPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PeriodAndSecurityTest {
    private fun tx(type: String, rupees: Long, day: LocalDate) =
        TransactionEntity(amountPaise = rupees * 100, type = type, categoryId = 1, date = day.toEpochDay())

    private val list = listOf(
        tx(TxType.INCOME, 50_000, LocalDate.of(2026, 11, 1)),
        tx(TxType.EXPENSE, 12_000, LocalDate.of(2026, 11, 2)),
        tx(TxType.EXPENSE, 3_000, LocalDate.of(2026, 11, 10)),
        tx(TxType.EXPENSE, 4_000, LocalDate.of(2026, 10, 20)),
        tx(TxType.EXPENSE, 9_000, LocalDate.of(2025, 12, 31))
    )
    private val nov15 = LocalDate.of(2026, 11, 15)

    // ---- Period ----

    @Test
    fun periodBounds() {
        val month = Period(PeriodMode.MONTH, nov15)
        assertEquals(LocalDate.of(2026, 11, 1), month.start)
        assertEquals(LocalDate.of(2026, 11, 30), month.end)
        assertEquals(30, month.totalDays)

        val year = Period(PeriodMode.YEAR, nov15)
        assertEquals(LocalDate.of(2026, 1, 1), year.start)
        assertEquals(LocalDate.of(2026, 12, 31), year.end)
        assertEquals(365, year.totalDays)

        val day = Period(PeriodMode.DAY, nov15)
        assertEquals(1, day.totalDays)
        assertEquals(nov15, day.start)
    }

    @Test
    fun shiftingDoesNotDrift() {
        assertEquals(LocalDate.of(2026, 2, 1), Period(PeriodMode.MONTH, LocalDate.of(2026, 1, 31)).shift(1).anchor)
        assertEquals(LocalDate.of(2025, 1, 1), Period(PeriodMode.YEAR, nov15).shift(-1).anchor)
        assertEquals(LocalDate.of(2026, 11, 16), Period(PeriodMode.DAY, nov15).shift(1).anchor)
    }

    @Test
    fun elapsedDaysCoversPastCurrentAndFuture() {
        val month = Period(PeriodMode.MONTH, nov15)
        assertEquals(15, month.elapsedDays(nov15))
        assertEquals(30, month.elapsedDays(LocalDate.of(2026, 12, 2)))
        assertEquals(1, month.elapsedDays(LocalDate.of(2026, 10, 10)))
    }

    @Test
    fun labelsReadNaturally() {
        assertEquals("Today · 15 Nov 2026", Period(PeriodMode.DAY, nov15).label(nov15))
        assertEquals("Yesterday · 14 Nov 2026", Period(PeriodMode.DAY, nov15.minusDays(1)).label(nov15))
        assertEquals("November 2026", Period(PeriodMode.MONTH, nov15).label(nov15))
        assertEquals("2026", Period(PeriodMode.YEAR, nov15).label(nov15))
    }

    @Test
    fun periodFilteringAndTotals() {
        val day = FinanceCalc.inPeriod(list, Period(PeriodMode.DAY, LocalDate.of(2026, 11, 2)))
        assertEquals(1, day.size)
        assertEquals(12_000_00L, FinanceCalc.summary(day).expense)

        assertEquals(4, FinanceCalc.inPeriod(list, Period(PeriodMode.YEAR, nov15)).size)   // 2025 entry excluded
        assertEquals(3, FinanceCalc.inPeriod(list, Period(PeriodMode.MONTH, nov15)).size)
    }

    @Test
    fun monthlyTotalsListAllTwelveMonths() {
        val months = FinanceCalc.monthlyTotals(list, 2026)
        assertEquals(12, months.size)
        assertEquals(15_000_00L, months[10].expense)   // November
        assertEquals(4_000_00L, months[9].expense)     // October
        assertEquals(0L, months[0].expense)
    }

    @Test
    fun dayTrendIncludesEmptyDays() {
        val pts = FinanceCalc.dayTrend(list, LocalDate.of(2026, 11, 3), 3)
        assertEquals(listOf(0L, 12_000_00L, 0L), pts.map { it.expense })
        assertEquals(50_000_00L, pts[0].income)
    }

    @Test
    fun yearProjectionScalesToYearLength() {
        val year = Period(PeriodMode.YEAR, nov15)
        val today = LocalDate.of(2026, 7, 2)              // 183rd day of 2026
        assertEquals(183, year.elapsedDays(today))
        assertEquals(18_300_00L * 365 / 183, FinanceCalc.projectedForPeriod(18_300_00L, year, today))
        assertEquals(18_300_00L, FinanceCalc.projectedForPeriod(18_300_00L, year, LocalDate.of(2027, 1, 5)))
    }

    // ---- PIN rules ----

    @Test
    fun pinMustBeExactlySixDigits() {
        assertNotNull(PinPolicy.validate("1234"))
        assertNotNull(PinPolicy.validate("12345"))
        assertNotNull(PinPolicy.validate("1234567"))
        assertNotNull(PinPolicy.validate("12a456"))
        assertNotNull(PinPolicy.validate(""))
    }

    @Test
    fun weakPinsAreRejected() {
        listOf("111111", "000000", "123456", "654321", "234567", "121212", "123123", "112233", "159753")
            .forEach { assertNotNull("$it should be rejected", PinPolicy.validate(it)) }
    }

    @Test
    fun reasonablePinsAreAccepted() {
        listOf("482917", "135791", "908273").forEach { assertNull("$it should be accepted", PinPolicy.validate(it)) }
    }

    // ---- Lockout ----

    @Test
    fun lockoutEscalatesAndCaps() {
        assertEquals(0L, LockoutPolicy.lockoutMillis(4))
        assertEquals(30_000L, LockoutPolicy.lockoutMillis(5))
        assertEquals(60_000L, LockoutPolicy.lockoutMillis(6))
        assertEquals(300_000L, LockoutPolicy.lockoutMillis(7))
        assertEquals(900_000L, LockoutPolicy.lockoutMillis(8))
        assertEquals(3_600_000L, LockoutPolicy.lockoutMillis(9))
        assertEquals(3_600_000L, LockoutPolicy.lockoutMillis(40))
    }

    @Test
    fun attemptsLeftCountsDown() {
        assertEquals(5, LockoutPolicy.attemptsLeft(0))
        assertEquals(2, LockoutPolicy.attemptsLeft(3))
        assertEquals(0, LockoutPolicy.attemptsLeft(9))
        assertTrue(LockoutPolicy.attemptsLeft(1) > LockoutPolicy.attemptsLeft(2))
        assertFalse(LockoutPolicy.attemptsLeft(5) > 0)
    }
}
