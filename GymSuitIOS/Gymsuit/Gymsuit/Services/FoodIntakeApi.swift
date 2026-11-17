import Foundation

/// Uploads food log entries (food_log.json) to the backend so the trainer
/// progress page can read calorie intake from the database. Uploads are
/// idempotent: the backend upserts by the entry id, so re-sending an entry
/// never double-counts. Failures are swallowed — a food upload must never
/// break health sync or the food UI.
public final class FoodIntakeApi {
    public static let shared = FoodIntakeApi()

    private let baseURL = "https://api.gymsuit.app/api/mobile/food"
    private let timeoutInterval: TimeInterval = 20.0

    private let dateFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "yyyy-MM-dd"
        f.locale = Locale(identifier: "en_US_POSIX")
        return f
    }()

    public init() {}

    /// - Parameter photos: compressed JPEG data keyed by entry id. Only
    ///   photos in this map are sent; entries without one keep whatever
    ///   photo the backend already has.
    @discardableResult
    public func sync(entries: [FoodEntry], photos: [String: Data] = [:]) async -> Bool {
        guard let token = AuthManager.shared.getAccessToken(), !token.isEmpty else { return false }
        guard let url = URL(string: "\(baseURL)/sync") else { return false }
        let body: [String: Any] = [
            "entries": entries.map { entry -> [String: Any] in
                var dict: [String: Any] = [
                    "externalId": entry.id,
                    "recordDate": dateFormatter.string(from: entry.date),
                    "meal": entry.meal.rawValue,
                    "dishName": entry.name,
                    "calories": entry.calories,
                    "proteinG": entry.proteinG,
                    "carbsG": entry.carbsG,
                    "fatG": entry.fatG,
                ]
                if let photo = photos[entry.id] {
                    dict["photoBase64"] = photo.base64EncodedString()
                }
                return dict
            }
        ]
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        request.timeoutInterval = timeoutInterval
        request.httpBody = try? JSONSerialization.data(withJSONObject: body)
        do {
            let (_, response) = try await URLSession.shared.data(for: request)
            return (response as? HTTPURLResponse).map { (200...299).contains($0.statusCode) } ?? false
        } catch {
            return false
        }
    }

    /// One food entry as the backend serves it from GET /api/mobile/food.
    private struct ServerFoodEntry: Decodable {
        let externalId: String
        let recordDate: String
        let meal: String?
        let dishName: String?
        let calories: Int?
        let proteinG: Double?
        let carbsG: Double?
        let fatG: Double?
    }

    /// A sensible within-day hour per meal, so pulled entries sort naturally
    /// (the server stores only the date).
    private func mealHour(_ meal: MealType) -> Int {
        switch meal {
        case .breakfast: return 8
        case .lunch: return 13
        case .snack: return 16
        case .dinner: return 20
        }
    }

    /// Fetches the member's food entries from `from` (default 30 days back)
    /// through today, so the app can show meals logged on another device, the
    /// diet website or an MCP client. Entries map back to `FoodEntry` by their
    /// externalId, so the caller merges by id and never duplicates a local one.
    /// Best-effort: any failure returns an empty list rather than disturbing
    /// the local log.
    public func fetch(from: Date = Calendar.current.date(byAdding: .day, value: -30, to: Date()) ?? Date()) async -> [FoodEntry] {
        guard let token = AuthManager.shared.getAccessToken(), !token.isEmpty else { return [] }
        let fromStr = dateFormatter.string(from: from)
        guard let url = URL(string: "\(baseURL)?from=\(fromStr)") else { return [] }
        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        request.timeoutInterval = timeoutInterval
        do {
            let (data, response) = try await URLSession.shared.data(for: request)
            guard let http = response as? HTTPURLResponse, (200...299).contains(http.statusCode) else { return [] }
            let decoded = try JSONDecoder().decode([ServerFoodEntry].self, from: data)
            let calendar = Calendar.current
            return decoded.map { dto in
                let meal = dto.meal.flatMap { MealType(rawValue: $0) } ?? .snack
                let day = dateFormatter.date(from: dto.recordDate) ?? Date()
                let dated = calendar.date(bySettingHour: mealHour(meal), minute: 0, second: 0, of: day) ?? day
                return FoodEntry(
                    id: dto.externalId,
                    name: dto.dishName ?? "",
                    meal: meal,
                    calories: dto.calories ?? 0,
                    proteinG: dto.proteinG ?? 0,
                    carbsG: dto.carbsG ?? 0,
                    fatG: dto.fatG ?? 0,
                    timestampMillis: Int64(dated.timeIntervalSince1970 * 1000)
                )
            }
        } catch {
            return []
        }
    }

    /// Free-text meal logging: sends "2 idlis for breakfast, biryani for lunch" to the
    /// backend, where the AI turns it into meals with estimated nutrition that are
    /// saved. Returns the saved entries for the caller to merge into the local log.
    public func aiLog(text: String, date: Date) async -> Result<[FoodEntry], AiLogError> {
        guard let token = AuthManager.shared.getAccessToken(), !token.isEmpty else {
            return .failure(AiLogError(message: "Please log in again."))
        }
        guard let url = URL(string: "\(baseURL)/ai-log") else {
            return .failure(AiLogError(message: "Couldn't reach GymSuit."))
        }
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        request.timeoutInterval = 60 // the AI call can take a while
        request.httpBody = try? JSONSerialization.data(withJSONObject: [
            "text": text, "date": dateFormatter.string(from: date),
        ])
        do {
            let (data, response) = try await URLSession.shared.data(for: request)
            guard let http = response as? HTTPURLResponse else {
                return .failure(AiLogError(message: "Couldn't reach GymSuit."))
            }
            guard (200...299).contains(http.statusCode) else {
                let msg = (try? JSONSerialization.jsonObject(with: data) as? [String: Any])?["message"] as? String
                return .failure(AiLogError(message: msg ?? "Couldn't log meals (\(http.statusCode))."))
            }
            let decoded = try JSONDecoder().decode([ServerFoodEntry].self, from: data)
            return .success(decoded.map(entry(from:)))
        } catch {
            return .failure(AiLogError(message: "Couldn't reach GymSuit. Check your connection."))
        }
    }

    private func entry(from dto: ServerFoodEntry) -> FoodEntry {
        let meal = dto.meal.flatMap { MealType(rawValue: $0) } ?? .snack
        let day = dateFormatter.date(from: dto.recordDate) ?? Date()
        let dated = Calendar.current.date(bySettingHour: mealHour(meal), minute: 0, second: 0, of: day) ?? day
        return FoodEntry(
            id: dto.externalId,
            name: dto.dishName ?? "",
            meal: meal,
            calories: dto.calories ?? 0,
            proteinG: dto.proteinG ?? 0,
            carbsG: dto.carbsG ?? 0,
            fatG: dto.fatG ?? 0,
            timestampMillis: Int64(dated.timeIntervalSince1970 * 1000)
        )
    }

    @discardableResult
    public func delete(externalId: String) async -> Bool {
        guard let token = AuthManager.shared.getAccessToken(), !token.isEmpty else { return false }
        guard let url = URL(string: "\(baseURL)/\(externalId)") else { return false }
        var request = URLRequest(url: url)
        request.httpMethod = "DELETE"
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        request.timeoutInterval = timeoutInterval
        do {
            let (_, response) = try await URLSession.shared.data(for: request)
            return (response as? HTTPURLResponse).map { (200...299).contains($0.statusCode) } ?? false
        } catch {
            return false
        }
    }
}
