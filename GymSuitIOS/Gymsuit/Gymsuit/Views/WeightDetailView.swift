import SwiftUI

/// Weight Statistics & Progress Graph Detail Screen
/// Shows current weight, target weight trajectory, projection graph, and recent logged history.
struct WeightDetailView: View {
    let date: Date
    var onBack: () -> Void

    @AppStorage("workout_user_weight_kg") private var currentWeightKg: Double = 74.5
    @AppStorage("workout_user_target_weight_kg") private var targetWeightKg: Double = 70.0
    @AppStorage("workout_user_height_cm") private var userHeightCm: Double = 172.72
    @AppStorage("workout_selected_goal") private var selectedGoalRaw: String = "lose_weight"
    @AppStorage("workout_user_birth_year") private var userBirthYear: Int = 2003
    @AppStorage("workout_user_days_per_week") private var userWorkoutDaysPerWeek: Int = 2
    @AppStorage("weight_unit") private var selectedUnit: String = "kg"

    @State private var isShowingRecordSheet: Bool = false
    @State private var selectedTab: Int = 0 // 0: Projection, 1: History
    @State private var historySamples: [(date: Date, kg: Double)] = []
    @State private var isLoadingHistory: Bool = true
    @State private var backendTargetDate: Date? = nil

    private var isLbs: Bool {
        selectedUnit.lowercased() == "lbs"
    }

    private var unitLabel: String {
        isLbs ? "lbs" : "kg"
    }

    private var displayCurrentWeight: Double {
        isLbs ? (currentWeightKg * 2.20462) : currentWeightKg
    }

    private var displayTargetWeight: Double {
        isLbs ? (targetWeightKg * 2.20462) : targetWeightKg
    }

    private var diffKg: Double {
        abs(currentWeightKg - targetWeightKg)
    }

    private var diffDisplay: Double {
        isLbs ? (diffKg * 2.20462) : diffKg
    }

    private var isGaining: Bool {
        targetWeightKg > currentWeightKg
    }

    private var projectionDays: Int {
        let days = Int(ceil(max(diffKg, 0.5) * 7.0))
        return max(days, 14)
    }

    private var targetDate: Date {
        if let backendDate = backendTargetDate {
            return backendDate
        }
        return Calendar.current.date(byAdding: .day, value: projectionDays, to: Date()) ?? Date()
    }

    private static let monthDayFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "MMM d, yyyy"
        return f
    }()

    private static let shortDateFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "MMM d"
        return f
    }()

    private static let isoDateFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "yyyy-MM-dd"
        f.locale = Locale(identifier: "en_US_POSIX")
        f.timeZone = TimeZone(secondsFromGMT: 0)
        return f
    }()

    var body: some View {
        ZStack {
            Color(hex: 0xFFF3F2F8).ignoresSafeArea()

            VStack(spacing: 0) {
                // Top App Bar
                topAppBar

                ScrollView(showsIndicators: false) {
                    VStack(spacing: 18) {
                        // Main Summary Card
                        heroWeightCard

                        // Graph Card (Projection or History)
                        graphCard

                        // Metrics Grid
                        metricsGrid

                        // Bottom Action: Record Weight
                        Button(action: { isShowingRecordSheet = true }) {
                            HStack(spacing: 8) {
                                Image(systemName: "plus.circle.fill")
                                    .font(.system(size: 16, weight: .semibold))
                                Text("Record Weight")
                                    .font(.system(size: 16, weight: .bold))
                            }
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .frame(height: 52)
                            .background(AppColors.primary)
                            .clipShape(RoundedRectangle(cornerRadius: 16))
                            .shadow(color: AppColors.primary.opacity(0.3), radius: 6, y: 3)
                        }
                        .padding(.top, 4)
                        .padding(.bottom, 30)
                    }
                    .padding(.horizontal, 20)
                    .padding(.top, 12)
                }
            }
        }
        .sheet(isPresented: $isShowingRecordSheet) {
            RecordWeightSheet(
                currentWeightKg: $currentWeightKg,
                onWeightSaved: { newWeight in
                    syncWeight(newWeight: newWeight)
                    Task { await loadHistory() }
                }
            )
        }
        .task {
            await loadBackendData()
            await loadHistory()
        }
    }

    // MARK: - Top App Bar

    private var topAppBar: some View {
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

            Text("Weight Statistics")
                .font(.system(size: 18, weight: .bold))
                .foregroundColor(AppColors.textPrimary)

            Spacer()

            Button(action: { isShowingRecordSheet = true }) {
                Image(systemName: "plus")
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
    }

    // MARK: - Hero Weight Card

    private var heroWeightCard: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack {
                HStack(spacing: 8) {
                    ZStack {
                        Circle()
                            .fill(AppColors.primary.opacity(0.12))
                            .frame(width: 38, height: 38)
                        Image(systemName: "scalemass.fill")
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundColor(AppColors.primary)
                    }

                    Text("CURRENT WEIGHT")
                        .font(.system(size: 12, weight: .bold))
                        .foregroundColor(AppColors.primary)
                }

                Spacer()

                // Goal badge
                Text(isGaining ? "Build Muscle" : "Weight Loss")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundColor(Color(hex: 0xFF059669))
                    .padding(.horizontal, 10)
                    .padding(.vertical, 4)
                    .background(Color(hex: 0xFFECFDF5))
                    .clipShape(Capsule())
            }

            HStack(alignment: .lastTextBaseline, spacing: 6) {
                Text(String(format: "%.1f", displayCurrentWeight))
                    .font(.system(size: 42, weight: .bold))
                    .foregroundColor(AppColors.textPrimary)
                Text(unitLabel)
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(AppColors.textSecondary)

                Spacer()

                if targetWeightKg > 0 {
                    VStack(alignment: .trailing, spacing: 2) {
                        Text("Target: \(String(format: "%.1f", displayTargetWeight)) \(unitLabel)")
                            .font(.system(size: 14, weight: .bold))
                            .foregroundColor(AppColors.textPrimary)

                        if abs(currentWeightKg - targetWeightKg) < 0.1 {
                            Text("Goal reached! 🎉")
                                .font(.system(size: 12, weight: .semibold))
                                .foregroundColor(Color(hex: 0xFF10B981))
                        } else {
                            Text(isGaining ? "+\(String(format: "%.1f", diffDisplay)) \(unitLabel) to go" : "-\(String(format: "%.1f", diffDisplay)) \(unitLabel) to go")
                                .font(.system(size: 12, weight: .semibold))
                                .foregroundColor(AppColors.primary)
                        }
                    }
                }
            }
        }
        .padding(20)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 24))
        .shadow(color: Color.black.opacity(0.04), radius: 6, y: 3)
    }

    // MARK: - Graph Card

    private var graphCard: some View {
        VStack(alignment: .leading, spacing: 16) {
            // Segmented Picker (Projection vs History)
            Picker("View", selection: $selectedTab) {
                Text("Projection Curve").tag(0)
                Text("Logged History").tag(1)
            }
            .pickerStyle(.segmented)

            if selectedTab == 0 {
                // S-Curve Trajectory towards Target Date
                VStack(alignment: .leading, spacing: 12) {
                    HStack {
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Goal Projection")
                                .font(.system(size: 15, weight: .bold))
                                .foregroundColor(AppColors.textPrimary)
                            Text("Projected reach on \(Self.monthDayFormatter.string(from: targetDate))")
                                .font(.system(size: 12))
                                .foregroundColor(AppColors.textSecondary)
                        }
                        Spacer()
                    }

                    // S-Curve Canvas
                    WeightProjectionCanvas(
                        currentWeightKg: currentWeightKg,
                        targetWeightKg: targetWeightKg,
                        targetDate: targetDate,
                        unit: unitLabel,
                        isLbs: isLbs
                    )
                    .frame(height: 200)
                }
            } else {
                // Logged History Line Chart
                VStack(alignment: .leading, spacing: 12) {
                    HStack {
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Weight History (30 Days)")
                                .font(.system(size: 15, weight: .bold))
                                .foregroundColor(AppColors.textPrimary)
                            Text("\(historySamples.count) logs recorded")
                                .font(.system(size: 12))
                                .foregroundColor(AppColors.textSecondary)
                        }
                        Spacer()
                    }

                    if isLoadingHistory {
                        HStack {
                            Spacer()
                            ProgressView()
                            Spacer()
                        }
                        .frame(height: 180)
                    } else if historySamples.isEmpty {
                        VStack(spacing: 8) {
                            Image(systemName: "chart.line.uptrend.xyaxis")
                                .font(.system(size: 32))
                                .foregroundColor(AppColors.textTertiary)
                            Text("No weight records in HealthKit yet")
                                .font(.system(size: 13, weight: .medium))
                                .foregroundColor(AppColors.textSecondary)
                            Text("Tap 'Record Weight' to add your first entry")
                                .font(.system(size: 11))
                                .foregroundColor(AppColors.textTertiary)
                        }
                        .frame(maxWidth: .infinity)
                        .frame(height: 180)
                    } else {
                        WeightHistoryCanvas(
                            samples: historySamples,
                            isLbs: isLbs,
                            unit: unitLabel
                        )
                        .frame(height: 200)
                    }
                }
            }
        }
        .padding(20)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 24))
        .shadow(color: Color.black.opacity(0.04), radius: 6, y: 3)
    }

    // MARK: - Key Metrics Grid

    private var metricsGrid: some View {
        LazyVGrid(columns: [GridItem(.flexible(), spacing: 12), GridItem(.flexible(), spacing: 12)], spacing: 12) {
            metricTile(
                title: "Starting Weight",
                value: String(format: "%.1f %@", displayCurrentWeight, unitLabel),
                subtitle: "Baseline measurement",
                icon: "arrow.down.right.circle.fill",
                tint: AppColors.primary
            )

            metricTile(
                title: "Target Weight",
                value: String(format: "%.1f %@", displayTargetWeight, unitLabel),
                subtitle: "Fitness goal",
                icon: "target",
                tint: Color(hex: 0xFF10B981)
            )

            metricTile(
                title: "Remaining",
                value: String(format: "%.1f %@", diffDisplay, unitLabel),
                subtitle: isGaining ? "To gain" : "To lose",
                icon: "flag.fill",
                tint: Color(hex: 0xFFF59E0B)
            )

            metricTile(
                title: "Est. Target Date",
                value: Self.shortDateFormatter.string(from: targetDate),
                subtitle: "Projection pace",
                icon: "calendar",
                tint: Color(hex: 0xFF8B5CF6)
            )
        }
    }

    private func metricTile(title: String, value: String, subtitle: String, icon: String, tint: Color) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                ZStack {
                    Circle()
                        .fill(tint.opacity(0.12))
                        .frame(width: 32, height: 32)
                    Image(systemName: icon)
                        .font(.system(size: 14))
                        .foregroundColor(tint)
                }
                Spacer()
            }

            VStack(alignment: .leading, spacing: 2) {
                Text(value)
                    .font(.system(size: 18, weight: .bold))
                    .foregroundColor(AppColors.textPrimary)
                Text(title)
                    .font(.system(size: 12, weight: .medium))
                    .foregroundColor(AppColors.textPrimary)
                Text(subtitle)
                    .font(.system(size: 10))
                    .foregroundColor(AppColors.textSecondary)
            }
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 18))
        .shadow(color: Color.black.opacity(0.02), radius: 4, y: 2)
    }

    // MARK: - Backend & HealthKit Sync

    private func syncWeight(newWeight: Double) {
        currentWeightKg = newWeight
        Task {
            _ = await HealthKitManager.shared.writeWeight(kg: newWeight, date: Date())

            _ = try? await WorkoutOnboardingApi.shared.syncOnboarding(
                goal: selectedGoalRaw,
                heightCm: userHeightCm,
                weightKg: newWeight,
                targetWeightKg: targetWeightKg > 0 ? targetWeightKg : newWeight,
                birthYear: userBirthYear > 1900 ? userBirthYear : 2003,
                workoutDaysPerWeek: userWorkoutDaysPerWeek > 0 ? userWorkoutDaysPerWeek : 2,
                targetDate: targetDate
            )
        }
    }

    private func loadBackendData() async {
        if let data = try? await WorkoutOnboardingApi.shared.fetchOnboarding() {
            await MainActor.run {
                if let w = data.weightKg, w > 0 {
                    self.currentWeightKg = w
                }
                if let tw = data.targetWeightKg, tw > 0 {
                    self.targetWeightKg = tw
                }
                if let tdStr = data.targetDate,
                   let parsed = Self.isoDateFormatter.date(from: tdStr) {
                    self.backendTargetDate = parsed
                }
            }
        }
    }

    private func loadHistory() async {
        let samples = await HealthKitManager.shared.fetchWeightSamples(days: 30)
        await MainActor.run {
            self.historySamples = samples
            self.isLoadingHistory = false
        }
    }
}

// MARK: - Weight Projection S-Curve Canvas

private struct WeightProjectionCanvas: View {
    let currentWeightKg: Double
    let targetWeightKg: Double
    let targetDate: Date
    let unit: String
    let isLbs: Bool

    private var displayCurrent: Double {
        isLbs ? currentWeightKg * 2.20462 : currentWeightKg
    }

    private var displayTarget: Double {
        isLbs ? targetWeightKg * 2.20462 : targetWeightKg
    }

    private var isGaining: Bool {
        targetWeightKg > currentWeightKg
    }

    var body: some View {
        GeometryReader { geo in
            let w = geo.size.width
            let h = geo.size.height

            let padX: CGFloat = 36
            let padY: CGFloat = 36
            let graphW = w - padX * 2
            let graphH = h - padY * 2

            let startPoint = CGPoint(x: padX, y: isGaining ? padY + graphH : padY)
            let endPoint = CGPoint(x: padX + graphW, y: isGaining ? padY : padY + graphH)

            // Control points for smooth S-curve
            let c1 = CGPoint(x: startPoint.x + graphW * 0.45, y: startPoint.y)
            let c2 = CGPoint(x: endPoint.x - graphW * 0.45, y: endPoint.y)

            ZStack {
                // Background Guidelines (Dashed)
                Path { p in
                    p.move(to: CGPoint(x: padX, y: startPoint.y))
                    p.addLine(to: CGPoint(x: padX + graphW, y: startPoint.y))

                    p.move(to: CGPoint(x: padX, y: endPoint.y))
                    p.addLine(to: CGPoint(x: padX + graphW, y: endPoint.y))
                }
                .stroke(Color(hex: 0xFFE2E8F0), style: StrokeStyle(lineWidth: 1, dash: [4, 4]))

                // Gradient Shaded Area underneath the curve
                Path { p in
                    p.move(to: CGPoint(x: startPoint.x, y: h - 8))
                    p.addLine(to: startPoint)
                    p.addCurve(to: endPoint, control1: c1, control2: c2)
                    p.addLine(to: CGPoint(x: endPoint.x, y: h - 8))
                    p.closeSubpath()
                }
                .fill(
                    LinearGradient(
                        colors: [AppColors.primary.opacity(0.18), AppColors.primary.opacity(0.01)],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                )

                // The S-Curve Line
                Path { p in
                    p.move(to: startPoint)
                    p.addCurve(to: endPoint, control1: c1, control2: c2)
                }
                .stroke(
                    LinearGradient(
                        colors: [AppColors.primary, Color(hex: 0xFF10B981)],
                        startPoint: .leading,
                        endPoint: .trailing
                    ),
                    style: StrokeStyle(lineWidth: 3.5, lineCap: .round)
                )

                // Start Point Dot & Label
                ZStack {
                    Circle()
                        .fill(AppColors.primary)
                        .frame(width: 12, height: 12)
                    Circle()
                        .stroke(Color.white, lineWidth: 2.5)
                        .frame(width: 12, height: 12)
                }
                .position(startPoint)

                Text(String(format: "%.1f %@", displayCurrent, unit))
                    .font(.system(size: 11, weight: .bold))
                    .foregroundColor(AppColors.primary)
                    .position(x: startPoint.x, y: startPoint.y + (isGaining ? 16 : -16))

                // End Point Dot & Target Label
                ZStack {
                    Circle()
                        .fill(Color(hex: 0xFF10B981))
                        .frame(width: 14, height: 14)
                    Circle()
                        .stroke(Color.white, lineWidth: 3)
                        .frame(width: 14, height: 14)
                }
                .position(endPoint)

                Text(String(format: "%.1f %@", displayTarget, unit))
                    .font(.system(size: 11, weight: .bold))
                    .foregroundColor(Color(hex: 0xFF10B981))
                    .position(x: endPoint.x - 8, y: endPoint.y + (isGaining ? -16 : 16))
            }
        }
    }
}

// MARK: - Weight History Canvas

private struct WeightHistoryCanvas: View {
    let samples: [(date: Date, kg: Double)]
    let isLbs: Bool
    let unit: String

    private static let dateFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "M/d"
        return f
    }()

    var body: some View {
        GeometryReader { geo in
            let w = geo.size.width
            let h = geo.size.height
            let padX: CGFloat = 30
            let padY: CGFloat = 26
            let graphW = w - padX * 2
            let graphH = h - padY * 2

            let values: [Double] = samples.map { isLbs ? $0.kg * 2.20462 : $0.kg }
            let minVal = (values.min() ?? 70.0) - 1.0
            let maxVal = (values.max() ?? 75.0) + 1.0
            let range = max(0.5, maxVal - minVal)

            let points: [CGPoint] = samples.indices.map { i in
                let x = samples.count == 1 ? padX + graphW / 2 : padX + (CGFloat(i) / CGFloat(samples.count - 1)) * graphW
                let norm = CGFloat((values[i] - minVal) / range)
                let y = padY + graphH * (1.0 - norm)
                return CGPoint(x: x, y: y)
            }

            ZStack {
                // Background gridlines
                ForEach([0.0, 0.5, 1.0], id: \.self) { frac in
                    let y = padY + graphH * CGFloat(frac)
                    let gridVal = maxVal - frac * range
                    HStack {
                        Text(String(format: "%.0f", gridVal))
                            .font(.system(size: 9))
                            .foregroundColor(AppColors.textTertiary)
                            .frame(width: 24, alignment: .trailing)
                        Rectangle()
                            .fill(Color(hex: 0xFFF1F5F9))
                            .frame(height: 1)
                    }
                    .position(x: w / 2, y: y)
                }

                // Line path
                if points.count > 1 {
                    Path { p in
                        p.move(to: points[0])
                        for pt in points.dropFirst() {
                            p.addLine(to: pt)
                        }
                    }
                    .stroke(AppColors.primary, style: StrokeStyle(lineWidth: 2.5, lineCap: .round, lineJoin: .round))

                    // Gradient area
                    Path { p in
                        p.move(to: CGPoint(x: points[0].x, y: h - 8))
                        p.addLine(to: points[0])
                        for pt in points.dropFirst() {
                            p.addLine(to: pt)
                        }
                        p.addLine(to: CGPoint(x: points.last!.x, y: h - 8))
                        p.closeSubpath()
                    }
                    .fill(
                        LinearGradient(
                            colors: [AppColors.primary.opacity(0.18), AppColors.primary.opacity(0.01)],
                            startPoint: .top,
                            endPoint: .bottom
                        )
                    )
                }

                // Data point circles
                ForEach(Array(points.enumerated()), id: \.offset) { index, pt in
                    Circle()
                        .fill(AppColors.primary)
                        .frame(width: 7, height: 7)
                        .overlay(Circle().stroke(Color.white, lineWidth: 1.5))
                        .position(pt)
                }
            }
        }
    }
}
