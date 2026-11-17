import SwiftUI

struct AppColors {
    static let primary = Color(hex: 0xFF2563EB) // Vivid modern blue accent
    static let primaryLight = Color(hex: 0xFFEFF6FF)
    static let surface = Color(hex: 0xFFF9FAFB)
    static let cardBackground = Color.white
    static let textPrimary = Color(hex: 0xFF111827)
    static let textSecondary = Color(hex: 0xFF6B7280)
    static let textTertiary = Color(hex: 0xFF9CA3AF)
    static let divider = Color(hex: 0xFFE5E7EB)
    static let border = Color(hex: 0xFFE2E8F0)
    
    // Status colors
    static let success = Color(hex: 0xFF10B981)
    static let warning = Color(hex: 0xFFF59E0B)
    static let danger = Color(hex: 0xFFEF4444)
    static let purple = Color(hex: 0xFF7C3AED)
    static let purpleLight = Color(hex: 0xFFEDE9FE)
    
    // Health / Metric accents
    static let calories = Color(hex: 0xFF6366F1)
    static let steps = Color(hex: 0xFF3B82F6)
    static let workout = Color(hex: 0xFF10B981)
    static let heartRate = Color(hex: 0xFFEF4444)
    static let sleep = Color(hex: 0xFF8B5CF6)
}

/// Apple-style grouped-list palette: light grey page, white cards, one blue
/// accent. Same values as the Android Meals and Challenges screens.
struct GroupedColors {
    static let page = Color(hex: 0xFFF2F2F7)
    static let card = Color.white
    static let label = Color(hex: 0xFF1C1C1E)
    static let secondaryLabel = Color(hex: 0xFF8E8E93)
    static let separator = Color(hex: 0xFFE5E5EA)
    static let accent = Color(hex: 0xFF007AFF)
    static let accentTint = Color(hex: 0xFFEAF2FF)
    static let orange = Color(hex: 0xFFFF9500)
    static let green = Color(hex: 0xFF34C759)
}

extension Color {
    init(hex: UInt32, alpha: Double = 1.0) {
        let red = Double((hex >> 16) & 0xff) / 255.0
        let green = Double((hex >> 8) & 0xff) / 255.0
        let blue = Double(hex & 0xff) / 255.0
        self.init(.sRGB, red: red, green: green, blue: blue, opacity: alpha)
    }
}
