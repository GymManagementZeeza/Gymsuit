package ca.zeezaglobal.gymsuitapp.data.summary

import ca.zeezaglobal.gymsuitapp.data.HealthConnectManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.Locale

/**
 * Snapshot of the real health metrics available on this device, gathered from
 * Health Connect. Every metric is nullable: missing data stays missing so the
 * summary can never fabricate numbers.
 */
data class WellnessSummaryInput(
    val healthConnectAvailable: Boolean,
    val stepsToday: Long?,
    val activeCaloriesKcal: Double?,
    val distanceMeters: Double?,
    val latestHeartRateBpm: Int?,
    val latestWeightKg: Double?,
    val sleepMinutesLastNight: Long?,
    val avgSleepMinutes7d: Double?,
    val workoutsLast7d: Int,
    val workedOutToday: Boolean
) {
    fun hasAnyData(): Boolean =
        stepsToday != null ||
            activeCaloriesKcal != null ||
            distanceMeters != null ||
            latestHeartRateBpm != null ||
            latestWeightKg != null ||
            sleepMinutesLastNight != null ||
            workoutsLast7d > 0
}

/**
 * Generates the daily wellness summary fully on-device.
 *
 * No network calls, no API keys, no cloud model: the summary is composed from
 * the user's real Health Connect metrics using a deterministic local engine,
 * so it works offline and can never hallucinate data that isn't there.
 * Swap [generateSummary] for an on-device LLM later without touching callers.
 */
class WellnessSummaryGenerator(
    private val healthConnectManager: HealthConnectManager
) {

    /** Gathers real metrics from Health Connect and composes the summary. */
    suspend fun generate(): String = withContext(Dispatchers.IO) {
        generateSummary(gatherInput())
    }

    private suspend fun gatherInput(): WellnessSummaryInput = coroutineScope {
        if (!healthConnectManager.isAvailable()) {
            return@coroutineScope WellnessSummaryInput(
                healthConnectAvailable = false,
                stepsToday = null,
                activeCaloriesKcal = null,
                distanceMeters = null,
                latestHeartRateBpm = null,
                latestWeightKg = null,
                sleepMinutesLastNight = null,
                avgSleepMinutes7d = null,
                workoutsLast7d = 0,
                workedOutToday = false
            )
        }

        val today = LocalDate.now()
        val steps = async { healthConnectManager.readTodaySteps() }
        val calories = async { healthConnectManager.readCaloriesForDate(today) }
        val distance = async { healthConnectManager.readTodayDistanceMeters() }
        val heartRate = async { healthConnectManager.readTodayLatestHeartRateBpm() }
        val weight = async { healthConnectManager.readLatestWeight() }
        val sleepSessions = async { healthConnectManager.readRecentSleepSessions(daysBack = 8) }
        val workoutDays = async { healthConnectManager.readWorkoutDays(daysBack = 7) }

        val sessions = sleepSessions.await()
        val lastNight = sessions.maxByOrNull { it.endTime }
        val recentDays = workoutDays.await()

        WellnessSummaryInput(
            healthConnectAvailable = true,
            stepsToday = steps.await(),
            activeCaloriesKcal = calories.await(),
            distanceMeters = distance.await(),
            latestHeartRateBpm = heartRate.await(),
            latestWeightKg = weight.await(),
            sleepMinutesLastNight = lastNight?.durationMinutes,
            avgSleepMinutes7d = sessions.takeIf { it.isNotEmpty() }
                ?.map { it.durationMinutes }?.average(),
            workoutsLast7d = recentDays.size,
            workedOutToday = recentDays.contains(today)
        )
    }
}

/**
 * Pure, deterministic summary composer. Only mentions metrics that are present
 * in [input]; returns 3-5 warm, conversational sentences.
 */
fun generateSummary(input: WellnessSummaryInput, today: LocalDate = LocalDate.now()): String {
    if (!input.healthConnectAvailable || !input.hasAnyData()) {
        return "I don't have any health data to work with yet. " +
            "Connect your fitness apps and grant Health Connect permissions, " +
            "and I'll turn your activity, sleep, and workouts into a daily recap right here on your phone."
    }

    val rotation = today.dayOfYear
    val sentences = mutableListOf<String>()

    // 1. Opener: the most notable real highlight.
    val opener = when {
        input.stepsToday != null && input.stepsToday >= 10_000 ->
            pick(rotation, listOf(
                "You smashed past 10,000 steps today with ${formatSteps(input.stepsToday)} — that's a seriously active day.",
                "${formatSteps(input.stepsToday)} steps and counting — you've blown past the 10k mark today."
            ))
        input.workedOutToday ->
            pick(rotation, listOf(
                "You got a workout in today — love to see it.",
                "Training box ticked for today. Nicely done."
            ))
        input.sleepMinutesLastNight != null && input.sleepMinutesLastNight >= 7 * 60 ->
            "You banked ${formatDuration(input.sleepMinutesLastNight)} of sleep last night — your recovery is thanking you."
        input.stepsToday != null && input.stepsToday >= 5_000 ->
            "You're moving well today with ${formatSteps(input.stepsToday)} steps on the board."
        input.workoutsLast7d >= 3 ->
            "You've trained ${input.workoutsLast7d} times this week — that consistency is compounding."
        input.stepsToday != null ->
            "You've logged ${formatSteps(input.stepsToday)} steps so far today."
        else -> "Here's how your health is shaping up."
    }
    sentences.add(opener)

    // 2. Supporting sentences for other present metrics (max 2).
    val supporting = mutableListOf<String>()
    input.activeCaloriesKcal?.let {
        supporting.add("You've burned around ${it.toInt()} active calories so far.")
    }
    input.distanceMeters?.let {
        supporting.add("That's about ${formatKm(it)} km covered.")
    }
    input.latestHeartRateBpm?.let {
        supporting.add("Your latest heart-rate reading is $it bpm.")
    }
    input.latestWeightKg?.let {
        supporting.add("Your latest weigh-in is ${formatKg(it)} kg.")
    }
    if (input.sleepMinutesLastNight != null && !(input.sleepMinutesLastNight >= 7 * 60)) {
        supporting.add("Last night's sleep was ${formatDuration(input.sleepMinutesLastNight)}.")
    }
    if (input.sleepMinutesLastNight == null) {
        input.avgSleepMinutes7d?.let {
            supporting.add("You've been averaging ${formatDuration(it.toLong())} of sleep lately.")
        }
    }
    sentences.addAll(supporting.take(2))

    // 3. One gentle nudge, only when the data suggests it.
    val nudge = when {
        input.stepsToday != null && input.stepsToday < 5_000 && !input.workedOutToday ->
            "A brisk 10-minute walk would nudge that step count up nicely."
        input.sleepMinutesLastNight != null && input.sleepMinutesLastNight < 6 * 60 ->
            "Try winding down 30 minutes earlier tonight — small change, big payoff."
        input.workoutsLast7d == 0 ->
            "No workouts logged this week yet — even a quick session counts."
        else -> null
    }
    nudge?.let { sentences.add(it) }

    // 4. Warm closer, rotated daily.
    sentences.add(
        pick(rotation, listOf(
            "Keep it up — your future self is already grateful.",
            "Small wins daily beat heroic efforts weekly.",
            "Rest, move, repeat. You've got this."
        ))
    )

    return sentences.joinToString(" ")
}

private fun pick(rotation: Int, options: List<String>): String =
    options[(rotation % options.size + options.size) % options.size]

private fun formatSteps(steps: Long): String =
    String.format(Locale.US, "%,d", steps)

private fun formatKm(meters: Double): String =
    String.format(Locale.US, "%.1f", meters / 1000.0)

private fun formatKg(kg: Double): String =
    String.format(Locale.US, "%.1f", kg)

private fun formatDuration(minutes: Long): String {
    val h = minutes / 60
    val m = minutes % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}
