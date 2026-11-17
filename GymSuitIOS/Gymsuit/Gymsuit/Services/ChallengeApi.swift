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

    /// POST /{id}/progress/daily — upsert this device's per-day values for the
    /// challenge window. The backend clamps entries to the challenge dates.
    public func postDailyProgress(challengeId: Int64, entries: [DailyProgressEntry]) async throws -> String {
        let payload: [String: Any] = [
            "entries": entries.map { entry in
                ["date": entry.date ?? "", "value": entry.value ?? 0.0]
            }
        ]
        let data = try await request(method: "POST", urlString: "\(baseURL)/\(challengeId)/progress/daily", payload: payload)
        let response = try decode(ChallengeMessageResponse.self, from: data)
        return response.message ?? "Daily progress synced"
    }

    /// GET /{id}/progress/daily — every participant's daily series for the challenge.
    public func fetchDailyProgress(challengeId: Int64) async throws -> [ParticipantDailySeries] {
        let data = try await request(method: "GET", urlString: "\(baseURL)/\(challengeId)/progress/daily", payload: nil)
        return try decode([ParticipantDailySeries].self, from: data)
    }

    /// POST /{id}/leave — leave the challenge.
    public func leaveChallenge(id: Int64) async throws -> String {
        let data = try await request(method: "POST", urlString: "\(baseURL)/\(id)/leave", payload: nil)
        let response = try decode(ChallengeMessageResponse.self, from: data)
        return response.message ?? "You left the challenge"
    }

    // MARK: - HTTP Helpers

    private func request(method: String, urlString: String, payload: [String: Any]?) async throws -> Data {
        let path = urlString.replacingOccurrences(of: baseURL, with: "").isEmpty
            ? "/" : urlString.replacingOccurrences(of: baseURL, with: "")
        guard let url = URL(string: urlString) else {
            log("\(method) \(path) — bad URL")
            throw URLError(.badURL)
        }
        guard let token = AuthManager.shared.getAccessToken(), !token.isEmpty else {
            log("\(method) \(path) — no access token, not sent")
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

        log("→ \(method) \(path)\(payload.map { " body: \(describe($0))" } ?? "")")
        let started = Date()

        let data: Data
        let response: URLResponse
        do {
            (data, response) = try await URLSession.shared.data(for: request)
        } catch {
            log("✗ \(method) \(path) failed after \(elapsedMs(since: started)) ms: \(error.localizedDescription)")
            throw error
        }

        guard let httpResponse = response as? HTTPURLResponse else {
            log("✗ \(method) \(path) — not an HTTP response")
            throw URLError(.badServerResponse)
        }

        let status = httpResponse.statusCode
        if (200...299).contains(status) {
            log("← \(status) \(method) \(path) in \(elapsedMs(since: started)) ms, \(data.count) bytes")
            return data
        } else {
            log("← \(status) \(method) \(path) in \(elapsedMs(since: started)) ms — error body: \(snippet(data))")
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
            log("✗ decode \(T.self) failed: \(error) — body: \(snippet(data))")
            throw NSError(
                domain: "ChallengeApi",
                code: -1,
                userInfo: [NSLocalizedDescriptionKey: "Could not understand the server response: \(error.localizedDescription)"]
            )
        }
    }

    // MARK: - Logging
    //
    // Same "[Tag] message" console style as FoodRecognitionApi. The bearer
    // token is never logged and invite emails are masked.

    private func log(_ message: String) {
        print("[ChallengeApi] \(message)")
    }

    private func elapsedMs(since start: Date) -> Int {
        Int(Date().timeIntervalSince(start) * 1000)
    }

    /// Request body for the log, with personal fields masked.
    private func describe(_ payload: [String: Any]) -> String {
        var safe = payload
        if let email = safe["email"] as? String {
            safe["email"] = Self.maskEmail(email)
        }
        if let entries = safe["entries"] as? [Any] {
            safe["entries"] = "[\(entries.count) days]"
        }
        guard let data = try? JSONSerialization.data(withJSONObject: safe, options: [.sortedKeys]),
              let text = String(data: data, encoding: .utf8) else {
            return "\(safe.keys.sorted())"
        }
        return text
    }

    /// First 500 characters of a response body.
    private func snippet(_ data: Data) -> String {
        guard !data.isEmpty else { return "<empty>" }
        let text = String(data: data, encoding: .utf8) ?? "<\(data.count) bytes, not UTF-8>"
        return text.count > 500 ? String(text.prefix(500)) + "…" : text
    }

    /// "john.doe@gmail.com" → "j***@gmail.com".
    private static func maskEmail(_ email: String) -> String {
        guard let at = email.firstIndex(of: "@"), at > email.startIndex else { return "***" }
        return "\(email[email.startIndex])***\(email[at...])"
    }
}
