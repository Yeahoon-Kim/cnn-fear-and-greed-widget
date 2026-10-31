package app.feargreed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class RefreshPolicyTest {
    // 2026-10-30 is a Friday with New York on EDT (UTC-4).
    private fun friday(utcTime: String) = Instant.parse("2026-10-30T${utcTime}:00Z")

    @Test
    fun `interval follows the New York trading day`() {
        assertEquals(Duration.ofHours(6), RefreshPolicy.minInterval(friday("13:29")))
        assertEquals(Duration.ofMinutes(30), RefreshPolicy.minInterval(friday("13:30")))
        assertEquals(Duration.ofMinutes(60), RefreshPolicy.minInterval(friday("20:00")))
        assertEquals(Duration.ofHours(6), RefreshPolicy.minInterval(friday("01:00")))
    }

    @Test
    fun `weekend uses the long interval`() {
        assertEquals(Duration.ofHours(12), RefreshPolicy.minInterval(Instant.parse("2026-10-31T15:00:00Z")))
    }

    @Test
    fun `market open follows daylight saving change`() {
        // After 2026-11-01 New York is on EST (UTC-5), so 09:30 is 14:30 UTC.
        assertEquals(Duration.ofHours(6), RefreshPolicy.minInterval(Instant.parse("2026-11-02T14:00:00Z")))
        assertEquals(Duration.ofMinutes(30), RefreshPolicy.minInterval(Instant.parse("2026-11-02T14:30:00Z")))
    }

    @Test
    fun `due check allows for worker jitter`() {
        val now = friday("15:00")
        assertTrue(RefreshPolicy.isDue(now, null))
        assertTrue(RefreshPolicy.isDue(now, now.minus(Duration.ofMinutes(26))))
        assertFalse(RefreshPolicy.isDue(now, now.minus(Duration.ofMinutes(10))))
    }

    @Test
    fun `evening fetch catches the final update near 20 00`() {
        val lastBeforeFinalUpdate = Instant.parse("2026-10-30T23:40:00Z")
        val nextTick = Instant.parse("2026-10-31T00:40:00Z")
        assertTrue(RefreshPolicy.isDue(nextTick, lastBeforeFinalUpdate))
    }
}
