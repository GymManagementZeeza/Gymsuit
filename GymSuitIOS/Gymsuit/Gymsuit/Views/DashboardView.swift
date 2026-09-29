import SwiftUI

enum DashboardTab: String, CaseIterable {
    case plan = "Plan"
    case workouts = "Workouts"
    case home = "Home"
    case analytics = "Analytics"
    case settings = "Settings"
    
    var iconName: String {
        switch self {
        case .plan: return "calendar"
        case .workouts: return "dumbbell.fill"
        case .home: return "house.fill"
        case .analytics: return "chart.bar.xaxis"
        case .settings: return "gearshape.fill"
        }
    }
}

struct DayItemModel: Identifiable, Equatable {
    let id = UUID()
    let dayName: String
    let dayNumber: String
    let date: Date
}

struct DashboardView: View {
    var onLogout: () -> Void
    var onNavigateToSleepDetail: (Date) -> Void
    var onNavigateToHeartRateDetail: (Date) -> Void
    var onNavigateToCaloriesDetail: (Date) -> Void
    
    @StateObject private var viewModel = DashboardViewModel()
    @ObservedObject private var authManager = AuthManager.shared
    @State private var selectedTab: DashboardTab = .home
    @State private var days: [DayItemModel] = []
    
    var body: some View {
        ZStack(alignment: .bottom) {
            Color(hex: 0xFFF8F9FD).ignoresSafeArea()
            
            VStack(spacing: 0) {
                // Top Custom Header
                HStack {
                    VStack(alignment: .leading, spacing: 3) {
                        Text(greetingText)
                            .font(.system(size: 13, weight: .medium))
                            .foregroundColor(AppColors.textSecondary)
                        Text(authManager.currentSession?.firstName.isEmpty == false ? authManager.currentSession!.firstName : "Athlete")
                            .font(.system(size: 22, weight: .bold))
                            .foregroundColor(AppColors.textPrimary)
                    }
                    
                    Spacer()
                    
                    Button(action: { selectedTab = .settings }) {
                        Image("avatar_gaze_1")
                            .resizable()
                            .scaledToFill()
                            .frame(width: 44, height: 44)
                            .clipShape(Circle())
                            .overlay(Circle().stroke(Color.white, lineWidth: 2))
                            .shadow(color: Color.black.opacity(0.08), radius: 4, y: 2)
                    }
                }
                .padding(.horizontal, 20)
                .padding(.top, 10)
                .padding(.bottom, 12)
                
                // 30-Day Horizontal Scroll Calendar Strip
                ScrollViewReader { proxy in
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 10) {
                            ForEach(days) { day in
                                let isSelected = Calendar.current.isDate(day.date, inSameDayAs: viewModel.selectedDate)
                                Button(action: {
                                    withAnimation(.spring()) {
                                        viewModel.onDateSelected(day.date)
                                    }
                                }) {
                                    VStack(spacing: 6) {
                                        Text(day.dayName)
                                            .font(.system(size: 11, weight: .medium))
                                            .foregroundColor(isSelected ? .white : AppColors.textSecondary)
                                        Text(day.dayNumber)
                                            .font(.system(size: 16, weight: .bold))
                                            .foregroundColor(isSelected ? .white : AppColors.textPrimary)
                                    }
                                    .frame(width: 48, height: 68)
                                    .background(isSelected ? AppColors.primary : Color.white)
                                    .clipShape(RoundedRectangle(cornerRadius: 18))
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 18)
                                            .stroke(isSelected ? Color.clear : AppColors.border, lineWidth: 1)
                                    )
                                    .shadow(color: isSelected ? AppColors.primary.opacity(0.3) : Color.clear, radius: 6, y: 3)
                                }
                                .id(day.id)
                            }
                        }
                        .padding(.horizontal, 20)
                        .padding(.vertical, 6)
                    }
                    .onAppear {
                        generateDays()
                        if let lastDay = days.last {
                            DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
                                proxy.scrollTo(lastDay.id, anchor: .trailing)
                            }
                        }
                    }
                }
                
                // Main Content by Selected Tab
                if selectedTab == .home {
                    homeContent
                } else if selectedTab == .settings {
                    SettingsView(onBack: { selectedTab = .home }, onLogout: onLogout)
                } else {
                    placeholderContent(for: selectedTab)
                }
            }
            .padding(.bottom, 74) // Space for bottom bar
            
            // Floating Bottom Tab Bar
            customTabBar
        }
    }
    
    // MARK: - Home Tab Content
    private var homeContent: some View {
        ScrollView {
            VStack(spacing: 20) {
                // AI Summary Insight Banner Card
                aiSummaryCard
                
                // Key Biometrics Grid Cards
                LazyVGrid(columns: [GridItem(.flexible(), spacing: 14), GridItem(.flexible(), spacing: 14)], spacing: 14) {
                    // Calories Card
                    Button(action: { onNavigateToCaloriesDetail(viewModel.selectedDate) }) {
                        MetricDashboardCard(
                            title: "Calories",
                            value: "\(Int(viewModel.caloriesBreakdown.totalKcal))",
                            unit: "kcal",
                            subtitle: "\(Int(viewModel.caloriesBreakdown.stepsKcal)) walk • \(Int(viewModel.caloriesBreakdown.workoutKcal)) workout",
                            icon: "flame.fill",
                            tint: AppColors.calories
                        )
                    }
                    
                    // Steps Card
                    MetricDashboardCard(
                        title: "Steps",
                        value: "\(viewModel.stepsCount.formatted())",
                        unit: "steps",
                        subtitle: "Goal: 10,000 steps",
                        icon: "figure.walk",
                        tint: AppColors.steps
                    )
                    
                    // Sleep Card
                    Button(action: { onNavigateToSleepDetail(viewModel.selectedDate) }) {
                        MetricDashboardCard(
                            title: "Sleep",
                            value: viewModel.sleepSession?.durationFormatted ?? "8h 05m",
                            unit: "",
                            subtitle: "\(viewModel.sleepSession?.startTimeFormatted ?? "11:15 PM") - \(viewModel.sleepSession?.endTimeFormatted ?? "7:20 AM")",
                            icon: "bed.double.fill",
                            tint: AppColors.sleep
                        )
                    }
                    
                    // Heart Rate Card
                    Button(action: { onNavigateToHeartRateDetail(viewModel.selectedDate) }) {
                        MetricDashboardCard(
                            title: "Heart Rate",
                            value: "\(viewModel.heartRateSummary?.latestBpm ?? 72)",
                            unit: "bpm",
                            subtitle: "\(viewModel.heartRateSummary?.minBpm ?? 58) min • \(viewModel.heartRateSummary?.maxBpm ?? 135) max",
                            icon: "heart.fill",
                            tint: AppColors.heartRate
                        )
                    }
                }
                
                // Featured Gym Workout Section
                VStack(alignment: .leading, spacing: 12) {
                    HStack {
                        Text("Today's Recommended Session")
                            .font(.system(size: 17, weight: .bold))
                            .foregroundColor(AppColors.textPrimary)
                        Spacer()
                    }
                    
                    ZStack(alignment: .bottomLeading) {
                        Image("fitness_workout")
                            .resizable()
                            .scaledToFill()
                            .frame(maxWidth: .infinity)
                            .frame(height: 180)
                            .clipped()
                        
                        LinearGradient(
                            colors: [Color.clear, Color.black.opacity(0.75)],
                            startPoint: .center,
                            endPoint: .bottom
                        )
                        
                        VStack(alignment: .leading, spacing: 4) {
                            Text("Full Body Hypertrophy")
                                .font(.system(size: 18, weight: .bold))
                                .foregroundColor(.white)
                            Text("45 min • 380 kcal • Intermediate")
                                .font(.system(size: 13, weight: .medium))
                                .foregroundColor(Color.white.opacity(0.85))
                        }
                        .padding(18)
                    }
                    .clipShape(RoundedRectangle(cornerRadius: 24))
                }
            }
            .padding(.horizontal, 20)
            .padding(.top, 10)
            .padding(.bottom, 20)
        }
    }
    
    // MARK: - AI Summary Card
    private var aiSummaryCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                HStack(spacing: 6) {
                    Image(systemName: "sparkles")
                        .foregroundColor(AppColors.purple)
                    Text("AI WELLNESS COACH")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundColor(AppColors.purple)
                }
                .padding(.horizontal, 10)
                .padding(.vertical, 5)
                .background(AppColors.purpleLight)
                .clipShape(Capsule())
                
                Spacer()
                
                Button(action: { viewModel.retryCurrentDate() }) {
                    Image(systemName: "arrow.clockwise")
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundColor(AppColors.textSecondary)
                }
            }
            
            switch viewModel.summaryState {
            case .idle, .loading:
                HStack(spacing: 12) {
                    ProgressView()
                    Text("Analyzing your biometrics and daily activity...")
                        .font(.system(size: 14))
                        .foregroundColor(AppColors.textSecondary)
                }
                .padding(.vertical, 8)
                
            case .success(let text):
                Text(text)
                    .font(.system(size: 14, weight: .regular))
                    .foregroundColor(AppColors.textPrimary)
                    .lineSpacing(4)
                
            case .error(let err):
                Text(err)
                    .font(.system(size: 13))
                    .foregroundColor(AppColors.textSecondary)
            }
        }
        .padding(18)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 24))
        .shadow(color: Color.black.opacity(0.04), radius: 6, y: 3)
    }
    
    // MARK: - Tab Bar
    private var customTabBar: some View {
        HStack {
            ForEach(DashboardTab.allCases, id: \.self) { tab in
                Spacer()
                Button(action: {
                    withAnimation(.spring(response: 0.3, dampingFraction: 0.7)) {
                        selectedTab = tab
                    }
                }) {
                    VStack(spacing: 4) {
                        Image(systemName: tab.iconName)
                            .font(.system(size: selectedTab == tab ? 20 : 18, weight: selectedTab == tab ? .bold : .medium))
                            .foregroundColor(selectedTab == tab ? AppColors.primary : AppColors.textTertiary)
                        
                        Text(tab.rawValue)
                            .font(.system(size: 10, weight: selectedTab == tab ? .bold : .medium))
                            .foregroundColor(selectedTab == tab ? AppColors.primary : AppColors.textTertiary)
                    }
                    .frame(height: 48)
                }
                Spacer()
            }
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 8)
        .background(
            Color.white
                .clipShape(RoundedRectangle(cornerRadius: 30))
                .shadow(color: Color.black.opacity(0.08), radius: 10, y: 4)
        )
        .padding(.horizontal, 16)
        .padding(.bottom, 12)
    }
    
    private func placeholderContent(for tab: DashboardTab) -> some View {
        VStack(spacing: 12) {
            Spacer()
            Image(systemName: tab.iconName)
                .font(.system(size: 48))
                .foregroundColor(AppColors.primary.opacity(0.4))
            Text("\(tab.rawValue) Coming Soon")
                .font(.system(size: 18, weight: .bold))
                .foregroundColor(AppColors.textPrimary)
            Text("GymSuit workout plans, schedules, and analytics module.")
                .font(.system(size: 14))
                .foregroundColor(AppColors.textSecondary)
            Spacer()
        }
    }
    
    private var greetingText: String {
        let hour = Calendar.current.component(.hour, from: Date())
        if hour < 12 { return "Good Morning," }
        if hour < 17 { return "Good Afternoon," }
        return "Good Evening,"
    }
    
    private func generateDays() {
        let calendar = Calendar.current
        let today = Date()
        let formatter = DateFormatter()
        formatter.dateFormat = "EEE"
        let dayNumFormatter = DateFormatter()
        dayNumFormatter.dateFormat = "d"
        
        var list: [DayItemModel] = []
        for i in (0..<30).reversed() {
            if let date = calendar.date(byAdding: .day, value: -i, to: today) {
                list.append(DayItemModel(
                    dayName: formatter.string(from: date),
                    dayNumber: dayNumFormatter.string(from: date),
                    date: date
                ))
            }
        }
        self.days = list
    }
}

private struct MetricDashboardCard: View {
    let title: String
    let value: String
    let unit: String
    let subtitle: String
    let icon: String
    let tint: Color
    
    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                ZStack {
                    Circle()
                        .fill(tint.opacity(0.12))
                        .frame(width: 36, height: 36)
                    Image(systemName: icon)
                        .font(.system(size: 16))
                        .foregroundColor(tint)
                }
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.system(size: 12, weight: .bold))
                    .foregroundColor(AppColors.textTertiary)
            }
            
            VStack(alignment: .leading, spacing: 2) {
                HStack(alignment: .firstTextBaseline, spacing: 4) {
                    Text(value)
                        .font(.system(size: 22, weight: .bold))
                        .foregroundColor(AppColors.textPrimary)
                    if !unit.isEmpty {
                        Text(unit)
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundColor(AppColors.textSecondary)
                    }
                }
                
                Text(title)
                    .font(.system(size: 14, weight: .medium))
                    .foregroundColor(AppColors.textPrimary)
                
                Text(subtitle)
                    .font(.system(size: 11))
                    .foregroundColor(AppColors.textSecondary)
                    .lineLimit(1)
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 22))
        .shadow(color: Color.black.opacity(0.03), radius: 6, y: 3)
    }
}
