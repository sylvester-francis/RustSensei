package com.sylvester.rustsensei.ui.theme

import androidx.compose.ui.graphics.Color

// ── Surface Hierarchy (6-step, blue-tinted near-blacks) ──────────────
val DarkBackground       = Color(0xFF0A0E14)
val DarkSurface          = Color(0xFF0F1319)
val DarkSurfaceVariant   = Color(0xFF151A23)
val DarkSurfaceContainer = Color(0xFF1C2130)
val DarkSurfaceContainerHigh    = Color(0xFF222838)
val DarkSurfaceContainerHighest = Color(0xFF2A3040)

// ── Accent Colors ────────────────────────────────────────────────────
val RustOrange       = Color(0xFFCE412B)  // Primary — CTAs, nav highlights
val NeonOrangeBright = Color(0xFFFF6B35)  // Hover/pressed, notification dots
val RustOrangeDark   = Color(0xFF9C1A0A)  // Container backgrounds, pressed
val NeonCyan         = Color(0xFF4DEEEA)  // Links, code keywords, streak counters
val WarningAmber     = Color(0xFFF59E0B)  // Warnings, in-progress badges, XP values
val SuccessGreen     = Color(0xFF3FB950)  // Correct answers, completed items
val ErrorNeon        = Color(0xFFFF453A)  // Wrong answers, errors
val CrispWhite       = Color(0xFFE8ECF2)  // Primary text (15.2:1 contrast)
val SecondaryText    = Color(0xFF8B95A5)  // Secondary text, timestamps (5.8:1)
val OutlineDark      = Color(0xFF1E2430)  // Borders, dividers

// Legacy aliases for compat
val DangerRed = ErrorNeon
val MediumGray = SecondaryText

// ── Light Palette ────────────────────────────────────────────────────
val LightBackground     = Color(0xFFFAFBFD)
val LightSurface        = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF0F2F5)

// ── Semantic Status Colors ───────────────────────────────────────────
val DifficultyBeginner     = SuccessGreen
val DifficultyIntermediate = WarningAmber
val DifficultyAdvanced     = ErrorNeon

// Code rendering
val InlineCodeBackground = Color(0xFF1C2130)  // surfaceContainer
val InlineCodeText       = Color(0xFFE8975A)  // warm orange
val CodeBlockBackground  = Color(0xFF0A0E14)  // deepest surface

// Completion states
val CompletedBadge  = SuccessGreen
val InProgressBadge = WarningAmber
val NotStartedBadge = SecondaryText

// ── Syntax Highlighting Palette ──────────────────────────────────────
val SyntaxKeyword   = Color(0xFFFF7B72)  // fn, let, mut, pub
val SyntaxString    = Color(0xFFE8975A)  // Strings
val SyntaxType      = Color(0xFF79C0FF)  // Types / Structs
val SyntaxFunction  = Color(0xFFD2A8FF)  // Functions / Methods
val SyntaxComment   = Color(0xFF6B7280)  // Comments
val SyntaxNumber    = Color(0xFF7EE787)  // Numbers / Literals
val SyntaxMacro     = Color(0xFFFFA657)  // Macros (println!, vec!)
val SyntaxLifetime  = Color(0xFFF778BA)  // Lifetimes ('a, 'static)
val SyntaxAttribute = Color(0xFFA5D6FF)  // Attributes (#[derive])
val SyntaxOperator  = Color(0xFFE8ECF2)  // Operators
val SyntaxLineNumber = Color(0xFF3B4252) // Line numbers

// ── Glow Effects ─────────────────────────────────────────────────────
val PrimaryGlow = Color(0x40CE412B)    // 0dp 0dp 12dp
val CyanGlow    = Color(0x304DEEEA)    // 0dp 0dp 8dp
val SuccessGlow = Color(0x303FB950)    // 0dp 0dp 8dp
val ErrorGlow   = Color(0x30FF453A)    // 0dp 0dp 8dp
