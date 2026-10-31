package app.feargreed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class FearGreedApiTest {
    private val fetchedAt = Instant.parse("2026-10-31T00:00:00Z")

    @Test
    fun `parses observed response`() {
        val body = """
            {"fear_and_greed":{"score":26.2386554621849,"rating":"fear",
            "timestamp":"2026-10-30T23:59:48+00:00","previous_close":34.6,
            "previous_1_week":16.68,"previous_1_month":30.31,"previous_1_year":37.05},
            "fear_and_greed_historical":{"data":[]}}
        """.trimIndent()

        val snapshot = FearGreedApi.parse(body, fetchedAt)!!

        assertEquals(26.2386, snapshot.score, 0.001)
        assertEquals("fear", snapshot.rating)
        assertEquals(34.6, snapshot.previousClose!!, 0.001)
        assertEquals(Instant.parse("2026-10-30T23:59:48Z"), snapshot.updated)
    }

    @Test
    fun `falls back to fetch time when timestamp is unreadable`() {
        val snapshot = FearGreedApi.parse("""{"fear_and_greed":{"score":50,"timestamp":"soon"}}""", fetchedAt)!!

        assertEquals(fetchedAt, snapshot.updated)
        assertNull(snapshot.previousClose)
    }

    @Test
    fun `rejects malformed or out of range bodies`() {
        assertNull(FearGreedApi.parse("not json", fetchedAt))
        assertNull(FearGreedApi.parse("""{"fear_and_greed":{}}""", fetchedAt))
        assertNull(FearGreedApi.parse("""{"fear_and_greed":{"score":120}}""", fetchedAt))
    }
}
