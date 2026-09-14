package com.supermemo.app.util

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ChecklistBlinkSettings(
    val isEnabled: Boolean = true,
    val colorHex: String = "#FF9800"
)

object PreferenceManager {

    private const val PREFS_NAME = "super_memo_prefs"
    private const val KEY_BLINK_ENABLED = "checklist_blink_enabled"
    private const val KEY_BLINK_COLOR = "checklist_blink_color"

    val PRESET_BLINK_COLORS = listOf(
        "#FF9800", // 活力橙
        "#FF5252", // 热情红
        "#00E676", // 荧光绿
        "#00B0FF", // 电光蓝
        "#E040FB", // 霓虹紫
        "#FFD600"  // 亮丽黄
    )

    private lateinit var prefs: SharedPreferences
    private val _settingsFlow = MutableStateFlow(ChecklistBlinkSettings())
    val settingsFlow: StateFlow<ChecklistBlinkSettings> = _settingsFlow.asStateFlow()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean(KEY_BLINK_ENABLED, true)
        val colorHex = prefs.getString(KEY_BLINK_COLOR, "#FF9800") ?: "#FF9800"
        _settingsFlow.value = ChecklistBlinkSettings(isEnabled, colorHex)
    }

    fun setBlinkEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BLINK_ENABLED, enabled).apply()
        _settingsFlow.value = _settingsFlow.value.copy(isEnabled = enabled)
    }

    fun setBlinkColor(colorHex: String) {
        prefs.edit().putString(KEY_BLINK_COLOR, colorHex).apply()
        _settingsFlow.value = _settingsFlow.value.copy(colorHex = colorHex)
    }
}
