import Foundation
import Combine

// MARK: - Points configuration

public enum PointsConfig {
    /// Points awarded for simply logging a workout.
    public static let pointsPerWorkout = 50
    /// Points awarded per logged set.
    public static let pointsPerSet = 5
    /// Points awarded per minute of workout duration.
    public static let pointsPerMinute = 2
    /// How many points equal one Indian rupee.
    public static let pointsPerRupee = 100

    public static func rupees(for points: Int) -> Double {
        Double(points) / Double(pointsPerRupee)
    }

    public static func pointsForWorkout(setCount: Int, durationMinutes: Int) -> Int {
        pointsPerWorkout + pointsPerSet * setCount + pointsPerMinute * durationMinutes
    }

    public static func formattedRupees(for points: Int) -> String {
        String(format: "\u{20B9}%.2f", rupees(for: points))
    }
}

// MARK: - History entry

public struct PointsEntry: Codable, Identifiable {
    public var id = UUID()
    public let date: Date
    /// Positive when earned, negative when redeemed.
    public let points: Int
    public let reason: String
    /// Rupee value for redemption entries.
    public let rupeeValue: Double?

    public var isRedemption: Bool { points < 0 }
}

// MARK: - Store

/// Persists the user's points balance and history as JSON in the app's documents
/// directory. Points are earned by logging workouts.
public final class PointsStore: ObservableObject {
    public static let shared = PointsStore()

    @Published public private(set) var balance: Int = 0
    @Published public private(set) var history: [PointsEntry] = []

    private let fileName = "gymsuit_points.json"

    private init() {
        load()
    }

    public var redeemableRupees: Double {
        PointsConfig.rupees(for: balance)
    }

    /// Awards points for a logged workout and returns the amount earned.
    @discardableResult
    public func awardForWorkout(exerciseName: String, setCount: Int, durationMinutes: Int) -> Int {
        let earned = PointsConfig.pointsForWorkout(setCount: setCount, durationMinutes: durationMinutes)
        balance += earned
        history.insert(
            PointsEntry(
                date: Date(),
                points: earned,
                reason: "Workout: \(exerciseName)",
                rupeeValue: nil
            ),
            at: 0
        )
        persist()
        return earned
    }

    /// Redeems points for rupees. Returns the rupee amount, or nil when the
    /// amount is invalid or exceeds the balance.
    public func redeem(points: Int) -> Double? {
        guard points > 0, points <= balance else { return nil }
        let rupees = PointsConfig.rupees(for: points)
        balance -= points
        history.insert(
            PointsEntry(
                date: Date(),
                points: -points,
                reason: "Redeemed for cash",
                rupeeValue: rupees
            ),
            at: 0
        )
        persist()
        return rupees
    }

    public func mergeRemote(remoteBalance: Int?, remoteTransactions: [PointsTransactionDto]?) {
        guard remoteBalance != nil || remoteTransactions != nil else { return }

        var currentMap: [String: PointsEntry] = [:]
        for entry in history {
            currentMap[entry.id.uuidString.lowercased()] = entry
        }

        let iso = ISO8601DateFormatter()
        let df = DateFormatter()
        df.locale = Locale(identifier: "en_US_POSIX")
        df.dateFormat = "yyyy-MM-dd'T'HH:mm:ss"

        if let remoteTxs = remoteTransactions {
            for r in remoteTxs {
                let key = r.id.lowercased()
                if currentMap[key] == nil {
                    let d: Date
                    if let c = r.createdAt {
                        d = iso.date(from: c) ?? df.date(from: c) ?? Date()
                    } else {
                        d = Date()
                    }
                    let newEntry = PointsEntry(
                        id: UUID(uuidString: r.id) ?? UUID(),
                        date: d,
                        points: r.points,
                        reason: r.reason,
                        rupeeValue: r.rupeeValue
                    )
                    currentMap[key] = newEntry
                }
            }
        }

        let mergedList = Array(currentMap.values).sorted { $0.date > $1.date }
        DispatchQueue.main.async {
            self.history = mergedList
            self.balance = remoteBalance ?? mergedList.reduce(0) { $0 + $1.points }
            self.persist()
        }
    }

    // MARK: - Persistence

    private var fileURL: URL {
        FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
            .appendingPathComponent(fileName)
    }

    private struct SavedState: Codable {
        let balance: Int
        let history: [PointsEntry]
    }

    private func load() {
        guard let data = try? Data(contentsOf: fileURL),
              let decoded = try? JSONDecoder().decode(SavedState.self, from: data) else {
            return
        }
        balance = decoded.balance
        history = decoded.history.sorted { $0.date > $1.date }
    }

    private func persist() {
        let state = SavedState(balance: balance, history: history)
        let data = try? JSONEncoder().encode(state)
        try? data?.write(to: fileURL, options: .atomic)
    }
}
