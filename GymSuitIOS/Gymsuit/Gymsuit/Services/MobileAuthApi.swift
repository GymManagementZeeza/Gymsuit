import Foundation

public final class MobileAuthApi {
    public static let shared = MobileAuthApi()
    
    private let baseURL = "https://api.gymsuit.app/api/mobile/auth"
    private let timeoutInterval: TimeInterval = 15.0
    
    public init() {}
    
    public func sendOtp(email: String, mode: String = "login") async throws -> String {
        let payload: [String: Any] = [
            "email": email.trimmingCharacters(in: .whitespacesAndNewlines).lowercased(),
            "mode": mode
        ]
        let json = try await postJson(urlString: "\(baseURL)/send-otp", payload: payload)
        return json["message"] as? String ?? "A 6-digit code has been sent to your email"
    }
    
    public func verifyLoginOtp(email: String, otp: String) async throws -> UserSession {
        let payload: [String: Any] = [
            "email": email.trimmingCharacters(in: .whitespacesAndNewlines).lowercased(),
            "otp": otp.trimmingCharacters(in: .whitespacesAndNewlines)
        ]
        let json = try await postJson(urlString: "\(baseURL)/verify-login", payload: payload)
        return try parseSession(json: json)
    }
    
    public func registerWithOtp(
        firstName: String,
        lastName: String,
        email: String,
        phone: String,
        otp: String
    ) async throws -> UserSession {
        let payload: [String: Any] = [
            "firstName": firstName.trimmingCharacters(in: .whitespacesAndNewlines),
            "lastName": lastName.trimmingCharacters(in: .whitespacesAndNewlines),
            "email": email.trimmingCharacters(in: .whitespacesAndNewlines).lowercased(),
            "phone": phone.trimmingCharacters(in: .whitespacesAndNewlines),
            "otp": otp.trimmingCharacters(in: .whitespacesAndNewlines)
        ]
        let json = try await postJson(urlString: "\(baseURL)/register-otp", payload: payload)
        return try parseSession(json: json)
    }
    
    public func login(email: String, password: String) async throws -> UserSession {
        let payload: [String: Any] = [
            "email": email.trimmingCharacters(in: .whitespacesAndNewlines).lowercased(),
            "password": password
        ]
        let json = try await postJson(urlString: "\(baseURL)/login", payload: payload)
        return try parseSession(json: json)
    }
    
    public func register(
        firstName: String,
        lastName: String,
        email: String,
        phone: String,
        password: String
    ) async throws -> UserSession {
        let payload: [String: Any] = [
            "firstName": firstName.trimmingCharacters(in: .whitespacesAndNewlines),
            "lastName": lastName.trimmingCharacters(in: .whitespacesAndNewlines),
            "email": email.trimmingCharacters(in: .whitespacesAndNewlines).lowercased(),
            "phone": phone.trimmingCharacters(in: .whitespacesAndNewlines),
            "password": password
        ]
        let json = try await postJson(urlString: "\(baseURL)/register", payload: payload)
        return try parseSession(json: json)
    }
    
    public func forgotPassword(email: String) async throws -> String {
        let payload: [String: Any] = [
            "email": email.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        ]
        let json = try await postJson(urlString: "\(baseURL)/forgot-password", payload: payload)
        return json["message"] as? String ?? "Verification code sent to your email"
    }
    
    public func resetPassword(email: String, otp: String, newPassword: String) async throws -> String {
        let payload: [String: Any] = [
            "email": email.trimmingCharacters(in: .whitespacesAndNewlines).lowercased(),
            "otp": otp.trimmingCharacters(in: .whitespacesAndNewlines),
            "newPassword": newPassword
        ]
        let json = try await postJson(urlString: "\(baseURL)/reset-password", payload: payload)
        return json["message"] as? String ?? "Password reset successfully"
    }
    
    public func refreshToken(refreshToken: String) async throws -> UserSession {
        let payload: [String: Any] = [
            "refreshToken": refreshToken
        ]
        let json = try await postJson(urlString: "\(baseURL)/refresh", payload: payload)
        return try parseSession(json: json)
    }
    
    public func updateProfile(email: String, firstName: String, lastName: String) async throws -> UserSession {
        let payload: [String: Any] = [
            "firstName": firstName.trimmingCharacters(in: .whitespacesAndNewlines),
            "lastName": lastName.trimmingCharacters(in: .whitespacesAndNewlines)
        ]
        let encodedEmail = email.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
            .addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? ""
        let json = try await postJson(urlString: "\(baseURL)/update-profile?email=\(encodedEmail)", payload: payload)
        return try parseSession(json: json)
    }
    
    // MARK: - HTTP Helpers
    private func postJson(urlString: String, payload: [String: Any]) async throws -> [String: Any] {
        guard let url = URL(string: urlString) else {
            throw URLError(.badURL)
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.timeoutInterval = timeoutInterval
        request.httpBody = try JSONSerialization.data(withJSONObject: payload)
        
        let (data, response) = try await URLSession.shared.data(for: request)
        
        guard let httpResponse = response as? HTTPURLResponse else {
            throw URLError(.badServerResponse)
        }
        
        if (200...299).contains(httpResponse.statusCode) {
            guard let json = try JSONSerialization.jsonObject(with: data) as? [String: Any] else {
                throw NSError(domain: "MobileAuthApi", code: -1, userInfo: [NSLocalizedDescriptionKey: "Invalid JSON response"])
            }
            return json
        } else {
            if let errorJson = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
               let message = errorJson["message"] as? String {
                throw NSError(domain: "MobileAuthApi", code: httpResponse.statusCode, userInfo: [NSLocalizedDescriptionKey: message])
            }
            let errorText = String(data: data, encoding: .utf8) ?? "HTTP \(httpResponse.statusCode)"
            throw NSError(domain: "MobileAuthApi", code: httpResponse.statusCode, userInfo: [NSLocalizedDescriptionKey: errorText])
        }
    }
    
    private func parseSession(json: [String: Any]) throws -> UserSession {
        guard let token = json["token"] as? String else {
            throw NSError(domain: "MobileAuthApi", code: -1, userInfo: [NSLocalizedDescriptionKey: "Missing session token"])
        }
        
        let memberId: Int64? = {
            if let num = json["memberId"] as? NSNumber {
                return num.int64Value
            }
            return nil
        }()
        
        let gymId: Int64? = {
            if let num = json["gymId"] as? NSNumber {
                return num.int64Value
            }
            return nil
        }()
        
        return UserSession(
            token: token,
            refreshToken: json["refreshToken"] as? String ?? "",
            email: json["email"] as? String ?? "",
            firstName: json["firstName"] as? String ?? "",
            lastName: json["lastName"] as? String ?? "",
            role: json["role"] as? String ?? "MEMBER",
            memberId: memberId,
            gymId: gymId,
            gymName: json["gymName"] as? String
        )
    }
}
