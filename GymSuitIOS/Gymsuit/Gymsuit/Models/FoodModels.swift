import Foundation

// MARK: - Meal type (mirrors Android MealType in FoodStore.kt)

public enum MealType: String, Codable, CaseIterable, Identifiable {
    case breakfast = "BREAKFAST"
    case lunch = "LUNCH"
    case dinner = "DINNER"
    case snack = "SNACK"

    public var id: String { rawValue }

    public var label: String {
        switch self {
        case .breakfast: return "Breakfast"
        case .lunch: return "Lunch"
        case .dinner: return "Dinner"
        case .snack: return "Snacks"
        }
    }
}

// MARK: - Food log entry (mirrors Android FoodEntry; same JSON keys in food_log.json)

public struct FoodEntry: Codable, Identifiable, Equatable {
    public var id: String
    public var name: String
    public var meal: MealType
    public var calories: Int
    public var proteinG: Double
    public var carbsG: Double
    public var fatG: Double
    public var timestampMillis: Int64

    public init(
        id: String = UUID().uuidString,
        name: String,
        meal: MealType,
        calories: Int,
        proteinG: Double = 0,
        carbsG: Double = 0,
        fatG: Double = 0,
        timestampMillis: Int64
    ) {
        self.id = id
        self.name = name
        self.meal = meal
        self.calories = calories
        self.proteinG = proteinG
        self.carbsG = carbsG
        self.fatG = fatG
        self.timestampMillis = timestampMillis
    }

    /// The moment the entry was logged (Android stores epoch millis).
    public var date: Date {
        Date(timeIntervalSince1970: Double(timestampMillis) / 1000)
    }
}

// MARK: - Offline food item (mirrors Android FoodItem in FoodDatabase.kt)

/// A searchable food with typical single-serving nutrition (rough estimates).
public struct FoodItem: Identifiable, Equatable {
    public var id: String { name }

    public let name: String
    public let serving: String
    public let kcal: Int
    public let proteinG: Int
    public let carbsG: Int
    public let fatG: Int
    public let aliases: String

    public init(name: String, serving: String, kcal: Int, proteinG: Int, carbsG: Int, fatG: Int, aliases: String = "") {
        self.name = name
        self.serving = serving
        self.kcal = kcal
        self.proteinG = proteinG
        self.carbsG = carbsG
        self.fatG = fatG
        self.aliases = aliases
    }
}

// MARK: - Recognition (mirrors Android FoodRecognitionResult)

/// Response of POST /api/mobile/food/recognize: {dish, confidence, probabilities}.
public struct FoodRecognitionResult: Decodable {
    public let dish: String
    public let confidence: Double
    public let probabilities: [String: Double]

    private enum CodingKeys: String, CodingKey {
        case dish, confidence, probabilities
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        dish = try container.decodeIfPresent(String.self, forKey: .dish) ?? ""
        confidence = try container.decodeIfPresent(Double.self, forKey: .confidence) ?? 0
        probabilities = try container.decodeIfPresent([String: Double].self, forKey: .probabilities) ?? [:]
    }
}

// MARK: - Scan flow models (mirror Android DishCandidate / FoodPrefill in FoodLogScreen.kt)

/// One server-recognised dish: model label, display name, confidence, and
/// nutrition resolved from the offline food lists.
public struct DishCandidate: Identifiable {
    public var id: String { dish }

    public let dish: String
    public let displayName: String
    public let confidence: Double
    public let nutrition: FoodItem?

    public init(dish: String, displayName: String, confidence: Double, nutrition: FoodItem?) {
        self.dish = dish
        self.displayName = displayName
        self.confidence = confidence
        self.nutrition = nutrition
    }
}

/// Values carried from a scan (or a picked search result) into the add sheet.
public struct FoodPrefill: Equatable {
    public let name: String
    public let kcal: Int
    public let proteinG: Int
    public let carbsG: Int
    public let fatG: Int

    public init(name: String, kcal: Int, proteinG: Int, carbsG: Int, fatG: Int) {
        self.name = name
        self.kcal = kcal
        self.proteinG = proteinG
        self.carbsG = carbsG
        self.fatG = fatG
    }
}

/// Model labels are snake_case ("kerala_porotta"); display them as words ("Kerala Porotta").
public func dishDisplayName(_ dish: String) -> String {
    if dish == "beeffry" { return "Beef Fry" }
    return dish.split(separator: "_")
        .map { word in word.prefix(1).uppercased() + String(word.dropFirst()) }
        .joined(separator: " ")
}
