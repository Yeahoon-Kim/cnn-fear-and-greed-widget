package app.feargreed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class FearGreedApiTest {
    private val fetchedAt = Instant.parse("2026-10-31T00:00:00Z")

    @Test
    fun `parses documented response`() {
        val body = """
            {"data":{"index":"CNN Fear & Greed (US stocks)","score":29.69,"rating":"fear",
            "source":"CNN Business","updated":"2026-06-11T23:59:42+00:00","week_ago":53.89,
            "month_ago":66.63,"year_ago":64.2,"previous_close":27.46},
            "meta":{"timestamp":"x","request_id":"y"},"status":"ok","message":"","success":true}
        """.trimIndent()

        val snapshot = FearGreedApi.parse(body, fetchedAt)!!

        assertEquals(29.69, snapshot.score, 0.001)
        assertEquals("fear", snapshot.rating)
        assertEquals(27.46, snapshot.previousClose!!, 0.001)
        assertEquals(Instant.parse("2026-06-11T23:59:42Z"), snapshot.updated)
    }

    @Test
    fun `falls back to fetch time when updated is unreadable`() {
        val snapshot = FearGreedApi.parse("""{"data":{"score":50,"updated":"soon"}}""", fetchedAt)!!

        assertEquals(fetchedAt, snapshot.updated)
        assertNull(snapshot.previousClose)
    }

    @Test
    fun `rejects malformed or out of range bodies`() {
        assertNull(FearGreedApi.parse("not json", fetchedAt))
        assertNull(FearGreedApi.parse("""{"data":{}}""", fetchedAt))
        assertNull(FearGreedApi.parse("""{"data":{"score":120}}""", fetchedAt))
    }
}
