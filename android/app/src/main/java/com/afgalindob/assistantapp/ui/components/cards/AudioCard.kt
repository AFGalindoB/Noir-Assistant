package com.afgalindob.assistantapp.ui.components.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.afgalindob.assistantapp.R
import com.afgalindob.assistantapp.data.domain.AudioDomain
import com.afgalindob.assistantapp.ui.components.BaseCard
import com.afgalindob.assistantapp.ui.components.CardEvent
import com.afgalindob.assistantapp.ui.theme.AccentPrimary
import com.afgalindob.assistantapp.ui.theme.OnAccentSecondary
import com.afgalindob.assistantapp.ui.theme.OnSurfacePrimary
import com.afgalindob.assistantapp.ui.theme.SurfaceContainerHigh

@Composable
fun AudioCard(
    audio: AudioDomain,
    isPlaying: Boolean,
    anyCardExpanded: Boolean,
    sliderProgress: Float,
    currentPosition: Long,
    totalDuration: Long,
    onTogglePlay: () -> Unit,
    onDelete: (onComplete: (Boolean) -> Unit) -> Unit,
    onSeek: (Float) -> Unit,
    onSeekRelative: (Long) -> Unit
) {
    BaseCard(
        expanded = isPlaying,
        anyCardExpanded = anyCardExpanded,
        onExpand = { },
        expandIconEnabled = false,
        onEvent = { event ->
            if (event is CardEvent.Swipe){
                onDelete { wasDeleted -> event.confirm(wasDeleted) }
            }
        },
        headerPrefix = {
            IconButton(
                onClick = { onTogglePlay() },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (isPlaying) AccentPrimary else SurfaceContainerHigh)
            ) {
                Icon(
                    painter = painterResource(
                        if (isPlaying) R.drawable.square_filled else R.drawable.play_arrow
                    ),
                    contentDescription = if (isPlaying) "Detener" else "Reproducir",
                    tint = if (isPlaying) OnSurfacePrimary else OnAccentSecondary
                )
            }
        },
        titleArea = {
            Text(
                text = audio.fileName,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(start = 8.dp),
                maxLines = if (isPlaying) Int.MAX_VALUE else 1,
                overflow = if (isPlaying) TextOverflow.Clip else TextOverflow.Ellipsis
            )
        },
        expandedContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ){
                    Text(
                        text = formatTime(currentPosition),
                        style = MaterialTheme.typography.labelMedium
                    )

                    Slider(
                        value = sliderProgress,
                        onValueChange = { onSeek(it) },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = AccentPrimary,
                            activeTrackColor = AccentPrimary,
                            inactiveTrackColor = SurfaceContainerHigh
                        ),
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp) // Usa el espacio disponible
                    )

                    Text(
                        text = formatTime(totalDuration), // Tiempo total a la derecha
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                // Botones +/-
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    IconButton(onClick = { onSeekRelative(-5000L) }) {
                        Icon(
                            painter = painterResource(R.drawable.replay_5),
                            contentDescription = "-5 seg",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { onSeekRelative(5000L) }) {
                        Icon(
                            painter = painterResource(R.drawable.forward_5),
                            contentDescription = "+5 seg",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    )
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}