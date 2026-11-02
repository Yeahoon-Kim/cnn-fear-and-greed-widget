package app.feargreed

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.format.DateFormat
import android.util.SizeF
import android.widget.RemoteViews
import java.util.Date
import kotlin.math.roundToInt

class FearGreedWidget : AppWidgetProvider() {

    override fun onEnabled(context: Context) {
        RefreshWorker.refreshNow(context, force = false)
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        RefreshWorker.schedule(context)
        ids.forEach { manager.updateAppWidget(it, buildViews(context, manager, it)) }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        manager: AppWidgetManager,
        id: Int,
        options: Bundle,
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            manager.updateAppWidget(id, buildViews(context, manager, id))
        }
    }

    override fun onDeleted(context: Context, ids: IntArray) {
        val store = WidgetStore(context)
        ids.forEach(store::deleteStyle)
    }

    companion object {
        private const val FULL_MIN_WIDTH_DP = 120
        private const val FULL_MIN_HEIGHT_DP = 130

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, FearGreedWidget::class.java))
            ids.forEach { manager.updateAppWidget(it, buildViews(context, manager, it)) }
        }

        fun update(context: Context, id: Int) {
            val manager = AppWidgetManager.getInstance(context)
            manager.updateAppWidget(id, buildViews(context, manager, id))
        }

        private fun buildViews(context: Context, manager: AppWidgetManager, id: Int): RemoteViews {
            val store = WidgetStore(context)
            val style = store.style(id)
            val compact = compactViews(context, store, style).also { setClick(context, it, id) }
            val full = fullViews(context, store, style).also { setClick(context, it, id) }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                return RemoteViews(
                    mapOf(
                        SizeF(1f, 1f) to compact,
                        SizeF(FULL_MIN_WIDTH_DP.toFloat(), FULL_MIN_HEIGHT_DP.toFloat()) to full,
                    ),
                )
            }
            val options = manager.getAppWidgetOptions(id)
            val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
            val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
            val isCompact = width in 1 until FULL_MIN_WIDTH_DP || height in 1 until FULL_MIN_HEIGHT_DP
            return if (isCompact) compact else full
        }

        fun compactViews(context: Context, store: WidgetStore, style: WidgetStyle): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_compact)
            val snapshot = store.snapshot
            val rating = snapshot?.let { Rating.of(it.score) }
            val light = Palettes.compact(style.theme, dark = style.mode == WidgetMode.DARK, rating = rating)
            val dark = Palettes.compact(style.theme, dark = style.mode != WidgetMode.LIGHT, rating = rating)
            applyBackground(context, views, style, light.background, dark.background)
            setColor(context, views, R.id.score, "setTextColor", light.number, dark.number)
            setColor(context, views, R.id.rating, "setTextColor", light.label, dark.label)
            if (snapshot != null && rating != null) {
                views.setTextViewText(R.id.score, snapshot.score.roundToInt().toString())
                val label = context.getString(rating.label)
                views.setTextViewText(
                    R.id.rating,
                    if (isStale(store)) context.getString(R.string.compact_stale_format, label) else label,
                )
            } else {
                views.setTextViewText(R.id.score, context.getString(R.string.no_value))
                views.setTextViewText(R.id.rating, statusText(context, store, null))
            }
            return views
        }

        fun fullViews(context: Context, store: WidgetStore, style: WidgetStyle): RemoteViews {
            val snapshot = store.snapshot
            val views = RemoteViews(context.packageName, R.layout.widget)
            val light = Palettes.standard(dark = style.mode == WidgetMode.DARK, rating = null)
            val dark = Palettes.standard(dark = style.mode != WidgetMode.LIGHT, rating = null)
            applyBackground(context, views, style, light.background, dark.background)
            listOf(R.id.score, R.id.rating).forEach {
                setColor(context, views, it, "setTextColor", light.primaryText, dark.primaryText)
            }
            listOf(R.id.delta, R.id.status).forEach {
                setColor(context, views, it, "setTextColor", light.secondaryText, dark.secondaryText)
            }

            views.setImageViewBitmap(R.id.gauge, GaugeRenderer.render(snapshot?.score))
            if (snapshot != null) {
                val rating = Rating.of(snapshot.score)
                views.setTextViewText(R.id.score, snapshot.score.roundToInt().toString())
                views.setTextViewText(R.id.rating, context.getString(rating.label))
                views.setTextColor(R.id.rating, rating.color)
            } else {
                views.setTextViewText(R.id.score, context.getString(R.string.no_value))
                views.setTextViewText(R.id.rating, "")
            }
            views.setTextViewText(R.id.delta, snapshot?.let { deltaText(context, it) }.orEmpty())
            views.setTextViewText(R.id.status, statusText(context, store, snapshot))
            return views
        }

        private fun applyBackground(
            context: Context,
            views: RemoteViews,
            style: WidgetStyle,
            light: Int,
            dark: Int,
        ) {
            views.setInt(R.id.widget_background, "setImageAlpha", style.backgroundAlpha)
            setColor(context, views, R.id.widget_background, "setColorFilter", light, dark)
        }

        // Android 12+ picks the light or dark color when the widget is drawn, so a
        // system mode switch needs no re-render; older versions use the current mode.
        private fun setColor(
            context: Context,
            views: RemoteViews,
            viewId: Int,
            method: String,
            light: Int,
            dark: Int,
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                views.setColorInt(viewId, method, light, dark)
            } else {
                val night = context.resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
                views.setInt(viewId, method, if (night) dark else light)
            }
        }

        private fun setClick(context: Context, views: RemoteViews, id: Int) {
            val intent = Intent(context, WidgetSettingsActivity::class.java)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                .setData(Uri.parse("feargreed://widget/$id"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            val pending = PendingIntent.getActivity(
                context,
                id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_root, pending)
        }

        private fun isStale(store: WidgetStore): Boolean =
            store.status == WidgetStatus.BLOCKED || store.status == WidgetStatus.ERROR

        private fun deltaText(context: Context, snapshot: Snapshot): String {
            val previous = snapshot.previousClose ?: return ""
            return context.getString(R.string.delta_format, previous, snapshot.score - previous)
        }

        fun lastCheckText(context: Context, store: WidgetStore): String {
            val attempt = store.lastAttempt ?: return context.getString(R.string.last_check_never)
            val result = when (store.status) {
                WidgetStatus.OK -> context.getString(R.string.check_ok)
                WidgetStatus.LOADING -> context.getString(R.string.status_loading)
                WidgetStatus.BLOCKED -> context.getString(R.string.status_blocked)
                WidgetStatus.ERROR -> context.getString(R.string.status_error)
            }
            val time = DateFormat.getTimeFormat(context).format(Date(attempt.toEpochMilli()))
            return context.getString(R.string.last_check_format, time, result)
        }

        private fun statusText(context: Context, store: WidgetStore, snapshot: Snapshot?): String {
            val updated = snapshot?.let { context.getString(R.string.updated_format, formatTime(context, it)) }
            val problem = when (store.status) {
                WidgetStatus.OK -> null
                WidgetStatus.LOADING -> context.getString(R.string.status_loading)
                WidgetStatus.BLOCKED -> context.getString(R.string.status_blocked)
                WidgetStatus.ERROR -> context.getString(R.string.status_error)
            }
            return when {
                problem != null && updated != null -> context.getString(R.string.status_stale_format, problem, updated)
                problem != null -> problem
                updated != null -> updated
                else -> context.getString(R.string.status_loading)
            }
        }

        fun formatTime(context: Context, snapshot: Snapshot): String {
            val date = Date(snapshot.updated.toEpochMilli())
            return "${DateFormat.format("M/d", date)} ${DateFormat.getTimeFormat(context).format(date)}"
        }
    }
}
