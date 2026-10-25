import WidgetKit
import SwiftUI

struct WorkoutsEntry: TimelineEntry {
    let date: Date
    let weekCount: Int?
    let lastWorkoutName: String?
    let lastWorkoutDate: String?
}

struct WorkoutsProvider: TimelineProvider {
    func placeholder(in context: Context) -> WorkoutsEntry {
        WorkoutsEntry(date: Date(), weekCount: 3, lastWorkoutName: "Barbell bench press", lastWorkoutDate: "Oct 24")
    }

    func getSnapshot(in context: Context, completion: @escaping (WorkoutsEntry) -> Void) {
        completion(loadEntry())
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<WorkoutsEntry>) -> Void) {
        let entry = loadEntry()
        let next = Calendar.current.date(byAdding: .minute, value: 30, to: Date())!
        completion(Timeline(entries: [entry], policy: .after(next)))
    }

    private func loadEntry() -> WorkoutsEntry {
        let store = WidgetSharedData.store
        // No snapshot yet (app hasn't written one): show the empty state
        // rather than stale numbers.
        guard let store,
              store.object(forKey: WidgetSharedData.workoutsWeekCountKey) != nil else {
            return WorkoutsEntry(date: Date(), weekCount: nil, lastWorkoutName: nil, lastWorkoutDate: nil)
        }
        return WorkoutsEntry(
            date: Date(),
            weekCount: store.integer(forKey: WidgetSharedData.workoutsWeekCountKey),
            lastWorkoutName: store.string(forKey: WidgetSharedData.lastWorkoutNameKey),
            lastWorkoutDate: store.string(forKey: WidgetSharedData.lastWorkoutDateKey)
        )
    }
}

struct WorkoutsWidgetView: View {
    let entry: WorkoutsEntry

    var body: some View {
        WidgetCard {
            VStack(alignment: .leading, spacing: 4) {
                WidgetTitle(text: "Workouts")
                if let count = entry.weekCount {
                    WidgetBigValue(text: "\(count) this week")
                    if let name = entry.lastWorkoutName, let date = entry.lastWorkoutDate {
                        WidgetSubtitle(text: "Last: \(name) · \(date)")
                    } else {
                        WidgetSubtitle(text: "Log your first workout")
                    }
                } else {
                    WidgetBigValue(text: "—")
                    WidgetSubtitle(text: "Open GymSuit to sync")
                }
            }
        }
    }
}

struct WorkoutsWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "GymsuitWorkoutsWidget", provider: WorkoutsProvider()) { entry in
            WorkoutsWidgetView(entry: entry)
        }
        .configurationDisplayName("GymSuit Workouts")
        .description("Workouts logged in the last 7 days plus your last workout.")
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}
