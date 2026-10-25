import Foundation

/// The workout a trainer sent to the signed-in member for today.
/// GET /api/mobile/trainer-workout/today — a 404 means nothing was sent today.
struct TrainerWorkoutSuggestion: Codable {
    let id: Int64
    let date: String
    let trainerName: String
    let message: String?
    let exercises: [SuggestedWorkoutExercise]
}

struct SuggestedWorkoutExercise: Codable, Identifiable {
    var id: String { exerciseName }
    let exerciseName: String
    let sets: Int
    let reps: Int
    let weightKg: Double
}

final class TrainerWorkoutApi {
    static let shared = TrainerWorkoutApi()

    private let baseURL = "https://api.gymsuit.app/api/mobile/trainer-workout"

    private init() {}

    func fetchToday() async throws -> TrainerWorkoutSuggestion {
        guard let url = URL(string: "\(baseURL)/today") else {
            throw URLError(.badURL)
        }
        guard let token = AuthManager.shared.getAccessToken(), !token.isEmpty else {
            throw NSError(
                domain: "TrainerWorkoutApi",
                code: 401,
                userInfo: [NSLocalizedDescriptionKey: "You are not signed in. Please log in again."]
            )
        }

        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")

        let (data, response) = try await URLSession.shared.data(for: request)
        guard let httpResponse = response as? HTTPURLResponse else {
            throw URLError(.badServerResponse)
        }
        guard (200..<300).contains(httpResponse.statusCode) else {
            throw NSError(
                domain: "TrainerWorkoutApi",
                code: httpResponse.statusCode,
                userInfo: [NSLocalizedDescriptionKey: "No trainer workout for today."]
            )
        }
        return try JSONDecoder().decode(TrainerWorkoutSuggestion.self, from: data)
    }
}
