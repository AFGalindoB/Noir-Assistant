package com.afgalindob.assistantapp.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.afgalindob.assistantapp.ui.theme.AccentPrimary
import com.afgalindob.assistantapp.ui.theme.OnSurfaceSecondary
import com.afgalindob.assistantapp.ui.theme.SurfaceContainerHigh

@Composable
fun EntitySnackbar(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier.padding(bottom = 16.dp)
    ) { data ->
        key(data) {
            AnimatedContent(
                targetState = data,
                transitionSpec = {
                    val enterTransition = slideInVertically(
                        animationSpec = tween(durationMillis = 500, easing = LinearOutSlowInEasing)
                    ) { height -> height } + fadeIn(animationSpec = tween(500))

                    val exitTransition = fadeOut(
                        animationSpec = tween(durationMillis = 800, easing = LinearEasing)
                    ) + slideOutVertically(
                        animationSpec = tween(durationMillis = 800)
                    ) { height -> -height / 4 } // Sube solo un 25% mientras desaparece

                    enterTransition togetherWith exitTransition
                },
                label = "SnackbarAnimation"
            ) { targetData ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = SurfaceContainerHigh,
                        contentColor = OnSurfaceSecondary
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = targetData.visuals.message,
                            style = MaterialTheme.typography.bodyLarge
                        )

                        targetData.visuals.actionLabel?.let { label ->
                            TextButton(
                                onClick = { targetData.performAction() }
                            ) {
                                Text(
                                    text = label.uppercase(),
                                    color = AccentPrimary,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}