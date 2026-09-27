package ca.zeezaglobal.gymsuitapp.data.local

import ca.zeezaglobal.gymsuitapp.data.DetailedSleepData

data class SleepInsightResult(
    val headline: String,
    val description: String,
    val badgeText: String
)

/**
 * On-device rule-based AI engine that produces clinical-grade, conversational
 * sleep coaching summaries matching the design reference style:
 * e.g., "A fragmented and restless night"
 */
object SleepAiSummaryEngine {

    fun generateInsight(data: DetailedSleepData): SleepInsightResult {
        if (!data.hasData || data.totalSleepMinutes <= 0) {
            return SleepInsightResult(
                headline = "No Sleep Logged",
                description = "We couldn't detect any sleep session recorded for this day. Wear your tracker to bed tonight to unlock detailed sleep stage insights.",
                badgeText = "Sleep tracking"
            )
        }

        val totalHours = data.totalSleepMinutes / 60
        val totalMins = data.totalSleepMinutes % 60
        val durationFormatted = if (totalHours > 0) "${totalHours}h ${totalMins}m" else "${totalMins}m"

        val awakeHours = data.awakeMinutes / 60
        val awakeMins = data.awakeMinutes % 60
        val awakeFormatted = if (awakeHours > 0) "${awakeHours}h ${awakeMins}m" else "${awakeMins}m"

        val interruptions = data.interruptionsCount
        val awakeRatio = data.awakeMinutes.toFloat() / data.totalSleepMinutes.toFloat()
        val deepRatio = data.deepMinutes.toFloat() / data.totalSleepMinutes.toFloat()

        return when {
            // Case 1: High interruptions or extensive time spent awake (matching user's image exactly)
            interruptions >= 5 || awakeRatio >= 0.18f -> {
                SleepInsightResult(
                    headline = "A fragmented and restless night",
                    description = "It looks like your sleep was broken up by over $interruptions interruptions, leading to nearly $awakeFormatted spent awake.",
                    badgeText = "Last night's sleep"
                )
            }
            // Case 2: Short sleep duration
            data.totalSleepMinutes < 330 -> { // under 5.5 hours
                SleepInsightResult(
                    headline = "Short sleep duration",
                    description = "You logged just $durationFormatted of sleep last night. Prioritizing an earlier bedtime tonight will help restore your cognitive baseline and muscle recovery.",
                    badgeText = "Last night's sleep"
                )
            }
            // Case 3: Excellent deep sleep & restoration
            deepRatio >= 0.20f && data.totalSleepMinutes >= 420 -> {
                SleepInsightResult(
                    headline = "Deep and restorative rest",
                    description = "Great consistency! You achieved optimal deep and REM sleep cycles over $durationFormatted with minimal disruption, promoting physical repair and peak focus.",
                    badgeText = "Last night's sleep"
                )
            }
            // Case 4: Long sleep / hibernation
            data.totalSleepMinutes >= 540 -> { // over 9 hours
                SleepInsightResult(
                    headline = "Extended restorative slumber",
                    description = "You caught up on rest with a solid $durationFormatted sleep session. Your body enjoyed ample restorative downtime across all major sleep cycles.",
                    badgeText = "Last night's sleep"
                )
            }
            // Case 5: Balanced standard night
            else -> {
                SleepInsightResult(
                    headline = "Balanced and steady sleep",
                    description = "You logged $durationFormatted of steady sleep with smooth transitions across Light, REM, and Deep sleep stages.",
                    badgeText = "Last night's sleep"
                )
            }
        }
    }
}
