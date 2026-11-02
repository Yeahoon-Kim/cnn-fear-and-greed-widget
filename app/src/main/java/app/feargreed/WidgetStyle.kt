package app.feargreed

import android.graphics.Color

/** Which light/dark appearance a widget uses. */
enum class WidgetMode { SYSTEM, LIGHT, DARK }

/** Color style; the point and full color themes only change the compact layout. */
enum class WidgetTheme { DEFAULT, POINT, FULL_COLOR }

data class WidgetStyle(
    val mode: WidgetMode = WidgetMode.SYSTEM,
    val theme: WidgetTheme = WidgetTheme.DEFAULT,
    val transparencyPercent: Int = 0,
) {
    val backgroundAlpha: Int
        get() = (100 - transparencyPercent.coerceIn(0, 100)) * 255 / 100
}

/** Colors for one widget surface in one of light or dark appearance. */
data class Palette(
    val background: Int,
    val number: Int,
    val label: Int,
    val primaryText: Int,
    val secondaryText: Int,
)

object Palettes {
    private val LIGHT_BACKGROUND = Color.parseColor("#F7F8FA")
    private val DARK_BACKGROUND = Color.parseColor("#20242C")
    private val INK = Color.parseColor("#1B1F27")
    private val INK_SOFT = Color.parseColor("#D91B1F27")
    private val WHITE_SOFT = Color.parseColor("#E6FFFFFF")

    fun compact(theme: WidgetTheme, dark: Boolean, rating: Rating?): Palette {
        val levelColor = rating?.color ?: return standard(dark, null)
        return when {
            theme == WidgetTheme.DEFAULT -> standard(dark, rating)
            !dark -> Palette(levelColor, Color.WHITE, WHITE_SOFT, Color.WHITE, WHITE_SOFT)
            theme == WidgetTheme.POINT -> standard(true, rating)
            else -> Palette(levelColor, INK, INK_SOFT, INK, INK_SOFT)
        }
    }

    fun standard(dark: Boolean, rating: Rating?): Palette {
        val primary = if (dark) Color.parseColor("#F2F4F7") else INK
        val secondary = if (dark) Color.parseColor("#A7AEBB") else Color.parseColor("#5F6673")
        return Palette(
            background = if (dark) DARK_BACKGROUND else LIGHT_BACKGROUND,
            number = rating?.color ?: primary,
            label = secondary,
            primaryText = primary,
            secondaryText = secondary,
        )
    }
}
