import Foundation

// MARK: - Exercise catalog (bundled exercises.json, exercise metadata sourced from
// DuarteSantos8/openGym's EXDB dataset. Exercise names, muscle data and instructions
// are MIT-licensed; the GIF/JPG media is (c) Gym Visual - see NOTICE in PR #22.)

public struct Exercise: Codable, Identifiable, Hashable {
    public let id: String
    public let name: String
    public let bodyPart: String
    public let equipment: String
    public let primaryMuscle: String
    public let secondaryMuscles: [String]
    public let instructions: [String]
    public let gif: String
    public let img: String

    enum CodingKeys: String, CodingKey {
        case id
        case name
        case bodyPart = "body_part"
        case equipment
        case primaryMuscle = "primary_muscle"
        case secondaryMuscles = "secondary_muscles"
        case instructions
        case gif
        case img
    }

    public var muscleGroup: String {
        ExerciseMuscleGroups.group(for: bodyPart)
    }

    public var gifURL: URL? {
        gif.isEmpty ? nil : URL(string: ExerciseMedia.gifBaseURL + gif)
    }

    public var imageURL: URL? {
        img.isEmpty ? nil : URL(string: ExerciseMedia.imageBaseURL + img)
    }

    public var primaryMuscleLabel: String {
        primaryMuscle.isEmpty ? bodyPart.capitalized : primaryMuscle.capitalized
    }
}

public enum ExerciseMedia {
    public static let gifBaseURL = "https://raw.githubusercontent.com/hasaneyldrm/exercises-dataset/main/videos/"
    public static let imageBaseURL = "https://raw.githubusercontent.com/hasaneyldrm/exercises-dataset/main/images/"
}

public enum ExerciseMuscleGroups {
    public static let all = ["Chest", "Back", "Shoulders", "Arms", "Legs", "Core", "Cardio", "Other"]

    public static func group(for bodyPart: String) -> String {
        switch bodyPart {
        case "chest":
            return "Chest"
        case "back":
            return "Back"
        case "shoulders":
            return "Shoulders"
        case "upper arms", "lower arms":
            return "Arms"
        case "upper legs", "lower legs":
            return "Legs"
        case "waist":
            return "Core"
        case "cardio":
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
