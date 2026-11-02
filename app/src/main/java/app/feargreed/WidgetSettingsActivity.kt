package app.feargreed

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.FrameLayout
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.work.WorkInfo
import androidx.work.WorkManager

class WidgetSettingsActivity : Activity() {
    private lateinit var store: WidgetStore
    private lateinit var style: WidgetStyle
    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private val workObserver = Observer<List<WorkInfo>> { infos ->
        if (infos.any { it.state.isFinished }) showPreview()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        widgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        )
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        setContentView(R.layout.activity_widget_settings)
        store = WidgetStore(this)
        style = store.style(widgetId)

        val modeGroup = findViewById<RadioGroup>(R.id.mode_group)
        modeGroup.check(
            when (style.mode) {
                WidgetMode.SYSTEM -> R.id.mode_system
                WidgetMode.LIGHT -> R.id.mode_light
                WidgetMode.DARK -> R.id.mode_dark
            },
        )
        modeGroup.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                R.id.mode_light -> WidgetMode.LIGHT
                R.id.mode_dark -> WidgetMode.DARK
                else -> WidgetMode.SYSTEM
            }
            style = style.copy(mode = mode)
            save()
        }

        val themeGroup = findViewById<RadioGroup>(R.id.theme_group)
        themeGroup.check(
            when (style.theme) {
                WidgetTheme.DEFAULT -> R.id.theme_default
                WidgetTheme.POINT -> R.id.theme_point
                WidgetTheme.FULL_COLOR -> R.id.theme_full_color
            },
        )
        themeGroup.setOnCheckedChangeListener { _, checkedId ->
            val theme = when (checkedId) {
                R.id.theme_point -> WidgetTheme.POINT
                R.id.theme_full_color -> WidgetTheme.FULL_COLOR
                else -> WidgetTheme.DEFAULT
            }
            style = style.copy(theme = theme)
            save()
        }

        val seekBar = findViewById<SeekBar>(R.id.transparency)
        seekBar.progress = style.transparencyPercent
        showTransparency()
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar, progress: Int, fromUser: Boolean) {
                style = style.copy(transparencyPercent = progress)
                showTransparency()
                showPreview()
            }

            override fun onStartTrackingTouch(bar: SeekBar) = Unit

            override fun onStopTrackingTouch(bar: SeekBar) = save()
        })

        findViewById<Button>(R.id.refresh).setOnClickListener {
            store.status = WidgetStatus.LOADING
            FearGreedWidget.updateAll(this)
            RefreshWorker.refreshNow(this)
            showPreview()
        }
        findViewById<Button>(R.id.open_cnn).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(CNN_URL)))
        }
        WorkManager.getInstance(this)
            .getWorkInfosForUniqueWorkLiveData(RefreshWorker.ONE_TIME_NAME)
            .observeForever(workObserver)
        showPreview()
    }

    override fun onResume() {
        super.onResume()
        showPreview()
        BatteryCard.bind(this)
    }

    override fun onDestroy() {
        if (::store.isInitialized) {
            WorkManager.getInstance(this)
                .getWorkInfosForUniqueWorkLiveData(RefreshWorker.ONE_TIME_NAME)
                .removeObserver(workObserver)
        }
        super.onDestroy()
    }

    private fun save() {
        store.saveStyle(widgetId, style)
        FearGreedWidget.update(this, widgetId)
        showPreview()
    }

    private fun showTransparency() {
        findViewById<TextView>(R.id.transparency_label).text =
            getString(R.string.transparency_format, style.transparencyPercent)
    }

    private fun showPreview() {
        val compact = findViewById<FrameLayout>(R.id.preview_compact)
        val full = findViewById<FrameLayout>(R.id.preview_full)
        compact.removeAllViews()
        full.removeAllViews()
        compact.addView(FearGreedWidget.compactViews(this, store, style).apply(this, compact))
        full.addView(FearGreedWidget.fullViews(this, store, style).apply(this, full))
        findViewById<TextView>(R.id.last_check).text = FearGreedWidget.lastCheckText(this, store)
    }

    private companion object {
        const val CNN_URL = "https://edition.cnn.com/markets/fear-and-greed"
    }
}
