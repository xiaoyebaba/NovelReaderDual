package com.richardyap.novelreader.models

/**
 * 阅读偏好设置（用户可调的阅读体验参数）。
 */
data class ReaderPrefs(
    val fontSize: Int = 20,
    val lineHeight: Float = 1.75f,
    val paragraphSpacing: Int = 14,
    val pagePadding: Int = 22,
    val theme: String = "paper",
    val pageMode: String = "scroll",
    val dimLevel: Int = 0,
    val colorTemperature: Int = 0,
    val customFontPath: String = "",
    val customBackgroundPath: String = "",
)
