package com.afgalindob.assistantapp.ui.components.overlays

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import com.afgalindob.assistantapp.ui.theme.SurfaceContainer

@Composable
fun CurtainOverlay(
    isVisible: Boolean,
    onFullyVisible: () -> Unit
) {
    val alphaAnim = remember { Animatable(initialValue = 0f) }

    // Control preciso de la animación
    LaunchedEffect(isVisible) {
        if (isVisible) {
            // Entrada: cortina bajando
            alphaAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 200,
                    easing = LinearEasing
                )
            )
            // ← Solo avisamos cuando terminó de entrar completamente
            onFullyVisible()
        } else {
            // Salida: cortina subiendo (sin avisar)
            alphaAnim.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = 200,
                    easing = FastOutSlowInEasing
                )
            )
        }
    }

    // Solo dibujamos cuando tiene algo de opacidad
    if (alphaAnim.value > 0.01f) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = alphaAnim.value }
                .background(SurfaceContainer)
                .pointerInput(Unit) {
                    // Bloquea completamente cualquier interacción
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent(pass = PointerEventPass.Initial)
                            // Consumimos todos los eventos
                        }
                    }
                }
        )
    }
}