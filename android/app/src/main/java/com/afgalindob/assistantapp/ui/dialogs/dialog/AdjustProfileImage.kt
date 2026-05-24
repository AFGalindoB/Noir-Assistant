package com.afgalindob.assistantapp.ui.dialogs.dialog

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.afgalindob.assistantapp.R
import com.afgalindob.assistantapp.ui.theme.AccentPrimary
import com.afgalindob.assistantapp.ui.theme.BackgroundColor
import com.afgalindob.assistantapp.ui.theme.OnSurfacePrimary

@Composable
fun AdjustProfileImage(
    imageUri: String?,
    initialX: Float = 0.5f,
    initialY: Float = 0.5f,
    initialZoom: Float = 1f,
    onDismiss: () -> Unit,
    onConfirm: (String?, Float, Float, Float) -> Unit,
) {
    var centerX by remember { mutableFloatStateOf(initialX) }
    var centerY by remember { mutableFloatStateOf(initialY) }
    var zoom by remember { mutableFloatStateOf(initialZoom) }

    var imageSize by remember { mutableStateOf(Size.Zero) }

    if (imageUri != null) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = BackgroundColor) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // --- CABECERA ---
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = OnSurfacePrimary) }
                        TextButton(onClick = { onConfirm(imageUri, centerX, centerY, zoom) }) {
                            Text(stringResource(R.string.accept), color = AccentPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // --- VIEWPORT DE EDICIÓN ---
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clipToBounds()
                            .background(Color.Black)
                            .pointerInput(imageSize) {
                                if (imageSize == Size.Zero) return@pointerInput

                                detectTransformGestures { _, pan, gestureZoom, _ ->
                                    zoom = (zoom * gestureZoom).coerceIn(1f, 5f)

                                    val viewSize = size.width.toFloat()
                                    val baseDim = if (imageSize.height < imageSize.width)
                                        imageSize.height else imageSize.width

                                    val currentScale = (viewSize / baseDim) * zoom

                                    // Conversión de píxeles de pantalla a coordenadas normalizadas
                                    centerX = (centerX - pan.x / (imageSize.width * currentScale)).coerceIn(0f, 1f)
                                    centerY = (centerY - pan.y / (imageSize.height * currentScale)).coerceIn(0f, 1f)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        val viewSize = constraints.maxWidth.toFloat()

                        AsyncImage(
                            model = imageUri,
                            contentDescription = null,
                            onSuccess = { state ->
                                imageSize = Size(
                                    state.painter.intrinsicSize.width,
                                    state.painter.intrinsicSize.height
                                )
                            },
                            modifier = Modifier
                                .wrapContentSize(unbounded = true, align = Alignment.TopStart)
                                .graphicsLayer {
                                    if (imageSize != Size.Zero) {
                                        val baseDim = if (imageSize.height < imageSize.width)
                                            imageSize.height else imageSize.width

                                        val finalScale = (viewSize / baseDim) * zoom

                                        scaleX = finalScale
                                        scaleY = finalScale

                                        translationX = (viewSize / 2f) - (centerX * imageSize.width * finalScale)
                                        translationY = (viewSize / 2f) - (centerY * imageSize.height * finalScale)

                                        transformOrigin = TransformOrigin(0f, 0f)
                                    }
                                },
                            contentScale = ContentScale.None
                        )

                        // --- MÁSCARA DE PREVISUALIZACIÓN ---
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val circleRadius = size.minDimension / 2f
                            drawContext.canvas.saveLayer(size.toRect(), Paint())
                            drawRect(color = Color.Black.copy(alpha = 0.6f))
                            drawCircle(
                                color = Color.Transparent,
                                radius = circleRadius,
                                center = center,
                                blendMode = BlendMode.Clear
                            )
                            drawCircle(
                                color = Color.White.copy(alpha = 0.3f),
                                radius = circleRadius,
                                center = center,
                                style = Stroke(width = 1.dp.toPx())
                            )

                            drawContext.canvas.restore()
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = stringResource(R.string.crop_dialog),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(24.dp).align(Alignment.CenterHorizontally),
                        color = OnSurfacePrimary.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}