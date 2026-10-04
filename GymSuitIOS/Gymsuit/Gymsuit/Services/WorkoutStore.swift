import Foundation
import Combine

/// Loads the bundled exercise catalog and persists the user's logged workouts.
/// Logged workouts are also written to HealthKit (see HealthKitManager.saveWorkout)
/// so they appear in Apple Health alongside everything else.
public final class WorkoutStore: ObservableObject {
    public static let shared = WorkoutStore()

    @Published public private(set) var exercises: [Exercise] = []
    @Published public private(set) var loggedWorkouts: [LoggedWorkout] = []

    private let logFileName = "logged_workouts.json"

    private init() {
        loadExercises()
        loadLog()
    }

    // MARK: - Exercise catalog

    private func loadExercises() {
        guard let url = Bundle.main.url(forResource: "exercises", withExtension: "json"),
              let data = try? Data(contentsOf: url),
              let decoded = try? JSONDecoder().decode([Exercise].self, from: data) else {
            return
        }
        exercises = decoded
    }

    public func filteredExercises(query: String, muscleGroup: String?) -> [Exercise] {
        exercises.filter { exercise in
            let matchesQuery = query.isEmpty ||
                exercise.name.localizedCaseInsensitiveContains(query) ||
                exercise.primaryMuscle.localizedCaseInsensitiveContains(query)
            let matchesGroup = muscleGroup == nil || exercise.muscleGroup == muscleGroup
            return matchesQuery && matchesGroup
        }
    }

    // MARK: - Workout log persistence

    private var logFileURL: URL {
        FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
            .appendingPathComponent(logFileName)
    }

    private func loadLog() {
        guard let data = try? Data(contentsOf: logFileURL),
              let decoded = try? JSONDecoder().decode([LoggedWorkout].self, from: data) else {
            return
        }
        loggedWorkouts = decoded.sorted { $0.date > $1.date }
    }

    private func persistLog() {
        let data = try? JSONEncoder().encode(loggedWorkouts)
        try? data?.write(to: logFileURL, options: .atomic)
    }

    public func logWorkout(_ workout: LoggedWorkout) {
        loggedWorkouts.insert(workout, at: 0)
        loggedWorkouts.sort { $0.date > $1.date }
        persistLog()
    }

    public func upsertWorkout(_ workout: LoggedWorkout) {
        if let idx = loggedWorkouts.firstIndex(where: { $0.id == workout.id }) {
            loggedWorkouts[idx] = workout
        } else {
            loggedWorkouts.insert(workout, at: 0)
        }
        loggedWorkouts.sort { $0.date > $1.date }
        persistLog()
    }

    public func markSynced(id: UUID) {
        guard let index = loggedWorkouts.firstIndex(where: { $0.id == id }) else { return }
        loggedWorkouts[index].syncedToHealthKit = true
        persistLog()
    }

    public func deleteWorkout(id: UUID) {
        loggedWorkouts.removeAll { $0.id == id }
        persistLog()
    }
}
