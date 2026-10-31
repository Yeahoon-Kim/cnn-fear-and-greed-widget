package app.feargreed

import android.graphics.Color

enum class WidgetTheme(
    val background: Int?,
    val primaryText: Int?,
    val secondaryText: Int?,
) {
    SYSTEM(null, null, null),
    LIGHT(Color.parseColor("#F7F8FA"), Color.parseColor("#1B1F27"), Color.parseColor("#5F6673")),
    DARK(Color.parseColor("#20242C"), Color.parseColor("#F2F4F7"), Color.parseColor("#A7AEBB")),
}

data class WidgetStyle(
    val theme: WidgetTheme = WidgetTheme.SYSTEM,
    val transparencyPercent: Int = 0,
) {
    val backgroundAlpha: Int
        get() = (100 - transparencyPercent.coerceIn(0, 100)) * 255 / 100
}
