package com.example.personalmusicplayer.ui.theme

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.example.personalmusicplayer.R

enum class ThemeGroup(@StringRes val titleRes: Int) {
    WHITE(R.string.settings_theme_white),
    BLACK(R.string.settings_theme_black),
}

private val LightText = Color(0xFF1C1B1F)
private val LightMuted = Color(0xFF6B6870)
private val DarkText = Color(0xFFF2EEF4)
private val DarkMuted = Color(0xFFA9A6B0)

/**
 * Темы: основной цвет, акцент, фон, карточки. Имена ORANGE/GREEN/BLUE сохранены,
 * поэтому ранее выбранная тема не сбрасывается. Порядок объявления = порядок в настройках.
 */
enum class AppTheme(
    val group: ThemeGroup,
    @StringRes val labelRes: Int,
    val primary: Color,
    val accent: Color,
    val background: Color,
    val card: Color,
    val onPrimary: Color = Color.White,
) {
    // ---- Белые ----
    ORANGE(ThemeGroup.WHITE, R.string.theme_orange, Color(0xFFF26B00), Color(0xFFFFA24D), Color(0xFFFFFBF7), Color(0xFFFFEEDD)),
    GREEN(ThemeGroup.WHITE, R.string.theme_green, Color(0xFF1E9E55), Color(0xFF7ED9A0), Color(0xFFF8FFF9), Color(0xFFE3F5E9)),
    BLUE(ThemeGroup.WHITE, R.string.theme_blue, Color(0xFF1E88E5), Color(0xFF79BFFF), Color(0xFFF7FBFF), Color(0xFFE1EFFC)),
    RED(ThemeGroup.WHITE, R.string.theme_red, Color(0xFFE53935), Color(0xFFFF8A80), Color(0xFFFFF9F8), Color(0xFFFDE6E4)),
    PURPLE(ThemeGroup.WHITE, R.string.theme_purple, Color(0xFF8A4FD8), Color(0xFFC3A1F2), Color(0xFFFBF8FF), Color(0xFFEEE5FA)),
    PINK(ThemeGroup.WHITE, R.string.theme_pink, Color(0xFFE8408A), Color(0xFFFF9DC4), Color(0xFFFFF8FB), Color(0xFFFCE4EE)),

    // ---- Чёрные ----
    BLACK_WHITE(ThemeGroup.BLACK, R.string.theme_black_white, Color(0xFFFFFFFF), Color(0xFFBDBDBD), Color(0xFF0A0A0A), Color(0xFF2B2B2B), onPrimary = Color(0xFF000000)),
    BLACK_ORANGE(ThemeGroup.BLACK, R.string.theme_black_orange, Color(0xFFF26B00), Color(0xFFFFA24D), Color(0xFF120D09), Color(0xFF3A2616)),
    BLACK_GREEN(ThemeGroup.BLACK, R.string.theme_black_green, Color(0xFF1E9E55), Color(0xFF7ED9A0), Color(0xFF0A120D), Color(0xFF183323)),
    BLACK_BLUE(ThemeGroup.BLACK, R.string.theme_black_blue, Color(0xFF1E88E5), Color(0xFF79BFFF), Color(0xFF0A1017), Color(0xFF18314A)),
    BLACK_RED(ThemeGroup.BLACK, R.string.theme_black_red, Color(0xFFE53935), Color(0xFFFF8A80), Color(0xFF130B0B), Color(0xFF3D1A19)),
    BLACK_PURPLE(ThemeGroup.BLACK, R.string.theme_black_purple, Color(0xFFA06CF0), Color(0xFFC3A1F2), Color(0xFF0F0B16), Color(0xFF2D2145)),
    BLACK_PINK(ThemeGroup.BLACK, R.string.theme_black_pink, Color(0xFFE8408A), Color(0xFFFF9DC4), Color(0xFF14090F), Color(0xFF3F1A2C));

    val isDark: Boolean get() = group == ThemeGroup.BLACK
    val text: Color get() = if (isDark) DarkText else LightText
    val textMuted: Color get() = if (isDark) DarkMuted else LightMuted
}
