package com.sylvester.rustsensei.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Design tokens defining the spatial rhythm, opacity scale, and common
 * dimensions used across every screen.
 *
 * Why tokens instead of inline literals:
 *  - One place to tune the visual rhythm of the entire app.
 *  - Grep-able: `Spacing.MD` tells you the intent; `12.dp` does not.
 *  - Enforces consistency — screens can't silently drift apart.
 */

// ── Spacing Scale (4-point grid) ────────────────────────────────────

object Spacing {
    val XXS: Dp = 2.dp
    val XS: Dp = 4.dp
    val SM: Dp = 8.dp
    val MD: Dp = 12.dp
    val LG: Dp = 16.dp
    val XL: Dp = 20.dp
    val XXL: Dp = 24.dp
    val XXXL: Dp = 32.dp
    val Section: Dp = 48.dp
}

// ── Opacity Scale ───────────────────────────────────────────────────
// Named for intent, not value — so "what does 0.15f mean here?" never arises.

object Alpha {
    /** Barely visible borders, separators. */
    const val BORDER = 0.15f
    /** Subtle dividers, faint glow edges. */
    const val DIVIDER = 0.18f
    /** Muted badges, secondary labels. */
    const val MUTED = 0.30f
    /** De-emphasized but readable (hint text, disabled controls). */
    const val HINT = 0.50f
    /** Comfortable secondary text. */
    const val SECONDARY = 0.60f
    /** Slightly softer than full — used for body copy on vibrant backgrounds. */
    const val SOFT = 0.70f
    /** Near-full but not harsh — good for subheadings. */
    const val HIGH = 0.85f
    /** Full opacity. Rarely referenced explicitly; here for completeness. */
    const val FULL = 1.00f
}

// ── Common Dimensions ───────────────────────────────────────────────

object Dimens {
    /** Standard screen horizontal padding. */
    val ScreenPadding: Dp = 16.dp
    /** Card internal padding. */
    val CardPadding: Dp = 16.dp
    /** Compact top-bar height (chat screen). */
    val CompactTopBarHeight: Dp = 48.dp
    /** M3 bottom navigation bar height. */
    val BottomBarHeight: Dp = 80.dp
    /** Standard icon size in navigation and action buttons. */
    val IconSM: Dp = 20.dp
    val IconMD: Dp = 24.dp
    val IconLG: Dp = 32.dp
    /** Standard button heights. */
    val ButtonHeight: Dp = 52.dp
    val ButtonHeightLarge: Dp = 56.dp
    /** Thin horizontal rule. */
    val Divider: Dp = 1.dp
    /** Card corner radius (matches theme medium shape). */
    val CardRadius: Dp = 12.dp
    /** Pill / full-round corner radius. */
    val PillRadius: Dp = 28.dp
    /** Large emoji avatar size for empty states. */
    val AvatarEmoji = 56.sp
}
