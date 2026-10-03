import Foundation

/// Metric a challenge is scored on. Raw values match the backend contract.
public enum ChallengeMetricType: String, CaseIterable {
    case steps = "STEPS"
    case workouts = "WORKOUTS"
    case calories = "CALORIES"
    case distanceKm = "DISTANCE_KM"

    public var displayName: String {
        switch self {
        case .steps: return "Steps"
        case .workouts: return "Workouts"
        case .calories: return "Calories"
        case .distanceKm: return "Distance"
        }
    }

    public var unitLabel: String {
        switch self {
        case .steps: return "steps"
        case .workouts: return "workouts"
        case .calories: return "kcal"
        case .distanceKm: return "km"
        }
    }

    public var iconName: String {
        switch self {
        case .steps: return "figure.walk"
        case .workouts: return "dumbbell.fill"
        case .calories: return "flame.fill"
        case .distanceKm: return "map.fill"
        }
    }

    /// Formats a leaderboard value for this metric. Never invents data.
    public func formatValue(_ value: Double) -> String {
        switch self {
        case .steps:
            return "\(Int(value).formatted(.number.grouping(.automatic)))"
        case .workouts:
            return "\(Int(value))"
        case .calories:
            return "\(Int(value).formatted(.number.grouping(.automatic)))"
        case .distanceKm:
            return String(format: "%.2f", value)
        }
    }

    public static func from(rawValue: String?) -> ChallengeMetricType {
        return ChallengeMetricType(rawValue: rawValue ?? "") ?? .steps
    }
}

public enum ChallengeStatus: String {
    case upcoming = "UPCOMING"
    case active = "ACTIVE"
    case ended = "ENDED"

    public var displayName: String {
        switch self {
        case .upcoming: return "Upcoming"
        case .active: return "Active"
        case .ended: return "Ended"
        }
    }

    public static func from(rawValue: String?) -> ChallengeStatus {
        return ChallengeStatus(rawValue: rawValue ?? "") ?? .upcoming
    }
}

public struct LeaderboardEntry: Codable {
    public let userId: Int64?
    public let displayName: String?
    public let value: Double?
    public let rank: Int?

    public init(userId: Int64? = nil, displayName: String? = nil, value: Double? = nil, rank: Int? = nil) {
        self.userId = userId
        self.displayName = displayName
        self.value = value
        self.rank = rank
    }
}

public struct ChallengeSummary: Codable, Identifiable {
    public let challengeId: Int64?
    public let name: String?
    public let metricType: String?
    public let startDate: String?
    public let endDate: String?
    public let status: String?
    public let participantCount: Int?
    public let myRank: Int?

    public var id: Int64 { challengeId ?? -1 }

    public var metric: ChallengeMetricType { ChallengeMetricType.from(rawValue: metricType) }
    public var challengeStatus: ChallengeStatus { ChallengeStatus.from(rawValue: status) }

    enum CodingKeys: String, CodingKey {
        case challengeId = "id"
        case name
        case metricType
        case startDate
        case endDate
        case status
        case participantCount
        case myRank
    }

    public init(
        challengeId: Int64? = nil,
        name: String? = nil,
        metricType: String? = nil,
        startDate: String? = nil,
        endDate: String? = nil,
        status: String? = nil,
        participantCount: Int? = nil,
        myRank: Int? = nil
    ) {
        self.challengeId = challengeId
        self.name = name
        self.metricType = metricType
        self.startDate = startDate
        self.endDate = endDate
        self.status = status
        self.participantCount = participantCount
        self.myRank = myRank
    }
}

public struct ChallengeDetail: Codable {
    public let challengeId: Int64?
    public let name: String?
    public let description: String?
    public let metricType: String?
    public let startDate: String?
    public let endDate: String?
    public let status: String?
    public let inviteCode: String?
    public let participantCount: Int?
    public let myRank: Int?
    public let createdByMe: Bool?
    public let leaderboard: [LeaderboardEntry]?

    public var metric: ChallengeMetricType { ChallengeMetricType.from(rawValue: metricType) }
    public var challengeStatus: ChallengeStatus { ChallengeStatus.from(rawValue: status) }

    enum CodingKeys: String, CodingKey {
        case challengeId = "id"
        case name
        case description
        case metricType
        case startDate
        case endDate
        case status
        case inviteCode
        case participantCount
        case myRank
        case createdByMe
        case leaderboard
    }

    public init(
        challengeId: Int64? = nil,
        name: String? = nil,
        description: String? = nil,
        metricType: String? = nil,
        startDate: String? = nil,
        endDate: String? = nil,
        status: String? = nil,
        inviteCode: String? = nil,
        participantCount: Int? = nil,
        myRank: Int? = nil,
        createdByMe: Bool? = nil,
        leaderboard: [LeaderboardEntry]? = nil
    ) {
        self.challengeId = challengeId
        self.name = name
        self.description = description
        self.metricType = metricType
        self.startDate = startDate
        self.endDate = endDate
        self.status = status
        self.inviteCode = inviteCode
        self.participantCount = participantCount
        self.myRank = myRank
        self.createdByMe = createdByMe
        self.leaderboard = leaderboard
    }
}

public struct ChallengeMessageResponse: Codable {
    public let message: String?

    public init(message: String? = nil) {
        self.message = message
    }
}
