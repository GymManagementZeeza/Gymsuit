import Foundation
import UserNotifications

/// iOS mirror of Android's MealReminders: one daily reminder per meal slot —
/// morning, noon, evening and night. Android checks the food log when a
/// reminder fires and skips meals already logged; iOS notifications can't run
/// that check at fire time without an extension, so `schedule()` arms only
/// the next occurrence of each slot and skips today's slot when that meal is
/// already logged. The Food tab re-arms on appear (Android restores its
/// schedule on app start), which keeps the behaviour aligned day to day.
public enum MealReminders {

    public struct Slot {
        public let id: String
        public let hour: Int
        public let minute: Int
        public let meal: MealType
        public let title: String
        public let text: String
    }

    public static let slots: [Slot] = [
        Slot(id: "MORNING", hour: 8, minute: 30, meal: .breakfast,
             title: "Good morning", text: "Log your breakfast to start the day on track."),
        Slot(id: "NOON", hour: 12, minute: 30, meal: .lunch,
             title: "Lunch time", text: "Don't forget to log what you ate for lunch."),
        Slot(id: "EVENING", hour: 17, minute: 30, meal: .snack,
             title: "Evening check-in", text: "Had a snack or tea? Log it now."),
        Slot(id: "NIGHT", hour: 20, minute: 30, meal: .dinner,
             title: "Dinner time", text: "Log your dinner to complete today's food diary.")
    ]

    private static let defaultsKey = "meal_reminders_enabled"
    private static let identifierPrefix = "meal_reminder_"

    public static var isEnabled: Bool {
        UserDefaults.standard.bool(forKey: defaultsKey)
    }

    public static func hasAuthorization() async -> Bool {
        let settings = await UNUserNotificationCenter.current().notificationSettings()
        return settings.authorizationStatus == .authorized || settings.authorizationStatus == .provisional
    }

    /// Asks for notification permission if not yet decided (Android requests
    /// POST_NOTIFICATIONS when the toggle is switched on). Returns whether
    /// reminders are allowed to fire.
    public static func requestAuthorization() async -> Bool {
        let center = UNUserNotificationCenter.current()
        let settings = await center.notificationSettings()
        switch settings.authorizationStatus {
        case .authorized, .provisional:
            return true
        case .notDetermined:
            return (try? await center.requestAuthorization(options: [.alert, .sound])) ?? false
        default:
            return false
        }
    }

    public static func setEnabled(_ enabled: Bool) {
        UserDefaults.standard.set(enabled, forKey: defaultsKey)
        if enabled {
            schedule()
        } else {
            cancel()
        }
    }

    /// Re-applies the schedule if reminders are on (Android calls restore()
    /// on app start; the Food tab calls this when it appears and after a
    /// meal is logged, so already-logged slots stay skipped).
    public static func restore() {
        if isEnabled { schedule() }
    }

    public static func schedule() {
        cancel()
        let center = UNUserNotificationCenter.current()
        let calendar = Calendar.current
        let now = Date()
        let loggedToday = Set(FoodStore.shared.entries(onDayOf: now).map { $0.meal })

        for slot in slots {
            var components = calendar.dateComponents([.year, .month, .day], from: now)
            components.hour = slot.hour
            components.minute = slot.minute
            components.second = 0
            guard var fireDate = calendar.date(from: components) else { continue }
            if fireDate <= now {
                // Today's slot has passed — arm it for tomorrow.
                fireDate = calendar.date(byAdding: .day, value: 1, to: fireDate) ?? fireDate
            } else if loggedToday.contains(slot.meal) {
                // That meal is already logged today — skip, like Android's worker.
                continue
            }

            let content = UNMutableNotificationContent()
            content.title = slot.title
            content.body = slot.text
            content.sound = .default

            let trigger = UNCalendarNotificationTrigger(
                dateMatching: calendar.dateComponents([.year, .month, .day, .hour, .minute], from: fireDate),
                repeats: false
            )
            let request = UNNotificationRequest(
                identifier: identifierPrefix + slot.id,
                content: content,
                trigger: trigger
            )
            center.add(request)
        }
    }

    public static func cancel() {
        UNUserNotificationCenter.current().removePendingNotificationRequests(
            withIdentifiers: slots.map { identifierPrefix + $0.id }
        )
    }
}
