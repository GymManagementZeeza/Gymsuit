package ca.zeezaglobal.gymsuitapp.data.model

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class HealthSyncModelsTest {

    @Test
    fun dailyHealthSyncDto_jsonRoundTrip() {
        val original = DailyHealthSyncDto(
            recordDate = "2026-10-04",
            steps = 14500L,
            activeCalories = 550.0,
            totalCalories = 2400.0,
            distanceMeters = 10200.0,
            latestHeartRateBpm = 78,
            restingHeartRateBpm = 62,
            minHeartRateBpm = 54,
            maxHeartRateBpm = 145,
            heartRateSamplesJson = "[{\"bpm\":78}]",
            sleepDurationMinutes = 460L,
            sleepStartTime = "2026-10-03T23:00:00Z",
            sleepEndTime = "2026-10-04T06:40:00Z",
            sleepStagesJson = "[]",
            weightKg = 75.5,
            sourceDevice = "ANDROID_HEALTH_CONNECT",
            updatedAt = "2026-10-04T08:00:00Z"
        )

        val json = original.toJsonObject()
        val parsed = DailyHealthSyncDto.fromJsonObject(json)

        assertEquals(original.recordDate, parsed.recordDate)
        assertEquals(original.steps, parsed.steps)
        assertEquals(original.activeCalories, parsed.activeCalories)
        assertEquals(original.totalCalories, parsed.totalCalories)
        assertEquals(original.distanceMeters, parsed.distanceMeters)
        assertEquals(original.latestHeartRateBpm, parsed.latestHeartRateBpm)
        assertEquals(original.sleepDurationMinutes, parsed.sleepDurationMinutes)
        assertEquals(original.weightKg, parsed.weightKg)
        assertEquals(original.sourceDevice, parsed.sourceDevice)
    }

    @Test
    fun workoutSyncDto_jsonRoundTrip() {
        val original = WorkoutSyncDto(
            externalId = "w-123",
            title = "Squat",
            startTime = "2026-10-04T07:00:00Z",
            endTime = "2026-10-04T08:00:00Z",
            durationMinutes = 60,
            caloriesBurned = 350.0,
            setCount = 5,
            totalReps = 40,
            totalVolumeKg = 3200.0,
            sourceDevice = "ANDROID_HEALTH_CONNECT"
        )

        val json = original.toJsonObject()
        val parsed = WorkoutSyncDto.fromJsonObject(json)

        assertEquals(original.externalId, parsed.externalId)
        assertEquals(original.title, parsed.title)
        assertEquals(original.durationMinutes, parsed.durationMinutes)
        assertEquals(original.caloriesBurned, parsed.caloriesBurned)
        assertEquals(original.setCount, parsed.setCount)
        assertEquals(original.totalReps, parsed.totalReps)
        assertEquals(original.totalVolumeKg, parsed.totalVolumeKg)
    }

    @Test
    fun pointsTransactionDto_jsonRoundTrip() {
        val original = PointsTransactionDto(
            id = "tx-999",
            points = 85,
            reason = "Workout: Deadlift",
            rupeeValue = null,
            createdAt = "2026-10-04T08:15:00Z",
            deviceId = "ANDROID_APP"
        )

        val json = original.toJsonObject()
        val parsed = PointsTransactionDto.fromJsonObject(json)

        assertEquals(original.id, parsed.id)
        assertEquals(original.points, parsed.points)
        assertEquals(original.reason, parsed.reason)
        assertEquals(original.createdAt, parsed.createdAt)
        assertEquals(original.deviceId, parsed.deviceId)
    }

    @Test
    fun healthSyncResponse_parsesNestedObjectsCorrectly() {
        val json = JSONObject().apply {
            put("serverSyncTime", "2026-10-04T08:30:00Z")
            put("uploadedDailyCount", 2)
            put("uploadedWorkoutCount", 1)
            put("pointsBalance", 450)

            put("remoteDailyRecords", JSONArray().apply {
                put(JSONObject().apply {
                    put("recordDate", "2026-10-04")
                    put("steps", 12000L)
                })
            })

            put("remoteWorkouts", JSONArray().apply {
                put(JSONObject().apply {
                    put("externalId", "w-1")
                    put("title", "Pull Up")
                    put("startTime", "2026-10-04T09:00:00Z")
                    put("endTime", "2026-10-04T09:30:00Z")
                })
            })

            put("pointsTransactions", JSONArray().apply {
                put(JSONObject().apply {
                    put("id", "tx-1")
                    put("points", 50)
                    put("reason", "Workout: Pull Up")
                })
            })
        }

        val response = HealthSyncResponse.fromJsonObject(json)

        assertEquals("2026-10-04T08:30:00Z", response.serverSyncTime)
        assertEquals(2, response.uploadedDailyCount)
        assertEquals(1, response.uploadedWorkoutCount)
        assertEquals(Integer.valueOf(450), response.pointsBalance)
        assertEquals(1, response.remoteDailyRecords.size)
        assertEquals(12000L, response.remoteDailyRecords[0].steps)
        assertEquals(1, response.remoteWorkouts.size)
        assertEquals("Pull Up", response.remoteWorkouts[0].title)
        assertNotNull(response.pointsTransactions)
        assertEquals(1, response.pointsTransactions!!.size)
        assertEquals("tx-1", response.pointsTransactions!![0].id)
    }
}
