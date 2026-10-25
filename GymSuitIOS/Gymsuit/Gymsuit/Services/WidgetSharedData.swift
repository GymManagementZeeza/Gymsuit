import Foundation

/// Shared widget data: the single source of truth for the snapshot the main
/// app writes and the widget extension reads. Lives in the app target (via
/// the synchronized Gymsuit group) and is added explicitly to the widget
/// extension target, so both sides stay in sync from one file.
public enum WidgetSharedData {
    public static let suiteName = "group.ca.zeezaglobal.Gymsuit"
    public static var store: UserDefaults? {
        UserDefaults(suiteName: suiteName)
    }

    public static let workoutsWeekCountKey = "widgetWorkoutsWeekCount"
    public static let lastWorkoutNameKey = "widgetLastWorkoutName"
    public static let lastWorkoutDateKey = "widgetLastWorkoutDate"
    public static let challengesKey = "widgetChallenges"

    public static let dayFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateFormat = "MMM d"
        return formatter
    }()

    public static let weekdayFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateFormat = "EEE"
        return formatter
    }()

    public static func loadChallenges() -> [WidgetChallenge] {
        guard let data = store?.data(forKey: challengesKey),
              let decoded = try? JSONDecoder().decode([WidgetChallenge].self, from: data) else {
            return []
        }
        return decoded
    }

    public static func saveChallenges(_ challenges: [WidgetChallenge]) {
        let data = try? JSONEncoder().encode(challenges)
        store?.set(data, forKey: challengesKey)
    }

    public static func formatMetricValue(metric: String, value: Double) -> String {
        switch metric {
        case "STEPS":
            return NumberFormatter.localizedString(from: NSNumber(value: Int(value)), number: .decimal)
        case "WORKOUTS":
            return "\(Int(value))"
        case "CALORIES":
            return "\(NumberFormatter.localizedString(from: NSNumber(value: Int(value)), number: .decimal)) kcal"
        case "DISTANCE_KM":
            return String(format: "%.1f km", value)
        default:
            return "\(Int(value))"
        }
    }

    public static func metricDisplayName(_ metric: String) -> String {
        switch metric {
        case "STEPS": return "Steps"
        case "WORKOUTS": return "Workouts"
        case "CALORIES": return "Calories"
        case "DISTANCE_KM": return "Distance"
        default: return metric
        }
    }
}

public struct WidgetMember: Codable, Equatable {
    public var name: String
    public var value: Double

    public init(name: String, value: Double) {
        self.name = name
        self.value = value
    }
}

public struct WidgetChallenge: Codable, Equatable, Identifiable {
    public var id: Int64
    public var name: String
    public var status: String
    public var metric: String
    public var participantCount: Int
    public var myRank: Int?
    public var members: [WidgetMember]

    public init(
        id: Int64,
        name: String,
        status: String,
        metric: String,
        participantCount: Int,
        myRank: Int?,
        members: [WidgetMember]
    ) {
        self.id = id
        self.name = name
        self.status = status
        self.metric = metric
        self.participantCount = participantCount
        self.myRank = myRank
        self.members = members
    }
}
