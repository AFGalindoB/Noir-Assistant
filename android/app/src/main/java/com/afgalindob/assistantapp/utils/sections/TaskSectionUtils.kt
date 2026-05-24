package com.afgalindob.assistantapp.utils.sections

import com.afgalindob.assistantapp.R
import com.afgalindob.assistantapp.data.domain.TaskDomain
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

object TaskSections {
    val OVERDUE = R.string.overdue
    val TODAY = R.string.today
    val TOMORROW = R.string.tomorrow
    val THIS_WEEK = R.string.this_week
    val NEXT_WEEK = R.string.next_week
    val THIS_MONTH = R.string.this_month
    val NO_DATE = R.string.no_date
    val MORE = R.string.more
}

fun TaskDomain.getTaskSection(): Int {
    val today = LocalDate.now()
    val taskDate = date?.let {
        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
    }

    return when {
        taskDate == null -> TaskSections.NO_DATE
        taskDate.isBefore(today) -> TaskSections.OVERDUE
        taskDate.isEqual(today) -> TaskSections.TODAY
        taskDate.isEqual(today.plusDays(1)) -> TaskSections.TOMORROW
        taskDate.isAfter(today) && taskDate.isBefore(today.with(TemporalAdjusters.next(DayOfWeek.MONDAY))) -> TaskSections.THIS_WEEK
        taskDate.isAfter(today.with(TemporalAdjusters.next(DayOfWeek.SUNDAY))) &&
                taskDate.isBefore(today.with(TemporalAdjusters.next(DayOfWeek.MONDAY)).plusWeeks(1)) -> TaskSections.NEXT_WEEK
        taskDate.isAfter(today) && taskDate.month == today.month -> TaskSections.THIS_MONTH
        else -> TaskSections.MORE
    }
}

fun taskSectionOrder(section: Int): Int {
    return when(section) {
        TaskSections.OVERDUE -> 0
        TaskSections.TODAY -> 1
        TaskSections.TOMORROW -> 2
        TaskSections.THIS_WEEK -> 3
        TaskSections.NEXT_WEEK -> 4
        TaskSections.THIS_MONTH -> 5
        TaskSections.NO_DATE -> 7
        else -> 6
    }
}