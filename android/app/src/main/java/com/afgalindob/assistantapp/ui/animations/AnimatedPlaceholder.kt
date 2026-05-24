package com.afgalindob.assistantapp.ui.animations

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AnimatedPlaceholder(
    text: String,
    isVisible: Boolean,
    isLabel: Boolean,
    isFocused: Boolean = false
) {
    val enterAnim = if (isLabel) {
        fadeIn(animationSpec = tween(400, 150))
    } else {
        fadeIn(animationSpec = tween(300, 100))
    }

    val exitAnim = fadeOut(animationSpec = tween(if (isLabel) 300 else 200))

    AnimatedVisibility(
        visible = isVisible,
        enter = enterAnim,
        exit = exitAnim,
        modifier = if (isLabel) Modifier.padding(start = 16.dp, top = 0.dp) else Modifier.padding(start = 16.dp)
    ) {
        Text(
            text = text,
            color = if (isLabel && isFocused) MaterialTheme.colorScheme.primary else Color.Gray,
            fontSize = if (isLabel) 12.sp else 16.sp,
            style = if (isLabel) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodyLarge
        )
    }
}