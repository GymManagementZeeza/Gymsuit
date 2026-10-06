package ca.zeezaglobal.gymsuitapp.data.model

import org.json.JSONObject

/** Metric a group challenge competes on. apiValue matches the backend contract. */
enum class ChallengeMetric(val apiValue: String, val label: String, val unit: String) {
    STEPS("STEPS", "Steps", "steps"),
    WORKOUTS("WORKOUTS", "Workouts", "workouts"),
    CALORIES("CALORIES", "Calories", "kcal"),
    DISTANCE_KM("DISTANCE_KM", "Distance", "km");

    companion object {
        fun fromApi(value: String?): ChallengeMetric =
            entries.firstOrNull { it.apiValue.equals(value, ignoreCase = true) } ?: STEPS
    }
}

enum class ChallengeStatus(val apiValue: String, val label: String) {
    UPCOMING("UPCOMING", "Upcoming"),
    ACTIVE("ACTIVE", "Active"),
    ENDED("ENDED", "Ended");

    companion object {
        fun fromApi(value: String?): ChallengeStatus =
            entries.firstOrNull { it.apiValue.equals(value, ignoreCase = true) } ?: UPCOMING
    }
}

data class LeaderboardEntry(
    val userId: String,
    val displayName: String,
    val value: Double,
    val rank: Int
)

data class ChallengeSummary(
    val id: String,
    val name: String,
    val metric: ChallengeMetric,
    val startDate: String,
    val endDate: String,
    val status: ChallengeStatus,
    val participantCount: Int,
    val myRank: Int?
)

data class ChallengeDetail(
    val id: String,
    val name: String,
    val description: String,
    val metric: ChallengeMetric,
    val startDate: String,
    val endDate: String,
    val status: ChallengeStatus,
    val inviteCode: String,
    val participantCount: Int,
    val myRank: Int?,
    val createdByMe: Boolean,
    val leaderboard: List<LeaderboardEntry>
)

/** Aggregated health totals over a challenge date range. Zeros are honest — never fabricated. */
data class ProgressTotals(
    val steps: Long,
    val workouts: Int,
    val calories: Double,
    val distanceKm: Double
)

private fun JSONObject.optId(key: String): String =
    if (isNull(key)) "" else opt(key)?.toString() ?: ""

private fun JSONObject.optRank(key: String): Int? =
    if (has(key) && !isNull(key)) optInt(key) else null

fun parseLeaderboardEntry(json: JSONObject): LeaderboardEntry = LeaderboardEntry(
    userId = json.optId("userId"),
    displayName = json.optString("displayName", "Member").ifBlank { "Member" },
    value = json.optDouble("value", 0.0),
    rank = json.optInt("rank", 0)
)

fun parseChallengeSummary(json: JSONObject): ChallengeSummary = ChallengeSummary(
    id = json.optId("id"),
    name = json.optString("name", "Challenge"),
    metric = ChallengeMetric.fromApi(json.optString("metricType", "")),
    startDate = json.optString("startDate", ""),
    endDate = json.optString("endDate", ""),
    status = ChallengeStatus.fromApi(json.optString("status", "")),
    participantCount = json.optInt("participantCount", 0),
    myRank = json.optRank("myRank")
)

fun parseChallengeDetail(json: JSONObject): ChallengeDetail {
    val board = mutableListOf<LeaderboardEntry>()
    val arr = json.optJSONArray("leaderboard")
    if (arr != null) {
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            board.add(parseLeaderboardEntry(obj))
        }
    }
    return ChallengeDetail(
        id = json.optId("id"),
        name = json.optString("name", "Challenge"),
        description = json.optString("description", ""),
        metric = ChallengeMetric.fromApi(json.optString("metricType", "")),
        startDate = json.optString("startDate", ""),
        endDate = json.optString("endDate", ""),
        status = ChallengeStatus.fromApi(json.optString("status", "")),
        inviteCode = json.optString("inviteCode", ""),
        participantCount = json.optInt("participantCount", 0),
        myRank = json.optRank("myRank"),
        createdByMe = json.optBoolean("createdByMe", false),
        leaderboard = board
    )
}

/** Formats a leaderboard value for the given metric. */
fun formatMetricValue(metric: ChallengeMetric, value: Double): String = when (metric) {
    ChallengeMetric.STEPS -> "%,d steps".format(value.toLong())
    ChallengeMetric.WORKOUTS -> "%d workouts".format(value.toInt())
    ChallengeMetric.CALORIES -> "%.0f kcal".format(value)
    ChallengeMetric.DISTANCE_KM -> "%.1f km".format(value)
}
