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
