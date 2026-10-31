package app.feargreed

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.work.WorkInfo
import androidx.work.WorkManager

class MainActivity : Activity() {
    private lateinit var store: WidgetStore
    private val workObserver = Observer<List<WorkInfo>> { infos ->
        if (infos.any { it.state == WorkInfo.State.SUCCEEDED }) showCurrent()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        store = WidgetStore(this)
        RefreshWorker.schedule(this)
        RefreshWorker.refreshNow(this, force = false)

        findViewById<Button>(R.id.refresh).setOnClickListener {
            store.status = WidgetStatus.LOADING
            FearGreedWidget.updateAll(this)
            RefreshWorker.refreshNow(this)
            showCurrent()
        }
        findViewById<Button>(R.id.pin).setOnClickListener { requestPin() }
        findViewById<Button>(R.id.battery).setOnClickListener {
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")),
            )
        }
        WorkManager.getInstance(this)
            .getWorkInfosForUniqueWorkLiveData(RefreshWorker.ONE_TIME_NAME)
            .observeForever(workObserver)
    }

    override fun onDestroy() {
        WorkManager.getInstance(this)
            .getWorkInfosForUniqueWorkLiveData(RefreshWorker.ONE_TIME_NAME)
            .removeObserver(workObserver)
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        showCurrent()
    }

    private fun showCurrent() {
        val snapshot = store.snapshot
        findViewById<TextView>(R.id.current).text = if (snapshot == null) {
            getString(R.string.current_empty)
        } else {
            getString(
                R.string.current_format,
                snapshot.score,
                getString(Rating.of(snapshot.score).label),
                getString(R.string.updated_format, FearGreedWidget.formatTime(this, snapshot)),
            )
        }
    }

    private fun requestPin() {
        val manager = AppWidgetManager.getInstance(this)
        if (manager.isRequestPinAppWidgetSupported) {
            manager.requestPinAppWidget(ComponentName(this, FearGreedWidget::class.java), null, null)
        } else {
            Toast.makeText(this, R.string.pin_unsupported, Toast.LENGTH_LONG).show()
        }
    }
}
