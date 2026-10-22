import Foundation

public enum WorkoutGoal: String, CaseIterable, Identifiable, Codable {
    case loseWeight = "lose_weight"
    case buildMuscle = "build_muscle"
    case keepFit = "keep_fit"

    public var id: String { rawValue }

    public var title: String {
        switch self {
        case .loseWeight: return "Lose Weight"
        case .buildMuscle: return "Build Muscle"
        case .keepFit: return "Keep Fit"
        }
    }

    public var emoji: String {
        switch self {
        case .loseWeight: return "🤗"
        case .buildMuscle: return "💪"
        case .keepFit: return "⚡"
        }
    }

    public var bannerTitle: String {
        switch self {
        case .loseWeight: return "Slim & fit ahead!"
        case .buildMuscle: return "Muscle & strength ahead!"
        case .keepFit: return "Active & energized ahead!"
        }
    }

    public var bannerDescription: String {
        switch self {
        case .loseWeight:
            return "Get ready to see a healthier, lighter you. Follow our tailored plan to conquer your fitness goal!"
        case .buildMuscle:
            return "Get ready to build lean mass, boost power, and sculpt a stronger physique with progressive overload training!"
        case .keepFit:
            return "Stay sharp, build daily endurance, and maintain high energy levels with balanced full-body workouts!"
        }
    }

    public var imageName: String {
        switch self {
        case .loseWeight: return "goal_lose_weight"
        case .buildMuscle: return "goal_build_muscle"
        case .keepFit: return "goal_keep_fit"
        }
    }

    public var blueImageName: String {
        switch self {
        case .loseWeight: return "goal_lose_weight_blue"
        case .buildMuscle: return "goal_build_muscle"
        case .keepFit: return "goal_keep_fit"
        }
    }
}
