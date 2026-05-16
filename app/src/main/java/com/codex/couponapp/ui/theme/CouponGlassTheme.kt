package com.polaralias.coupio.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val CouponColorScheme = lightColorScheme(
    primary = Color(0xFF3D7462),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1F2E8),
    onPrimaryContainer = Color(0xFF123B2F),
    secondary = Color(0xFF5B7A9A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD8E7F5),
    background = Color(0xFFF4F7FB),
    onBackground = Color(0xFF1C2833),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C2833),
    surfaceVariant = Color(0xFFE8EFF7),
    outline = Color(0x66FFFFFF),
)

private val CouponTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
)

@Composable
fun CouponGlassTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CouponColorScheme,
        typography = CouponTypography,
        shapes = Shapes(),
        content = content,
    )
}
