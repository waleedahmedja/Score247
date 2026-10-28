package com.waleedahmedja.score247.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────────────────────────────────────
// COLOUR SYSTEM
//
// One dominant field-green, warm charcoal neutrals, one amber accent, one muted red.
// Every colour is a *semantic token* with a light and a dark value (see the palettes
// below). Screens just write `color = Ink` and get the right value for the current
// theme. All text/background pairs below meet WCAG AA (≥ 4.5:1) in both themes.
// ─────────────────────────────────────────────────────────────────────────────

@Immutable
class S247Palette(
    val isDark: Boolean,

    // Neutrals
    val ink: Color,        // primary text
    val ink700: Color,     // toast / inverse surface
    val ink500: Color,     // secondary text
    val ink300: Color,     // tertiary text, placeholders (still ≥ 4.5:1)
    val ink200: Color,     // borders, dividers
    val ink100: Color,     // subtle fills
    val ink50: Color,      // app background
    val paper: Color,      // card / dialog surface

    // Brand
    val pitch: Color,      // primary action
    val pitchL: Color,     // lighter brand variant (borders, highlights)
    val pitchTint: Color,  // green-tinted fill
    val onPitch: Color,    // text/icons on `pitch`

    // Accent + danger
    val amber: Color,      val amberTint: Color,
    val red: Color,        val redTint: Color,

    // Scoreboard header — dark in BOTH themes, like a stadium scoreboard
    val headerBg: Color,
    val headerMuted: Color,
    val headerDim: Color,
    val headerChip: Color,
    val headerAccentBg: Color,
    val headerAccent: Color,
)

val LightPalette = S247Palette(
    isDark = false,
    ink = Color(0xFF111210), ink700 = Color(0xFF252623), ink500 = Color(0xFF4F504D),
    ink300 = Color(0xFF6B6C68), ink200 = Color(0xFFD4D5D1), ink100 = Color(0xFFEEEFEB),
    ink50 = Color(0xFFF7F8F4), paper = Color(0xFFFFFFFF),
    pitch = Color(0xFF23744A), pitchL = Color(0xFF3D9962), pitchTint = Color(0xFFE6F4EC),
    onPitch = Color(0xFFFFFFFF),
    amber = Color(0xFF9A5200), amberTint = Color(0xFFFFF3E0),
    red = Color(0xFFB03A3A), redTint = Color(0xFFFAEBEB),
    headerBg = Color(0xFF0F1410), headerMuted = Color(0xFF8EA08F), headerDim = Color(0xFF7E9181),
    headerChip = Color(0xFF1E271F), headerAccentBg = Color(0xFF3A2A0C), headerAccent = Color(0xFFF5B544),
)

val DarkPalette = S247Palette(
    isDark = true,
    ink = Color(0xFFECEEE8), ink700 = Color(0xFF2A312C), ink500 = Color(0xFFB4BAB4),
    ink300 = Color(0xFF8E958F), ink200 = Color(0xFF2F3731), ink100 = Color(0xFF1F2521),
    ink50 = Color(0xFF0D100E), paper = Color(0xFF161A17),
    pitch = Color(0xFF4FBF84), pitchL = Color(0xFF6FD39C), pitchTint = Color(0xFF13271C),
    onPitch = Color(0xFF04130A),
    amber = Color(0xFFF2B04C), amberTint = Color(0xFF2C2210),
    red = Color(0xFFF08080), redTint = Color(0xFF2F1818),
    headerBg = Color(0xFF060907), headerMuted = Color(0xFF8EA08F), headerDim = Color(0xFF7E9181),
    headerChip = Color(0xFF161D17), headerAccentBg = Color(0xFF3A2A0C), headerAccent = Color(0xFFF5B544),
)

val LocalPalette = staticCompositionLocalOf { LightPalette }

// Token accessors — keep call sites short and theme-aware: `color = Ink`.
val Ink:            Color @Composable @ReadOnlyComposable get() = LocalPalette.current.ink
val Ink700:         Color @Composable @ReadOnlyComposable get() = LocalPalette.current.ink700
val Ink500:         Color @Composable @ReadOnlyComposable get() = LocalPalette.current.ink500
val Ink300:         Color @Composable @ReadOnlyComposable get() = LocalPalette.current.ink300
val Ink200:         Color @Composable @ReadOnlyComposable get() = LocalPalette.current.ink200
val Ink100:         Color @Composable @ReadOnlyComposable get() = LocalPalette.current.ink100
val Ink50:          Color @Composable @ReadOnlyComposable get() = LocalPalette.current.ink50
val Paper:          Color @Composable @ReadOnlyComposable get() = LocalPalette.current.paper
val Pitch:          Color @Composable @ReadOnlyComposable get() = LocalPalette.current.pitch
val PitchL:         Color @Composable @ReadOnlyComposable get() = LocalPalette.current.pitchL
val PitchTint:      Color @Composable @ReadOnlyComposable get() = LocalPalette.current.pitchTint
val OnPitch:        Color @Composable @ReadOnlyComposable get() = LocalPalette.current.onPitch
val Amber:          Color @Composable @ReadOnlyComposable get() = LocalPalette.current.amber
val AmberTint:      Color @Composable @ReadOnlyComposable get() = LocalPalette.current.amberTint
val Red:            Color @Composable @ReadOnlyComposable get() = LocalPalette.current.red
val RedTint:        Color @Composable @ReadOnlyComposable get() = LocalPalette.current.redTint
val HeaderBg:       Color @Composable @ReadOnlyComposable get() = LocalPalette.current.headerBg
val HeaderMuted:    Color @Composable @ReadOnlyComposable get() = LocalPalette.current.headerMuted
val HeaderDim:      Color @Composable @ReadOnlyComposable get() = LocalPalette.current.headerDim
val HeaderChip:     Color @Composable @ReadOnlyComposable get() = LocalPalette.current.headerChip
val HeaderAccentBg: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.headerAccentBg
val HeaderAccent:   Color @Composable @ReadOnlyComposable get() = LocalPalette.current.headerAccent

/** True while the dark palette is active. */
val IsDarkTheme: Boolean @Composable @ReadOnlyComposable get() = LocalPalette.current.isDark

// ─────────────────────────────────────────────────────────────────────────────
// SHAPE TOKENS — large, soft "squircle" corners
// ─────────────────────────────────────────────────────────────────────────────
val ShapeCard    = RoundedCornerShape(20.dp)
val ShapeDialog  = RoundedCornerShape(28.dp)
val ShapeButton  = RoundedCornerShape(16.dp)
val ShapeInput   = RoundedCornerShape(14.dp)
val ShapeChip    = RoundedCornerShape(12.dp)
val ShapePill    = RoundedCornerShape(50)
val ShapeIconMd  = RoundedCornerShape(14.dp)
val ShapeIconSm  = RoundedCornerShape(10.dp)
val ShapeRunTile = RoundedCornerShape(16.dp)

// ─────────────────────────────────────────────────────────────────────────────
// TYPOGRAPHY
// Score digits use tabular figures ("tnum") so the number doesn't jitter sideways as
// it counts up. To go further, load a distinctive display face (e.g. via
// androidx.compose.ui:ui-text-google-fonts) and swap it in for FontFamily.Default.
// ─────────────────────────────────────────────────────────────────────────────
private const val TABULAR = "tnum"

val AppTypography = Typography(
    // Hero numbers — live score
    displayLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Black,
        fontSize = 72.sp, lineHeight = 76.sp, letterSpacing = (-3).sp, fontFeatureSettings = TABULAR),
    displayMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold,
        fontSize = 36.sp, lineHeight = 40.sp, letterSpacing = (-1.5).sp, fontFeatureSettings = TABULAR),
    displaySmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold,
        fontSize = 28.sp, lineHeight = 32.sp, letterSpacing = (-0.5).sp, fontFeatureSettings = TABULAR),

    // Section headings
    headlineLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.3).sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp, lineHeight = 24.sp, letterSpacing = (-0.2).sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = (-0.1).sp),

    // Body
    bodyLarge  = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall  = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.1.sp),

    // Labels
    labelLarge  = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.3.sp, fontFeatureSettings = TABULAR),
    labelSmall  = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium,
        fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.5.sp, fontFeatureSettings = TABULAR),

    // Titles
    titleLarge  = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp, lineHeight = 24.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium,
        fontSize = 14.sp, lineHeight = 20.sp, fontFeatureSettings = TABULAR),
    titleSmall  = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium,
        fontSize = 12.sp, lineHeight = 16.sp),
)

// ─────────────────────────────────────────────────────────────────────────────
// MATERIAL 3 THEME
// Our own tokens drive the custom components; this scheme keeps stock Material
// widgets (text fields, switches, dialogs) on-brand and theme-correct.
// ─────────────────────────────────────────────────────────────────────────────
private fun S247Palette.toColorScheme() =
    if (isDark) darkColorScheme(
        primary = pitch, onPrimary = onPitch, primaryContainer = pitchTint, onPrimaryContainer = pitch,
        secondary = amber, onSecondary = Color.Black, secondaryContainer = amberTint, onSecondaryContainer = amber,
        error = red, onError = Color.Black, errorContainer = redTint, onErrorContainer = red,
        background = ink50, onBackground = ink, surface = paper, onSurface = ink,
        surfaceVariant = ink100, onSurfaceVariant = ink500, outline = ink200, outlineVariant = ink100,
    ) else lightColorScheme(
        primary = pitch, onPrimary = onPitch, primaryContainer = pitchTint, onPrimaryContainer = pitch,
        secondary = amber, onSecondary = Color.White, secondaryContainer = amberTint, onSecondaryContainer = amber,
        error = red, onError = Color.White, errorContainer = redTint, onErrorContainer = red,
        background = ink50, onBackground = ink, surface = paper, onSurface = ink,
        surfaceVariant = ink100, onSurfaceVariant = ink500, outline = ink200, outlineVariant = ink100,
    )

@Composable
fun Score247Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val palette = if (darkTheme) DarkPalette else LightPalette
    CompositionLocalProvider(LocalPalette provides palette) {
        MaterialTheme(
            colorScheme = palette.toColorScheme(),
            typography  = AppTypography,
            content     = content,
        )
    }
}
