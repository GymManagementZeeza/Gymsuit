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
    private let remoteExercisesFileName = "remote_exercises_cache.json"

    private init() {
        loadExercises()
        loadLog()
    }

    // MARK: - Exercise catalog

    private var remoteExercisesFileURL: URL {
        FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
            .appendingPathComponent(remoteExercisesFileName)
    }

    private func loadExercises() {
        // 1. Try to load previously cached remote exercises
        if let data = try? Data(contentsOf: remoteExercisesFileURL),
           let decoded = try? JSONDecoder().decode([Exercise].self, from: data),
           !decoded.isEmpty {
            exercises = decoded
            return
        }

        // 2. Fall back to bundled initial exercises.json
        guard let url = Bundle.main.url(forResource: "exercises", withExtension: "json"),
              let data = try? Data(contentsOf: url),
              let decoded = try? JSONDecoder().decode([Exercise].self, from: data) else {
            return
        }
        exercises = decoded
    }

    /// Fetches the latest dynamic exercise GIFs from the backend API.
    /// Called whenever the workout screen is opened to ensure daily newly
    /// uploaded GIFs are immediately available in the app.
    public func fetchLatestExercisesFromBackend() async {
        guard let url = URL(string: ExerciseMedia.gifBaseURL) else { return }
        do {
            var request = URLRequest(url: url)
            request.timeoutInterval = 10
            request.setValue("application/json", forHTTPHeaderField: "Accept")
            let (data, response) = try await URLSession.shared.data(for: request)
            if let http = response as? HTTPURLResponse, http.statusCode == 200, !data.isEmpty {
                let decoded = try JSONDecoder().decode([Exercise].self, from: data)
                if !decoded.isEmpty {
                    await MainActor.run {
                        self.exercises = decoded
                    }
                    try? data.write(to: self.remoteExercisesFileURL, options: .atomic)
                }
            }
        } catch {
            print("Notice: using cached exercises: \(error.localizedDescription)")
        }
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

    public func clearAllWorkouts() {
        loggedWorkouts.removeAll()
        persistLog()
    }
}
