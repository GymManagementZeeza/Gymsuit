import WidgetKit
import SwiftUI

struct StepsEntry: TimelineEntry {
    let date: Date
    let steps: Int64?
}

struct StepsProvider: TimelineProvider {
    func placeholder(in context: Context) -> StepsEntry {
        StepsEntry(date: Date(), steps: 7234)
    }

    func getSnapshot(in context: Context, completion: @escaping (StepsEntry) -> Void) {
        Task {
            let steps = await HealthKitWidgetReader.stepsToday()
            completion(StepsEntry(date: Date(), steps: steps))
        }
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<StepsEntry>) -> Void) {
        Task {
            let steps = await HealthKitWidgetReader.stepsToday()
            let entry = StepsEntry(date: Date(), steps: steps)
            let next = Calendar.current.date(byAdding: .minute, value: 30, to: Date())!
            completion(Timeline(entries: [entry], policy: .after(next)))
        }
    }
}

struct StepsWidgetView: View {
    let entry: StepsEntry

    var body: some View {
        WidgetCard {
            VStack(alignment: .leading, spacing: 4) {
                WidgetTitle(text: "Steps")
                if let steps = entry.steps {
                    WidgetBigValue(text: NumberFormatter.localizedString(from: NSNumber(value: steps), number: .decimal))
                    let progress = min(Double(steps) / 10_000.0, 1.0)
                    GeometryReader { geometry in
                        ZStack(alignment: .leading) {
                            Capsule()
                                .fill(Color(red: 0.22, green: 0.25, blue: 0.29))
                            Capsule()
                                .fill(Color(red: 0.0, green: 0.38, blue: 1.0))
                                .frame(width: geometry.size.width * progress)
                        }
                    }
                    .frame(height: 6)
                    .padding(.top, 6)
                    WidgetSubtitle(text: "\(Int(progress * 100))% of 10,000 goal")
                } else {
                    WidgetBigValue(text: "—")
                    WidgetSubtitle(text: "No data yet")
                }
            }
        }
    }
}

struct StepsWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "GymsuitStepsWidget", provider: StepsProvider()) { entry in
            StepsWidgetView(entry: entry)
        }
        .configurationDisplayName("GymSuit Steps")
        .description("Today's step count with progress toward the 10,000 goal.")
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}
