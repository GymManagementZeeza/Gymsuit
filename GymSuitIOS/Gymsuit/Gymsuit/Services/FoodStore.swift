import Foundation
import Combine

/// Persists food log entries as JSON in the app's Documents directory.
/// Mirrors Android's FoodStore (food_log.json, same keys, ascending by time);
/// follows the same ObservableObject + JSON file pattern as WorkoutStore.
public final class FoodStore: ObservableObject {
    public static let shared = FoodStore()

    @Published public private(set) var entries: [FoodEntry] = []

    private let fileName = "food_log.json"
    private let uploadedKey = "food_uploaded_entry_ids"
    private let pendingDeletesKey = "food_pending_delete_ids"
    private let ownerKey = "food_owner_email"

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

    /// Entries the backend has confirmed. Logged entries never change, so each one uploads once.
    public func uploadedIds() -> Set<String> {
        Set(UserDefaults.standard.stringArray(forKey: uploadedKey) ?? [])
    }

    public func markUploaded(ids: [String]) {
        UserDefaults.standard.set(Array(uploadedIds().union(ids)), forKey: uploadedKey)
    }

    /// Remembers a local delete until the backend confirms it, so offline deletes aren't lost.
    public func queueRemoteDelete(id: String) {
        UserDefaults.standard.set(Array(pendingRemoteDeletes().union([id])), forKey: pendingDeletesKey)
    }

    public func pendingRemoteDeletes() -> Set<String> {
        Set(UserDefaults.standard.stringArray(forKey: pendingDeletesKey) ?? [])
    }

    public func clearRemoteDelete(id: String) {
        var pending = pendingRemoteDeletes()
        pending.remove(id)
        UserDefaults.standard.set(Array(pending), forKey: pendingDeletesKey)
    }

    /// Call on login and registration. The log and photos belong to one
    /// account: a different account starts empty instead of uploading them as
    /// its own, while the same account logging back in keeps anything not yet
    /// uploaded.
    public func claim(forEmail email: String) {
        let user = email.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        guard !user.isEmpty else { return }
        if let owner = UserDefaults.standard.string(forKey: ownerKey), owner != user {
            clearAll()
        }
        UserDefaults.standard.set(user, forKey: ownerKey)
    }

    /// Removes the log, its photos and all sync tracking from this device.
    public func clearAll() {
        entries = []
        try? FileManager.default.removeItem(at: fileURL)
        UserDefaults.standard.removeObject(forKey: uploadedKey)
        UserDefaults.standard.removeObject(forKey: pendingDeletesKey)
        UserDefaults.standard.removeObject(forKey: ownerKey)
        FoodPhotoStore.shared.clearAll()
    }

    /// Entries logged on the same calendar day as `date` (device time zone,
    /// like Android's ZoneId.systemDefault()).
    public func entries(onDayOf date: Date, calendar: Calendar = .current) -> [FoodEntry] {
        entries.filter { calendar.isDate($0.date, inSameDayAs: date) }
    }
}

/// Splits entries into upload batches, keeping each request well under
/// nginx's 10 MB limit and the 20 s client timeout. A photo is sent as base64,
/// about a third larger than `photoSizes` says. An entry that alone exceeds
/// `maxPhotoBytes` still gets a batch of its own.
func batchFoodUploads(
    _ entries: [FoodEntry],
    photoSizes: [String: Int],
    maxEntries: Int = 50,
    maxPhotoBytes: Int = 2_000_000
) -> [[FoodEntry]] {
    var batches: [[FoodEntry]] = []
    var current: [FoodEntry] = []
    var currentBytes = 0
    for entry in entries {
        let size = photoSizes[entry.id] ?? 0
        if !current.isEmpty && (current.count >= maxEntries || currentBytes + size > maxPhotoBytes) {
            batches.append(current)
            current = []
            currentBytes = 0
        }
        current.append(entry)
        currentBytes += size
    }
    if !current.isEmpty {
        batches.append(current)
    }
    return batches
}
