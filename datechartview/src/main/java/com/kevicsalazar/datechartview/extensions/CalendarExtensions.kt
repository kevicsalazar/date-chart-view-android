package com.kevicsalazar.datechartview.extensions

import java.util.*

/**
 * @author Kevin Salazar
 * @link https://github.com/kevicsalazar
 */

internal fun today() = Calendar.getInstance().truncate()

internal val Calendar.year get() = get(Calendar.YEAR)

internal val Calendar.month get() = get(Calendar.MONTH)

internal val Calendar.dayOfMonth get() = get(Calendar.DAY_OF_MONTH)

internal fun Calendar.changeDate(year: Int, month: Int, dayOfMonth: Int) = apply {
    set(Calendar.YEAR, year)
    set(Calendar.MONTH, month)
    set(Calendar.DAY_OF_MONTH, dayOfMonth)
}

internal fun Calendar.changeDay(dayOfMonth: Int) = apply { set(Calendar.DAY_OF_MONTH, dayOfMonth) }

internal fun Calendar.dateByAddingDays(days: Int) = apply { add(Calendar.DAY_OF_YEAR, days) }

internal fun Calendar.dateBySubtractingDays(days: Int) = apply { add(Calendar.DAY_OF_YEAR, -days) }

internal fun Calendar.isSameDayAsDate(otherCalendar: Calendar): Boolean {
    return year == otherCalendar.year &&
            month == otherCalendar.month &&
            dayOfMonth == otherCalendar.dayOfMonth
}

internal fun Calendar.isLaterThanDate(date: Calendar): Boolean = compareTo(date) == 1

internal fun Calendar.truncate() = apply {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}