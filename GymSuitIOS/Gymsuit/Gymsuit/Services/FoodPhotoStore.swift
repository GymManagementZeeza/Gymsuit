import Foundation
import UIKit

/// Keeps the compressed meal photo for each scanned food entry on-device
/// until the backend confirms it received it. Photos are stored as JPEG
/// (longest side 1024px, via foodScanJPEGData) under Documents/food_photos/
/// <entryId>.jpg — never inside food_log.json, so the log itself stays small.
///
/// Upload tracking lives in UserDefaults: an entry id is marked uploaded
/// only after a successful sync, so a failed upload is retried by the next
/// sync instead of being lost, and already-uploaded photos are never
/// re-sent on every periodic sync.
public final class FoodPhotoStore {
    public static let shared = FoodPhotoStore()

    private let uploadedKey = "food_photo_uploaded_ids"

    private var directory: URL {
        let dir = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
            .appendingPathComponent("food_photos")
        try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        return dir
    }

    private func fileURL(for entryId: String) -> URL {
        directory.appendingPathComponent("\(entryId).jpg")
    }

    @discardableResult
    public func save(id: String, image: UIImage) -> Bool {
        guard let data = image.foodScanJPEGData() else { return false }
        do {
            try data.write(to: fileURL(for: id), options: .atomic)
            return true
        } catch {
            return false
        }
    }

    public func read(id: String) -> Data? {
        try? Data(contentsOf: fileURL(for: id))
    }

    public func delete(id: String) {
        try? FileManager.default.removeItem(at: fileURL(for: id))
        var uploaded = Set(UserDefaults.standard.stringArray(forKey: uploadedKey) ?? [])
        if uploaded.remove(id) != nil {
            UserDefaults.standard.set(Array(uploaded), forKey: uploadedKey)
        }
    }

    /// Sizes in bytes of the photos that exist locally but have not been
    /// confirmed uploaded yet, keyed by entry id. Sizes rather than data, so a
    /// backlog is only read into memory one upload batch at a time.
    public func pendingPhotoSizes() -> [String: Int] {
        let uploaded = Set(UserDefaults.standard.stringArray(forKey: uploadedKey) ?? [])
        var pending: [String: Int] = [:]
        let files = (try? FileManager.default.contentsOfDirectory(at: directory, includingPropertiesForKeys: [.fileSizeKey])) ?? []
        for file in files where file.pathExtension == "jpg" {
            let entryId = file.deletingPathExtension().lastPathComponent
            if !uploaded.contains(entryId) {
                pending[entryId] = (try? file.resourceValues(forKeys: [.fileSizeKey]))?.fileSize ?? 0
            }
        }
        return pending
    }

    /// Removes every cached photo and its upload tracking.
    public func clearAll() {
        try? FileManager.default.removeItem(at: directory)
        UserDefaults.standard.removeObject(forKey: uploadedKey)
    }

    public func markUploaded(ids: [String]) {
        guard !ids.isEmpty else { return }
        var uploaded = Set(UserDefaults.standard.stringArray(forKey: uploadedKey) ?? [])
        uploaded.formUnion(ids)
        UserDefaults.standard.set(Array(uploaded), forKey: uploadedKey)
    }
}
