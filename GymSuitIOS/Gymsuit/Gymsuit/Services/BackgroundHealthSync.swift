import BackgroundTasks
import Foundation

/// Periodic background health sync (BGAppRefreshTask).
///
/// Honest platform limits, not bugs:
/// - iOS decides *when* to run this; there is no exact interval. The system
///   learns from usage — frequent app users get more background runs.
/// - Each run gets roughly 30 seconds. The sync is a no-op when logged out.
/// - For near-instant wakeups on new HealthKit data, HKObserverQuery background
///   delivery would be the deeper integration; this task is the periodic sweep.
enum BackgroundHealthSync {

    static let taskIdentifier = "ca.zeezaglobal.Gymsuit.healthsync"

    /// How soon to ask for the next run. iOS treats this as a hint, not a promise.
    private static let interval: TimeInterval = 4 * 60 * 60

    private static let enabledKey = "healthAutoSyncEnabled"

    /// Whether background health sync is enabled (Settings → "Apple Health
    /// Auto-Sync"). Defaults to true. When disabled, no background task is
    /// scheduled; foreground syncs are unaffected.
    static var isEnabled: Bool {
        get { UserDefaults.standard.object(forKey: enabledKey) as? Bool ?? true }
        set { UserDefaults.standard.set(newValue, forKey: enabledKey) }
    }

    static func register() {
        BGTaskScheduler.shared.register(forTaskWithIdentifier: taskIdentifier, using: nil) { task in
            guard let refreshTask = task as? BGAppRefreshTask else {
                task.setTaskCompleted(success: false)
                return
            }
            handle(task: refreshTask)
        }
    }

    static func schedule() {
        guard isEnabled else { return }
        let request = BGAppRefreshTaskRequest(identifier: taskIdentifier)
        request.earliestBeginDate = Date(timeIntervalSinceNow: interval)
        do {
            try BGTaskScheduler.shared.submit(request)
        } catch {
            // Already scheduled or scheduler busy — harmless, next foregrounding retries.
            print("[BackgroundHealthSync] schedule failed: \(error)")
        }
    }

    /// Cancels any pending background sync task (called when auto-sync is
    /// turned off in Settings).
    static func cancel() {
        BGTaskScheduler.shared.cancel(taskRequestWithIdentifier: taskIdentifier)
    }

    private static func handle(task: BGAppRefreshTask) {
        // Reschedule first so the cadence survives even if this run is killed.
        schedule()

        let state = TaskState()
        let work = Task {
            let ok = await HealthSyncManager.shared.sync()
            if ok {
                // Evening daily summary ("You walked X steps today...").
                // Never fails the sync: the notifier guards its own errors.
                await MetricNotifications.maybeNotify()
            }
            state.complete(task: task, success: ok)
        }
        task.expirationHandler = {
            work.cancel()
            state.complete(task: task, success: false)
        }
    }

    /// Guards against completing the same BGTask twice (race between work
    /// finishing and the expiration handler firing).
    private final class TaskState: @unchecked Sendable {
        private var done = false
        private let lock = NSLock()

        func complete(task: BGTask, success: Bool) {
            lock.lock()
            defer { lock.unlock() }
            guard !done else { return }
            done = true
            task.setTaskCompleted(success: success)
        }
    }
}
