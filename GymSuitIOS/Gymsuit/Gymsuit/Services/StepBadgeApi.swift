import Foundation

/// Network client for the step milestone badges endpoint.
/// Base: https://api.gymsuit.app/api/mobile/badges — JWT Bearer <redacted>
/// Mirrors the ChallengeApi request/decoding pattern.
public final class StepBadgeApi {
    public static let shared = StepBadgeApi()

    private let baseURL = "https://api.gymsuit.app/api/mobile/badges"
    private let timeoutInterval: TimeInterval = 15.0

    public init() {}

    // MARK: - Endpoints

    /// GET / — the signed-in user's badges, newest first.
    public func fetchBadges() async throws -> [StepBadge] {
        let data = try await request(method: "GET", urlString: baseURL, payload: nil)
        return try decode([StepBadge].self, from: data)
    }

    // MARK: - HTTP Helpers

    private func request(method: String, urlString: String, payload: [String: Any]?) async throws -> Data {
        guard let url = URL(string: urlString) else {
            throw URLError(.badURL)
        }
        guard let token = AuthManager.shared.getAccessToken(), !token.isEmpty else {
            throw NSError(
                domain: "StepBadgeApi",
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
                throw NSError(domain: "StepBadgeApi", code: httpResponse.statusCode, userInfo: [NSLocalizedDescriptionKey: message])
            }
            let errorText = String(data: data, encoding: .utf8) ?? "HTTP \(httpResponse.statusCode)"
            throw NSError(domain: "StepBadgeApi", code: httpResponse.statusCode, userInfo: [NSLocalizedDescriptionKey: errorText])
        }
    }

    private func decode<T: Decodable>(_ type: T.Type, from data: Data) throws -> T {
        do {
            return try JSONDecoder().decode(T.self, from: data)
        } catch {
            throw NSError(
                domain: "StepBadgeApi",
                code: -1,
                userInfo: [NSLocalizedDescriptionKey: "Could not understand the server response: \(error.localizedDescription)"]
            )
        }
    }
}
