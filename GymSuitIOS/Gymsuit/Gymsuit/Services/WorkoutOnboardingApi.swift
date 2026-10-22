import Foundation

public struct WorkoutOnboardingData: Codable {
    public let memberId: Int64?
    public let workoutGoal: String?
    public let heightCm: Double?
    public let weightKg: Double?
    public let targetWeightKg: Double?
    public let birthYear: Int?
    public let dateOfBirth: String?
    public let workoutDaysPerWeek: Int?
    public let targetDate: String?
    public let message: String?
}

public final class WorkoutOnboardingApi {
    public static let shared = WorkoutOnboardingApi()

    private let baseURL = "https://api.gymsuit.app/api/mobile/onboarding"
    private let timeoutInterval: TimeInterval = 15.0

    private static let isoDateFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "yyyy-MM-dd"
        f.locale = Locale(identifier: "en_US_POSIX")
        f.timeZone = TimeZone(secondsFromGMT: 0)
        return f
    }()

    public init() {}

    /// Syncs all collected onboarding questionnaire data to the backend database.
    @discardableResult
    public func syncOnboarding(
        goal: String,
        heightCm: Double,
        weightKg: Double,
        targetWeightKg: Double,
        birthYear: Int,
        workoutDaysPerWeek: Int,
        targetDate: Date
    ) async throws -> WorkoutOnboardingData {
        guard let url = URL(string: "\(baseURL)/sync") else {
            throw URLError(.badURL)
        }

        let targetDateStr = Self.isoDateFormatter.string(from: targetDate)

        var payload: [String: Any] = [
            "workoutGoal": goal,
            "heightCm": heightCm,
            "weightKg": weightKg,
            "targetWeightKg": targetWeightKg,
            "birthYear": birthYear,
            "workoutDaysPerWeek": workoutDaysPerWeek,
            "targetDate": targetDateStr
        ]

        if let email = AuthManager.shared.currentSession?.email, !email.isEmpty {
            payload["email"] = email
        }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.timeoutInterval = timeoutInterval

        if let token = AuthManager.shared.getAccessToken(), !token.isEmpty {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }

        request.httpBody = try JSONSerialization.data(withJSONObject: payload)

        let (data, response) = try await URLSession.shared.data(for: request)

        guard let httpResponse = response as? HTTPURLResponse else {
            throw URLError(.badServerResponse)
        }

        guard (200...299).contains(httpResponse.statusCode) else {
            if let errorJson = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
               let message = errorJson["message"] as? String, !message.isEmpty {
                throw NSError(domain: "WorkoutOnboardingApi", code: httpResponse.statusCode, userInfo: [NSLocalizedDescriptionKey: message])
            }
            let errorText = String(data: data, encoding: .utf8) ?? "HTTP \(httpResponse.statusCode)"
            throw NSError(domain: "WorkoutOnboardingApi", code: httpResponse.statusCode, userInfo: [NSLocalizedDescriptionKey: errorText])
        }

        return try JSONDecoder().decode(WorkoutOnboardingData.self, from: data)
    }

    /// Fetches saved onboarding data from the backend database.
    public func fetchOnboarding() async throws -> WorkoutOnboardingData {
        var components = URLComponents(string: baseURL)!
        if let email = AuthManager.shared.currentSession?.email, !email.isEmpty {
            components.queryItems = [URLQueryItem(name: "email", value: email)]
        }

        guard let url = components.url else {
            throw URLError(.badURL)
        }

        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.timeoutInterval = timeoutInterval

        if let token = AuthManager.shared.getAccessToken(), !token.isEmpty {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }

        let (data, response) = try await URLSession.shared.data(for: request)

        guard let httpResponse = response as? HTTPURLResponse, (200...299).contains(httpResponse.statusCode) else {
            throw URLError(.badServerResponse)
        }

        return try JSONDecoder().decode(WorkoutOnboardingData.self, from: data)
    }

    /// Automatically syncs locally persisted onboarding data if completed but not yet synced.
    public func syncPendingOnboardingIfNeeded() {
        let defaults = UserDefaults.standard
        guard defaults.bool(forKey: "workout_goal_setup_completed") else { return }

        let goal = defaults.string(forKey: "workout_selected_goal") ?? "lose_weight"
        let heightCm = defaults.double(forKey: "workout_user_height_cm")
        let weightKg = defaults.double(forKey: "workout_user_weight_kg")
        let targetWeightKg = defaults.double(forKey: "workout_user_target_weight_kg")
        let birthYear = defaults.integer(forKey: "workout_user_birth_year")
        let days = defaults.integer(forKey: "workout_user_days_per_week")

        guard heightCm > 0, weightKg > 0 else { return }

        let acceleratedDays = max(Int(ceil(max(abs(weightKg - targetWeightKg), 1.0) * 7.0)), 14)
        let targetDate = Calendar.current.date(byAdding: .day, value: acceleratedDays, to: Date()) ?? Date()

        Task {
            _ = try? await syncOnboarding(
                goal: goal,
                heightCm: heightCm,
                weightKg: weightKg,
                targetWeightKg: targetWeightKg > 0 ? targetWeightKg : weightKg,
                birthYear: birthYear > 1900 ? birthYear : 2003,
                workoutDaysPerWeek: days > 0 ? days : 2,
                targetDate: targetDate
            )
        }
    }

    /// Clears and deletes all workout onboarding data from backend and local storage.
    public func deleteOnboardingData() async throws {
        var components = URLComponents(string: baseURL)!
        if let email = AuthManager.shared.currentSession?.email, !email.isEmpty {
            components.queryItems = [URLQueryItem(name: "email", value: email)]
        }

        guard let url = components.url else {
            throw URLError(.badURL)
        }

        var request = URLRequest(url: url)
        request.httpMethod = "DELETE"
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.timeoutInterval = timeoutInterval

        if let token = AuthManager.shared.getAccessToken(), !token.isEmpty {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }

        _ = try? await URLSession.shared.data(for: request)

        // Clear local UserDefaults onboarding state and workout store
        await MainActor.run {
            let defaults = UserDefaults.standard
            defaults.set(false, forKey: "workout_goal_setup_completed")
            defaults.removeObject(forKey: "workout_selected_goal")
            defaults.removeObject(forKey: "workout_user_height_cm")
            defaults.removeObject(forKey: "workout_user_weight_kg")
            defaults.removeObject(forKey: "workout_user_target_weight_kg")
            defaults.removeObject(forKey: "workout_user_birth_year")
            defaults.removeObject(forKey: "workout_user_days_per_week")
            defaults.synchronize()

            WorkoutStore.shared.clearAllWorkouts()
        }
    }
}
