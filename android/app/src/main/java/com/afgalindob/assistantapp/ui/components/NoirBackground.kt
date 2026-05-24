package com.afgalindob.assistantapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.afgalindob.assistantapp.ui.theme.BackgroundColor

@Composable
fun NoirBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundColor) // Tu negro puro
    ) {

        repeat(4) { i ->
            NoirAmbientFirefly(index = i)
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            content = content
        )
    }
}