import Foundation

/// Display unit for weights. Storage is ALWAYS kilograms (see below).
public enum WeightUnit: String {
    case kg
    case lb

    public var symbol: String { rawValue }
}

/// Weight unit preference + conversion helpers.
///
/// Canonical storage unit is kilograms: `WorkoutSet.weightKg` has always been
/// entered and stored as kg (every weight field in the app was labelled
/// "Weight (kg)"), and volume totals / HealthKit sync are computed from it.
/// The lb/kg preference therefore only changes what the UI shows and what the
/// user types — entered values are converted to kg before they are stored,
/// and stored values are converted for display. Toggling the unit never
/// rewrites stored history.
public enum WeightUnits {
    /// 1 kg = 2.20462 lb.
    public static let lbPerKg = 2.20462

    /// UserDefaults key for the persisted preference.
    public static let storageKey = "weight_unit"

    public static func other(_ unit: WeightUnit) -> WeightUnit {
        unit == .kg ? .lb : .kg
    }

    /// Stored kg -> value in the display unit.
    public static func kgToDisplay(_ kg: Double, unit: WeightUnit) -> Double {
        unit == .lb ? kg * lbPerKg : kg
    }

    /// Value typed in the display unit -> stored kg.
    public static func displayToKg(_ value: Double, unit: WeightUnit) -> Double {
        unit == .lb ? value / lbPerKg : value
    }

    /// One decimal place, trailing ".0" trimmed (100.0 -> "100", 45.36 -> "45.4").
    public static func format(_ value: Double) -> String {
        let rounded = String(format: "%.1f", value)
        return rounded.hasSuffix(".0") ? String(rounded.dropLast(2)) : rounded
    }

    /// "100 kg" / "45.4 kg" style label for a stored kg value in the given unit.
    public static func label(kg: Double, unit: WeightUnit) -> String {
        "\(format(kgToDisplay(kg, unit: unit))) \(unit.symbol)"
    }
}
