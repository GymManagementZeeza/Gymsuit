import Foundation

// MARK: - Exercise catalog (bundled exercises.json, exercise metadata sourced from
// DuarteSantos8/openGym's EXDB dataset. Exercise names, muscle data and instructions
// are MIT-licensed. Exercise GIFs are our own dataset, served by the backend
// on demand — the old Gym Visual remote GIFs are no longer used.)

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

    /// Exercise GIF (own dataset), served by the backend on demand.
    /// Nil when this exercise has no GIF yet — the old Gym Visual remote
    /// GIFs are no longer used.
    public var gifURL: URL? {
        guard let file = ExerciseMedia.localGIFFile(for: name) else { return nil }
        return URL(string: ExerciseMedia.gifBaseURL + file)
    }

    public var imageURL: URL? {
        img.isEmpty ? nil : URL(string: ExerciseMedia.imageBaseURL + img)
    }

    public var primaryMuscleLabel: String {
        primaryMuscle.isEmpty ? bodyPart.capitalized : primaryMuscle.capitalized
    }
}

public enum ExerciseMedia {
    public static let imageBaseURL = "https://raw.githubusercontent.com/hasaneyldrm/exercises-dataset/main/images/"

    /// Backend endpoint serving the exercise GIFs on demand, so the app
    /// doesn't bundle them. Same hardcoded host as the other API clients —
    /// repoint it together with them when moving to the self-hosted server.
    public static let gifBaseURL = "https://api.gymsuit.app/api/exercise-gifs/"

    /// Exercise GIF dataset index (own GIFs, served by the backend).
    /// An exercise shows a GIF only if its exact (lowercased) name is in
    /// this map. To add a GIF: drop the file in the backend's
    /// src/main/resources/exercise-gifs/ and add one line here.
    /// NOTE: squat.gif is on the backend but unmapped — the catalog has no
    /// plain bodyweight-squat entry. Map it when one is added.
    private static let localGIFs: [String: String] = [
        "barbell bench press": "bench-press.gif",
        "barbell full squat": "weighted-squat.gif",
        "barbell deadlift": "deadlift.gif",
        "dumbbell biceps curl": "dumbbell-curl.gif",
        "dumbbell hammer curl": "hammer-curl.gif",
        "lever seated fly": "machine-chest-fly.gif",
        "dumbbell front raise": "dumbbell-front-raise.gif"
    ]

    public static func localGIFFile(for exerciseName: String) -> String? {
        localGIFs[exerciseName.lowercased().trimmingCharacters(in: .whitespaces)]
    }
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
    case endCollectionFailed
    case finishFailed
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

