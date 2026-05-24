package com.afgalindob.assistantapp.ui.components.bottomsheet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.afgalindob.assistantapp.R
import com.afgalindob.assistantapp.ui.theme.AccentSecondary
import com.afgalindob.assistantapp.ui.theme.OnAccentSecondary
import com.afgalindob.assistantapp.ui.theme.OnSurfacePrimary
import com.afgalindob.assistantapp.ui.theme.OnSurfaceSecondary
import com.afgalindob.assistantapp.ui.theme.SurfaceContainer
import com.afgalindob.assistantapp.ui.theme.SurfaceVariant
import dev.chrisbanes.snapper.ExperimentalSnapperApi
import dev.chrisbanes.snapper.SnapOffsets
import dev.chrisbanes.snapper.rememberLazyListSnapperLayoutInfo
import dev.chrisbanes.snapper.rememberSnapperFlingBehavior
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeReminderSheet(
    initialTime: String,
    onDismiss: () -> Unit,
    onTimeSelected: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    // Parseamos la hora inicial (08:00)
    val parts = initialTime.split(":")
    val initialHour = parts.getOrNull(0)?.toInt() ?: 8
    val initialMinute = parts.getOrNull(1)?.toInt() ?: 0

    var selectedHour by remember { mutableIntStateOf(initialHour) }
    var selectedMinute by remember { mutableIntStateOf(initialMinute) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceContainer
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.select_time),
                style = MaterialTheme.typography.headlineMedium,
                color = OnSurfacePrimary
            )

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth().height(150.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Rueda de Horas
                FiniteWheelPicker(
                    items = (0..23).toList(),
                    initialItem = initialHour,
                    onItemSelected = { selectedHour = it }
                )

                Text(
                    text = " : ",
                    style = MaterialTheme.typography.headlineSmall,
                    color = OnSurfaceSecondary
                )

                // Rueda de Minutos
                FiniteWheelPicker(
                    items = (0..59).toList(),
                    initialItem = initialMinute,
                    onItemSelected = { selectedMinute = it }
                )
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    val formattedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                    onTimeSelected(formattedTime)
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentSecondary),
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Text(stringResource(R.string.confirm), color = OnAccentSecondary)
            }
        }
    }
}

@OptIn(ExperimentalSnapperApi::class)
@Composable
private fun FiniteWheelPicker(
    items: List<Int>,
    initialItem: Int,
    onItemSelected: (Int) -> Unit
) {
    val itemHeight = 48.dp
    val pickerHeight = 150.dp

    val density = LocalDensity.current
    val itemHeightPx = with(density) { itemHeight.toPx() }
    val pickerHeightPx = with(density) { pickerHeight.toPx() }

    // El centro exacto del componente Box
    val centerLinePx = pickerHeightPx / 2f

    val initialIndex = items.indexOf(initialItem).coerceAtLeast(0)

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialIndex
    )
    val layoutInfo = rememberLazyListSnapperLayoutInfo(
        lazyListState = listState,
        snapOffsetForItem = SnapOffsets.Center
    )

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            layoutInfo.currentItem?.index?.let { index ->
                if (index in items.indices) {
                    onItemSelected(items[index])
                }
            }
        }
    }

    Box(
        modifier = Modifier.width(80.dp).height(pickerHeight),
        contentAlignment = Alignment.Center
    ) {
        // Marcador central
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .background(
                    SurfaceVariant.copy(alpha = 0.1f),
                    RoundedCornerShape(8.dp)
                )
        )

        LazyColumn(
            state = listState,
            flingBehavior = rememberSnapperFlingBehavior(listState),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = (pickerHeight - itemHeight) / 2)
        ) {
            items(items.size) { index ->
                val item = items[index]

                val alpha by remember(index) {
                    derivedStateOf {
                        val itemInfo = listState.layoutInfo.visibleItemsInfo.find { it.index == index }
                        if (itemInfo != null) {
                            // CORRECCIÓN: El offset de la LazyColumn empieza después del contentPadding superior
                            // Sumamos el padding superior al offset del item para obtener su posición real en el Box
                            val verticalPaddingPx = (pickerHeightPx - itemHeightPx) / 2f
                            val itemTopInBox = itemInfo.offset + verticalPaddingPx
                            val itemCenterInBox = itemTopInBox + (itemInfo.size / 2f)

                            val distance = abs(itemCenterInBox - centerLinePx)

                            // Ajustamos el rango para que el cambio de alpha sea más nítido en el centro
                            val maxRange = itemHeightPx
                            (1f - (distance / maxRange)).coerceIn(0.2f, 1f)
                        } else {
                            0.2f
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .graphicsLayer {
                            this.alpha = alpha
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = String.format("%02d", item),
                        style = MaterialTheme.typography.displaySmall,
                        color = if (alpha > 0.8f) OnSurfacePrimary else OnSurfacePrimary.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}