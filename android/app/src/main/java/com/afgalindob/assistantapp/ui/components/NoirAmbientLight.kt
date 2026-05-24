package com.afgalindob.assistantapp.ui.components

import android.os.Build
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.afgalindob.assistantapp.ui.theme.AccentSecondary
import kotlin.random.Random

@Composable
fun NoirAmbientFirefly(
    index: Int
) {
    var size by remember { mutableStateOf(Size.Zero) }

    // Parámetros de la trayectoria actual (se resetean cada vez que la luz sale)
    var angle by remember { mutableFloatStateOf((0..360).random().toFloat()) }
    var frequency by remember { mutableFloatStateOf(0.5f + Random.nextFloat() * 1.5f) }
    var amplitude by remember { mutableFloatStateOf(0.1f + Random.nextFloat() * 0.2f) }

    val infiniteTransition = rememberInfiniteTransition(label = "Firefly")

    // El progreso de la luz a lo largo de su línea (de -0.2 a 1.2 para asegurar que salga de pantalla)
    val progress by infiniteTransition.animateFloat(
        initialValue = -0.2f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (15000..25000).random(),
                delayMillis = (2000..8000).random(), // El delay que mencionaste
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ), label = "Progress"
    )

    // Resetear parámetros cuando la luz comienza un nuevo ciclo
    LaunchedEffect(progress <= -0.1f) {
        if (progress <= -0.1f) {
            angle = (0..360).random().toFloat()
            frequency = 0.5f + Random.nextFloat() * 1.5f
            amplitude = 0.1f + Random.nextFloat() * 0.2f
        }
    }

    val currentAlpha by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "Alpha"
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Modifier.blur(130.dp)
                else Modifier
            )
            .onGloballyPositioned { size = it.size.toSize() }
    ) {
        if (size != Size.Zero) {
            // --- Lógica de Senoidal con Rotación ---
            // 'progress' es nuestra línea base (eje X local)
            // 'wave' es la oscilación senoidal (eje Y local)
            val wave = amplitude * kotlin.math.sin(progress * frequency * 2f * Math.PI.toFloat())

            // Transformamos coordenadas locales a globales usando el ángulo
            val rad = Math.toRadians(angle.toDouble()).toFloat()

            // Punto centrado que rotamos
            val lx = progress - 0.5f
            val ly = wave

            val rotatedX = lx * kotlin.math.cos(rad) - ly * kotlin.math.sin(rad)
            val rotatedY = lx * kotlin.math.sin(rad) + ly * kotlin.math.cos(rad)

            // Mapeamos al centro de la pantalla
            val x = 0.5f + rotatedX
            val y = 0.5f + rotatedY

            val baseDim = minOf(size.width, size.height)
            val radius = baseDim * 0.4f // Tamaño generoso para el efecto de ola

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(AccentSecondary.copy(alpha = currentAlpha), Color.Transparent),
                    center = Offset(size.width * x, size.height * y),
                    radius = radius
                ),
                center = Offset(size.width * x, size.height * y),
                radius = radius
            )
        }
    }
}