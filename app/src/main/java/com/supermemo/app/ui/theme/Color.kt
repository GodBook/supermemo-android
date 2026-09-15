package com.supermemo.app.ui.theme

import androidx.compose.ui.graphics.Color

// 1.1.5 全新 Ocean & Sky 晶莹玻璃拟态质感调色板 (与全新图标高度呼应)
val SkyPrimary = Color(0xFF0284C7)         // Sky 600 蔚蓝主色
val SkyOnPrimary = Color(0xFFFFFFFF)
val SkyPrimaryContainer = Color(0xFFE0F2FE) // 晶莹浅冰青
val SkyOnPrimaryContainer = Color(0xFF0369A1)

val SkySecondary = Color(0xFF0D9488)       // Ocean Teal 600 清爽青绿
val SkyOnSecondary = Color(0xFFFFFFFF)
val SkySecondaryContainer = Color(0xFFCCFBF1)
val SkyOnSecondaryContainer = Color(0xFF115E59)

val SkyTertiary = Color(0xFF0284C7)
val SkyOnTertiary = Color(0xFFFFFFFF)
val SkyTertiaryContainer = Color(0xFFE0F7FA)

val SkyBackground = Color(0xFFF8FAFC)      // Slate 50 柔和云白底色
val SkySurface = Color(0xFFFFFFFF)
val SkySurfaceVariant = Color(0xFFF1F5F9)  // Slate 100 质感微灰
val SkyOutline = Color(0xFF94A3B8)
val SkyOutlineVariant = Color(0xFFE2E8F0)

// 暗色模式 (Luminous Deep Slate Navy)
val SkyDarkPrimary = Color(0xFF38BDF8)     // Sky 400 发光流青
val SkyDarkOnPrimary = Color(0xFF00354E)
val SkyDarkPrimaryContainer = Color(0xFF0369A1)
val SkyDarkOnPrimaryContainer = Color(0xFFBAE6FD)

val SkyDarkSecondary = Color(0xFF2DD4BF)   // Luminous Mint Teal
val SkyDarkOnSecondary = Color(0xFF003731)
val SkyDarkSecondaryContainer = Color(0xFF134E4A)
val SkyDarkOnSecondaryContainer = Color(0xFF99F6E4)

val SkyDarkTertiary = Color(0xFF67E8F9)
val SkyDarkOnTertiary = Color(0xFF00363D)
val SkyDarkTertiaryContainer = Color(0xFF0E7490)

val SkyDarkBackground = Color(0xFF0B1320)  // 深邃子夜黑青底色
val SkyDarkSurface = Color(0xFF131D2E)     // 沉浸卡片底色
val SkyDarkSurfaceVariant = Color(0xFF1E293B)
val SkyDarkOutline = Color(0xFF64748B)
val SkyDarkOutlineVariant = Color(0xFF334155)

// AMOLED Pure Black Colors
val AmoledBackground = Color(0xFF000000)
val AmoledSurface = Color(0xFF090E17)
val AmoledSurfaceVariant = Color(0xFF141B26)

// 预设备忘录卡片高质感自然底色 (扩充自然灵动色系)
val NoteCardColors = listOf(
    "#FFFFFF", // 默认纯白 / 适配暗色
    "#F0F9FF", // 晶莹蔚蓝 (Crystal Azure)
    "#F0FDFA", // 清透薄荷 (Mint Aqua)
    "#FFF7ED", // 暖阳初杏 (Warm Peach)
    "#FFF1F2", // 晨曦柔粉 (Morning Blossom)
    "#FEFCE8", // 柔光浅黄 (Soft Lemon)
    "#F5F3FF", // 幽兰淡紫 (Lavender Glow)
    "#F8FAFC"  // 极简冷灰 (Slate Minimal)
)

fun parseHexColor(hex: String?, defaultColor: Color): Color {
    if (hex.isNullOrBlank()) return defaultColor
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        defaultColor
    }
}
