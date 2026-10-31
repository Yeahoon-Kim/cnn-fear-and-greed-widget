package app.feargreed

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
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
            applyStyle(views, style, primary = listOf(R.id.score), secondary = listOf(R.id.rating))
            val snapshot = store.snapshot
            if (snapshot != null) {
                val rating = Rating.of(snapshot.score)
                views.setTextViewText(R.id.score, snapshot.score.roundToInt().toString())
                views.setTextColor(R.id.score, rating.color)
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
            applyStyle(
                views,
                style,
                primary = listOf(R.id.score, R.id.rating),
                secondary = listOf(R.id.delta, R.id.status),
            )

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

        // The system theme relies on the night-qualified colors in the layouts.
        private fun applyStyle(
            views: RemoteViews,
            style: WidgetStyle,
            primary: List<Int>,
            secondary: List<Int>,
        ) {
            views.setInt(R.id.widget_background, "setImageAlpha", style.backgroundAlpha)
            style.theme.background?.let { views.setInt(R.id.widget_background, "setColorFilter", it) }
            style.theme.primaryText?.let { color -> primary.forEach { views.setTextColor(it, color) } }
            style.theme.secondaryText?.let { color -> secondary.forEach { views.setTextColor(it, color) } }
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
