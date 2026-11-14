import Foundation

/// The meal plan the member's nutritionist sent: today's, or the most recent earlier one.
/// GET /api/mobile/meal-plan/current — a 404 means no plan has been sent.
struct MealPlan: Codable, Equatable {
    let id: Int64
    let date: String
    let nutritionistName: String
    let breakfast: String?
    let lunch: String?
    let eveningSnack: String?
    let dinner: String?
    let note: String?
}

final class MealPlanApi {
    static let shared = MealPlanApi()

    private let baseURL = "https://api.gymsuit.app/api/mobile/meal-plan"

    private init() {}

    func fetchCurrent() async throws -> MealPlan {
        guard let url = URL(string: "\(baseURL)/current") else {
            throw URLError(.badURL)
        }
        guard let token = AuthManager.shared.getAccessToken(), !token.isEmpty else {
            throw NSError(
                domain: "MealPlanApi",
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
                domain: "MealPlanApi",
                code: httpResponse.statusCode,
                userInfo: [NSLocalizedDescriptionKey: "No meal plan yet."]
            )
        }
        return try JSONDecoder().decode(MealPlan.self, from: data)
    }
}
