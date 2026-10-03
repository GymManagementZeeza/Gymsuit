import Foundation

public struct SleepInsightResult {
    public let headline: String
    public let description: String
    public let badgeText: String
    
    public init(headline: String, description: String, badgeText: String = "Last night's sleep") {
        self.headline = headline
        self.description = description
        self.badgeText = badgeText
    }
}

public struct SleepAiSummaryEngine {
    public static func generateInsight(from data: DetailedSleepData) -> SleepInsightResult {
        guard data.hasData, data.totalSleepMinutes > 0 else {
            return SleepInsightResult(
                headline: "No Sleep Logged",
                description: "We couldn't detect any sleep session recorded for this day. Wear your tracker to bed tonight to unlock detailed sleep stage insights.",
                badgeText: "Sleep tracking"
            )
        }
        
        let totalHours = data.totalSleepMinutes / 60
        let totalMins = data.totalSleepMinutes % 60
        let durationFormatted = totalHours > 0 ? "\(totalHours)h \(totalMins)m" : "\(totalMins)m"
        
        let awakeHours = data.awakeMinutes / 60
        let awakeMins = data.awakeMinutes % 60
        let awakeFormatted = awakeHours > 0 ? "\(awakeHours)h \(awakeMins)m" : "\(awakeMins)m"
        
        let interruptions = data.interruptionsCount
        let awakeRatio = Double(data.awakeMinutes) / Double(data.totalSleepMinutes)
        let deepRatio = Double(data.deepMinutes) / Double(data.totalSleepMinutes)
        
        if interruptions >= 5 || awakeRatio >= 0.18 {
            return SleepInsightResult(
                headline: "A fragmented and restless night",
                description: "It looks like your sleep was broken up by over \(interruptions) interruptions, leading to nearly \(awakeFormatted) spent awake.",
                badgeText: "Last night's sleep"
            )
        } else if data.totalSleepMinutes < 330 {
            return SleepInsightResult(
                headline: "Short sleep duration",
                description: "You logged just \(durationFormatted) of sleep last night. Prioritizing an earlier bedtime tonight will help restore your cognitive baseline and muscle recovery.",
                badgeText: "Last night's sleep"
            )
        } else if deepRatio >= 0.20 && data.totalSleepMinutes >= 420 {
            return SleepInsightResult(
                headline: "Deep and restorative rest",
                description: "Great consistency! You achieved optimal deep and REM sleep cycles over \(durationFormatted) with minimal disruption, promoting physical repair and peak focus.",
                badgeText: "Last night's sleep"
            )
        } else if data.totalSleepMinutes >= 540 {
            return SleepInsightResult(
                headline: "Extended restorative slumber",
                description: "You caught up on rest with a solid \(durationFormatted) sleep session. Your body enjoyed ample restorative downtime across all major sleep cycles.",
                badgeText: "Last night's sleep"
            )
        } else {
            return SleepInsightResult(
                headline: "Balanced and steady sleep",
                description: "You logged \(durationFormatted) of steady sleep with smooth transitions across Light, REM, and Deep sleep stages.",
                badgeText: "Last night's sleep"
            )
        }
    }
}

public protocol LocalAiEngine {
    func generateSummary(request: AiSummarizeRequest) async -> String
}

public final class OnDeviceRuleInsightEngine: LocalAiEngine {
    public init() {}
    
    public func generateSummary(request: AiSummarizeRequest) async -> String {
        guard request.deviceSdkAvailable else {
            return "Apple Health is not connected. Grant permission to see your activity, sleep and heart rate summary."
        }
        
        let data = request.healthData
        var sentences: [String] = []
        
        let steps = data.todaySteps
        let prevSteps = data.previousSteps
        let calories = data.todayActiveCaloriesKcal
        let prevCalories = data.previousActiveCaloriesKcal
        let sleepMinutes = data.latestSleepMinutes
        let prevSleepMinutes = data.previousSleepMinutes
        let sleepFormatted = data.latestSleepFormatted
        let exercises = data.exerciseSessions
        
        let hasAnyData = steps != nil || calories != nil || sleepMinutes != nil || data.latestHeartRateBpm != nil || !exercises.isEmpty
        if !hasAnyData {
            return "No health data has been recorded today. Check that Apple Health permissions are enabled."
        }
        
        // RULE 1: STEPS
        if let steps = steps, let prevSteps = prevSteps, prevSteps > 0 {
            let delta = steps - prevSteps
            let percent = (Double(delta) / Double(prevSteps)) * 100.0
            if percent >= 75.0 && delta >= 3500 {
                sentences.append("Steps increased \(Int(percent))% to \(steps.formatted()), up from \(prevSteps.formatted()) yesterday.")
            } else if percent <= -50.0 && prevSteps >= 7000 {
                sentences.append("Steps fell \(abs(Int(percent)))% to \(steps.formatted()), down from \(prevSteps.formatted()) yesterday.")
            }
        } else if let steps = steps {
            if steps >= 14000 {
                sentences.append("You recorded \(steps.formatted()) steps, a high activity level.")
            } else if (1...900).contains(steps) {
                sentences.append("Only \(steps.formatted()) steps recorded so far today.")
            }
        }
        
        // RULE 2: SLEEP
        if let sleepMinutes = sleepMinutes, let prevSleepMinutes = prevSleepMinutes, prevSleepMinutes > 0 {
            let delta = sleepMinutes - prevSleepMinutes
            let deltaHours = Double(abs(delta)) / 60.0
            let fmt = sleepFormatted ?? "\(sleepMinutes / 60)h \(sleepMinutes % 60)m"
            if delta <= -150 {
                sentences.append("Sleep was \(fmt), \(String(format: "%.1f", deltaHours)) hours less than yesterday. Prioritise rest tonight.")
            } else if delta >= 180 {
                sentences.append("Sleep was \(fmt), \(String(format: "%.1f", deltaHours)) hours more than yesterday.")
            }
        } else if let sleepMinutes = sleepMinutes {
            let fmt = sleepFormatted ?? "\(sleepMinutes / 60)h \(sleepMinutes % 60)m"
            if sleepMinutes < 300 {
                sentences.append("Sleep was only \(fmt), below the recommended 7 to 9 hours.")
            } else if sleepMinutes >= 630 {
                sentences.append("Sleep was \(fmt), longer than the typical 7 to 9 hours.")
            }
        }
        
        // RULE 3: CALORIES
        if let calories = calories, let prevCalories = prevCalories, prevCalories > 0 {
            let delta = calories - prevCalories
            let percent = (delta / prevCalories) * 100.0
            if percent >= 80.0 && delta >= 300.0 {
                sentences.append("Active calories rose \(Int(percent))% to \(Int(calories)) kcal, from \(Int(prevCalories)) kcal yesterday.")
            } else if percent <= -60.0 && prevCalories >= 500.0 {
                sentences.append("Active calories fell \(abs(Int(percent)))% compared with yesterday.")
            }
        } else if let calories = calories, calories >= 750 {
            sentences.append("Active calories reached \(Int(calories)) kcal today.")
        }
        
        // RULE 4: WORKOUT SESSIONS
        if !exercises.isEmpty {
            let firstTitle = exercises.first?.title ?? ""
            let workoutTitle = firstTitle.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? "workout" : firstTitle
            if sentences.isEmpty {
                sentences.append("You completed a \(workoutTitle) session today.")
            }
        }
        
        // Fallback
        if sentences.isEmpty {
            sentences.append("Your activity, sleep and heart rate are steady with no notable changes today.")
        }
        
        return sentences.joined(separator: " ")
    }
}
