package com.montra.app.domain

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class PeriodMode(val label: String) {
    DAY("Day"), MONTH("Month"), YEAR("Year")
}

/** A day, a month or a year, identified by any date inside it ([anchor]). */
data class Period(val mode: PeriodMode, val anchor: LocalDate) {

    val start: LocalDate
        get() = when (mode) {
            PeriodMode.DAY -> anchor
            PeriodMode.MONTH -> anchor.withDayOfMonth(1)
            PeriodMode.YEAR -> anchor.withDayOfYear(1)
        }

    val end: LocalDate
        get() = when (mode) {
            PeriodMode.DAY -> anchor
            PeriodMode.MONTH -> anchor.withDayOfMonth(anchor.lengthOfMonth())
            PeriodMode.YEAR -> anchor.withDayOfYear(anchor.lengthOfYear())
        }

    val yearMonth: YearMonth get() = YearMonth.from(anchor)

    val totalDays: Int get() = (end.toEpochDay() - start.toEpochDay() + 1).toInt()

    fun contains(date: LocalDate): Boolean = !date.isBefore(start) && !date.isAfter(end)

    fun shift(delta: Int): Period = copy(
        anchor = when (mode) {
            PeriodMode.DAY -> anchor.plusDays(delta.toLong())
            PeriodMode.MONTH -> anchor.withDayOfMonth(1).plusMonths(delta.toLong())
            PeriodMode.YEAR -> anchor.withDayOfYear(1).plusYears(delta.toLong())
        }
    )

    /** Days of this period that have happened so far (whole period if it is in the past), at least 1. */
    fun elapsedDays(today: LocalDate): Int {
        val last = if (today.isBefore(end)) today else end
        if (last.isBefore(start)) return 1
        return (last.toEpochDay() - start.toEpochDay() + 1).toInt()
    }

    fun label(today: LocalDate): String = when (mode) {
        PeriodMode.DAY -> when (anchor) {
            today -> "Today · " + anchor.format(SHORT_DATE)
            today.minusDays(1) -> "Yesterday · " + anchor.format(SHORT_DATE)
            else -> anchor.format(LONG_DATE)
        }
        PeriodMode.MONTH -> anchor.format(MONTH_YEAR)
        PeriodMode.YEAR -> anchor.year.toString()
    }

    /** "yesterday", "last month", "last year" — for comparisons with the previous period. */
    val previousName: String
        get() = when (mode) {
            PeriodMode.DAY -> "the previous day"
            PeriodMode.MONTH -> "last month"
            PeriodMode.YEAR -> "last year"
        }

    companion object {
        private val SHORT_DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
        private val LONG_DATE = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH)
        private val MONTH_YEAR = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
    }
}
