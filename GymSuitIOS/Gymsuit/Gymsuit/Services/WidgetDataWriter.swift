import Foundation
import WidgetKit

/// Writes the snapshots the home-screen widgets read from the shared App
/// Group. Steps and heart rate need no snapshot: the widgets query HealthKit
/// directly. Workouts come from the local log; challenges come from the
/// backend. Every write reloads widget timelines, and every function is
/// best-effort — a widget write must never break the app flow that called it.
enum WidgetDataWriter {

    /// Workouts widget: count logged in the last 7 days + last workout.
    /// Called from WorkoutStore whenever the log is loaded or persisted, so
    /// both existing logs and new entries reach the widget.
    static func writeWorkoutsSnapshot(workouts: [LoggedWorkout]) {
        guard let store = WidgetSharedData.store else { return }
        let weekStart = Calendar.current.date(
            byAdding: .day, value: -6,
            to: Calendar.current.startOfDay(for: Date())
        ) ?? Date()
        let weekCount = workouts.filter { $0.date >= weekStart }.count
        store.set(weekCount, forKey: WidgetSharedData.workoutsWeekCountKey)
        if let last = workouts.sorted(by: { $0.date > $1.date }).first {
            store.set(last.exerciseName, forKey: WidgetSharedData.lastWorkoutNameKey)
            store.set(
                WidgetSharedData.dayFormatter.string(from: last.date),
                forKey: WidgetSharedData.lastWorkoutDateKey
            )
        } else {
            store.removeObject(forKey: WidgetSharedData.lastWorkoutNameKey)
            store.removeObject(forKey: WidgetSharedData.lastWorkoutDateKey)
        }
        WidgetCenter.shared.reloadAllTimelines()
    }

    /// Challenges widget: writes the synced list, then fetches details for
    /// active challenges so the widget has their members and progress.
    /// Called from the challenges list load.
    static func refreshChallengeSnapshots(summaries: [ChallengeSummary]) async {
        var snapshots = summaries.map { summary in
            WidgetChallenge(
                id: summary.challengeId ?? -1,
                name: summary.name ?? "",
                status: summary.status ?? "",
                metric: summary.metricType ?? "",
                participantCount: summary.participantCount ?? 0,
                myRank: summary.myRank,
                members: []
            )
        }
        WidgetSharedData.saveChallenges(snapshots)
        WidgetCenter.shared.reloadAllTimelines()
        for summary in summaries where summary.status == ChallengeStatus.active.rawValue {
            if let detail = try? await ChallengeApi.shared.fetchDetail(id: summary.id) {
                writeChallengeDetail(detail)
            }
        }
        // Keep any previously snapshotted detail whose challenge is no
        // longer in the list from lingering.
        snapshots = WidgetSharedData.loadChallenges()
        let liveIds = Set(summaries.map { $0.challengeId ?? -1 })
        let pruned = snapshots.filter { liveIds.contains($0.id) }
        if pruned.count != snapshots.count {
            WidgetSharedData.saveChallenges(pruned)
            WidgetCenter.shared.reloadAllTimelines()
        }
    }

    /// Merges one challenge detail (leaderboard top 3) into the snapshot.
    /// Called from the challenge detail load.
    static func writeChallengeDetail(_ detail: ChallengeDetail) {
        var current = WidgetSharedData.loadChallenges()
        let members = (detail.leaderboard ?? [])
            .sorted { ($0.rank ?? 999) < ($1.rank ?? 999) }
            .prefix(3)
            .map { WidgetMember(name: $0.displayName ?? "—", value: $0.value ?? 0) }
        let snapshot = WidgetChallenge(
            id: detail.challengeId ?? -1,
            name: detail.name ?? "",
            status: detail.status ?? "",
            metric: detail.metricType ?? "",
            participantCount: detail.participantCount ?? 0,
            myRank: detail.myRank,
            members: Array(members)
        )
        if let index = current.firstIndex(where: { $0.id == snapshot.id }) {
            current[index] = snapshot
        } else {
            current.append(snapshot)
        }
        WidgetSharedData.saveChallenges(current)
        WidgetCenter.shared.reloadAllTimelines()
    }
}
