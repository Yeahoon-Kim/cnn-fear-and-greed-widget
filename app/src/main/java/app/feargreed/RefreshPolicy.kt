package app.feargreed

import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

object RefreshPolicy {
    private val NEW_YORK = ZoneId.of("America/New_York")
    private val MARKET_OPEN = LocalTime.of(9, 30)
    private val MARKET_CLOSE = LocalTime.of(16, 0)
    private val AFTER_CLOSE_END = LocalTime.of(21, 0)

    // The periodic worker fires about every 30 minutes, so allow for scheduling jitter.
    private val SLACK = Duration.ofMinutes(5)

    fun minInterval(now: Instant): Duration {
        val local = now.atZone(NEW_YORK)
        if (local.dayOfWeek == DayOfWeek.SATURDAY || local.dayOfWeek == DayOfWeek.SUNDAY) {
            return Duration.ofHours(12)
        }
        val time = local.toLocalTime()
        return when {
            time >= MARKET_OPEN && time < MARKET_CLOSE -> Duration.ofMinutes(30)
            time >= MARKET_CLOSE && time < AFTER_CLOSE_END -> Duration.ofMinutes(60)
            else -> Duration.ofHours(6)
        }
    }

    fun isDue(now: Instant, lastAttempt: Instant?): Boolean {
        if (lastAttempt == null) return true
        return Duration.between(lastAttempt, now) >= minInterval(now) - SLACK
    }
}
