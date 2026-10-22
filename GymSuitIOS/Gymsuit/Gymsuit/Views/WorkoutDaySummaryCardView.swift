import SwiftUI
import HealthKit

struct WorkoutDaySummaryCardView: View {
    var selectedDate: Date
    @ObservedObject private var workoutStore = WorkoutStore.shared
    @Environment(\.scenePhase) private var scenePhase

    @State private var hkWorkouts: [HKWorkout] = []
    @State private var hkWorkoutKcal: Double = 0.0
    @State private var hkWorkoutMinutes: Int = 0

    private var calendar: Calendar { Calendar.current }

    private var dayLoggedWorkouts: [LoggedWorkout] {
        workoutStore.loggedWorkouts.filter {
            calendar.isDate($0.date, inSameDayAs: selectedDate)
        }
    }

    /// Total workout count: HealthKit workouts + any unsynced in-app workouts
    private var workoutCount: Int {
        let unsyncedCount = dayLoggedWorkouts.filter { !$0.syncedToHealthKit }.count
        return hkWorkouts.count + unsyncedCount
    }

    /// Total active calories burned from workouts on the selected day
    private var totalCaloriesKcal: Int {
        let unsyncedKcal = dayLoggedWorkouts
            .filter { !$0.syncedToHealthKit }
            .reduce(0.0) { $0 + Double($1.durationMinutes) * 7.5 }
        let total = hkWorkoutKcal + unsyncedKcal
        return Int(round(total))
    }

    /// Total minutes spent working out on the selected day
    private var totalMinutes: Int {
        let unsyncedMinutes = dayLoggedWorkouts
            .filter { !$0.syncedToHealthKit }
            .reduce(0) { $0 + $1.durationMinutes }
        return hkWorkoutMinutes + unsyncedMinutes
    }

    var body: some View {
        HStack(spacing: 0) {
            // 1. Workout Count Column
            metricColumn(
                iconName: "medal",
                value: "\(workoutCount)",
                label: "Workout"
            )

            // 2. Kcal Column
            metricColumn(
                iconName: "flame",
                value: "\(totalCaloriesKcal)",
                label: "Kcal"
            )

            // 3. Minute Column
            metricColumn(
                iconName: "clock",
                value: "\(totalMinutes)",
                label: "Minute"
            )
        }
        .padding(.vertical, 22)
        .padding(.horizontal, 16)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .stroke(Color(hex: 0xFFF1F5F9), lineWidth: 1)
        )
        .shadow(color: Color.black.opacity(0.04), radius: 10, y: 4)
        .task(id: selectedDate) {
            await loadDayData()
        }
        .onChange(of: workoutStore.loggedWorkouts.count) { _, _ in
            Task { await loadDayData() }
        }
        .onChange(of: scenePhase) { _, newPhase in
            if newPhase == .active {
                Task { await loadDayData() }
            }
        }
        .onReceive(NotificationCenter.default.publisher(for: .healthKitWorkoutsDidChange)) { _ in
            Task { await loadDayData() }
        }
    }

    private func metricColumn(iconName: String, value: String, label: String) -> some View {
        VStack(spacing: 8) {
            // Icon with soft blue offset circle shadow
            ZStack(alignment: .bottomTrailing) {
                Circle()
                    .fill(Color(hex: 0xFF93C5FD).opacity(0.85))
                    .frame(width: 17, height: 17)
                    .offset(x: 3, y: 3)

                Image(systemName: iconName)
                    .font(.system(size: 22, weight: .regular))
                    .foregroundColor(Color(hex: 0xFF0F172A))
            }
            .frame(width: 32, height: 32)
            .padding(.bottom, 2)

            // Value
            Text(value)
                .font(.system(size: 28, weight: .bold))
                .foregroundColor(Color(hex: 0xFF0F172A))

            // Label
            Text(label)
                .font(.system(size: 15, weight: .medium))
                .foregroundColor(Color(hex: 0xFF64748B))
        }
        .frame(maxWidth: .infinity)
    }

    private func loadDayData() async {
        if !HealthKitManager.shared.isAuthorized && HealthKitManager.shared.isAvailable {
            _ = await HealthKitManager.shared.requestAuthorization()
        }
        let summary = await HealthKitManager.shared.fetchWorkoutSummary(for: selectedDate)
        await MainActor.run {
            self.hkWorkouts = summary.workouts
            self.hkWorkoutKcal = summary.caloriesKcal
            self.hkWorkoutMinutes = summary.durationMinutes
        }
    }
}
