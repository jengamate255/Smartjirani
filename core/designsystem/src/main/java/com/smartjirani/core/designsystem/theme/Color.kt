package com.smartjirani.core.designsystem.theme

import androidx.compose.ui.graphics.Color

// Savannah Emerald — security, vitality, primary actions.
val UjamaaEmeraldDeep = Color(0xFF00442F)
val UjamaaEmerald = Color(0xFF135D43)
val UjamaaEmeraldPressed = Color(0xFF0E4431)
val UjamaaEmeraldMist = Color(0xFF8ED4B2)
val UjamaaMint = Color(0xFFABF1CF)
val UjamaaMintDim = Color(0xFF90D5B4)
val UjamaaEmeraldGlow = Color(0x1F135D43)

// Terracotta Clay — mutual aid, urgency, communal participation.
val UjamaaTerracotta = Color(0xFFD95D39)
val UjamaaTerracottaDeep = Color(0xFFA73918)
val UjamaaTerracottaContainer = Color(0xFFFE7952)
val UjamaaTerracottaOnContainer = Color(0xFF6C1900)
val UjamaaTerracottaMist = Color(0xFFFFDBD1)

// Sunlit Amber — milestones, recognition, notices.
val UjamaaAmber = Color(0xFFF4A261)
val UjamaaAmberDeep = Color(0xFF5D2E00)
val UjamaaAmberContainer = Color(0xFF7E4205)
val UjamaaAmberOnContainer = Color(0xFFFFB57D)

// Ink & canvas — warm linen tones instead of clinical blue-whites.
val UjamaaInk = Color(0xFF1C1E1B)
val UjamaaInkSoft = Color(0xFF1A1C19)
val UjamaaInkVariant = Color(0xFF3F4943)
val UjamaaLinen = Color(0xFFFBF9F5)
val UjamaaCanvas = Color(0xFFFAFAF4)
val UjamaaClay = Color(0xFFF4F1EA)
val UjamaaClayDeep = Color(0xFFEDE8DF)

// Tonal surface stacking (warm ambient tinting, no cool grays).
val UjamaaSurfaceDim = Color(0xFFDADAD5)
val UjamaaSurfaceBright = Color(0xFFFAFAF4)
val UjamaaSurfaceLowest = Color(0xFFFFFFFF)
val UjamaaSurfaceLow = Color(0xFFF4F4EF)
val UjamaaSurfaceMid = Color(0xFFEEEEE9)
val UjamaaSurfaceHigh = Color(0xFFE8E8E3)
val UjamaaSurfaceHighest = Color(0xFFE3E3DE)

val UjamaaOutline = Color(0xFF707973)
val UjamaaOutlineVariant = Color(0xFFBFC9C1)
val UjamaaSurfaceTint = Color(0xFF246A4F)

// Error & inverse roles.
val UjamaaError = Color(0xFFBA1A1A)
val UjamaaOnError = Color(0xFFFFFFFF)
val UjamaaErrorContainer = Color(0xFFFFDAD6)
val UjamaaOnErrorContainer = Color(0xFF93000A)
val UjamaaInverseSurface = Color(0xFF2F312E)
val UjamaaInverseOnSurface = Color(0xFFF1F1EC)
val UjamaaInversePrimary = UjamaaMintDim

val UjamaaOnSurface = Color(0xFF1A1C19)
val UjamaaOnPrimary = Color(0xFFFFFFFF)

// Derived dark-mode counterparts (the design system ships light-only).
val UjamaaDarkCanvas = Color(0xFF121410)
val UjamaaDarkSurface = Color(0xFF1A1C19)
val UjamaaDarkSurfaceMid = Color(0xFF242622)
val UjamaaDarkSurfaceHigh = Color(0xFF2F312E)
val UjamaaDarkSurfaceHighest = Color(0xFF3A3D38)
val UjamaaDarkOnSurface = Color(0xFFE3E3DE)
val UjamaaDarkOnSurfaceVariant = Color(0xFFBFC9C1)

// Legacy names still referenced outside the theme package.
val SmartGreen = UjamaaEmerald
val SmartGreenLight = UjamaaEmeraldMist
val SmartBlue = UjamaaAmberContainer
val SmartGold = UjamaaAmber
val SmartOrange = UjamaaTerracotta
val SmartRed = UjamaaError
val SmartGrayLight = UjamaaSurfaceLow
