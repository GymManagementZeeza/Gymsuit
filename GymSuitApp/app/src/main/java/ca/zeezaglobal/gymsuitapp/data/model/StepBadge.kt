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
    val awardedAt: Instant?,
    /** "STEPS" or "CALORIES". For CALORIES, [stepsAchieved] holds the kcal burned. */
    val kind: String = "STEPS"
) {
    val isCalories: Boolean get() = kind == "CALORIES"

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
            },
            kind = o.optString("kind", "STEPS").ifBlank { "STEPS" }
        )
    }
}

/** A daily badge: [kind] is STEPS or CALORIES, [threshold] is steps or kcal in one day. */
data class DailyBadgeSpec(val kind: String, val threshold: Int, val name: String) {
    val unit: String get() = if (kind == "CALORIES") "kcal burned" else "steps"
}

/** The two daily badges (they reset each day). */
object StepBadgeConfig {
    val daily: List<DailyBadgeSpec> = listOf(
        DailyBadgeSpec("STEPS", 10000, "Step Champion"),
        DailyBadgeSpec("CALORIES", 1000, "Calorie Crusher")
    )
}
