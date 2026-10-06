package ca.zeezaglobal.gymsuitapp.data.model

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class ChallengeModelsTest {

    @Test
    fun challengeMetric_fromApiHandlesVariousInputs() {
        assertEquals(ChallengeMetric.STEPS, ChallengeMetric.fromApi("STEPS"))
        assertEquals(ChallengeMetric.STEPS, ChallengeMetric.fromApi("steps"))
        assertEquals(ChallengeMetric.WORKOUTS, ChallengeMetric.fromApi("WORKOUTS"))
        assertEquals(ChallengeMetric.CALORIES, ChallengeMetric.fromApi("CALORIES"))
        assertEquals(ChallengeMetric.DISTANCE_KM, ChallengeMetric.fromApi("DISTANCE_KM"))
        assertEquals(ChallengeMetric.STEPS, ChallengeMetric.fromApi(null))
        assertEquals(ChallengeMetric.STEPS, ChallengeMetric.fromApi("unknown_metric"))
    }

    @Test
    fun challengeStatus_fromApiHandlesVariousInputs() {
        assertEquals(ChallengeStatus.ACTIVE, ChallengeStatus.fromApi("ACTIVE"))
        assertEquals(ChallengeStatus.ACTIVE, ChallengeStatus.fromApi("active"))
        assertEquals(ChallengeStatus.UPCOMING, ChallengeStatus.fromApi("UPCOMING"))
        assertEquals(ChallengeStatus.ENDED, ChallengeStatus.fromApi("ENDED"))
        assertEquals(ChallengeStatus.UPCOMING, ChallengeStatus.fromApi(null))
        assertEquals(ChallengeStatus.UPCOMING, ChallengeStatus.fromApi("invalid_status"))
    }

    @Test
    fun formatMetricValue_formatsCorrectly() {
        assertEquals("10,000 steps", formatMetricValue(ChallengeMetric.STEPS, 10000.0))
        assertEquals("5.5 km", formatMetricValue(ChallengeMetric.DISTANCE_KM, 5.5))
        assertEquals("650 kcal", formatMetricValue(ChallengeMetric.CALORIES, 650.0))
        assertEquals("12 workouts", formatMetricValue(ChallengeMetric.WORKOUTS, 12.0))
    }

    @Test
    fun parseChallengeSummary_parsesFieldsCorrectly() {
        val json = JSONObject().apply {
            put("id", "ch-101")
            put("name", "October Step Sprint")
            put("metricType", "STEPS")
            put("startDate", "2026-10-01")
            put("endDate", "2026-10-31")
            put("status", "ACTIVE")
            put("participantCount", 42)
            put("myRank", 3)
        }

        val summary = parseChallengeSummary(json)

        assertEquals("ch-101", summary.id)
        assertEquals("October Step Sprint", summary.name)
        assertEquals(ChallengeMetric.STEPS, summary.metric)
        assertEquals("2026-10-01", summary.startDate)
        assertEquals("2026-10-31", summary.endDate)
        assertEquals(ChallengeStatus.ACTIVE, summary.status)
        assertEquals(42, summary.participantCount)
        assertEquals(Integer.valueOf(3), summary.myRank)
    }

    @Test
    fun parseChallengeDetail_parsesLeaderboardCorrectly() {
        val json = JSONObject().apply {
            put("id", "ch-202")
            put("name", "Push-up Blitz")
            put("description", "Daily workouts challenge")
            put("metricType", "WORKOUTS")
            put("startDate", "2026-10-01")
            put("endDate", "2026-10-15")
            put("status", "ACTIVE")
            put("inviteCode", "BLITZ26")
            put("participantCount", 10)
            put("myRank", 1)
            put("createdByMe", true)
            put("leaderboard", JSONArray().apply {
                put(JSONObject().apply {
                    put("userId", "user-1")
                    put("displayName", "Athul")
                    put("value", 15.0)
                    put("rank", 1)
                })
                put(JSONObject().apply {
                    put("userId", "user-2")
                    put("displayName", "Rahul")
                    put("value", 12.0)
                    put("rank", 2)
                })
            })
        }

        val detail = parseChallengeDetail(json)

        assertEquals("ch-202", detail.id)
        assertEquals("BLITZ26", detail.inviteCode)
        assertTrue(detail.createdByMe)
        assertEquals(2, detail.leaderboard.size)
        assertEquals("Athul", detail.leaderboard[0].displayName)
        assertEquals(1, detail.leaderboard[0].rank)
        assertEquals(15.0, detail.leaderboard[0].value, 0.001)
        assertEquals("Rahul", detail.leaderboard[1].displayName)
    }
}
