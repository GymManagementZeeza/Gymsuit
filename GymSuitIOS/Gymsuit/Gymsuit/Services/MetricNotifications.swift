import Foundation
import UserNotifications

/// Daily metric summary notification, posted from the background health sync
/// (`BackgroundHealthSync`) so users get a reason to open the app — and
/// opening the app runs a fresh sync (see ContentView).
///
/// Rules, deliberately boring:
/// - At most one notification per calendar day, and only on a sync that runs
///   at or after 6 PM local time, so "today" actually has a day's worth of
///   steps in it. iOS decides when background tasks run, so on days with no
///   evening run there is simply no notification that day.
/// - Only posts when there is something to report (steps > 0 or a sleep
///   record). `fetchSleepSession(for: Date())` already covers the night
///   before the given date, so today's session is last night's sleep.
/// - Only posts when notification authorization is already granted; it never
///   prompts from the background. Users control this in system Settings.
/// - Tapping the notification opens the app (default tap behaviour).
enum MetricNotifications {

    private static let lastNotifiedKey = "metric_notification_last_date"
    private static let eveningHour = 18

    static func maybeNotify() async {
        let calendar = Calendar.current
        let now = Date()
        guard calendar.component(.hour, from: now) >= eveningHour else { return }

        let todayKey = dateKey(now)
        guard UserDefaults.standard.string(forKey: lastNotifiedKey) != todayKey else { return }

        let settings = await UNUserNotificationCenter.current().notificationSettings()
        guard settings.authorizationStatus == .authorized || settings.authorizationStatus == .provisional else { return }

        let steps = await HealthKitManager.shared.fetchSteps(for: now)
        let sleepMinutes = await HealthKitManager.shared.fetchSleepSession(for: now)?.durationMinutes ?? 0
        guard steps > 0 || sleepMinutes > 0 else { return }

        var lines: [String] = []
        if steps > 0 {
            lines.append("You walked \(formatNumber(steps)) steps today.")
        }
        if sleepMinutes > 0 {
            lines.append("You slept \(formatDuration(sleepMinutes)) last night.")
        }

        let content = UNMutableNotificationContent()
        content.title = "Your day in GymSuit"
        content.body = lines.joined(separator: "\n")
        content.sound = .default

        let request = UNNotificationRequest(
            identifier: "daily_metric_summary_\(todayKey)",
            content: content,
            trigger: nil
        )
        do {
            try await UNUserNotificationCenter.current().add(request)
            UserDefaults.standard.set(todayKey, forKey: lastNotifiedKey)
            print("[MetricNotifications] Daily metric notification posted (steps=\(steps), sleepMin=\(sleepMinutes))")
        } catch {
            print("[MetricNotifications] Failed to post: \(error)")
        }
    }

    private static func dateKey(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter.string(from: date)
    }

    private static func formatNumber(_ value: Int64) -> String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .decimal
        return formatter.string(from: NSNumber(value: value)) ?? "\(value)"
    }

    private static func formatDuration(_ totalMinutes: Int64) -> String {
        let hours = totalMinutes / 60
        let mins = totalMinutes % 60
        return hours > 0 ? "\(hours)h \(mins)m" : "\(mins)m"
    }
}
