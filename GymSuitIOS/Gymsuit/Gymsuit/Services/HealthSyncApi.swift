import Foundation

public final class HealthSyncApi {
    public static let shared = HealthSyncApi()

    private let baseURL = "https://api.gymsuit.app/api/mobile/health"
    private let timeoutInterval: TimeInterval = 20.0

    public init() {}

    public func sync(request: HealthSyncRequest) async throws -> HealthSyncResponse {
        guard let url = URL(string: "\(baseURL)/sync") else {
            throw URLError(.badURL)
        }
        guard let token = AuthManager.shared.getAccessToken(), !token.isEmpty else {
            throw NSError(
                domain: "HealthSyncApi",
                code: 401,
                userInfo: [NSLocalizedDescriptionKey: "Authentication required to sync health data."]
            )
        }

        var urlRequest = URLRequest(url: url)
        urlRequest.httpMethod = "POST"
        urlRequest.setValue("application/json", forHTTPHeaderField: "Content-Type")
        urlRequest.setValue("application/json", forHTTPHeaderField: "Accept")
        urlRequest.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        urlRequest.timeoutInterval = timeoutInterval

        let encoder = JSONEncoder()
        urlRequest.httpBody = try encoder.encode(request)

        let (data, response) = try await URLSession.shared.data(for: urlRequest)
        guard let httpResponse = response as? HTTPURLResponse else {
            throw URLError(.badServerResponse)
        }

        if (200...299).contains(httpResponse.statusCode) {
            let decoder = JSONDecoder()
            return try decoder.decode(HealthSyncResponse.self, from: data)
        } else {
            let errorText = String(data: data, encoding: .utf8) ?? "HTTP \(httpResponse.statusCode)"
            throw NSError(domain: "HealthSyncApi", code: httpResponse.statusCode, userInfo: [NSLocalizedDescriptionKey: errorText])
        }
    }
}
