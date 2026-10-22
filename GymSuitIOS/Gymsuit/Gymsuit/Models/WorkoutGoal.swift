import Foundation

public enum WorkoutGoal: String, CaseIterable, Identifiable, Codable {
    case loseWeight = "lose_weight"
    case buildMuscle = "build_muscle"

    public var id: String { rawValue }

    public var title: String {
        switch self {
        case .loseWeight: return "Lose Weight"
        case .buildMuscle: return "Gain Weight"
        }
    }

    public var emoji: String {
        switch self {
        case .loseWeight: return "🤗"
        case .buildMuscle: return "💪"
        }
    }

    public var bannerTitle: String {
        switch self {
        case .loseWeight: return "Slim & fit ahead!"
        case .buildMuscle: return "Gain weight & strength ahead!"
        }
    }

    public var bannerDescription: String {
        switch self {
        case .loseWeight:
            return "Get ready to see a healthier, lighter you. Follow our tailored plan to conquer your fitness goal!"
        case .buildMuscle:
            return "Get ready to build lean mass, boost power, and reach your target weight with progressive overload training!"
        }
    }

    public var imageName: String {
        switch self {
        case .loseWeight: return "goal_lose_weight"
        case .buildMuscle: return "goal_build_muscle"
        }
    }

    public var blueImageName: String {
        switch self {
        case .loseWeight: return "goal_lose_weight_blue"
        case .buildMuscle: return "goal_build_muscle"
        }
    }
}
