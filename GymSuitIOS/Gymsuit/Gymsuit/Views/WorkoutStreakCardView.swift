import SwiftUI

struct WorkoutStreakCardView: View {
    @Binding var selectedDate: Date
    @ObservedObject private var workoutStore = WorkoutStore.shared
    @Environment(\.scenePhase) private var scenePhase

    @AppStorage("workout_personal_best_days") private var storedPersonalBest: Int = 0
    @State private var hkWorkoutDays: Set<Date> = []

    private var calendar: Calendar { Calendar.current }
    private var today: Date { calendar.startOfDay(for: Date()) }

    private let dayHeaders = ["S", "M", "T", "W", "T", "F", "S"]

    private var currentWeekDays: [Date] {
        let weekday = calendar.component(.weekday, from: today) // 1 = Sunday ... 7 = Saturday
        let sunday = calendar.date(byAdding: .day, value: -(weekday - 1), to: today) ?? today
        return (0..<7).compactMap { calendar.date(byAdding: .day, value: $0, to: sunday) }
    }

    /// Combined set of real workout dates (in-app logs + Apple Health / HealthKit sessions)
    private var allWorkoutDays: Set<Date> {
        var days = Set(workoutStore.loggedWorkouts.map { calendar.startOfDay(for: $0.date) })
        days.formUnion(hkWorkoutDays)
        return days
    }

    private var workedOutToday: Bool {
        allWorkoutDays.contains(today)
    }

    /// Real active day streak:
    /// - If worked out today: consecutive days counting backwards from today.
    /// - If not worked out today yet: checks if ongoing streak from yesterday exists.
    /// - Otherwise: 0.
    private var currentStreak: Int {
        guard !allWorkoutDays.isEmpty else { return 0 }

        if workedOutToday {
            var streak = 0
            var checkDate = today
            while allWorkoutDays.contains(checkDate) {
                streak += 1
                guard let prev = calendar.date(byAdding: .day, value: -1, to: checkDate) else { break }
                checkDate = prev
            }
            return streak
        } else {
            guard let yesterday = calendar.date(byAdding: .day, value: -1, to: today),
                  allWorkoutDays.contains(yesterday) else {
                return 0
            }
            var streak = 0
            var checkDate = yesterday
            while allWorkoutDays.contains(checkDate) {
                streak += 1
                guard let prev = calendar.date(byAdding: .day, value: -1, to: checkDate) else { break }
                checkDate = prev
            }
            return streak
        }
    }

    /// Longest consecutive days streak ever recorded in the workout history
    private var personalBest: Int {
        let historyMax = calculateMaxStreak(from: allWorkoutDays)
        let best = max(currentStreak, max(historyMax, storedPersonalBest))
        return best
    }

    private func calculateMaxStreak(from daysSet: Set<Date>) -> Int {
        guard !daysSet.isEmpty else { return 0 }
        let sortedDays = daysSet.sorted()
        var maxStreak = 0
        var current = 0
        var lastDay: Date? = nil

        for day in sortedDays {
            if let last = lastDay {
                if let nextDay = calendar.date(byAdding: .day, value: 1, to: last),
                   calendar.isDate(nextDay, inSameDayAs: day) {
                    current += 1
                } else if !calendar.isDate(last, inSameDayAs: day) {
                    current = 1
                }
            } else {
                current = 1
            }
            if current > maxStreak {
                maxStreak = current
            }
            lastDay = day
        }
        return maxStreak
    }

    var body: some View {
        VStack(spacing: 18) {
            // MARK: - Weekdays Grid
            HStack(spacing: 0) {
                ForEach(Array(currentWeekDays.enumerated()), id: \.offset) { index, date in
                    let isToday = calendar.isDateInToday(date)
                    let isSelected = calendar.isDate(date, inSameDayAs: selectedDate)
                    let isPast = date < today
                    let hasWorkout = allWorkoutDays.contains(date)
                    let dayNumber = calendar.component(.day, from: date)

                    Button(action: {
                        withAnimation(.spring(response: 0.3, dampingFraction: 0.75)) {
                            selectedDate = date
                        }
                    }) {
                        VStack(spacing: 12) {
                            // Day of week letter (S, M, T, W, T, F, S)
                            Text(dayHeaders[index])
                                .font(.system(size: 15, weight: isSelected ? .bold : .medium))
                                .foregroundColor(isSelected ? Color(hex: 0xFF0062FF) : Color(hex: 0xFF8E8E93))

                            // Day number / Active indicator
                            ZStack {
                                if hasWorkout {
                                    // Real workout completed on this day -> blue checkmark badge!
                                    if isSelected && !isToday {
                                        Circle()
                                            .stroke(Color(hex: 0xFF0062FF).opacity(0.35), lineWidth: 3)
                                            .frame(width: 44, height: 44)
                                    }

                                    Circle()
                                        .fill(Color(hex: 0xFF0062FF))
                                        .frame(width: 36, height: 36)

                                    Image(systemName: "checkmark")
                                        .font(.system(size: 16, weight: .bold))
                                        .foregroundColor(.white)
                                } else if isToday {
                                    // Today with no workout done yet -> outlined ring indicating today
                                    Circle()
                                        .stroke(Color(hex: 0xFF0062FF).opacity(0.85), lineWidth: 2)
                                        .frame(width: 36, height: 36)

                                    Text("\(dayNumber)")
                                        .font(.system(size: 18, weight: .bold))
                                        .foregroundColor(Color(hex: 0xFF0062FF))
                                } else if isSelected {
                                    // Selected past/future day without workout
                                    Circle()
                                        .stroke(Color(hex: 0xFF0062FF).opacity(0.6), lineWidth: 2)
                                        .frame(width: 36, height: 36)

                                    Text("\(dayNumber)")
                                        .font(.system(size: 18, weight: .bold))
                                        .foregroundColor(Color(hex: 0xFF0062FF))
                                } else {
                                    // Past or future day with no workout
                                    Text("\(dayNumber)")
                                        .font(.system(size: 18, weight: isPast ? .bold : .semibold))
                                        .foregroundColor(isPast ? Color(hex: 0xFF0F172A) : Color(hex: 0xFF94A3B8))
                                        .frame(width: 36, height: 36)
                                }
                            }
                        }
                        .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.top, 4)

            // MARK: - Divider
            Rectangle()
                .fill(Color(hex: 0xFFF1F5F9))
                .frame(height: 1)
                .padding(.top, 4)

            // MARK: - Stats Section
            HStack(alignment: .bottom) {
                // Day Streak
                VStack(alignment: .leading, spacing: 6) {
                    Text("Day Streak")
                        .font(.system(size: 15, weight: .medium))
                        .foregroundColor(Color(hex: 0xFF64748B))

                    HStack(spacing: 8) {
                        Image(systemName: "flame.fill")
                            .font(.system(size: 20))
                            .foregroundColor(currentStreak > 0 ? Color(hex: 0xFFEF4444) : Color(hex: 0xFFCBD5E1))

                        Text("\(currentStreak)")
                            .font(.system(size: 22, weight: .bold))
                            .foregroundColor(Color(hex: 0xFF0F172A))
                    }
                }

                Spacer()

                // Personal Best
                VStack(alignment: .trailing, spacing: 6) {
                    Text("Personal Best")
                        .font(.system(size: 15, weight: .medium))
                        .foregroundColor(Color(hex: 0xFF64748B))

                    Text("\(personalBest) \(personalBest == 1 ? "day" : "days")")
                        .font(.system(size: 20, weight: .bold))
                        .foregroundColor(Color(hex: 0xFF0F172A))
                }
            }
            .padding(.horizontal, 4)
            .padding(.bottom, 4)
        }
        .padding(20)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .stroke(Color(hex: 0xFFF1F5F9), lineWidth: 1)
        )
        .shadow(color: Color.black.opacity(0.04), radius: 10, y: 4)
        .task {
            await loadRealWorkouts()
        }
        .onChange(of: workoutStore.loggedWorkouts.count) { _, _ in
            Task { await loadRealWorkouts() }
        }
        .onChange(of: scenePhase) { _, newPhase in
            if newPhase == .active {
                Task { await loadRealWorkouts() }
            }
        }
        .onReceive(NotificationCenter.default.publisher(for: .healthKitWorkoutsDidChange)) { _ in
            Task { await loadRealWorkouts() }
        }
    }

    private func loadRealWorkouts() async {
        if !HealthKitManager.shared.isAuthorized && HealthKitManager.shared.isAvailable {
            _ = await HealthKitManager.shared.requestAuthorization()
        }
        let sessions = await HealthKitManager.shared.readRecentWorkoutSessions(daysBack: 90)
        let days = Set(sessions.map { calendar.startOfDay(for: $0.date) })
        await MainActor.run {
            self.hkWorkoutDays = days
            if self.personalBest > self.storedPersonalBest {
                self.storedPersonalBest = self.personalBest
            }
        }
    }
}
