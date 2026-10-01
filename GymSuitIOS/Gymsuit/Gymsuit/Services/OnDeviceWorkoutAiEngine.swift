import Foundation
import Combine

// MARK: - Workout body parts (ported from Android `defaultBodyParts` in WorkoutsScreen.kt)
//
// The parallel HealthKit agent is adding these to HealthKitManager.swift (same module):
//   public struct HealthWorkoutSession { public let date: Date; public let title: String; ... }
//   public struct HealthWorkoutSignals { public let sessions: [HealthWorkoutSession]; public let lastSleepMinutes: Int? }
// This engine only reads `sessions[].date`, `sessions[].title` and `lastSleepMinutes`.

public enum WorkoutBodyPart: String, CaseIterable {
    case chest
    case back
    case shoulders
    case biceps
    case triceps
    case legs
    case core
    case calves

    public var displayName: String {
        switch self {
        case .chest: return "Chest"
        case .back: return "Back"
        case .shoulders: return "Shoulders"
        case .biceps: return "Biceps"
        case .triceps: return "Triceps"
        case .legs: return "Legs"
        case .core: return "Core/Abs"
        case .calves: return "Calves"
        }
    }

    /// First word of the display name, uppercased — e.g. "CHEST", "CORE" (matches Android's substringBefore(" /")).
    var badgeWord: String {
        displayName.split(separator: "/").first.map { $0.trimmingCharacters(in: .whitespaces).uppercased() } ?? displayName.uppercased()
    }
}

// MARK: - Recommendation result

public struct WorkoutRecommendation {
    public let headline: String
    public let splitBadge: String
    public let description: String
    public let recommendedParts: [WorkoutBodyPart]
    public let recoveryNote: String?
    public let isDone: Bool
}

// MARK: - Workout split presets

public struct WorkoutSplitExercise {
    public let exerciseName: String
    public let catalogExerciseId: String
    public let targetSets: Int
    public let targetReps: Int
    public let targetWeightKg: Double
}

public struct WorkoutSplit {
    public let name: String // "Push" / "Pull" / "Legs"
    public let parts: [WorkoutBodyPart]
    public let exercises: [WorkoutSplitExercise]
}

// MARK: - On-device workout recommendation engine (ported from Android OnDeviceWorkoutAiEngine)

public final class OnDeviceWorkoutAiEngine {

    private static let recommendCount = 2
    private static let shortSleepMinutes = 360

    public static let defaultBodyParts: [WorkoutBodyPart] = [
        .chest, .back, .shoulders, .biceps, .triceps, .legs, .core, .calves
    ]

    public static let workoutSplits: [WorkoutSplit] = [
        WorkoutSplit(
            name: "Push",
            parts: [.chest, .shoulders, .triceps],
            exercises: [
                WorkoutSplitExercise(exerciseName: "Barbell Bench Press", catalogExerciseId: "0025", targetSets: 4, targetReps: 8, targetWeightKg: 65.0),
                WorkoutSplitExercise(exerciseName: "Overhead Shoulder Press", catalogExerciseId: "0997", targetSets: 3, targetReps: 10, targetWeightKg: 40.0),
                WorkoutSplitExercise(exerciseName: "Incline Dumbbell Press", catalogExerciseId: "1254", targetSets: 3, targetReps: 10, targetWeightKg: 22.0),
                WorkoutSplitExercise(exerciseName: "Triceps Dips", catalogExerciseId: "0019", targetSets: 3, targetReps: 12, targetWeightKg: 0.0),
            ]
        ),
        WorkoutSplit(
            name: "Pull",
            parts: [.back, .biceps],
            exercises: [
                WorkoutSplitExercise(exerciseName: "Pull-Ups", catalogExerciseId: "0652", targetSets: 4, targetReps: 8, targetWeightKg: 0.0),
                WorkoutSplitExercise(exerciseName: "Barbell Bent Over Row", catalogExerciseId: "0027", targetSets: 4, targetReps: 8, targetWeightKg: 60.0),
                WorkoutSplitExercise(exerciseName: "Cable Lat Pulldown", catalogExerciseId: "2330", targetSets: 3, targetReps: 10, targetWeightKg: 50.0),
                WorkoutSplitExercise(exerciseName: "Bicep Curls", catalogExerciseId: "1634", targetSets: 3, targetReps: 12, targetWeightKg: 14.0),
            ]
        ),
        WorkoutSplit(
            name: "Legs",
            parts: [.legs, .calves],
            exercises: [
                WorkoutSplitExercise(exerciseName: "Squats", catalogExerciseId: "0043", targetSets: 4, targetReps: 10, targetWeightKg: 70.0),
                WorkoutSplitExercise(exerciseName: "Romanian Deadlifts", catalogExerciseId: "0085", targetSets: 4, targetReps: 8, targetWeightKg: 80.0),
                WorkoutSplitExercise(exerciseName: "Leg Press", catalogExerciseId: "0739", targetSets: 3, targetReps: 10, targetWeightKg: 120.0),
                WorkoutSplitExercise(exerciseName: "Lunges", catalogExerciseId: "0336", targetSets: 3, targetReps: 12, targetWeightKg: 16.0),
            ]
        ),
    ]

    // MARK: - Body-part mapping (same rules as Android)

    /// Maps a catalog exercise to one of the default body parts.
    public static func bodyPartName(for exercise: Exercise) -> WorkoutBodyPart? {
        switch exercise.bodyPart {
        case "chest": return .chest
        case "back": return .back
        case "shoulders": return .shoulders
        case "upper arms":
            return exercise.primaryMuscle.range(of: "tricep", options: .caseInsensitive) != nil ? .triceps : .biceps
        case "lower arms": return .biceps
        case "upper legs": return .legs
        case "lower legs": return .calves
        case "waist": return .core
        default: return nil
        }
    }

    /// Keyword matching for HealthKit workout titles (same order as Android — order matters).
    public static func bodyPartFromTitle(_ title: String) -> WorkoutBodyPart? {
        let t = title.lowercased()
        if t.contains("chest") || t.contains("bench") || t.contains("push") { return .chest }
        if t.contains("back") || t.contains("pull") || t.contains("row") { return .back }
        if t.contains("shoulder") { return .shoulders }
        if t.contains("bicep") || t.contains("curl") { return .biceps }
        if t.contains("tricep") { return .triceps }
        if t.contains("leg") || t.contains("squat") || t.contains("lunge") { return .legs }
        if t.contains("calf") || t.contains("calves") { return .calves }
        if t.contains("core") || t.contains("abs") || t.contains("plank") { return .core }
        return nil
    }

    // MARK: - Recommendation (synchronous; UI handles shimmer timing)

    public static func generateRecommendation(
        selectedDate: Date,
        exercises: [Exercise],
        loggedWorkouts: [LoggedWorkout],
        signals: HealthWorkoutSignals
    ) -> WorkoutRecommendation {
        let calendar = Calendar.current
        let selectedDay = calendar.startOfDay(for: selectedDate)
        var byId: [String: Exercise] = [:]
        for exercise in exercises { byId[exercise.id] = exercise } // last wins, like Kotlin associateBy

        func day(_ date: Date) -> Date { calendar.startOfDay(for: date) }
        func daysBetween(_ from: Date, _ to: Date) -> Int {
            calendar.dateComponents([.day], from: day(from), to: day(to)).day ?? 0
        }

        // Body part -> most recent day it was trained (up to and including the selected date)
        var lastTrained: [WorkoutBodyPart: Date] = [:]
        for log in loggedWorkouts {
            let logDay = day(log.date)
            guard logDay <= selectedDay else { continue }
            let exercise = byId[log.exerciseId]
                ?? exercises.first { $0.name.compare(log.exerciseName, options: .caseInsensitive) == .orderedSame }
            guard let exercise, let part = bodyPartName(for: exercise) else { continue }
            if lastTrained[part].map({ $0 < logDay }) ?? true {
                lastTrained[part] = logDay
            }
        }

        // HealthKit sessions: map to a body part when the title names one, else count as general load
        var hkLoadDays = Set<Date>()
        var hasUnmappedHkToday = false
        for session in signals.sessions {
            let sessionDay = day(session.date)
            guard sessionDay <= selectedDay else { continue }
            hkLoadDays.insert(sessionDay)
            let part = exercises.first { $0.name.compare(session.title, options: .caseInsensitive) == .orderedSame }
                .flatMap { bodyPartName(for: $0) }
                ?? bodyPartFromTitle(session.title)
            if let part {
                if lastTrained[part].map({ $0 < sessionDay }) ?? true {
                    lastTrained[part] = sessionDay
                }
            } else if calendar.isDate(session.date, inSameDayAs: selectedDate) {
                hasUnmappedHkToday = true
            }
        }

        let trainedToday = defaultBodyParts.filter {
            lastTrained[$0].map { calendar.isDate($0, inSameDayAs: selectedDate) } ?? false
        }

        if trainedToday.count + (hasUnmappedHkToday ? 1 : 0) >= recommendCount {
            let description: String
            if trainedToday.isEmpty {
                description = "HealthKit shows a workout today. That's enough for today, so rest up and recover."
            } else {
                let names = trainedToday.map(\.displayName).joined(separator: " & ")
                description = "You trained \(names) today\(hasUnmappedHkToday ? " plus a HealthKit workout" : ""). That's enough for today, so rest up and recover."
            }
            return WorkoutRecommendation(
                headline: "Good job today!",
                splitBadge: "DONE",
                description: description,
                recommendedParts: [],
                recoveryNote: nil,
                isDone: true
            )
        }

        // Never-trained first, then the longest-rested; anything already done today is skipped.
        // Index tiebreak keeps defaultBodyParts order (Android's sortedByDescending is stable).
        let candidates = defaultBodyParts
            .enumerated()
            .filter { !trainedToday.contains($0.element) }
            .sorted { lhs, rhs in
                let lhsDays = lastTrained[lhs.element].map { daysBetween($0, selectedDate) } ?? Int.max
                let rhsDays = lastTrained[rhs.element].map { daysBetween($0, selectedDate) } ?? Int.max
                if lhsDays != rhsDays { return lhsDays > rhsDays }
                return lhs.offset < rhs.offset
            }
            .map(\.element)

        // Recovery check from HealthKit: recent training load and last night's sleep
        let loggedDays = Set(lastTrained.values).union(hkLoadDays)
        let recentLoadDays = (1...3).filter { offset in
            guard let d = calendar.date(byAdding: .day, value: -offset, to: selectedDate) else { return false }
            return loggedDays.contains(day(d))
        }.count
        let shortSleep = (signals.lastSleepMinutes ?? Int.max) < shortSleepMinutes
        let heavyLoad = recentLoadDays >= 3
        let pickCount = (shortSleep || heavyLoad) ? 1 : recommendCount
        let picks = Array(candidates.prefix(pickCount))

        var noteParts: [String] = []
        if shortSleep, let m = signals.lastSleepMinutes {
            noteParts.append("You slept \(m / 60)h \(m % 60)m, so keep the volume light.")
        }
        if heavyLoad {
            noteParts.append("You've trained 3 days in a row, so don't overdo it.")
        }
        let recoveryNote = noteParts.isEmpty ? nil : noteParts.joined(separator: " ")

        let reasons = picks.map { part -> String in
            guard let last = lastTrained[part] else {
                return "\(part.displayName) hasn't been trained yet."
            }
            let days = daysBetween(last, selectedDate)
            return "\(part.displayName) was last trained \(days) day\(days == 1 ? "" : "s") ago."
        }.joined(separator: " ")
        let intro = trainedToday.isEmpty
            ? ""
            : "You already trained \(trainedToday.map(\.displayName).joined(separator: ", ")) today, so switch to something fresh. "

        return WorkoutRecommendation(
            headline: trainedToday.isEmpty ? "Today's Recommendation" : "Next Up",
            splitBadge: picks.map(\.badgeWord).joined(separator: " & "),
            description: intro + reasons,
            recommendedParts: picks,
            recoveryNote: recoveryNote,
            isDone: false
        )
    }
}

// MARK: - Workout session state (ported from Android WorkoutSessionState)

/// Hoisted state of one running workout: stopwatch plus what has been completed per body part.
public final class WorkoutSessionState: ObservableObject {
    public static let exercisesPerPartTarget = 3

    @Published public var active: Bool = false
    @Published public var parts: [WorkoutBodyPart] = []
    @Published public var elapsedSeconds: Int = 0
    @Published public var running: Bool = false

    private struct DoneExercise {
        let exerciseId: String
        let part: WorkoutBodyPart
        let sets: Int
    }
    private var done: [DoneExercise] = []

    public init() {}

    public func start(bodyParts: [WorkoutBodyPart]) {
        parts = bodyParts
        elapsedSeconds = 0
        running = true
        done.removeAll()
        active = true
    }

    public func end() {
        active = false
        running = false
        done.removeAll()
    }

    public func tick() {
        elapsedSeconds += 1
    }

    public func record(exercise: Exercise, part: WorkoutBodyPart, sets: Int) {
        objectWillChange.send()
        done.append(DoneExercise(exerciseId: exercise.id, part: part, sets: sets))
    }

    /// Distinct exercises completed for a body part.
    public func exercisesDone(part: WorkoutBodyPart) -> Int {
        Set(done.filter { $0.part == part }.map(\.exerciseId)).count
    }

    public func setsDone(part: WorkoutBodyPart) -> Int {
        done.filter { $0.part == part }.reduce(0) { $0 + $1.sets }
    }

    public func isExerciseDone(exerciseId: String) -> Bool {
        done.contains { $0.exerciseId == exerciseId }
    }

    public func progress(part: WorkoutBodyPart) -> Float {
        min(max(Float(exercisesDone(part: part)) / Float(Self.exercisesPerPartTarget), 0), 1)
    }

    public func overallProgress() -> Float {
        guard !parts.isEmpty else { return 0 }
        return parts.map { progress(part: $0) }.reduce(0, +) / Float(parts.count)
    }
}
