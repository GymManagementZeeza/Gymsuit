import Foundation
import UIKit

public final class HealthSyncApi {
    public static let shared = HealthSyncApi()

    private let baseURL = "https://api.gymsuit.app/api/mobile/health"
    private let timeoutInterval: TimeInterval = 20.0

    public init() {}

    public func registerDevice() async {
        guard let token = AuthManager.shared.getAccessToken(), !token.isEmpty else { return }
        guard let url = URL(string: "https://api.gymsuit.app/api/v1/devices") else { return }

        let deviceId = UIDevice.current.identifierForVendor?.uuidString ?? UUID().uuidString
        let systemVersion = UIDevice.current.systemVersion
        let model = UIDevice.current.model
        let appVersion = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0.0"

        let body: [String: Any] = [
            "deviceId": deviceId,
            "platform": "IOS",
            "deviceType": "PHONE",
            "manufacturer": "Apple",
            "model": model,
            "osVersion": systemVersion,
            "appVersion": appVersion,
            "healthSource": "HEALTH_KIT"
        ]

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        request.timeoutInterval = 10.0
        request.httpBody = try? JSONSerialization.data(withJSONObject: body)

        _ = try? await URLSession.shared.data(for: request)
    }

    public func sync(request: HealthSyncRequest) async throws -> HealthSyncResponse {
        await registerDevice()
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
