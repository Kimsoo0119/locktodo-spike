package com.example.locktodo.ui

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** design-tokens.md(R2 확정)를 코드로. 컴포넌트는 LocalAppTheme.current 만 읽는다(when(theme) 금지). */
enum class ThemeId { A, B, C }

data class AppTheme(
    val id: ThemeId,
    val displayName: String,
    val bg: Brush,
    val bgLock: Brush,
    val cardFill: Color,
    val cardBorder: Color?,
    val text: Color,
    val mutedStrong: Color,
    val mutedWeak: Color,
    val accent: Color,
    val accentPressed: Color,
    val onAccent: Color,
    val ripple: Color,
    val destructive: Color,
    val destructiveConfirm: Color,
    val divider: Color?,
    val checkboxShape: Shape,
    val cardShape: Shape,
    val fieldShape: Shape,
    val clockWeight: FontWeight,
    val itemWeight: FontWeight,
    val dateWeight: FontWeight,
)

private fun grad(vararg c: Long) = Brush.linearGradient(c.map { Color(it) })

val ThemeA = AppTheme(
    id = ThemeId.A, displayName = "다크",
    bg = grad(0xFF2B1F54, 0xFF1A1438, 0xFF0D0A1F),
    bgLock = grad(0xFF2B1F54, 0xFF1A1438, 0xFF0D0A1F),
    cardFill = Color(0xFF332B4F),               // 불투명 등가(권장)
    cardBorder = Color.White.copy(alpha = 0.16f),
    text = Color.White,
    mutedStrong = Color(0xFFB8B2D8),
    mutedWeak = Color(0xFF8A85A8),
    accent = Color(0xFF7C6CFF),
    accentPressed = Color(0xFF6A5AE0),
    onAccent = Color.White,
    ripple = Color.White.copy(alpha = 0.12f),
    destructive = Color.White.copy(alpha = 0.45f),
    destructiveConfirm = Color(0xFFFF6B6B),
    divider = null,
    checkboxShape = RoundedCornerShape(6.dp),
    cardShape = RoundedCornerShape(24.dp),
    fieldShape = RoundedCornerShape(14.dp),
    clockWeight = FontWeight.Light, itemWeight = FontWeight.Normal, dateWeight = FontWeight.Normal,
)

val ThemeB = AppTheme(
    id = ThemeId.B, displayName = "라이트",
    bg = SolidColor(Color(0xFFF4F5F7)),
    bgLock = SolidColor(Color(0xFFECEEF1)),     // 야간 눈부심 −4%
    cardFill = Color.White,
    cardBorder = null,
    text = Color(0xFF23262E),
    mutedStrong = Color(0xFF9AA0AD),
    mutedWeak = Color(0xFFB6BAC3),
    accent = Color(0xFF4F6BED),
    accentPressed = Color(0xFF3F58D8),
    onAccent = Color.White,
    ripple = Color(0xFF4F6BED).copy(alpha = 0.12f),
    destructive = Color(0xFF9AA0AD),
    destructiveConfirm = Color(0xFFE5484D),
    divider = Color(0xFFF0F1F4),
    checkboxShape = CircleShape,
    cardShape = RoundedCornerShape(22.dp),
    fieldShape = RoundedCornerShape(12.dp),
    clockWeight = FontWeight.SemiBold, itemWeight = FontWeight.Medium, dateWeight = FontWeight.Medium,
)

val ThemeC = AppTheme(
    id = ThemeId.C, displayName = "따뜻",
    bg = grad(0xFFFFE9D6, 0xFFFFD9C2, 0xFFFFC9BE),
    bgLock = grad(0xFFFFE9D6, 0xFFFFD9C2, 0xFFFFC9BE),
    cardFill = Color(0xFFFFF6EF),               // 불투명 등가
    cardBorder = Color.White.copy(alpha = 0.55f),
    text = Color(0xFF5B3A2E),
    mutedStrong = Color(0xFFB07D63),
    mutedWeak = Color(0xFFC9A392),
    accent = Color(0xFFFF8A5C),
    accentPressed = Color(0xFFF2754A),
    onAccent = Color(0xFF5B3A2E),               // 코랄 위 갈색 ✓ 4.33:1
    ripple = Color(0xFFFF8A5C).copy(alpha = 0.14f),
    destructive = Color(0xFFC9A392),
    destructiveConfirm = Color(0xFFE5484D),
    divider = null,
    checkboxShape = RoundedCornerShape(12.dp),
    cardShape = RoundedCornerShape(26.dp),
    fieldShape = RoundedCornerShape(16.dp),
    clockWeight = FontWeight.Bold, itemWeight = FontWeight.SemiBold, dateWeight = FontWeight.SemiBold,
)

val ALL_THEMES = listOf(ThemeA, ThemeB, ThemeC)
fun themeById(id: ThemeId) = ALL_THEMES.first { it.id == id }

val LocalAppTheme = compositionLocalOf { ThemeA }
