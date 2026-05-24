package com.afgalindob.assistantapp.ui.dialogs.dialog

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.afgalindob.assistantapp.R
import com.afgalindob.assistantapp.ui.components.CameraPreview
import com.afgalindob.assistantapp.ui.theme.AccentSecondary
import com.afgalindob.assistantapp.ui.theme.BackgroundColor
import com.afgalindob.assistantapp.ui.theme.OnAccentPrimary

@Composable
fun QrScannerDialog(
    onDismiss: () -> Unit,
    onCodeDetected: (String) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .fillMaxHeight(0.5f)
                .clip(RoundedCornerShape(28.dp))
                .background(BackgroundColor)
        ) {
            CameraPreview(onCodeDetected = onCodeDetected)

            ScannerOverlay()

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .background(BackgroundColor.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    painter = painterResource(R.drawable.close),
                    contentDescription = "Cerrar",
                    tint = OnAccentPrimary
                )
            }
        }
    }
}

@Composable
fun ScannerOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val strokeWidth = 4.dp.toPx()
        val cornerLength = 40.dp.toPx()
        val size = size.minDimension * 0.7f
        val left = (this.size.width - size) / 2
        val top = (this.size.height - size) / 2

        // Dibujamos las esquinas para guiar al usuario
        drawPath(
            path = Path().apply {
                // Esquina superior izquierda
                moveTo(left, top + cornerLength)
                lineTo(left, top)
                lineTo(left + cornerLength, top)

                // Esquina superior derecha
                moveTo(left + size - cornerLength, top)
                lineTo(left + size, top)
                lineTo(left + size, top + cornerLength)

                // Esquina inferior derecha
                moveTo(left + size, top + size - cornerLength)
                lineTo(left + size, top + size)
                lineTo(left + size - cornerLength, top + size)

                // Esquina inferior izquierda
                moveTo(left + cornerLength, top + size)
                lineTo(left, top + size)
                lineTo(left, top + size - cornerLength)
            },
            color = AccentSecondary,
            style = Stroke(width = strokeWidth)
        )
    }
}