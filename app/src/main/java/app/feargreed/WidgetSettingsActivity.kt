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

class WidgetSettingsActivity : Activity() {
    private lateinit var store: WidgetStore
    private lateinit var style: WidgetStyle
    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID

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

        val themeGroup = findViewById<RadioGroup>(R.id.theme_group)
        themeGroup.check(
            when (style.theme) {
                WidgetTheme.SYSTEM -> R.id.theme_system
                WidgetTheme.LIGHT -> R.id.theme_light
                WidgetTheme.DARK -> R.id.theme_dark
            },
        )
        themeGroup.setOnCheckedChangeListener { _, checkedId ->
            val theme = when (checkedId) {
                R.id.theme_light -> WidgetTheme.LIGHT
                R.id.theme_dark -> WidgetTheme.DARK
                else -> WidgetTheme.SYSTEM
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
        showPreview()
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
    }

    private companion object {
        const val CNN_URL = "https://edition.cnn.com/markets/fear-and-greed"
    }
}
