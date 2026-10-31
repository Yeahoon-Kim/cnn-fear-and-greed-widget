package app.feargreed

import android.graphics.Color
import androidx.annotation.StringRes

enum class Rating(val upperBound: Double, @param:StringRes val label: Int, val color: Int) {
    EXTREME_FEAR(25.0, R.string.rating_extreme_fear, Color.parseColor("#D64545")),
    FEAR(45.0, R.string.rating_fear, Color.parseColor("#E8894A")),
    NEUTRAL(56.0, R.string.rating_neutral, Color.parseColor("#C9B037")),
    GREED(76.0, R.string.rating_greed, Color.parseColor("#7FB069")),
    EXTREME_GREED(100.0, R.string.rating_extreme_greed, Color.parseColor("#2E9E6B"));

    companion object {
        fun of(score: Double): Rating =
            entries.firstOrNull { score < it.upperBound } ?: EXTREME_GREED
    }
}
