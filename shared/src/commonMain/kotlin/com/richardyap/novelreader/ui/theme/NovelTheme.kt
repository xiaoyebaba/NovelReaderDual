package com.richardyap.novelreader.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.richardyap.novelreader.models.ReaderPrefs

// ─── Color definitions ──────────────────────────────────────────────
// All colors use Color(0xFFxxxxxx) format as required.

/** 纸白 (Paper) — 类纸质书白底 */
object PaperColors {
    val background = Color(0xFFFAF8F2)
    val surface = Color(0xFFF5F0E8)
    val onBackground = Color(0xFF2C2416)
    val onSurface = Color(0xFF3D3526)
    val primary = Color(0xFF5C4B31)
    val secondary = Color(0xFF8B7355)
    val tertiary = Color(0xFFB8A88A)
}

/** 羊皮纸 (Parchment) — 仿古羊皮纸色调 */
object ParchmentColors {
    val background = Color(0xFFF4ECD8)
    val surface = Color(0xFFEDE3C8)
    val onBackground = Color(0xFF3B3020)
    val onSurface = Color(0xFF4A3D2B)
    val primary = Color(0xFF6B5B3E)
    val secondary = Color(0xFF8C7B5A)
    val tertiary = Color(0xFFB8A378)
}

/** 护眼绿 (Eye Green) — 柔光豆沙绿 */
object EyeGreenColors {
    val background = Color(0xFFC8DCC8)
    val surface = Color(0xFFBBD0BB)
    val onBackground = Color(0xFF2D3820)
    val onSurface = Color(0xFF3D4A2E)
    val primary = Color(0xFF4A6238)
    val secondary = Color(0xFF6B8254)
    val tertiary = Color(0xFF8CA070)
}

/** 水墨黑 (Ink Black) — 深灰黑底白字 */
object InkBlackColors {
    val background = Color(0xFF1A1A1A)
    val surface = Color(0xFF242424)
    val onBackground = Color(0xFFD0D0D0)
    val onSurface = Color(0xFFB8B8B8)
    val primary = Color(0xFFA0A0A0)
    val secondary = Color(0xFF808080)
    val tertiary = Color(0xFF606060)
}

/** 深夜蓝 (Night Blue) — 深蓝暗底 */
object NightBlueColors {
    val background = Color(0xFF0D1B2A)
    val surface = Color(0xFF152238)
    val onBackground = Color(0xFFC8D6E5)
    val onSurface = Color(0xFFA8B8CC)
    val primary = Color(0xFF6B8DBF)
    val secondary = Color(0xFF5A7AA8)
    val tertiary = Color(0xFF446688)
}

// ─── System-like dark color scheme (for actual UI chrome) ────────────
private val DarkChromeScheme = darkColorScheme(
    primary = Color(0xFF90CAF9),
    onPrimary = Color(0xFF0D1B2A),
    primaryContainer = Color(0xFF1E3A5F),
    onPrimaryContainer = Color(0xFFD6E4FF),
    secondary = Color(0xFF80CBC4),
    onSecondary = Color(0xFF003735),
    secondaryContainer = Color(0xFF00504B),
    onSecondaryContainer = Color(0xFFA7F0E8),
    tertiary = Color(0xFFFFB74D),
    onTertiary = Color(0xFF2E1800),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE0E0E0),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = Color(0xFFC0C0C0),
    error = Color(0xFFEF5350),
    outline = Color(0xFF666666),
)

private val LightChromeScheme = lightColorScheme(
    primary = Color(0xFF1565C0),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD6E4FF),
    onPrimaryContainer = Color(0xFF001B3E),
    secondary = Color(0xFF00897B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFA7F0E8),
    onSecondaryContainer = Color(0xFF00201C),
    tertiary = Color(0xFFE65100),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = Color(0xFF49454F),
    error = Color(0xFFD32F2F),
    outline = Color(0xFFBDBDBD),
)

// ─── Reader surface colors (for reading area only) ──────────────────
@Immutable
data class ReaderSurfaceColors(
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val divider: Color,
    val selectionBackground: Color,
    val selectionText: Color,
    val progressTrack: Color,
    val progressIndicator: Color,
)

/** CompositionLocal that carries the current reader-surface palette. */
val LocalReaderColors = compositionLocalOf {
    ReaderSurfaceColors(
        background = PaperColors.background,
        onBackground = PaperColors.onBackground,
        surface = PaperColors.surface,
        onSurface = PaperColors.onSurface,
        primary = PaperColors.primary,
        secondary = PaperColors.secondary,
        tertiary = PaperColors.tertiary,
        divider = PaperColors.primary.copy(alpha = 0.18f),
        selectionBackground = PaperColors.primary.copy(alpha = 0.30f),
        selectionText = PaperColors.onBackground,
        progressTrack = PaperColors.tertiary.copy(alpha = 0.35f),
        progressIndicator = PaperColors.primary,
    )
}

// ─── Theme skin resolver ─────────────────────────────────────────────
private fun resolveBaseColors(theme: String, isDark: Boolean): ReaderSurfaceColors {
    return when (theme.lowercase()) {
        "parchment", "羊皮纸" -> ReaderSurfaceColors(
            background = ParchmentColors.background,
            onBackground = ParchmentColors.onBackground,
            surface = ParchmentColors.surface,
            onSurface = ParchmentColors.onSurface,
            primary = ParchmentColors.primary,
            secondary = ParchmentColors.secondary,
            tertiary = ParchmentColors.tertiary,
            divider = ParchmentColors.primary.copy(alpha = 0.18f),
            selectionBackground = ParchmentColors.primary.copy(alpha = 0.30f),
            selectionText = ParchmentColors.onBackground,
            progressTrack = ParchmentColors.tertiary.copy(alpha = 0.35f),
            progressIndicator = ParchmentColors.primary,
        )
        "eyegreen", "护眼绿" -> ReaderSurfaceColors(
            background = EyeGreenColors.background,
            onBackground = EyeGreenColors.onBackground,
            surface = EyeGreenColors.surface,
            onSurface = EyeGreenColors.onSurface,
            primary = EyeGreenColors.primary,
            secondary = EyeGreenColors.secondary,
            tertiary = EyeGreenColors.tertiary,
            divider = EyeGreenColors.primary.copy(alpha = 0.18f),
            selectionBackground = EyeGreenColors.primary.copy(alpha = 0.30f),
            selectionText = EyeGreenColors.onBackground,
            progressTrack = EyeGreenColors.tertiary.copy(alpha = 0.35f),
            progressIndicator = EyeGreenColors.primary,
        )
        "inkblack", "水墨黑" -> ReaderSurfaceColors(
            background = InkBlackColors.background,
            onBackground = InkBlackColors.onBackground,
            surface = InkBlackColors.surface,
            onSurface = InkBlackColors.onSurface,
            primary = InkBlackColors.primary,
            secondary = InkBlackColors.secondary,
            tertiary = InkBlackColors.tertiary,
            divider = InkBlackColors.primary.copy(alpha = 0.18f),
            selectionBackground = InkBlackColors.primary.copy(alpha = 0.30f),
            selectionText = InkBlackColors.onBackground,
            progressTrack = InkBlackColors.tertiary.copy(alpha = 0.35f),
            progressIndicator = InkBlackColors.primary,
        )
        "nightblue", "深夜蓝" -> ReaderSurfaceColors(
            background = NightBlueColors.background,
            onBackground = NightBlueColors.onBackground,
            surface = NightBlueColors.surface,
            onSurface = NightBlueColors.onSurface,
            primary = NightBlueColors.primary,
            secondary = NightBlueColors.secondary,
            tertiary = NightBlueColors.tertiary,
            divider = NightBlueColors.primary.copy(alpha = 0.18f),
            selectionBackground = NightBlueColors.primary.copy(alpha = 0.30f),
            selectionText = NightBlueColors.onBackground,
            progressTrack = NightBlueColors.tertiary.copy(alpha = 0.35f),
            progressIndicator = NightBlueColors.primary,
        )
        else -> ReaderSurfaceColors(
            background = PaperColors.background,
            onBackground = PaperColors.onBackground,
            surface = PaperColors.surface,
            onSurface = PaperColors.onSurface,
            primary = PaperColors.primary,
            secondary = PaperColors.secondary,
            tertiary = PaperColors.tertiary,
            divider = PaperColors.primary.copy(alpha = 0.18f),
            selectionBackground = PaperColors.primary.copy(alpha = 0.30f),
            selectionText = PaperColors.onBackground,
            progressTrack = PaperColors.tertiary.copy(alpha = 0.35f),
            progressIndicator = PaperColors.primary,
        )
    }
}

/**
 * Apply warm color-temperature offset to the reader-surface palette.
 * @param colors base palette
 * @param temperature 0-100, where 100 shifts red channel up
 */
private fun applyColorTemperature(colors: ReaderSurfaceColors, temperature: Int): ReaderSurfaceColors {
    if (temperature <= 0) return colors
    val factor = (temperature / 100f).coerceIn(0f, 1f) * 0.25f // max 25 % red boost

    fun warmShift(c: Color): Color {
        val r = (c.red + factor).coerceAtMost(1f)
        return Color(r, c.green, c.blue, c.alpha)
    }

    return colors.copy(
        background = warmShift(colors.background),
        surface = warmShift(colors.surface),
        primary = warmShift(colors.primary),
        secondary = warmShift(colors.secondary),
        tertiary = warmShift(colors.tertiary),
    )
}

/**
 * Apply dim-level overlay to the reader-surface palette.
 * @param colors base palette
 * @param dimLevel 0-100, where 100 reduces luminance by ~50 %
 */
private fun applyDimLevel(colors: ReaderSurfaceColors, dimLevel: Int): ReaderSurfaceColors {
    if (dimLevel <= 0) return colors
    val factor = (dimLevel / 100f).coerceIn(0f, 1f) * 0.50f

    fun dim(c: Color): Color {
        return Color(
            red = c.red * (1f - factor),
            green = c.green * (1f - factor),
            blue = c.blue * (1f - factor),
            alpha = c.alpha,
        )
    }

    return colors.copy(
        background = dim(colors.background),
        surface = dim(colors.surface),
        divider = dim(colors.divider),
        selectionBackground = dim(colors.selectionBackground),
        progressTrack = dim(colors.progressTrack),
        progressIndicator = dim(colors.progressIndicator),
    )
}

// ─── Public theme Composable ─────────────────────────────────────────

/**
 * NovelTheme — 小说阅读器主题包装器。
 *
 * 接收 [ReaderPrefs] 并综合应用以下参数：
 * - theme: 皮肤选择（paper / parchment / eyeGreen / inkBlack / nightBlue）
 * - dimLevel: 亮度调节 (0-100)
 * - colorTemperature: 暖色温调节 (0-100)
 * - fontSize / lineHeight / paragraphSpacing / pagePadding 通过
 *   [LocalReaderPrefs] 向下传递，由各界面自行使用。
 *
 * 界面 Chrome（设置页、书架等）使用 Material3 color scheme，
 * 阅读区域使用 [LocalReaderColors] 中的调色板。
 */
@Composable
fun NovelTheme(
    prefs: ReaderPrefs,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val isDark = when (prefs.theme.lowercase()) {
        "inkblack", "inkblack", "水墨黑",
        "nightblue", "nightblue", "深夜蓝" -> true
        else -> darkTheme
    }

    val chromeScheme = if (isDark) DarkChromeScheme else LightChromeScheme

    // Build reader-surface colors via pipeline
    val readerColors = remember(prefs.theme, prefs.colorTemperature, prefs.dimLevel) {
        val base = resolveBaseColors(prefs.theme, isDark)
        val warmed = applyColorTemperature(base, prefs.colorTemperature)
        applyDimLevel(warmed, prefs.dimLevel)
    }

    // Derive reader chrome from current background luminance
    val readerBackground = readerColors.background
    val readerIsDark = readerBackground.luminance() < 0.5f
    val effectiveChrome = if (readerIsDark) DarkChromeScheme else LightChromeScheme

    CompositionLocalProvider(
        LocalReaderColors provides readerColors,
        LocalReaderPrefs provides prefs,
    ) {
        MaterialTheme(
            colorScheme = effectiveChrome,
            content = content,
        )
    }
}

/**
 * CompositionLocal carrying current [ReaderPrefs] through the tree.
 * Screens use it for font-size, spacing, etc.
 */
val LocalReaderPrefs = compositionLocalOf {
    ReaderPrefs()
}

/**
 * Helper: resolve a density-independent pixel value from sp.
 */
val ReaderPrefs.fontSizeSp: Float get() = fontSize.toFloat().coerceIn(14f, 30f)

val ReaderPrefs.lineHeightFloat: Float get() = lineHeight.coerceIn(1.2f, 2.5f)

val ReaderPrefs.paragraphSpacingDp: Int get() = paragraphSpacing.coerceIn(4, 24)

val ReaderPrefs.pagePaddingDp: Int get() = pagePadding.coerceIn(8, 48)

/** Display-friendly names for each skin. */
val ReaderPrefs.themeDisplayName: String
    get() = when (theme.lowercase()) {
        "parchment", "羊皮纸" -> "羊皮纸"
        "eyegreen", "eyegreen", "eyegreen", "护眼绿" -> "护眼绿"
        "inkblack", "inkblack", "水墨黑" -> "水墨黑"
        "nightblue", "nightblue", "深夜蓝" -> "深夜蓝"
        else -> "纸白"
    }

/** All available skin keys. */
val allThemeKeys = listOf("paper", "parchment", "eyeGreen", "inkBlack", "nightBlue")

/** All available skin display names. */
val allThemeNames = listOf("纸白", "羊皮纸", "护眼绿", "水墨黑", "深夜蓝")
