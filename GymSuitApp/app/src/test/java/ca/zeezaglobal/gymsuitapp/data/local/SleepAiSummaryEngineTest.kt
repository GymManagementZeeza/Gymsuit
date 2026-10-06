package ca.zeezaglobal.gymsuitapp.data.local

import ca.zeezaglobal.gymsuitapp.data.DetailedSleepData
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class SleepAiSummaryEngineTest {

    private val now = Instant.now()
    private val today = LocalDate.now()

    @Test
    fun generateInsight_noData_returnsNoSleepLogged() {
        val data = DetailedSleepData(
            sessionDate = today,
            startTime = now,
            endTime = now,
            startTimeFormatted = "",
            midTimeFormatted = "",
            endTimeFormatted = "",
            totalSleepMinutes = 0,
            awakeMinutes = 0,
            remMinutes = 0,
            lightMinutes = 0,
            deepMinutes = 0,
            stages = emptyList(),
            interruptionsCount = 0,
            hasData = false
        )

        val insight = SleepAiSummaryEngine.generateInsight(data)

        assertEquals("No Sleep Logged", insight.headline)
        assertEquals("Sleep tracking", insight.badgeText)
    }

    @Test
    fun generateInsight_highInterruptions_returnsFragmentedNight() {
        val data = DetailedSleepData(
            sessionDate = today,
            startTime = now.minusSeconds(28800),
            endTime = now,
            startTimeFormatted = "11:00 PM",
            midTimeFormatted = "3:00 AM",
            endTimeFormatted = "7:00 AM",
            totalSleepMinutes = 420,
            awakeMinutes = 90,
            remMinutes = 80,
            lightMinutes = 180,
            deepMinutes = 70,
            stages = emptyList(),
            interruptionsCount = 6, // >= 5 interruptions
            hasData = true
        )

        val insight = SleepAiSummaryEngine.generateInsight(data)

        assertEquals("A fragmented and restless night", insight.headline)
        assertTrue(insight.description.contains("6 interruptions"))
    }

    @Test
    fun generateInsight_shortSleepDuration_returnsShortSleep() {
        val data = DetailedSleepData(
            sessionDate = today,
            startTime = now.minusSeconds(14400),
            endTime = now,
            startTimeFormatted = "2:00 AM",
            midTimeFormatted = "4:00 AM",
            endTimeFormatted = "6:00 AM",
            totalSleepMinutes = 240, // 4 hours (< 330 mins)
            awakeMinutes = 10,
            remMinutes = 40,
            lightMinutes = 150,
            deepMinutes = 40,
            stages = emptyList(),
            interruptionsCount = 1,
            hasData = true
        )

        val insight = SleepAiSummaryEngine.generateInsight(data)

        assertEquals("Short sleep duration", insight.headline)
    }

    @Test
    fun generateInsight_deepRestorativeRest_returnsDeepAndRestorative() {
        val data = DetailedSleepData(
            sessionDate = today,
            startTime = now.minusSeconds(28800),
            endTime = now,
            startTimeFormatted = "11:00 PM",
            midTimeFormatted = "3:00 AM",
            endTimeFormatted = "7:00 AM",
            totalSleepMinutes = 480, // 8 hours (>= 420 mins)
            awakeMinutes = 20,
            remMinutes = 110,
            lightMinutes = 230,
            deepMinutes = 120, // 25% deep sleep (>= 20%)
            stages = emptyList(),
            interruptionsCount = 2,
            hasData = true
        )

        val insight = SleepAiSummaryEngine.generateInsight(data)

        assertEquals("Deep and restorative rest", insight.headline)
    }
}
