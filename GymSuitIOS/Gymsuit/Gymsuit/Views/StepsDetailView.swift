//
//  StepsDetailView.swift
//  Gymsuit
//

import SwiftUI

public struct DailyStepItem: Identifiable, Equatable {
    public let id = UUID()
    public let date: Date
    public let dayName: String
    public let dayNumber: String
    public let steps: Int64
    public let isToday: Bool
}

struct StepsDetailView: View {
    let date: Date
    var onBack: () -> Void

    @State private var weeklySteps: [DailyStepItem] = []
    @State private var selectedDayIndex: Int = 6 // Default to the latest day
    @State private var dailyGoal: Int64 = 10_000
    @State private var isLoading: Bool = true
    @State private var showGoalDialog: Bool = false
    @State private var inputGoalText: String = "10000"

    private let healthKit = HealthKitManager.shared

    private var selectedDay: DailyStepItem? {
        guard weeklySteps.indices.contains(selectedDayIndex) else {
            return weeklySteps.last
        }
        return weeklySteps[selectedDayIndex]
    }

    private var maxStepsInWeek: Int64 {
        let maxVal = weeklySteps.map(\.steps).max() ?? 0
        return max(maxVal, dailyGoal)
    }

    private var totalWeeklySteps: Int64 {
        weeklySteps.reduce(0) { $0 + $1.steps }
    }

    private var averageDailySteps: Int64 {
        weeklySteps.isEmpty ? 0 : totalWeeklySteps / Int64(weeklySteps.count)
    }

    var body: some View {
        ZStack {
            Color(hex: 0xFFF3F2F8).ignoresSafeArea()

            VStack(spacing: 0) {
                // Top App Bar
                HStack {
                    Button(action: onBack) {
                        Image(systemName: "chevron.left")
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(AppColors.textPrimary)
                            .frame(width: 44, height: 44)
                            .background(Color.white)
                            .clipShape(Circle())
                            .shadow(color: Color.black.opacity(0.04), radius: 4, y: 2)
                    }

                    Spacer()

                    Text("Step Activity")
                        .font(.system(size: 18, weight: .bold))
                        .foregroundColor(AppColors.textPrimary)

                    Spacer()

                    Button(action: {
                        inputGoalText = "\(dailyGoal)"
                        showGoalDialog = true
                    }) {
                        Image(systemName: "pencil")
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(AppColors.primary)
                            .frame(width: 44, height: 44)
                            .background(Color.white)
                            .clipShape(Circle())
                            .shadow(color: Color.black.opacity(0.04), radius: 4, y: 2)
                    }
                }
                .padding(.horizontal, 20)
                .padding(.top, 10)
                .padding(.bottom, 12)

                ScrollView(showsIndicators: false) {
                    VStack(spacing: 20) {
                        if isLoading {
                            VStack(spacing: 12) {
                                ProgressView()
                                    .scaleEffect(1.2)
                                Text("Loading 7-day step history…")
                                    .font(.system(size: 14, weight: .medium))
                                    .foregroundColor(AppColors.textSecondary)
                            }
                            .frame(maxWidth: .infinity, minHeight: 250)
                            .background(Color.white)
                            .clipShape(RoundedRectangle(cornerRadius: 24))
                            .shadow(color: Color.black.opacity(0.03), radius: 6, y: 3)
                        } else {
                            // Selected Day Focus Card
                            if let current = selectedDay {
                                selectedDayHeaderCard(current)
                            }

                            // 7 Days Bar Graph Card
                            weeklyBarChartCard

                            // Quick Stats Cards (Distance, Calories, Active Time)
                            if let current = selectedDay {
                                quickStatsRow(current)
                            }

                            // 7-Day History List
                            dailyBreakdownListCard
                        }
                    }
                    .padding(.horizontal, 20)
                    .padding(.bottom, 30)
                }
            }
        }
        .onAppear {
            load7DaysData()
        }
        .alert("Set Daily Step Goal", isPresented: $showGoalDialog) {
            TextField("Steps", text: $inputGoalText)
                .keyboardType(.numberPad)
            Button("Cancel", role: .cancel) {}
            Button("Save") {
                if let newGoal = Int64(inputGoalText), newGoal > 0 {
                    dailyGoal = newGoal
                }
            }
        } message: {
            Text("Enter your daily target steps.")
        }
    }

    // MARK: - Selected Day Header Card
    private func selectedDayHeaderCard(_ item: DailyStepItem) -> some View {
        let progress = min(1.0, Double(item.steps) / Double(max(1, dailyGoal)))
        let isGoalMet = item.steps >= dailyGoal

        return VStack(spacing: 16) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text(item.isToday ? "Today's Steps" : formattedDateString(item.date))
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(AppColors.textSecondary)

                    HStack(alignment: .firstTextBaseline, spacing: 6) {
                        Text("\(item.steps.formatted())")
                            .font(.system(size: 36, weight: .bold, design: .rounded))
                            .foregroundColor(AppColors.textPrimary)

                        Text("steps")
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundColor(AppColors.textSecondary)
                    }
                }

                Spacer()

                // Goal Pill
                HStack(spacing: 5) {
                    Image(systemName: isGoalMet ? "checkmark.circle.fill" : "target")
                        .font(.system(size: 13, weight: .bold))
                    Text("\(Int(progress * 100))% of goal")
                        .font(.system(size: 12, weight: .bold))
                }
                .foregroundColor(isGoalMet ? Color(hex: 0xFF10B981) : Color(hex: 0xFF3B82F6))
                .padding(.horizontal, 12)
                .padding(.vertical, 7)
                .background(
                    (isGoalMet ? Color(hex: 0xFF10B981) : Color(hex: 0xFF3B82F6))
                        .opacity(0.12)
                )
                .clipShape(Capsule())
            }

            // Progress Bar
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Capsule()
                        .fill(Color(hex: 0xFFEFF6FF))
                        .frame(height: 10)

                    Capsule()
                        .fill(
                            LinearGradient(
                                colors: [Color(hex: 0xFF60A5FA), Color(hex: 0xFF2563EB)],
                                startPoint: .leading,
                                endPoint: .trailing
                            )
                        )
                        .frame(width: max(10, geo.size.width * CGFloat(progress)), height: 10)
                        .animation(.easeOut(duration: 0.6), value: progress)
                }
            }
            .frame(height: 10)

            HStack {
                Text("0")
                    .font(.system(size: 11, weight: .medium))
                    .foregroundColor(AppColors.textTertiary)
                Spacer()
                Text("Goal: \(dailyGoal.formatted())")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundColor(AppColors.textSecondary)
            }
        }
        .padding(20)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 24))
        .shadow(color: Color.black.opacity(0.03), radius: 6, y: 3)
    }

    // MARK: - 7 Days Bar Graph Card
    private var weeklyBarChartCard: some View {
        VStack(alignment: .leading, spacing: 18) {
            HStack {
                VStack(alignment: .leading, spacing: 3) {
                    Text("7-Day Activity")
                        .font(.system(size: 16, weight: .bold))
                        .foregroundColor(AppColors.textPrimary)

                    Text("Tap a day to inspect details")
                        .font(.system(size: 12, weight: .medium))
                        .foregroundColor(AppColors.textSecondary)
                }

                Spacer()

                VStack(alignment: .trailing, spacing: 2) {
                    Text("Daily Avg")
                        .font(.system(size: 11, weight: .medium))
                        .foregroundColor(AppColors.textSecondary)
                    Text("\(averageDailySteps.formatted())")
                        .font(.system(size: 15, weight: .bold))
                        .foregroundColor(Color(hex: 0xFF2563EB))
                }
            }

            // The 7 Bars
            VStack(spacing: 8) {
                // Goal reference line
                HStack(spacing: 8) {
                    Rectangle()
                        .fill(Color(hex: 0xFFE2E8F0))
                        .frame(height: 1)
                    Text("Goal \(dailyGoal / 1000)k")
                        .font(.system(size: 10, weight: .semibold))
                        .foregroundColor(Color(hex: 0xFF94A3B8))
                }

                HStack(alignment: .bottom, spacing: 12) {
                    ForEach(Array(weeklySteps.enumerated()), id: \.element.id) { index, item in
                        let isSelected = (index == selectedDayIndex)
                        let ratio = min(1.0, Double(item.steps) / Double(max(1, maxStepsInWeek)))
                        let barHeight = max(18.0, 150.0 * ratio)

                        Button(action: {
                            withAnimation(.spring(response: 0.35, dampingFraction: 0.75)) {
                                selectedDayIndex = index
                            }
                        }) {
                            VStack(spacing: 8) {
                                // Step count tag above bar if selected
                                if isSelected {
                                    Text(item.steps >= 1000 ? "\(item.steps / 1000)k" : "\(item.steps)")
                                        .font(.system(size: 10, weight: .bold))
                                        .foregroundColor(Color(hex: 0xFF2563EB))
                                        .padding(.horizontal, 6)
                                        .padding(.vertical, 2)
                                        .background(Color(hex: 0xFFEFF6FF))
                                        .clipShape(Capsule())
                                        .transition(.scale.combined(with: .opacity))
                                } else {
                                    Spacer()
                                        .frame(height: 16)
                                }

                                // Bar
                                ZStack(alignment: .bottom) {
                                    // Background Track
                                    Capsule()
                                        .fill(Color(hex: 0xFFF1F5F9))
                                        .frame(width: 32, height: 150)

                                    // Active Fill
                                    Capsule()
                                        .fill(
                                            isSelected
                                                ? LinearGradient(
                                                    colors: [Color(hex: 0xFF3B82F6), Color(hex: 0xFF1D4ED8)],
                                                    startPoint: .top,
                                                    endPoint: .bottom
                                                )
                                                : LinearGradient(
                                                    colors: item.steps >= dailyGoal
                                                        ? [Color(hex: 0xFF93C5FD), Color(hex: 0xFF60A5FA)]
                                                        : [Color(hex: 0xFFCBD5E1), Color(hex: 0xFF94A3B8)],
                                                    startPoint: .top,
                                                    endPoint: .bottom
                                                )
                                        )
                                        .frame(width: 32, height: CGFloat(barHeight))
                                }

                                // Day label (Mon, Tue, etc.)
                                VStack(spacing: 2) {
                                    Text(item.dayName)
                                        .font(.system(size: 12, weight: isSelected ? .bold : .medium))
                                        .foregroundColor(isSelected ? Color(hex: 0xFF1D4ED8) : AppColors.textSecondary)

                                    Text(item.dayNumber)
                                        .font(.system(size: 10, weight: .semibold))
                                        .foregroundColor(isSelected ? Color(hex: 0xFF1D4ED8) : AppColors.textTertiary)
                                }
                            }
                        }
                        .buttonStyle(PlainButtonStyle())
                        .frame(maxWidth: .infinity)
                    }
                }
                .frame(height: 200)
            }
        }
        .padding(20)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 24))
        .shadow(color: Color.black.opacity(0.03), radius: 6, y: 3)
    }

    // MARK: - Quick Stats Row
    private func quickStatsRow(_ item: DailyStepItem) -> some View {
        let distanceKm = Double(item.steps) * 0.00076
        let caloriesBurned = Double(item.steps) * 0.04
        let activeMinutes = item.steps / 100

        return HStack(spacing: 12) {
            statMetricCard(
                title: "Distance",
                value: String(format: "%.1f", distanceKm),
                unit: "km",
                icon: "figure.walk",
                tint: Color(hex: 0xFF3B82F6)
            )

            statMetricCard(
                title: "Burned",
                value: "\(Int(caloriesBurned))",
                unit: "kcal",
                icon: "flame.fill",
                tint: Color(hex: 0xFFF97316)
            )

            statMetricCard(
                title: "Active Time",
                value: "\(activeMinutes)",
                unit: "mins",
                icon: "clock.fill",
                tint: Color(hex: 0xFF10B981)
            )
        }
    }

    private func statMetricCard(title: String, value: String, unit: String, icon: String, tint: Color) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Image(systemName: icon)
                    .font(.system(size: 13, weight: .bold))
                    .foregroundColor(tint)
                Spacer()
            }

            VStack(alignment: .leading, spacing: 2) {
                HStack(alignment: .firstTextBaseline, spacing: 3) {
                    Text(value)
                        .font(.system(size: 18, weight: .bold, design: .rounded))
                        .foregroundColor(AppColors.textPrimary)
                    Text(unit)
                        .font(.system(size: 11, weight: .medium))
                        .foregroundColor(AppColors.textSecondary)
                }

                Text(title)
                    .font(.system(size: 11, weight: .medium))
                    .foregroundColor(AppColors.textSecondary)
            }
        }
        .padding(14)
        .frame(maxWidth: .infinity)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 20))
        .shadow(color: Color.black.opacity(0.03), radius: 5, y: 2)
    }

    // MARK: - 7-Day History List
    private var dailyBreakdownListCard: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("Day by Day History")
                .font(.system(size: 16, weight: .bold))
                .foregroundColor(AppColors.textPrimary)

            VStack(spacing: 10) {
                ForEach(Array(weeklySteps.enumerated().reversed()), id: \.element.id) { index, item in
                    let isSelected = (index == selectedDayIndex)
                    let isGoalMet = item.steps >= dailyGoal

                    Button(action: {
                        withAnimation { selectedDayIndex = index }
                    }) {
                        HStack(spacing: 14) {
                            // Date label
                            VStack(alignment: .leading, spacing: 2) {
                                Text(item.isToday ? "Today" : item.dayName)
                                    .font(.system(size: 14, weight: .bold))
                                    .foregroundColor(isSelected ? Color(hex: 0xFF2563EB) : AppColors.textPrimary)

                                Text(formattedDateShort(item.date))
                                    .font(.system(size: 11, weight: .medium))
                                    .foregroundColor(AppColors.textSecondary)
                            }
                            .frame(width: 80, alignment: .leading)

                            Spacer()

                            // Steps count
                            Text("\(item.steps.formatted())")
                                .font(.system(size: 16, weight: .bold, design: .rounded))
                                .foregroundColor(AppColors.textPrimary)

                            Text("steps")
                                .font(.system(size: 12, weight: .medium))
                                .foregroundColor(AppColors.textSecondary)

                            // Status Icon
                            Image(systemName: isGoalMet ? "checkmark.circle.fill" : "circle")
                                .font(.system(size: 16, weight: .bold))
                                .foregroundColor(isGoalMet ? Color(hex: 0xFF10B981) : Color(hex: 0xFFCBD5E1))
                        }
                        .padding(.vertical, 10)
                        .padding(.horizontal, 14)
                        .background(isSelected ? Color(hex: 0xFFEFF6FF) : Color(hex: 0xFFF8FAFC))
                        .clipShape(RoundedRectangle(cornerRadius: 14))
                    }
                    .buttonStyle(PlainButtonStyle())
                }
            }
        }
        .padding(20)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 24))
        .shadow(color: Color.black.opacity(0.03), radius: 6, y: 3)
    }

    // MARK: - Data Fetching
    private func load7DaysData() {
        Task {
            let calendar = Calendar.current
            var items: [DailyStepItem] = []

            for offset in (0..<7).reversed() {
                guard let targetDate = calendar.date(byAdding: .day, value: -offset, to: date) else { continue }
                let steps = await healthKit.fetchSteps(for: targetDate)
                let weekdayIndex = calendar.component(.weekday, from: targetDate) - 1
                let dayName = calendar.shortWeekdaySymbols[weekdayIndex]
                let dayNumber = "\(calendar.component(.day, from: targetDate))"
                let isToday = calendar.isDateInToday(targetDate)

                items.append(DailyStepItem(
                    date: targetDate,
                    dayName: dayName,
                    dayNumber: dayNumber,
                    steps: steps,
                    isToday: isToday
                ))
            }

            await MainActor.run {
                self.weeklySteps = items
                self.selectedDayIndex = max(0, items.count - 1)
                self.isLoading = false
            }
        }
    }

    private func formattedDateString(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "EEEE, MMM d"
        return formatter.string(from: date)
    }

    private func formattedDateShort(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "MMM d"
        return formatter.string(from: date)
    }
}
