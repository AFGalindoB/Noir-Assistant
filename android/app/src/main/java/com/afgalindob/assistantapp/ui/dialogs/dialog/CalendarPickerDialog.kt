package com.afgalindob.assistantapp.ui.dialogs.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth
import com.afgalindob.assistantapp.R
import java.time.Month
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TextButton
import androidx.compose.ui.res.stringResource
import com.afgalindob.assistantapp.ui.theme.AccentPrimary
import com.afgalindob.assistantapp.ui.theme.OnAccentPrimary
import com.afgalindob.assistantapp.ui.theme.OnSurfacePrimary
import com.afgalindob.assistantapp.ui.theme.OnSurfaceSecondary
import com.afgalindob.assistantapp.ui.theme.SurfaceContainerHigh
import com.afgalindob.assistantapp.utils.DateUtils
import java.time.format.TextStyle
import java.util.Locale

enum class CalendarViewMode {
    DAY,
    MONTH
}

@Composable
fun CalendarDialog(
    selectedDate: Long?,
    onDateSelected: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    val today = LocalDate.now()
    val tomorrow = today.plusDays(1)
    val nextWeek = today.plusDays(7)

    val initialLocalDate = remember(selectedDate) {
        selectedDate?.let { DateUtils.fromTimestamp(it) }
    }
    var viewMode by remember { mutableStateOf(CalendarViewMode.DAY) }
    Dialog(onDismissRequest = onDismiss) {

        var currentMonth by remember(initialLocalDate) {
            mutableStateOf(
                initialLocalDate?.let { YearMonth.from(it) } ?: YearMonth.now()
            )
        }
        var tempSelectedDate by remember(initialLocalDate) {
            mutableStateOf(initialLocalDate ?: today)
        }

        val quickOptions = listOf(
            R.string.today to today,
            R.string.tomorrow to tomorrow,
            R.string.next_week to nextWeek
        )

        val days = listOf(
            R.string.monday,
            R.string.tuesday,
            R.string.wednesday,
            R.string.thursday,
            R.string.friday,
            R.string.saturday,
            R.string.sunday
        )

        Card(
            colors = CardDefaults.cardColors(
                containerColor = SurfaceContainerHigh,
                contentColor = OnSurfacePrimary
            )
        ) {

            Column (
                modifier = Modifier
                    .padding(8.dp)
            ){

                // -- HEADER --
                Header(
                    currentMonth = currentMonth,
                    viewMode = viewMode,

                    onPrev = {
                        currentMonth =
                            if (viewMode == CalendarViewMode.DAY)
                                currentMonth.minusMonths(1)
                            else
                                currentMonth.minusYears(1)

                    },

                    onNext = {
                        currentMonth =
                            if (viewMode == CalendarViewMode.DAY)
                                 currentMonth.plusMonths(1)
                            else
                                 currentMonth.plusYears(1)

                    },

                    onLabelClick = {
                        viewMode =
                            if (viewMode == CalendarViewMode.DAY)
                                CalendarViewMode.MONTH
                            else
                                CalendarViewMode.DAY
                    },

                    contentColor = OnSurfacePrimary

                )

                // -- CONTENT --
                when (viewMode) {

                    CalendarViewMode.DAY -> Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {

                            days.forEachIndexed { index, day ->

                                Text(
                                    text = stringResource(day),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (index == days.lastIndex) AccentPrimary
                                    else OnSurfaceSecondary
                                )
                            }
                        }

                        DayView(
                            month = currentMonth,
                            selectedDate = tempSelectedDate,
                            onDateSelected = { date ->

                                if (tempSelectedDate == date) {
                                    onDateSelected(DateUtils.toTimestamp(date))
                                    onDismiss()
                                } else {
                                    tempSelectedDate = date
                                }
                            }
                        )
                    }

                    CalendarViewMode.MONTH -> MonthView(
                        onMonthSelected = {
                            currentMonth = YearMonth.of(currentMonth.year, it)
                            viewMode = CalendarViewMode.DAY
                        },
                        contentColor = OnSurfacePrimary
                    )
                }

                quickOptions.forEach { (labelRes, date) ->
                    TextButton(
                        onClick = {
                            onDateSelected(DateUtils.toTimestamp(date))
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 4.dp, horizontal = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(labelRes),
                                color = OnSurfacePrimary,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = OnSurfaceSecondary.copy(alpha = 0.2f))

                // -- FOOTER --
                TextButton(
                    onClick = {
                        onDateSelected(null)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.close),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = AccentPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.no_date),
                            color = AccentPrimary,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Header(
    currentMonth: YearMonth,
    viewMode: CalendarViewMode,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onLabelClick: () -> Unit,
    contentColor: Color
) {

    Row(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        IconButton(onClick = onPrev) {
            Icon(
                painter = painterResource(R.drawable.previous),
                contentDescription = "Previous",
                tint = contentColor
            )
        }

        Text(
            text =
                if (viewMode == CalendarViewMode.DAY) {

                    val monthName = currentMonth.month.getDisplayName(
                        TextStyle.FULL,
                        Locale.getDefault()
                    )

                    monthName.replaceFirstChar { it.uppercase() } +
                            " " + currentMonth.year

                } else {
                    currentMonth.year.toString()
                },
            modifier = Modifier
                .clickable { onLabelClick() }
                .padding(horizontal = 6.dp),
            style = MaterialTheme.typography.headlineMedium
        )

        IconButton(onClick = onNext) {
            Icon(
                painter = painterResource(R.drawable.forward),
                contentDescription = "Next",
                tint = contentColor
            )
        }
    }
}

@Composable
fun DayView(
    month: YearMonth,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit
) {

    val daysInMonth = month.lengthOfMonth()
    val firstDay = month.atDay(1).dayOfWeek.value

    LazyVerticalGrid(columns = GridCells.Fixed(7)) {

        items(firstDay - 1) {
            Box {}
        }

        items(daysInMonth) { index ->

            val day = index + 1
            val date = month.atDay(day)

            val selected = date == selectedDate

            Box(
                modifier = Modifier
                    .padding(5.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) AccentPrimary else Color.Transparent
                    )
                    .clickable { onDateSelected(date) }
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = day.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (selected) OnAccentPrimary else OnSurfacePrimary
                )
            }
        }
    }
}

@Composable
fun MonthView(
    onMonthSelected: (Int) -> Unit,
    contentColor: Color
) {

    val months = Month.entries.toTypedArray()

    LazyVerticalGrid(columns = GridCells.Fixed(3)) {

        items(months.size) { index ->

            val month = months[index]

            Box(
                modifier = Modifier
                    .padding(3.dp)
                    .clickable {
                        onMonthSelected(month.value)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = month.getDisplayName(
                        TextStyle.FULL,
                        Locale.getDefault()
                    ).replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor
                )
            }
        }
    }
}