package app.feargreed

import android.content.Context
import java.time.Instant

enum class WidgetStatus { OK, LOADING, BLOCKED, ERROR }

class WidgetStore(context: Context) {
    private val prefs = context.getSharedPreferences("fear_greed", Context.MODE_PRIVATE)

    var status: WidgetStatus
        get() = WidgetStatus.valueOf(prefs.getString("status", WidgetStatus.OK.name)!!)
        set(value) = prefs.edit().putString("status", value.name).apply()

    var lastAttempt: Instant?
        get() = instantOrNull("last_attempt")
        set(value) = prefs.edit().putLong("last_attempt", value?.toEpochMilli() ?: 0L).apply()

    var blockedUntil: Instant?
        get() = instantOrNull("blocked_until")
        set(value) = prefs.edit().putLong("blocked_until", value?.toEpochMilli() ?: 0L).apply()

    var snapshot: Snapshot?
        get() {
            if (!prefs.contains("score")) return null
            return Snapshot(
                score = prefs.getFloat("score", 0f).toDouble(),
                rating = prefs.getString("rating", "").orEmpty(),
                previousClose = if (prefs.contains("previous_close")) prefs.getFloat("previous_close", 0f).toDouble() else null,
                updated = Instant.ofEpochMilli(prefs.getLong("updated", 0L)),
            )
        }
        set(value) {
            val editor = prefs.edit()
            if (value == null) {
                editor.remove("score").remove("rating").remove("previous_close").remove("updated")
            } else {
                editor.putFloat("score", value.score.toFloat())
                    .putString("rating", value.rating)
                    .putLong("updated", value.updated.toEpochMilli())
                if (value.previousClose != null) {
                    editor.putFloat("previous_close", value.previousClose.toFloat())
                } else {
                    editor.remove("previous_close")
                }
            }
            editor.apply()
        }

    // Before v0.5.0 the light/dark choice was stored under "theme_<id>".
    fun style(widgetId: Int): WidgetStyle {
        val savedMode = prefs.getString("mode_$widgetId", null) ?: prefs.getString("theme_$widgetId", null)
        val mode = WidgetMode.entries.firstOrNull { it.name == savedMode } ?: WidgetMode.SYSTEM
        val savedTheme = prefs.getString("color_theme_$widgetId", null)
        val theme = WidgetTheme.entries.firstOrNull { it.name == savedTheme } ?: WidgetTheme.DEFAULT
        return WidgetStyle(mode, theme, prefs.getInt("transparency_$widgetId", 0))
    }

    fun saveStyle(widgetId: Int, style: WidgetStyle) {
        prefs.edit()
            .remove("theme_$widgetId")
            .putString("mode_$widgetId", style.mode.name)
            .putString("color_theme_$widgetId", style.theme.name)
            .putInt("transparency_$widgetId", style.transparencyPercent)
            .apply()
    }

    fun deleteStyle(widgetId: Int) {
        prefs.edit()
            .remove("theme_$widgetId")
            .remove("mode_$widgetId")
            .remove("color_theme_$widgetId")
            .remove("transparency_$widgetId")
            .apply()
    }

    private fun instantOrNull(key: String): Instant? =
        prefs.getLong(key, 0L).takeIf { it > 0L }?.let(Instant::ofEpochMilli)
}
