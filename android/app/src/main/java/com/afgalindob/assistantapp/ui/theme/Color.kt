package com.afgalindob.assistantapp.ui.theme

import androidx.compose.ui.graphics.Color

// =================================================================================
// 1. Superficies (Contenedores y Fondos)
// Define "dónde" se colocan los elementos, de lo más profundo a lo más superficial.
// =================================================================================

// Fondo principal de toda la pantalla (negro puro de tu captura)
val BackgroundColor = Color(0xFF000000)

// Superficie de nivel 1 (el color de las tarjetas/tarjetas de tareas)
val SurfaceContainer = Color(0xFF1C222E)

// Superficie de nivel 2 (para elementos ligeramente más elevados, como los "chips" de la barra inferior)
val SurfaceContainerHigh = Color(0xFF161B26)

// Color para elementos interactivos en estado neutro (un gris oscuro/medio)
val SurfaceVariant = Color(0xFF4A474D)

// =================================================================================
// 2. Colores de Contenido (On-Surfaces)
// Define el color de lo que va "encima" de las superficies (texto, iconos).
// =================================================================================

val OnSurfacePrimary = Color(0xFFE2E2E2) // Texto principal
val OnSurfaceSecondary = Color(0xFF94A3B8) // Texto secundario

// =================================================================================
// 3. Acentos y Colores de Marca (Tus Morados)
// Estos colores destacan y guían al usuario.
// =================================================================================

val AccentPrimary = Color(0xFF6E31FF) // Color para acciones principales
val AccentSecondary = Color(0xFF31215A) // Color para estados secundarios.
val OnAccentPrimary = Color(0xFFFFFFFF) // El color del texto o icono encima de AccentPrimary
val OnAccentSecondary = Color(0xFFE2D7FF) // El color encima de AccentSecondary

// =================================================================================
// 4. Estados Especiales y Colores Utilitarios
// =================================================================================

// Color de error (para validaciones o alertas)
val ErrorColor = Color(0xFFCF6679)