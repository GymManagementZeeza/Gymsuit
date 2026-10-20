import Foundation
import Combine

/// Persists food log entries as JSON in the app's Documents directory.
/// Mirrors Android's FoodStore (food_log.json, same keys, ascending by time);
/// follows the same ObservableObject + JSON file pattern as WorkoutStore.
public final class FoodStore: ObservableObject {
    public static let shared = FoodStore()

    @Published public private(set) var entries: [FoodEntry] = []

    private let fileName = "food_log.json"

    private init() {
        load()
    }

    private var fileURL: URL {
        FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
            .appendingPathComponent(fileName)
    }

    private func load() {
        guard let data = try? Data(contentsOf: fileURL),
              let decoded = try? JSONDecoder().decode([FoodEntry].self, from: data) else {
            return
        }
        entries = decoded.sorted { $0.timestampMillis < $1.timestampMillis }
    }

    private func persist() {
        let data = try? JSONEncoder().encode(entries)
        try? data?.write(to: fileURL, options: .atomic)
    }

    public func add(_ entry: FoodEntry) {
        entries.append(entry)
        entries.sort { $0.timestampMillis < $1.timestampMillis }
        persist()
    }

    public func delete(id: String) {
        entries.removeAll { $0.id == id }
        persist()
    }

    /// Entries logged on the same calendar day as `date` (device time zone,
    /// like Android's ZoneId.systemDefault()).
    public func entries(onDayOf date: Date, calendar: Calendar = .current) -> [FoodEntry] {
        entries.filter { calendar.isDate($0.date, inSameDayAs: date) }
    }
}
