import Foundation

/// Network client for the group fitness challenge endpoints.
/// Base: https://api.gymsuit.app/api/mobile/challenges — JWT Bearer required.
public final class ChallengeApi {
    public static let shared = ChallengeApi()

    private let baseURL = "https://api.gymsuit.app/api/mobile/challenges"
    private let timeoutInterval: TimeInterval = 15.0

    public init() {}

    // MARK: - Endpoints

    /// POST / — create a challenge. Dates are "yyyy-MM-dd".
    public func createChallenge(
        name: String,
        description: String?,
        metricType: ChallengeMetricType,
        startDate: String,
        endDate: String
    ) async throws -> ChallengeDetail {
        var payload: [String: Any] = [
            "name": name.trimmingCharacters(in: .whitespacesAndNewlines),
            "metricType": metricType.rawValue,
            "startDate": startDate,
            "endDate": endDate
        ]
        let trimmedDescription = description?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        if !trimmedDescription.isEmpty {
            payload["description"] = trimmedDescription
        }
        let data = try await request(method: "POST", urlString: baseURL, payload: payload)
        return try decode(ChallengeDetail.self, from: data)
    }

    /// GET / — list challenge summaries.
    public func fetchChallenges() async throws -> [ChallengeSummary] {
        let data = try await request(method: "GET", urlString: baseURL, payload: nil)
        return try decode([ChallengeSummary].self, from: data)
    }

    /// GET /{id} — full detail incl. leaderboard.
    public func fetchDetail(id: Int64) async throws -> ChallengeDetail {
        let data = try await request(method: "GET", urlString: "\(baseURL)/\(id)", payload: nil)
        return try decode(ChallengeDetail.self, from: data)
    }

    /// POST /join {code} — join via invite code.
    public func join(code: String) async throws -> ChallengeDetail {
        let payload: [String: Any] = [
            "code": code.trimmingCharacters(in: .whitespacesAndNewlines)
        ]
        let data = try await request(method: "POST", urlString: "\(baseURL)/join", payload: payload)
        return try decode(ChallengeDetail.self, from: data)
    }

    /// POST /{id}/invites {email} — invite someone by email.
    public func invite(email: String, challengeId: Int64) async throws -> String {
        let payload: [String: Any] = [
            "email": email.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        ]
        let data = try await request(method: "POST", urlString: "\(baseURL)/\(challengeId)/invites", payload: payload)
        let response = try decode(ChallengeMessageResponse.self, from: data)
        return response.message ?? "Invite sent"
    }

    /// POST /{id}/progress — submit HealthKit totals for the challenge window.
    public func postProgress(
        challengeId: Int64,
        steps: Int64,
        workouts: Int,
        calories: Double,
        distanceKm: Double
    ) async throws -> String {
        let payload: [String: Any] = [
            "steps": steps,
            "workouts": workouts,
            "calories": calories,
            "distanceKm": distanceKm
        ]
        let data = try await request(method: "POST", urlString: "\(baseURL)/\(challengeId)/progress", payload: payload)
        let response = try decode(ChallengeMessageResponse.self, from: data)
        return response.message ?? "Progress synced"
    }

    /// POST /{id}/leave — leave the challenge.
    public func leaveChallenge(id: Int64) async throws -> String {
        let data = try await request(method: "POST", urlString: "\(baseURL)/\(id)/leave", payload: nil)
        let response = try decode(ChallengeMessageResponse.self, from: data)
        return response.message ?? "You left the challenge"
    }

    // MARK: - HTTP Helpers

    private func request(method: String, urlString: String, payload: [String: Any]?) async throws -> Data {
        guard let url = URL(string: urlString) else {
            throw URLError(.badURL)
        }
        guard let token = AuthManager.shared.getAccessToken(), !token.isEmpty else {
            throw NSError(
                domain: "ChallengeApi",
                code: 401,
                userInfo: [NSLocalizedDescriptionKey: "You are not signed in. Please log in again."]
            )
        }

        var request = URLRequest(url: url)
        request.httpMethod = method
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        request.timeoutInterval = timeoutInterval
        if let payload = payload {
            request.httpBody = try JSONSerialization.data(withJSONObject: payload)
        }

        let (data, response) = try await URLSession.shared.data(for: request)

        guard let httpResponse = response as? HTTPURLResponse else {
            throw URLError(.badServerResponse)
        }

        if (200...299).contains(httpResponse.statusCode) {
            return data
        } else {
            if let errorJson = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
               let message = errorJson["message"] as? String, !message.isEmpty {
                throw NSError(domain: "ChallengeApi", code: httpResponse.statusCode, userInfo: [NSLocalizedDescriptionKey: message])
            }
            let errorText = String(data: data, encoding: .utf8) ?? "HTTP \(httpResponse.statusCode)"
            throw NSError(domain: "ChallengeApi", code: httpResponse.statusCode, userInfo: [NSLocalizedDescriptionKey: errorText])
        }
    }

    private func decode<T: Decodable>(_ type: T.Type, from data: Data) throws -> T {
        do {
            return try JSONDecoder().decode(T.self, from: data)
        } catch {
            throw NSError(
                domain: "ChallengeApi",
                code: -1,
                userInfo: [NSLocalizedDescriptionKey: "Could not understand the server response."]
            )
        }
    }
}
