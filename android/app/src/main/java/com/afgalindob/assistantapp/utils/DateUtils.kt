package com.afgalindob.assistantapp.utils

import java.time.Instant
import java.time.ZoneId
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateUtils {

    fun toTimestamp(date: LocalDate): Long =
        date.atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

    fun now(): Long = System.currentTimeMillis()

    fun today(): Long = LocalDate.now()
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

    fun fromTimestamp(timestamp: Long): LocalDate =
        Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()

    fun formatReadable(date: LocalDate): String =
        date.format(
            DateTimeFormatter.ofPattern(
                "EEE - dd MMMM yyyy",
                Locale.getDefault()
            )
        ).capitalizeWords()

    fun getExpirationTimestamp(days: Long): Long {
        return LocalDate.now()
            .plusDays(days)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }
}