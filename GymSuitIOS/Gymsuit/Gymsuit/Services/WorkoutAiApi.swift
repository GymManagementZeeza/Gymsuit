import Foundation
import CryptoKit

/// Error shown to the user when an AI log request fails.
public struct AiLogError: Error {
    public let message: String
}

/// Free-text workout logging. Sends "bench press 3x10 at 40 kg, then a 30 minute run"
/// to the backend, where the AI structures it into workouts that are saved
/// (calories estimated). Mirrors Android's `WorkoutApi.aiLog`.
public final class WorkoutAiApi {
    public static let shared = WorkoutAiApi()

    private let url = URL(string: "https://api.gymsuit.app/api/mobile/workout/ai-log")!

    private struct ServerWorkout: Decodable {
        let externalId: String
        let title: String?
        let startTime: String?
        let durationMinutes: Int?
        let caloriesBurned: Double?
        let setCount: Int?
        let totalReps: Int?
        let totalVolumeKg: Double?
    }

    public func aiLog(text: String, date: Date) async -> Result<[LoggedWorkout], AiLogError> {
        guard let token = AuthManager.shared.getAccessToken(), !token.isEmpty else {
            return .failure(AiLogError(message: "Please log in again."))
        }
        let df = DateFormatter()
        df.dateFormat = "yyyy-MM-dd"
        df.locale = Locale(identifier: "en_US_POSIX")
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        request.timeoutInterval = 60 // the AI call can take a while
        request.httpBody = try? JSONSerialization.data(withJSONObject: [
            "text": text, "date": df.string(from: date),
        ])
        do {
            let (data, response) = try await URLSession.shared.data(for: request)
            guard let http = response as? HTTPURLResponse else {
                return .failure(AiLogError(message: "Couldn't reach GymSuit."))
            }
            guard (200...299).contains(http.statusCode) else {
                let msg = (try? JSONSerialization.jsonObject(with: data) as? [String: Any])?["message"] as? String
                return .failure(AiLogError(message: msg ?? "Couldn't log workouts (\(http.statusCode))."))
            }
            let decoded = try JSONDecoder().decode([ServerWorkout].self, from: data)
            return .success(decoded.map(Self.toLoggedWorkout))
        } catch {
            return .failure(AiLogError(message: "Couldn't reach GymSuit. Check your connection."))
        }
    }

    /// Stable UUID for a server externalId, so the same session always maps to the same local id.
    static func uuid(for externalId: String) -> UUID {
        let hash = Array(SHA256.hash(data: Data(externalId.utf8)))
        return UUID(uuid: (hash[0], hash[1], hash[2], hash[3], hash[4], hash[5], hash[6], hash[7],
                           hash[8], hash[9], hash[10], hash[11], hash[12], hash[13], hash[14], hash[15]))
    }

    /// The server stores local wall-clock times ("2026-11-17T12:00:00"), so parse in the device zone.
    private static func parseLocal(_ string: String) -> Date? {
        let trimmed = String(string.prefix(19))
        let df = DateFormatter()
        df.locale = Locale(identifier: "en_US_POSIX")
        df.dateFormat = "yyyy-MM-dd'T'HH:mm:ss"
        return df.date(from: trimmed)
    }

    private static func toLoggedWorkout(_ w: ServerWorkout) -> LoggedWorkout {
        let start = w.startTime.flatMap(parseLocal) ?? Date()
        let setCount = w.setCount ?? 0
        let totalReps = w.totalReps ?? 0
        let volume = w.totalVolumeKg ?? 0
        var sets: [WorkoutSet] = []
        if setCount > 0 && totalReps > 0 {
            let repsPerSet = max(1, Int((Double(totalReps) / Double(setCount)).rounded()))
            let weightPerRep = volume > 0 ? volume / Double(totalReps) : 0
            sets = (0..<setCount).map { _ in WorkoutSet(reps: repsPerSet, weightKg: weightPerRep) }
        }
        let kcal = w.caloriesBurned.map { Int($0.rounded()) }
        let title = w.title ?? "Workout"
        return LoggedWorkout(
            id: uuid(for: w.externalId),
            exerciseId: title.lowercased().replacingOccurrences(of: " ", with: "_"),
            exerciseName: title,
            date: start,
            sets: sets,
            durationMinutes: w.durationMinutes ?? 0,
            notes: kcal.map { "≈ \($0) kcal" } ?? "",
            syncedToHealthKit: true,
            caloriesBurned: kcal
        )
    }
}
