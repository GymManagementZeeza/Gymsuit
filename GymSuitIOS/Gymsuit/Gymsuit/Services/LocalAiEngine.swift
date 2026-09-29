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
            return "Apple Health isn't connected yet! We can't spy on your workout heroics (or your afternoon couch hibernation) until you grant permission."
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
            return "Zero data logged for today! Either you discovered teleportation, or you've perfected the ancient art of becoming a statue. Go move a muscle!"
        }
        
        // RULE 1: STEPS
        if let steps = steps, let prevSteps = prevSteps, prevSteps > 0 {
            let delta = steps - prevSteps
            let percent = (Double(delta) / Double(prevSteps)) * 100.0
            if percent >= 75.0 && delta >= 3500 {
                sentences.append("Whoa, someone set your sneakers on fire! You leaped from \(prevSteps.formatted()) to \(steps.formatted()) steps today (+\(Int(percent))% surge). Were you running away from responsibilities or chasing down the ice cream truck?")
            } else if percent <= -50.0 && prevSteps >= 7000 {
                sentences.append("Your step count took a hilarious nose-dive from \(prevSteps.formatted()) yesterday down to \(steps.formatted()) today (-\(abs(Int(percent)))% drop). Did your couch develop gravitational pull?")
            }
        } else if let steps = steps {
            if steps >= 14000 {
                sentences.append("You clocked a wild \(steps.formatted()) steps! Are you training for an ultramarathon or did you lose your car keys in a corn maze?")
            } else if (1...900).contains(steps) {
                sentences.append("You've only clocked \(steps.formatted()) steps today. Even a three-toed sloth is looking at your step tracker with judgment.")
            }
        }
        
        // RULE 2: SLEEP
        if let sleepMinutes = sleepMinutes, let prevSleepMinutes = prevSleepMinutes, prevSleepMinutes > 0 {
            let delta = sleepMinutes - prevSleepMinutes
            let deltaHours = Double(abs(delta)) / 60.0
            if delta <= -150 {
                let fmt = sleepFormatted ?? "\(sleepMinutes / 60)h \(sleepMinutes % 60)m"
                sentences.append("Sleep alert: you dropped \(String(format: "%.1f", deltaHours)) hours of sleep compared to yesterday, waking up after just \(fmt). Fueled purely by iced coffee and chaotic energy today!")
            } else if delta >= 180 {
                let fmt = sleepFormatted ?? "\(sleepMinutes / 60)h \(sleepMinutes % 60)m"
                sentences.append("Rip Van Winkle award goes to you today! You slept \(fmt) (\(String(format: "%.1f", deltaHours)) hours longer than yesterday). Your bed must be thanking you for the thorough inspection.")
            }
        } else if let sleepMinutes = sleepMinutes {
            if sleepMinutes < 300 {
                sentences.append("A whopping \(sleepFormatted ?? "\(sleepMinutes / 60)h") of sleep logged. You're practically operating in zombie mode — please avoid operating heavy machinery and don't reply to risky texts.")
            } else if sleepMinutes >= 630 {
                sentences.append("\(sleepFormatted ?? "\(sleepMinutes / 60)h") of slumber recorded! That wasn't just a nap, you were in hibernation.")
            }
        }
        
        // RULE 3: CALORIES
        if let calories = calories, let prevCalories = prevCalories, prevCalories > 0 {
            let delta = calories - prevCalories
            let percent = (delta / prevCalories) * 100.0
            if percent >= 80.0 && delta >= 300.0 {
                sentences.append("Calorie burn exploded by +\(Int(percent))% today (\(Int(calories)) kcal vs \(Int(prevCalories)) kcal yesterday). Absolute beast mode!")
            } else if percent <= -60.0 && prevCalories >= 500.0 {
                sentences.append("Active calorie burn tanked by \(abs(Int(percent)))% compared to yesterday. Today was clearly dedicated to aggressive energy conservation.")
            }
        } else if let calories = calories, calories >= 750 {
            sentences.append("Torched \(Int(calories)) active calories today! You basically incinerated dinner before even eating it.")
        }
        
        // Fallbacks
        if sentences.isEmpty {
            let steadyJokes = [
                "No wild plot twists in your metrics today — you were suspiciously consistent. Keep it up, steady Eddie!",
                "Your numbers today are so consistent you might actually be a well-calibrated robot. Keep rolling!",
                "No dramatic health drama or sudden marathons detected today. Just smooth, respectable, drama-free living.",
                "Smooth sailing across your metrics today. Neither lazy nor completely out of your mind — perfectly balanced, as all things should be."
            ]
            let idx = Int(steps ?? 0) % steadyJokes.count
            sentences.append(steadyJokes[idx])
        } else {
            let signoffs = [
                "Drink some water and stay legendary!",
                "Don't let your couch plot revenge tomorrow!",
                "Keep this energy up and tomorrow might just be legendary!",
                "Listen to your body (and maybe stretch before getting off that chair)!"
            ]
            let sIdx = Int(steps ?? 0) % signoffs.count
            sentences.append(signoffs[sIdx])
        }
        
        return sentences.joined(separator: " ")
    }
}
