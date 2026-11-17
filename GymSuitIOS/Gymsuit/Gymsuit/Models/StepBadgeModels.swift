import Foundation
import Combine

// MARK: - Model

/// A step milestone badge as served by GET /api/mobile/badges.
/// `date` is the walking day the badge was earned for ("yyyy-MM-dd");
/// `awardedAt` is when the backend granted it (ISO-8601 instant).
public struct StepBadge: Codable, Identifiable, Equatable {
    public let id: Int64
    public let threshold: Int
    public let badgeName: String
    public let stepsAchieved: Int64
    public let date: String?
    public let awardedAt: String?
    /// "STEPS" or "CALORIES". For CALORIES, `stepsAchieved` holds the kcal burned.
    public let kind: String?

    public var isCalories: Bool { kind == "CALORIES" }

    public var formattedDate: String {
        guard let date, let parsed = Self.dayFormatter.date(from: date) else { return "" }
        return Self.displayFormatter.string(from: parsed)
    }

    private static let dayFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "yyyy-MM-dd"
        f.locale = Locale(identifier: "en_US_POSIX")
        return f
    }()

    private static let displayFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "MMM d"
        return f
    }()
}

// MARK: - Configuration

/// The two daily badges (they reset each day): 10,000 steps and 1,000 kcal burned.
public enum StepBadgeConfig {
    public struct Milestone {
        public let kind: String
        public let threshold: Int
        public let name: String
        public var unit: String { kind == "CALORIES" ? "kcal burned" : "steps" }
    }

    public static let daily: [Milestone] = [
        Milestone(kind: "STEPS", threshold: 10000, name: "Step Champion"),
        Milestone(kind: "CALORIES", threshold: 1000, name: "Calorie Crusher")
    ]
}

// MARK: - Store

/// Holds the user's step milestone badges from the backend and tracks which
/// badge celebrations have been seen (UserDefaults, so each badge's
/// celebration shows exactly once).
public final class StepBadgeStore: ObservableObject {
    public static let shared = StepBadgeStore()

    @Published public private(set) var badges: [StepBadge] = []

    private let seenPrefix = "gymsuit_step_badge_seen"

    private init() {}

    @MainActor
    public func refresh() async {
        do {
            badges = try await StepBadgeApi.shared.fetchBadges()
        } catch {
            // Silent: the Home screen still renders with locked cards.
        }
    }

    public func isSeen(_ id: Int64) -> Bool {
        UserDefaults.standard.bool(forKey: "\(seenPrefix)_\(id)")
    }

    public func markSeen(_ id: Int64) {
        UserDefaults.standard.set(true, forKey: "\(seenPrefix)_\(id)")
    }
}
