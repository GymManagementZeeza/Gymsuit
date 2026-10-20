package ca.zeezaglobal.gymsuitapp.data.model

import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate

/**
 * A step milestone badge as served by GET /api/mobile/badges.
 * [date] is the walking day the badge was earned for; [awardedAt] is when
 * the backend granted it.
 */
data class StepBadge(
    val id: Long,
    val threshold: Int,
    val badgeName: String,
    val stepsAchieved: Long,
    val date: LocalDate?,
    val awardedAt: Instant?
) {
    companion object {
        fun fromJson(o: JSONObject): StepBadge = StepBadge(
            id = o.optLong("id"),
            threshold = o.optInt("threshold"),
            badgeName = o.optString("badgeName", ""),
            stepsAchieved = o.optLong("stepsAchieved"),
            date = o.optString("date").takeIf { it.isNotBlank() }?.let {
                try { LocalDate.parse(it) } catch (e: Exception) { null }
            },
            awardedAt = o.optString("awardedAt").takeIf { it.isNotBlank() }?.let {
                try { Instant.parse(it) } catch (e: Exception) { null }
            }
        )
    }
}

/** The three milestone thresholds with their badge names, ascending. */
object StepBadgeConfig {
    val milestones: List<Pair<Int, String>> = listOf(
        5000 to "Stride Starter",
        10000 to "Step Champion",
        20000 to "Trail Blazer"
    )
}
