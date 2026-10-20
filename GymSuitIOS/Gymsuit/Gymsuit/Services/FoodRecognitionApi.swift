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
        print("[FoodRecognitionApi] Starting recognition with \(jpegData.count) bytes of JPEG data")
        guard AuthManager.shared.getAccessToken()?.isEmpty == false else {
            print("[FoodRecognitionApi] Access token missing or empty, aborting with unauthorized")
            throw FoodRecognitionError.unauthorized
        }

        do {
            return try await send(jpegData: jpegData)
        } catch FoodRecognitionError.unauthorized {
            // Access token may simply have expired — refresh once and retry
            // before telling the user to log in again.
            print("[FoodRecognitionApi] Received 401 Unauthorized, attempting to refresh session...")
            guard await AuthManager.shared.refreshSession() else {
                print("[FoodRecognitionApi] Session refresh failed, throwing unauthorized")
                throw FoodRecognitionError.unauthorized
            }
            print("[FoodRecognitionApi] Session refresh succeeded, retrying request...")
            return try await send(jpegData: jpegData)
        } catch {
            print("[FoodRecognitionApi] Recognition failed: \(error)")
            throw error
        }
    }

    private func send(jpegData: Data) async throws -> FoodRecognitionResult {
        guard let url = URL(string: endpoint) else {
            print("[FoodRecognitionApi] Invalid URL: \(endpoint)")
            throw URLError(.badURL)
        }
        guard let token = AuthManager.shared.getAccessToken(), !token.isEmpty else {
            print("[FoodRecognitionApi] No access token available for request")
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

        print("[FoodRecognitionApi] --> POST \(endpoint) (\(jpegData.count) bytes image, total body: \(body.count) bytes)")

        let data: Data
        let response: URLResponse
        do {
            (data, response) = try await URLSession.shared.data(for: request)
        } catch {
            print("[FoodRecognitionApi] <-- Network request failed: \(error.localizedDescription) (\(error))")
            throw error
        }

        guard let httpResponse = response as? HTTPURLResponse else {
            print("[FoodRecognitionApi] <-- Invalid server response (not HTTP)")
            throw URLError(.badServerResponse)
        }

        print("[FoodRecognitionApi] <-- HTTP \(httpResponse.statusCode)")

        if (200...299).contains(httpResponse.statusCode) {
            let responseString = String(data: data, encoding: .utf8) ?? "<non-utf8 data: \(data.count) bytes>"
            print("[FoodRecognitionApi] <-- Response Body: \(responseString)")
            do {
                let result = try JSONDecoder().decode(FoodRecognitionResult.self, from: data)
                print("[FoodRecognitionApi] Decoded result successfully: dish='\(result.dish)', confidence=\(result.confidence), candidate probabilities=\(result.probabilities)")
                return result
            } catch {
                print("[FoodRecognitionApi] JSON decode error: \(error.localizedDescription). Raw response: \(responseString)")
                throw FoodRecognitionError.failed("Could not understand the server response: \(error.localizedDescription)")
            }
        }
        if httpResponse.statusCode == 401 {
            print("[FoodRecognitionApi] <-- 401 Unauthorized")
            throw FoodRecognitionError.unauthorized
        }
        if httpResponse.statusCode == 503 {
            print("[FoodRecognitionApi] <-- 503 Service Unavailable (inference server down)")
            throw FoodRecognitionError.unavailable
        }
        let errorText = String(data: data, encoding: .utf8) ?? "HTTP \(httpResponse.statusCode)"
        print("[FoodRecognitionApi] <-- Error Response Body: \(errorText)")
        if let errorJson = try? JSONSerialization.jsonObject(with: data) as? [String: Any] {
            if let message = (errorJson["message"] as? String) ?? (errorJson["error"] as? String), !message.isEmpty {
                print("[FoodRecognitionApi] <-- Parsed Error: \(message)")
                throw FoodRecognitionError.failed("Food recognition failed (\(httpResponse.statusCode)): \(message)")
            }
        }
        throw FoodRecognitionError.failed("Food recognition failed (\(httpResponse.statusCode)): \(errorText)")
    }

    private func append(_ string: String, to data: inout Data) {
        if let bytes = string.data(using: .utf8) {
            data.append(bytes)
        }
    }
}
