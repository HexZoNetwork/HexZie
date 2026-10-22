package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// HexZie Dark-First Color Palette
val HexzieBackground = Color(0xFF090A0E)
val HexzieSurface = Color(0xFF111318)
val HexzieSurfaceVariant = Color(0xFF181B22)
val HexzieSurfaceElevated = Color(0xFF20242E)
val HexzieOnBackground = Color(0xFFF3F4F6)
val HexzieOnSurface = Color(0xFFF3F4F6)
val HexzieOnSurfaceMuted = Color(0xFF94A3B8)
val HexzieOutline = Color(0xFF282F3E)
val HexzieOutlineGlow = Color(0xFF384358)

// Primary Accents (Cybernetic Electric & Neon Cyan)
val HexzieAccent = Color(0xFF3B82F6)
val HexzieAccentElectric = Color(0xFF2563EB)
val HexzieCyan = Color(0xFF06B6D4)
val HexzieViolet = Color(0xFF8B5CF6)

// Semantic Action Colors
val HexzieAllowGreen = Color(0xFF10B981)
val HexzieDenyRed = Color(0xFFEF4444)
val HexziePendingAmber = Color(0xFFF59E0B)

// Light variants
val HexzieBackgroundLight = Color(0xFFF8F9FA)
val HexzieSurfaceLight = Color(0xFFFFFFFF)
val HexzieSurfaceVariantLight = Color(0xFFEFEFEF)
val HexzieOnBackgroundLight = Color(0xFF111215)
val HexzieOnSurfaceLight = Color(0xFF111215)
val HexzieOutlineLight = Color(0xFFD6D6DB)

// Cybernetic Glow Brushes
val HexzieCyberGradiant = Brush.linearGradient(
    colors = listOf(HexzieAccent, HexzieCyan)
)

val HexzieBorderGlowBrush = Brush.linearGradient(
    colors = listOf(HexzieAccent.copy(alpha = 0.6f), HexzieCyan.copy(alpha = 0.2f), HexzieOutline)
)

val HexzieCardGlowBrush = Brush.verticalGradient(
    colors = listOf(HexzieSurfaceElevated, HexzieSurfaceVariant)
)
