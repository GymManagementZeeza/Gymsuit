import Foundation

/// Errors from the food recognition endpoint, mirroring Android's
/// FoodRecognitionApi: `.unavailable` is the backend's 503 (its inference
/// server is down); anything else is a generic failure.
public enum FoodRecognitionError: Error {
    case unavailable
    case unauthorized
    case failed(String)
}

/// Meal recognition via the backend (`POST /api/mobile/food/recognize`), which
/// proxies to the trained FastAPI inference server. The model lives on the
/// server — the app only uploads the photo. Multipart body, field `image`.
/// Mirrors Android's FoodRecognitionApi (same endpoint, boundary, timeout).
public final class FoodRecognitionApi {
    public static let shared = FoodRecognitionApi()

    private let endpoint = "https://api.gymsuit.app/api/mobile/food/recognize"
    private let timeoutInterval: TimeInterval = 20.0
    private let boundary = "GymSuitFoodBoundary"

    public init() {}

    public func recognize(jpegData: Data) async throws -> FoodRecognitionResult {
        guard AuthManager.shared.getAccessToken()?.isEmpty == false else {
            throw FoodRecognitionError.unauthorized
        }

        do {
            return try await send(jpegData: jpegData)
        } catch FoodRecognitionError.unauthorized {
            // Access token may simply have expired — refresh once and retry
            // before telling the user to log in again.
            guard await AuthManager.shared.refreshSession() else {
                throw FoodRecognitionError.unauthorized
            }
            return try await send(jpegData: jpegData)
        }
    }

    private func send(jpegData: Data) async throws -> FoodRecognitionResult {
        guard let url = URL(string: endpoint) else {
            throw URLError(.badURL)
        }
        guard let token = AuthManager.shared.getAccessToken(), !token.isEmpty else {
            throw FoodRecognitionError.unauthorized
        }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("multipart/form-data; boundary=\(boundary)", forHTTPHeaderField: "Content-Type")
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        request.timeoutInterval = timeoutInterval

        var body = Data()
        append("--\(boundary)\r\n", to: &body)
        append("Content-Disposition: form-data; name=\"image\"; filename=\"dish.jpg\"\r\n", to: &body)
        append("Content-Type: image/jpeg\r\n\r\n", to: &body)
        body.append(jpegData)
        append("\r\n--\(boundary)--\r\n", to: &body)
        request.httpBody = body

        let (data, response) = try await URLSession.shared.data(for: request)

        guard let httpResponse = response as? HTTPURLResponse else {
            throw URLError(.badServerResponse)
        }

        if (200...299).contains(httpResponse.statusCode) {
            do {
                return try JSONDecoder().decode(FoodRecognitionResult.self, from: data)
            } catch {
                throw FoodRecognitionError.failed("Could not understand the server response: \(error.localizedDescription)")
            }
        }
        if httpResponse.statusCode == 401 {
            throw FoodRecognitionError.unauthorized
        }
        if httpResponse.statusCode == 503 {
            throw FoodRecognitionError.unavailable
        }
        if let errorJson = try? JSONSerialization.jsonObject(with: data) as? [String: Any] {
            if let message = (errorJson["error"] as? String) ?? (errorJson["message"] as? String), !message.isEmpty {
                throw FoodRecognitionError.failed("Food recognition failed (\(httpResponse.statusCode)): \(message)")
            }
        }
        let errorText = String(data: data, encoding: .utf8) ?? "HTTP \(httpResponse.statusCode)"
        throw FoodRecognitionError.failed("Food recognition failed (\(httpResponse.statusCode)): \(errorText)")
    }

    private func append(_ string: String, to data: inout Data) {
        if let bytes = string.data(using: .utf8) {
            data.append(bytes)
        }
    }
}
