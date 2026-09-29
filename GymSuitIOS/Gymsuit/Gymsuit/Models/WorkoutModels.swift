import Foundation

// MARK: - Exercise catalog (bundled exercises.json, sourced from the open-source
// Glowupp-app/open-exercisedb dataset, MIT licensed)

public struct Exercise: Codable, Identifiable, Hashable {
    public let id: String
    public let name: String
    public let description: String
    public let difficulty: Int
    public let equipment: [String]
    public let primaryMuscle: String
    public let secondaryMuscles: [String]
    public let typicalSetsReps: String?
    public let executionTips: [String]

    enum CodingKeys: String, CodingKey {
        case id
        case name
        case description
        case difficulty
        case equipment
        case primaryMuscle = "primary_muscle"
        case secondaryMuscles = "secondary_muscles"
        case typicalSetsReps = "typical_sets_reps"
        case executionTips = "execution_tips"
    }

    public var muscleGroup: String {
        ExerciseMuscleGroups.group(for: primaryMuscle)
    }

    public var equipmentLabel: String {
        equipment.map { $0.capitalized }.joined(separator: ", ")
    }
}

public enum ExerciseMuscleGroups {
    public static let all = ["Chest", "Back", "Shoulders", "Arms", "Legs", "Core", "Full Body", "Cardio", "Other"]

    public static func group(for muscle: String) -> String {
        switch muscle {
        case "Chest", "Pectoralis Major":
            return "Chest"
        case "Back", "Back (Full)", "Latissimus Dorsi", "Rhomboids", "Erector Spinae":
            return "Back"
        case "Shoulders", "Deltoids":
            return "Shoulders"
        case "Biceps", "Biceps Brachii", "Brachialis", "Brachioradialis", "Triceps", "Forearms (Front)":
            return "Arms"
        case "Quadriceps", "Hamstrings", "Glutes", "Gluteus Medius", "Calves", "Gastrocnemius (Calves)":
            return "Legs"
        case "Core", "Rectus Abdominis", "Rectus Abdominis (Lower)", "Lower Rectus Abdominis", "Obliques":
            return "Core"
        case "Full Body":
            return "Full Body"
        case "Cardio":
            return "Cardio"
        default:
            return "Other"
        }
    }
}

// MARK: - Logged workout

public enum WorkoutSaveError: Error {
    case collectionFailed
}

public struct WorkoutSet: Codable, Identifiable, Hashable {
    public var id = UUID()
    public var reps: Int
    public var weightKg: Double
}

public struct LoggedWorkout: Codable, Identifiable {
    public var id = UUID()
    public let exerciseId: String
    public let exerciseName: String
    public let date: Date
    public var sets: [WorkoutSet]
    public var durationMinutes: Int
    public var notes: String
    public var syncedToHealthKit: Bool

    public var totalReps: Int {
        sets.reduce(0) { $0 + $1.reps }
    }

    public var totalVolumeKg: Double {
        sets.reduce(0) { $0 + Double($1.reps) * $1.weightKg }
    }
}
