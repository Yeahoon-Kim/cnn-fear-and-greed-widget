package app.feargreed

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.cos
import kotlin.math.sin

object GaugeRenderer {
    private const val WIDTH = 600
    private const val HEIGHT = 320
    private const val STROKE = 44f
    private const val GAP_DEGREES = 1.5f
    private val NEEDLE_COLOR = Color.parseColor("#8A8F98")

    fun render(score: Double?): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val centerX = WIDTH / 2f
        val centerY = HEIGHT - 24f
        val radius = centerX - STROKE
        val arc = RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius)

        val band = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = STROKE
        }
        var start = 0.0
        for (rating in Rating.entries) {
            band.color = rating.color
            if (score == null) band.alpha = 70
            val sweep = (rating.upperBound - start) * 1.8
            canvas.drawArc(arc, 180f + (start * 1.8).toFloat() + GAP_DEGREES / 2, sweep.toFloat() - GAP_DEGREES, false, band)
            start = rating.upperBound
        }

        if (score != null) {
            val angle = Math.toRadians(180.0 + score.coerceIn(0.0, 100.0) * 1.8)
            val length = radius - STROKE
            val needle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = NEEDLE_COLOR
                strokeWidth = 12f
                strokeCap = Paint.Cap.ROUND
            }
            canvas.drawLine(
                centerX,
                centerY,
                centerX + (length * cos(angle)).toFloat(),
                centerY + (length * sin(angle)).toFloat(),
                needle,
            )
            canvas.drawCircle(centerX, centerY, 16f, needle)
        }
        return bitmap
    }
}
