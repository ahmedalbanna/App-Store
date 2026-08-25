package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Light Mode Geometric Balance Palette — Phase 2.4: OasisDarkTeal was misnamed (0xFF4F378B is tertiary purple, not teal)
val OasisTertiary = Color(0xFF4F378B)
@Deprecated("Renamed to OasisTertiary — was misnamed (purple, not teal)", ReplaceWith("OasisTertiary"))
val OasisDarkTeal = OasisTertiary
val OasisPrimary = Color(0xFF6750A4)
val OasisSecondary = Color(0xFF9278D1)
val OasisSandLight = Color(0xFFF3EDF7)
val OasisBackgroundLight = Color(0xFFFEF7FF)
val OasisSurfaceLight = Color(0xFFFFFFFF)

// Dark Mode Geometric Balance Palette
val OasisMintDark = Color(0xFFCCC2DC)
val OasisPrimaryDark = Color(0xFFD0BCFF)
val OasisSecondaryDark = Color(0xFF49454F)
val OasisBgDark = Color(0xFF141218)
val OasisSurfaceDark = Color(0xFF1C1B1F)
val OasisSurfaceVariantDark = Color(0xFF2B2930)

val TextWhite = Color(0xFFE6E1E5)
val TextDark = Color(0xFF1D1B20)
