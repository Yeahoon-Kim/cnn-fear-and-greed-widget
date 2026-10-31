package app.feargreed

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.format.DateFormat
import android.widget.RemoteViews
import java.util.Date
import kotlin.math.roundToInt

class FearGreedWidget : AppWidgetProvider() {

    override fun onEnabled(context: Context) {
        RefreshWorker.refreshNow(context, force = false)
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        RefreshWorker.schedule(context)
        val views = buildViews(context)
        ids.forEach { manager.updateAppWidget(it, views) }
    }

    companion object {
        private const val CNN_URL = "https://edition.cnn.com/markets/fear-and-greed"

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, FearGreedWidget::class.java))
            if (ids.isEmpty()) return
            val views = buildViews(context)
            ids.forEach { manager.updateAppWidget(it, views) }
        }

        private fun buildViews(context: Context): RemoteViews {
            val store = WidgetStore(context)
            val snapshot = store.snapshot
            val hasKey = store.apiKey.isNotBlank()
            val views = RemoteViews(context.packageName, R.layout.widget)

            views.setImageViewBitmap(R.id.gauge, GaugeRenderer.render(snapshot?.score))
            if (snapshot != null) {
                val rating = Rating.of(snapshot.score)
                views.setTextViewText(R.id.score, snapshot.score.roundToInt().toString())
                views.setTextViewText(R.id.rating, context.getString(rating.label))
                views.setTextColor(R.id.rating, rating.color)
                views.setTextViewText(R.id.delta, deltaText(context, snapshot))
            } else {
                views.setTextViewText(R.id.score, context.getString(R.string.no_value))
                views.setTextViewText(R.id.rating, "")
                views.setTextViewText(R.id.delta, "")
            }
            views.setTextViewText(R.id.status, statusText(context, store, snapshot, hasKey))

            val target = if (hasKey) {
                Intent(Intent.ACTION_VIEW, Uri.parse(CNN_URL))
            } else {
                Intent(context, MainActivity::class.java)
            }
            val pending = PendingIntent.getActivity(
                context,
                0,
                target,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_root, pending)
            return views
        }

        private fun deltaText(context: Context, snapshot: Snapshot): String {
            val previous = snapshot.previousClose ?: return ""
            return context.getString(R.string.delta_format, previous, snapshot.score - previous)
        }

        private fun statusText(context: Context, store: WidgetStore, snapshot: Snapshot?, hasKey: Boolean): String {
            if (!hasKey) return context.getString(R.string.status_no_key)
            val updated = snapshot?.let { context.getString(R.string.updated_format, formatTime(context, it)) }
            val problem = when (store.status) {
                WidgetStatus.OK -> null
                WidgetStatus.LOADING -> context.getString(R.string.status_loading)
                WidgetStatus.AUTH -> context.getString(R.string.status_auth)
                WidgetStatus.QUOTA -> context.getString(R.string.status_quota)
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
