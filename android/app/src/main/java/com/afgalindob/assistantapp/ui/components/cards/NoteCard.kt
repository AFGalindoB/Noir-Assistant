package com.afgalindob.assistantapp.ui.components.cards

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.afgalindob.assistantapp.data.domain.NoteDomain
import com.afgalindob.assistantapp.ui.components.BaseCard
import com.afgalindob.assistantapp.ui.components.CardEvent
import com.afgalindob.assistantapp.ui.theme.OnSurfaceSecondary
import com.afgalindob.assistantapp.utils.DateUtils
import com.afgalindob.assistantapp.R

sealed interface NoteEvent {
    object Edit : NoteEvent
    object Delete : NoteEvent
}

@Composable
fun NoteCard(
    note: NoteDomain,
    date: Long? = null,
    onTrash: Boolean = false,
    expanded: Boolean,
    anyCardExpanded: Boolean,
    onExpand: () -> Unit,
    enableSwipe: Boolean = true,
    onEvent: (NoteEvent) -> Unit,
    actionArea: @Composable BoxScope.() -> Unit
) {
    BaseCard(
        expanded = expanded,
        anyCardExpanded = anyCardExpanded,
        onExpand = onExpand,
        enableSwipe = enableSwipe,
        onEvent = { event ->
            if (event is CardEvent.Swipe) onEvent(NoteEvent.Delete)
        },
        titleArea = {
            Column (modifier = Modifier.weight(1f)){
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.headlineMedium,
                    maxLines = if (expanded) Int.MAX_VALUE else 1,
                    overflow = if (expanded) TextOverflow.Clip else TextOverflow.Ellipsis
                )
                date?.let {
                    Text(
                        text = if (onTrash)
                            stringResource(R.string.will_be_deleted) + " " + DateUtils.formatReadable(DateUtils.fromTimestamp(it))
                        else
                            DateUtils.formatReadable(DateUtils.fromTimestamp(it)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceSecondary,
                    )
                }
            }
        },
        expandedContent = {
            Text(
                text = note.content,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 48.dp)
            )
        },
        actionArea = actionArea
    )
}