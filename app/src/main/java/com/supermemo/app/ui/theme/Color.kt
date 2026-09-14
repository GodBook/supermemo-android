package com.supermemo.app.ui.theme

import androidx.compose.ui.graphics.Color

// Primary M3 Palette
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6750A4)
val PurpleGrey40 = Color(0xFF625B71)
val Pink40 = Color(0xFF7D5260)

// AMOLED Pure Black Colors
val AmoledBackground = Color(0xFF000000)
val AmoledSurface = Color(0xFF121212)
val AmoledSurfaceVariant = Color(0xFF1E1E1E)

// 预设备忘录卡片高质感底色
val NoteCardColors = listOf(
    "#FFFFFF", // 默认纯白 / 适配暗色
    "#FFF0F5", // 柔粉
    "#FFF8DC", // 浅金黄
    "#F0FFF0", // 蜜瓜绿
    "#F0F8FF", // 爱丽丝蓝
    "#E6E6FA", // 薰衣草紫
    "#FFF5EE", // 海贝色
    "#F5F5DC"  // 米色
)

fun parseHexColor(hex: String?, defaultColor: Color): Color {
    if (hex.isNullOrBlank()) return defaultColor
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        defaultColor
    }
}
