package com.afgalindob.assistantapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.afgalindob.assistantapp.R
import com.afgalindob.assistantapp.ui.theme.AccentPrimary
import com.afgalindob.assistantapp.ui.theme.OnSurfaceSecondary
import com.afgalindob.assistantapp.ui.theme.SurfaceContainer
import kotlinx.coroutines.launch

sealed interface CardEvent {
    data class Swipe(val confirm: (Boolean) -> Unit) : CardEvent
}

@Composable
fun BaseCard(
    expanded: Boolean,
    anyCardExpanded: Boolean,
    enableSwipe: Boolean = true,
    expandIconEnabled: Boolean = true,
    onExpand: () -> Unit,
    onEvent: (CardEvent) -> Unit,

    headerPrefix: @Composable (() -> Unit)? = null,
    titleArea: @Composable RowScope.() -> Unit,
    expandedContent: @Composable ColumnScope.() -> Unit,
    actionArea: @Composable (BoxScope.() -> Unit)? = null
) {
    // Obtenemos el ancho de la pantalla para saber hasta dónde desplazar
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val density = LocalDensity.current
    val screenWidthPx = with(density) { screenWidth.toPx() }

    // Usamos Animatable para tener control total sobre la animación del desplazamiento
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    val borderWidth by animateDpAsState(
        targetValue = if (expanded) 2.dp else 0.dp,
        animationSpec = tween(durationMillis = 300),
        label = "BorderAnimation"
    )
    val scale by animateFloatAsState(
        targetValue = when {
            expanded -> 1.0f
            anyCardExpanded -> 0.95f
            else -> 1.0f
        },
        animationSpec = tween(durationMillis = 300),
        label = "ScaleAnimation"
    )
    val alpha by animateFloatAsState(
        targetValue = when {
            expanded -> 1.0f
            anyCardExpanded -> 0.7f
            else -> 1.0f
        },
        animationSpec = tween(durationMillis = 300),
        label = "AlphaAnimation"
    )
    val rotationAngle by animateFloatAsState(
        targetValue = if (expanded) -180f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "RotationAnimation"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                alpha = alpha,
                translationX = offsetX.value,
                transformOrigin = TransformOrigin.Center,
                clip = true
            )
            .then(
                if (enableSwipe) {
                    Modifier.pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                scope.launch {
                                    if (offsetX.value > 300f) {
                                        onEvent(CardEvent.Swipe { isAccepted ->
                                            scope.launch {
                                                if (isAccepted){
                                                    offsetX.animateTo(
                                                        targetValue = screenWidthPx,
                                                        animationSpec = tween(
                                                            durationMillis = 600, // Doblamos el tiempo
                                                            easing = FastOutSlowInEasing // Comienza rápido y frena al final
                                                        )
                                                    )
                                                } else {
                                                    offsetX.animateTo(
                                                        targetValue = 0f,
                                                        animationSpec = spring(
                                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                                            stiffness = Spring.StiffnessLow
                                                        )
                                                    )
                                                }
                                            }
                                        })
                                    } else {
                                        // Devolvemos a su posición original
                                        offsetX.animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessLow
                                            )
                                        )
                                    }
                                }
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                // Actualización inmediata durante el arrastre
                                scope.launch {
                                    offsetX.snapTo((offsetX.value + dragAmount).coerceAtLeast(0f))
                                }
                            }
                        )
                    }
                } else Modifier
            )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(20.dp),
            border = if (expanded) BorderStroke(borderWidth, AccentPrimary) else null,
            colors = CardDefaults.cardColors( containerColor = SurfaceContainer),
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (expanded) 8.dp else 2.dp
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {

                // --- HEADER ---
                Row(verticalAlignment = Alignment.CenterVertically) {
                    headerPrefix?.invoke()

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                indication = null,
                                interactionSource = null
                            ) { onExpand() }
                            .padding(8.dp)
                    ) {
                        titleArea()
                        if (expandIconEnabled){
                            Icon(
                                painter = painterResource(R.drawable.expand_more),
                                contentDescription = "Expand",
                                tint = OnSurfaceSecondary,
                                modifier = Modifier
                                    .padding(5.dp)
                                    .graphicsLayer(rotationZ = rotationAngle)
                            )
                        }
                    }
                }

                // --- CONTENT ---
                AnimatedVisibility(expanded) {
                    Column {
                        Spacer(Modifier.height(10.dp))

                        Box(modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp)) {
                            Column(modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = if (actionArea != null) 48.dp else 0.dp)
                            ) {
                                expandedContent()
                            }
                            actionArea?.invoke(this)
                        }
                    }
                }
            }
        }
    }
}