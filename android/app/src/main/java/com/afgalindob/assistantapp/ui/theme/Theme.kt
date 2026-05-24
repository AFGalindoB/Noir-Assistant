package com.afgalindob.assistantapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

import androidx.compose.material3.ColorScheme

val DarkColorPalette: ColorScheme = darkColorScheme(
    primary = AccentPrimary,
    onPrimary = OnAccentPrimary,
    secondary = AccentSecondary,
    onSecondary = OnAccentSecondary,

    // Fondos y Superficies
    background = BackgroundColor,
    onBackground = OnSurfacePrimary,
    surface = SurfaceContainer,
    onSurface = OnSurfacePrimary,
    surfaceVariant = SurfaceVariant, // Para el checkbox sin marcar y el icono de filtro
    onSurfaceVariant = OnSurfaceSecondary, // Para el texto secundario

    // Superficie elevada para contenedores
    surfaceContainerHighest = SurfaceContainerHigh, // Para los 'chips' de la barra inferior

    // Esto ayuda a que el check del checkbox se vea correcto
    inversePrimary = AccentPrimary,

    // Error
    error = ErrorColor
)

@Composable
fun AssistantTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorPalette,
        typography = AppTypography,
        content = content
    )
}