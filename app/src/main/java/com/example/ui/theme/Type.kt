package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val AppSans = FontFamily.SansSerif

val Typography = Typography(
    displaySmall = TextStyle(
        fontFamily = AppSans,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.3).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = AppSans,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.2).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = AppSans,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 34.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = AppSans,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 30.sp
    ),
    titleLarge = TextStyle(
        fontFamily = AppSans,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = AppSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    titleSmall = TextStyle(
        fontFamily = AppSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = AppSans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = AppSans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    ),
    bodySmall = TextStyle(
        fontFamily = AppSans,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp
    ),
    labelLarge = TextStyle(
        fontFamily = AppSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    labelMedium = TextStyle(
        fontFamily = AppSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = AppSans,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    )
)
