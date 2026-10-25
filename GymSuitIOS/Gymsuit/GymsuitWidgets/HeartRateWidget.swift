import WidgetKit
import SwiftUI

struct HeartRateEntry: TimelineEntry {
    let date: Date
    let latestBpm: Int?
    /// Day label for fallback data ("Yesterday", "Mon", ...); nil = today.
    let dayLabel: String?
    let minBpm: Int?
    let maxBpm: Int?
    let notAuthorized: Bool
}

struct HeartRateProvider: TimelineProvider {
    func placeholder(in context: Context) -> HeartRateEntry {
        HeartRateEntry(date: Date(), latestBpm: 72, dayLabel: nil, minBpm: 58, maxBpm: 112, notAuthorized: false)
    }

    func getSnapshot(in context: Context, completion: @escaping (HeartRateEntry) -> Void) {
        Task {
            completion(await loadEntry())
        }
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<HeartRateEntry>) -> Void) {
        Task {
            let entry = await loadEntry()
            let next = Calendar.current.date(byAdding: .minute, value: 30, to: Date())!
            completion(Timeline(entries: [entry], policy: .after(next)))
        }
    }

    /// Same behavior as the Android heart rate widget: today first, then the
    /// most recent day with data in the last week (labeled), and an explicit
    /// hint when heart-rate access itself is missing.
    private func loadEntry() async -> HeartRateEntry {
        if HealthKitWidgetReader.heartRateAuthorization() != .sharingAuthorized {
            return HeartRateEntry(date: Date(), latestBpm: nil, dayLabel: nil, minBpm: nil, maxBpm: nil, notAuthorized: true)
        }
        if let today = await HealthKitWidgetReader.heartRateForDay(Date()) {
            return HeartRateEntry(date: Date(), latestBpm: today.latest, dayLabel: nil, minBpm: today.min, maxBpm: today.max, notAuthorized: false)
        }
        for daysAgo in 1...6 {
            let day = Calendar.current.date(byAdding: .day, value: -daysAgo, to: Date())!
            if let past = await HealthKitWidgetReader.heartRateForDay(day) {
                let label = daysAgo == 1 ? "Yesterday" : WidgetSharedData.weekdayFormatter.string(from: day)
                return HeartRateEntry(date: Date(), latestBpm: past.latest, dayLabel: label, minBpm: past.min, maxBpm: past.max, notAuthorized: false)
            }
        }
        return HeartRateEntry(date: Date(), latestBpm: nil, dayLabel: nil, minBpm: nil, maxBpm: nil, notAuthorized: false)
    }
}

struct HeartRateWidgetView: View {
    let entry: HeartRateEntry

    var body: some View {
        WidgetCard {
            VStack(alignment: .leading, spacing: 4) {
                WidgetTitle(text: "Heart rate")
                if let bpm = entry.latestBpm {
                    WidgetBigValue(text: "\(bpm) bpm")
                    if let dayLabel = entry.dayLabel {
                        WidgetSubtitle(text: "\(dayLabel) · Min \(entry.minBpm ?? 0) · Max \(entry.maxBpm ?? 0) bpm")
                    } else {
                        WidgetSubtitle(text: "Min \(entry.minBpm ?? 0) · Max \(entry.maxBpm ?? 0) bpm")
                    }
                } else {
                    WidgetBigValue(text: "—")
                    WidgetSubtitle(text: entry.notAuthorized ? "Heart rate access not granted" : "No data yet")
                }
            }
        }
    }
}

struct HeartRateWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "GymsuitHeartRateWidget", provider: HeartRateProvider()) { entry in
            HeartRateWidgetView(entry: entry)
        }
        .configurationDisplayName("GymSuit Heart Rate")
        .description("Latest heart rate today with the day's min and max.")
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}
