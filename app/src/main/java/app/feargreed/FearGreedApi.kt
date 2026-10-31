package app.feargreed

import android.util.Log
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

data class Snapshot(
    val score: Double,
    val rating: String,
    val previousClose: Double?,
    val updated: Instant,
)

sealed interface FetchResult {
    data class Success(val snapshot: Snapshot) : FetchResult
    data object Blocked : FetchResult
    data object Failed : FetchResult
}

object FearGreedApi {
    private const val TAG = "FearGreedApi"
    private const val TIMEOUT_MS = 15_000
    private const val USER_AGENT = "FearGreedWidget/0.1 (personal project)"

    fun fetch(): FetchResult {
        val connection = URL(BuildConfig.API_URL).openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.setRequestProperty("User-Agent", USER_AGENT)
            connection.setRequestProperty("Accept", "application/json")
            val status = connection.responseCode
            Log.i(TAG, "response status=$status")
            when (status) {
                200 -> {
                    val body = connection.inputStream.bufferedReader().use { it.readText() }
                    parse(body, Instant.now())?.let { FetchResult.Success(it) } ?: FetchResult.Failed
                }
                403, 418, 429 -> FetchResult.Blocked
                else -> FetchResult.Failed
            }
        } catch (e: IOException) {
            Log.w(TAG, "fetch failed", e)
            FetchResult.Failed
        } finally {
            connection.disconnect()
        }
    }

    fun parse(body: String, fetchedAt: Instant): Snapshot? {
        val data = try {
            JSONObject(body).getJSONObject("fear_and_greed")
        } catch (_: org.json.JSONException) {
            return null
        }
        val score = data.optDouble("score")
        if (score.isNaN() || score !in 0.0..100.0) return null
        val previousClose = data.optDouble("previous_close").takeUnless { it.isNaN() }
        return Snapshot(
            score = score,
            rating = data.optString("rating"),
            previousClose = previousClose,
            updated = parseInstant(data.optString("timestamp")) ?: fetchedAt,
        )
    }

    private fun parseInstant(value: String): Instant? = try {
        OffsetDateTime.parse(value).toInstant()
    } catch (_: DateTimeParseException) {
        null
    }
}
