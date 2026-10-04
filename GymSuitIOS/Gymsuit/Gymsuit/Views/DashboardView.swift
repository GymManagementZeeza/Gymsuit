import SwiftUI

enum DashboardTab: String, CaseIterable {
    case plan = "Plan"
    case workouts = "Workouts"
    case home = "Home"
    case challenges = "Challenges"
    case settings = "Settings"
    
    var iconName: String {
        switch self {
        case .plan: return "calendar"
        case .workouts: return "dumbbell.fill"
        case .home: return "house.fill"
        case .challenges: return "person.3.fill"
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
    @ObservedObject private var syncManager = HealthSyncManager.shared
    @State private var selectedTab: DashboardTab = .home
    @State private var days: [DayItemModel] = []
    // Hoisted here so an active guided session survives tab switches and locks navigation.
    @StateObject private var workoutSession = WorkoutSessionState()
    
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
                    
                    HStack(spacing: 8) {
                        PointsPill()
                        
                        // Cloud Sync Button
                        Button(action: {
                            guard !syncManager.isSyncing else { return }
                            Task {
                                let success = await syncManager.sync()
                                if success {
                                    viewModel.refreshMetrics(for: viewModel.selectedDate)
                                }
                            }
                        }) {
                            Image(systemName: syncManager.isSyncing ? "arrow.triangle.2.circlepath" : "icloud.and.arrow.up")
                                .font(.system(size: 15, weight: .semibold))
                                .foregroundColor(Color(hex: 0xFF16A34A))
                                .frame(width: 38, height: 38)
                                .background(Color(hex: 0xFFF0FDF4))
                                .clipShape(Circle())
                                .overlay(Circle().stroke(Color(hex: 0xFFBBF7D0), lineWidth: 1))
                                .rotationEffect(.degrees(syncManager.isSyncing ? 360 : 0))
                                .animation(syncManager.isSyncing ? Animation.linear(duration: 1).repeatForever(autoreverses: false) : .default, value: syncManager.isSyncing)
                        }
                        .disabled(syncManager.isSyncing)
                        
                        Button(action: {
                            guard !workoutSession.active else { return }
                            selectedTab = .settings
                        }) {
                            Image("avatar_gaze_1")
                                .resizable()
                                .scaledToFill()
                                .frame(width: 44, height: 44)
                                .clipShape(Circle())
                                .overlay(Circle().stroke(Color.white, lineWidth: 2))
                                .shadow(color: Color.black.opacity(0.08), radius: 4, y: 2)
                        }
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
                        viewModel.onDateSelected(viewModel.selectedDate)
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
                } else if selectedTab == .workouts {
                    WorkoutsView()
                        .environmentObject(workoutSession)
                } else if selectedTab == .challenges {
                    ChallengesView()
                } else if selectedTab == .settings {
                    SettingsView(onBack: { selectedTab = .home }, onLogout: onLogout)
                } else {
                    placeholderContent(for: selectedTab)
                }
            }
            .padding(.bottom, 74) // Space for bottom bar
            .onChange(of: syncManager.syncStatus) { _, status in
                if case .success = status {
                    viewModel.refreshMetrics(for: viewModel.selectedDate)
                }
            }
            .onChange(of: workoutSession.active) { _, active in
                // While a guided session runs, lock the user on the Workouts tab.
                if active {
                    selectedTab = .workouts
                }
            }
            
            // Floating Bottom Tab Bar — hidden while a session runs: it must be
            // ended from the Workouts tab before navigating away.
            if !workoutSession.active {
                customTabBar
            }
        }
    }
    
    // MARK: - Home Tab Content
    private var homeContent: some View {
        ScrollView {
            VStack(spacing: 20) {
                // AI Summary Insight Banner Card
                aiSummaryCard
                
                // Workout Card — tap opens the Workouts tab
                WorkoutCardWidget(onOpenWorkouts: { selectedTab = .workouts })
                
                // Key Biometrics Grid Cards
                LazyVGrid(columns: [GridItem(.flexible(), spacing: 14), GridItem(.flexible(), spacing: 14)], spacing: 14) {
                    // Calories Card
                    Button(action: { onNavigateToCaloriesDetail(viewModel.selectedDate) }) {
                        MetricDashboardCard(
                            title: "Calories",
                            value: viewModel.isLoadingMetrics ? "…" : "\(Int(viewModel.caloriesBreakdown.totalKcal))",
                            unit: "kcal burned",
                            subtitle: "Steps \(Int(viewModel.caloriesBreakdown.stepsKcal)) • Workouts \(Int(viewModel.caloriesBreakdown.workoutKcal))",
                            icon: "flame.fill",
                            tint: AppColors.calories
                        )
                    }
                    
                    // Steps Card
                    MetricDashboardCard(
                        title: "Steps",
                        value: viewModel.isLoadingMetrics ? "…" : "\(viewModel.stepsCount.formatted())",
                        unit: "steps",
                        subtitle: "Goal: 10,000 steps",
                        icon: "figure.walk",
                        tint: AppColors.steps
                    )
                    
                    // Sleep Card
                    Button(action: { onNavigateToSleepDetail(viewModel.selectedDate) }) {
                        MetricDashboardCard(
                            title: "Sleep",
                            value: viewModel.sleepSession?.durationFormatted ?? (viewModel.isLoadingMetrics ? "…" : "—"),
                            unit: "",
                            subtitle: {
                                if let s = viewModel.sleepSession {
                                    return "\(s.startTimeFormatted) - \(s.endTimeFormatted)"
                                } else {
                                    return "No data yet"
                                }
                            }(),
                            icon: "bed.double.fill",
                            tint: AppColors.sleep
                        )
                    }
                    
                    // Heart Rate Card
                    Button(action: { onNavigateToHeartRateDetail(viewModel.selectedDate) }) {
                        MetricDashboardCard(
                            title: "Heart Rate",
                            value: {
                                if let h = viewModel.heartRateSummary, h.hasData {
                                    return "\(h.latestBpm)"
                                } else if viewModel.isLoadingMetrics {
                                    return "…"
                                } else {
                                    return "—"
                                }
                            }(),
                            unit: "bpm",
                            subtitle: {
                                if let h = viewModel.heartRateSummary, h.hasData {
                                    let range = "\(h.minBpm) min • \(h.maxBpm) max"
                                    return h.relativeTime.isEmpty ? range : "\(h.relativeTime) • \(range)"
                                } else {
                                    return "No data yet"
                                }
                            }(),
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
                    // Locked while a guided session is active — it must be ended first.
                    guard !workoutSession.active else { return }
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
            Text("GymSuit workout plans and schedules module.")
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

// MARK: - Workout Card Widget (ported from Android WorkoutCardWidget)
//
// Green gradient card that turns white once any workout is done today.
// Dots reflect real data only: app-logged workouts ∪ HealthKit sessions.
// Tapping opens the Workouts tab.
private struct WorkoutCardWidget: View {
    var onOpenWorkouts: () -> Void
    
    @ObservedObject private var workoutStore = WorkoutStore.shared
    @State private var hkWorkoutDays: Set<Date> = []
    
    private var calendar: Calendar { Calendar.current }
    private var today: Date { calendar.startOfDay(for: Date()) }
    private var darkGreen: Color { Color(hex: 0xFF065F46) }
    private var deepGreen: Color { Color(hex: 0xFF064E3B) }
    
    private var monthDays: [Date] {
        let comps = calendar.dateComponents([.year, .month], from: Date())
        guard let monthStart = calendar.date(from: comps),
              let range = calendar.range(of: .day, in: .month, for: monthStart) else { return [] }
        return range.compactMap { calendar.date(byAdding: .day, value: $0 - 1, to: monthStart) }
    }
    
    private var loggedDays: Set<Date> {
        Set(workoutStore.loggedWorkouts.map { calendar.startOfDay(for: $0.date) })
    }
    
    private var allWorkoutDays: Set<Date> { loggedDays.union(hkWorkoutDays) }
    private var workedOutToday: Bool { allWorkoutDays.contains(today) }
    
    private var workoutsThisMonth: Int {
        monthDays.filter { allWorkoutDays.contains($0) }.count
    }
    
    private var monthName: String {
        let formatter = DateFormatter()
        formatter.dateFormat = "MMM"
        return formatter.string(from: Date())
    }
    
    var body: some View {
        Button(action: onOpenWorkouts) {
            VStack(alignment: .leading, spacing: 0) {
                // Header: icon, title & month tag
                HStack {
                    HStack(spacing: 6) {
                        Image(systemName: "dumbbell.fill")
                            .font(.system(size: 16))
                        Text("Workout")
                            .font(.system(size: 13, weight: .bold))
                    }
                    .foregroundColor(darkGreen)
                    Spacer()
                    Text(monthName)
                        .font(.system(size: 11, weight: .bold))
                        .foregroundColor(darkGreen)
                        .padding(.horizontal, 7)
                        .padding(.vertical, 2)
                        .background(darkGreen.opacity(0.12))
                        .clipShape(RoundedRectangle(cornerRadius: 8))
                }
                
                // Big count: completed days this month
                HStack(alignment: .lastTextBaseline, spacing: 4) {
                    Text("\(workoutsThisMonth)")
                        .font(.system(size: 26, weight: .heavy))
                        .foregroundColor(deepGreen)
                    Text("/\(monthDays.count) days")
                        .font(.system(size: 13, weight: .bold))
                        .foregroundColor(darkGreen.opacity(0.75))
                }
                .padding(.top, 10)
                
                // Dot grid for the days of the month
                dotGrid
                    .padding(.top, 14)
            }
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(
                Group {
                    if workedOutToday {
                        Color.white
                    } else {
                        LinearGradient(
                            colors: [Color(hex: 0xFFDCFCE7), Color(hex: 0xFFA7F3D0)],
                            startPoint: .top,
                            endPoint: .bottom
                        )
                    }
                }
            )
            .clipShape(RoundedRectangle(cornerRadius: 22))
            .shadow(color: Color.black.opacity(0.04), radius: 6, y: 3)
        }
        .task { await loadHKWorkoutDays() }
        .onChange(of: workoutStore.loggedWorkouts.count) { _, _ in
            Task { await loadHKWorkoutDays() }
        }
    }
    
    private var dotGrid: some View {
        let columns = 6
        let rows = (monthDays.count + columns - 1) / columns
        return VStack(alignment: .leading, spacing: 6) {
            ForEach(0..<rows, id: \.self) { row in
                HStack(spacing: 6) {
                    ForEach(0..<columns, id: \.self) { col in
                        let idx = row * columns + col
                        if idx < monthDays.count {
                            let date = monthDays[idx]
                            let didWorkout = allWorkoutDays.contains(date)
                            let isToday = calendar.isDate(date, inSameDayAs: today)
                            let isFuture = date > today
                            Circle()
                                .fill(dotFill(didWorkout: didWorkout, isToday: isToday, isFuture: isFuture))
                                .frame(width: 14, height: 14)
                                .overlay(
                                    Group {
                                        if isToday {
                                            Circle().stroke(todayBorder(didWorkout: didWorkout), lineWidth: 2)
                                        }
                                    }
                                )
                        } else {
                            Spacer().frame(width: 14, height: 14)
                        }
                    }
                }
            }
        }
    }
    
    private func dotFill(didWorkout: Bool, isToday: Bool, isFuture: Bool) -> Color {
        if didWorkout && workedOutToday { return Color(hex: 0xFF10B981) }
        if didWorkout { return .white }
        if isToday { return Color(hex: 0xFF059669).opacity(0.6) }
        if isFuture { return Color(hex: 0xFF059669).opacity(0.15) }
        return Color(hex: 0xFF059669).opacity(0.35)
    }
    
    private func todayBorder(didWorkout: Bool) -> Color {
        (didWorkout || workedOutToday) ? darkGreen : .white
    }
    
    private func loadHKWorkoutDays() async {
        let manager = HealthKitManager.shared
        let days = monthDays.filter { $0 <= today }
        var found = Set<Date>()
        await withTaskGroup(of: Date?.self) { group in
            for day in days {
                group.addTask {
                    let sessions = await manager.fetchWorkouts(for: day)
                    return sessions.isEmpty ? nil : day
                }
            }
            for await day in group {
                if let day { found.insert(day) }
            }
        }
        hkWorkoutDays = found
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
