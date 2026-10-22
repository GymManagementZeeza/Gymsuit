import SwiftUI

public struct WorkoutProgressProjectionCardView: View {
    @AppStorage("workout_user_weight_kg") private var currentWeightKg: Double = 91.7
    @AppStorage("workout_user_target_weight_kg") private var targetWeightKg: Double = 71.0
    @AppStorage("workout_user_height_cm") private var userHeightCm: Double = 172.72
    @AppStorage("workout_selected_goal") private var selectedGoalRaw: String = WorkoutGoal.loseWeight.rawValue
    @AppStorage("workout_user_birth_year") private var userBirthYear: Int = 2003
    @AppStorage("workout_user_days_per_week") private var userWorkoutDaysPerWeek: Int = 2
    @Environment(\.scenePhase) private var scenePhase

    // Backend-synced values
    @State private var backendTargetWeightKg: Double? = nil
    @State private var backendTargetDate: Date? = nil
    @State private var backendCurrentWeightKg: Double? = nil
    @State private var isShowingRecordWeightSheet: Bool = false

    private var effectiveCurrentWeightKg: Double {
        backendCurrentWeightKg ?? currentWeightKg
    }

    private var effectiveTargetWeightKg: Double {
        backendTargetWeightKg ?? targetWeightKg
    }

    private var isGaining: Bool {
        effectiveTargetWeightKg > effectiveCurrentWeightKg
    }

    private var weightDifferenceKg: Double {
        abs(effectiveCurrentWeightKg - effectiveTargetWeightKg)
    }

    private var projectionDays: Int {
        let days = Int(ceil(max(weightDifferenceKg, 0.5) * 7.0))
        return max(days, 14)
    }

    private var effectiveTargetDate: Date {
        if let d = backendTargetDate {
            return d
        }
        return Calendar.current.date(byAdding: .day, value: projectionDays, to: Date()) ?? Date()
    }

    private static let monthDayFormatter: DateFormatter = {
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

    public init() {}

    public var body: some View {
        VStack(spacing: 16) {
            // MARK: - Header Title
            VStack(spacing: 6) {
                Text("Based on your body data, you'll reach")
                    .font(.system(size: 15, weight: .medium))
                    .foregroundColor(Color(hex: 0xFF64748B))

                HStack(alignment: .firstTextBaseline, spacing: 6) {
                    Text(String(format: "%.1f kg", effectiveTargetWeightKg))
                        .font(.system(size: 28, weight: .bold))
                        .foregroundColor(Color(hex: 0xFF0F172A))

                    Text("on")
                        .font(.system(size: 24, weight: .regular))
                        .foregroundColor(Color(hex: 0xFF0F172A))

                    Text(Self.monthDayFormatter.string(from: effectiveTargetDate))
                        .font(.system(size: 28, weight: .bold))
                        .foregroundColor(Color(hex: 0xFF0062FF))
                }
            }
            .padding(.top, 4)

            // MARK: - Projection Graph
            projectionCanvas
                .frame(height: 220)
                .padding(.top, 8)

            // MARK: - Record Weight Action Button
            Button(action: {
                isShowingRecordWeightSheet = true
            }) {
                HStack(spacing: 8) {
                    Image(systemName: "scalemass.fill")
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundColor(Color(hex: 0xFF0062FF))

                    Text("Record Weight")
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundColor(Color(hex: 0xFF0062FF))
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 13)
                .background(Color(hex: 0xFFEFF6FF))
                .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: 14, style: .continuous)
                        .stroke(Color(hex: 0xFF0062FF).opacity(0.18), lineWidth: 1)
                )
            }
            .buttonStyle(.plain)
            .padding(.top, 4)
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
            await loadBackendData()
        }
        .onChange(of: scenePhase) { _, newPhase in
            if newPhase == .active {
                Task { await loadBackendData() }
            }
        }
        .sheet(isPresented: $isShowingRecordWeightSheet) {
            RecordWeightSheet(
                currentWeightKg: Binding(
                    get: { effectiveCurrentWeightKg },
                    set: { newW in
                        currentWeightKg = newW
                        backendCurrentWeightKg = newW
                    }
                ),
                onWeightSaved: { newWeight in
                    syncWeightToBackendAndHealthKit(newWeight: newWeight)
                }
            )
        }
    }

    // MARK: - Graph Canvas matching reference image
    private var projectionCanvas: some View {
        GeometryReader { geometry in
            let w = geometry.size.width
            let h = geometry.size.height

            let startX: CGFloat = 45
            let targetX: CGFloat = w - 65

            // Curve Y coordinates
            let startY: CGFloat = isGaining ? (h - 60) : 38
            let targetY: CGFloat = isGaining ? 38 : (h - 60)
            let bottomY: CGFloat = h - 26

            ZStack {
                // 1. Soft Gradient Fill Area under the curve
                Path { path in
                    path.move(to: CGPoint(x: 0, y: isGaining ? bottomY : startY))
                    path.addLine(to: CGPoint(x: startX, y: startY))
                    path.addCurve(
                        to: CGPoint(x: targetX, y: targetY),
                        control1: CGPoint(x: startX + (targetX - startX) * 0.45, y: isGaining ? (startY - 6) : (startY + 6)),
                        control2: CGPoint(x: startX + (targetX - startX) * 0.55, y: targetY)
                    )
                    path.addLine(to: CGPoint(x: w, y: targetY))
                    path.addLine(to: CGPoint(x: w, y: bottomY))
                    path.addLine(to: CGPoint(x: 0, y: bottomY))
                    path.closeSubpath()
                }
                .fill(
                    LinearGradient(
                        colors: isGaining ? [
                            Color(hex: 0xFF93C5FD).opacity(0.35),
                            Color(hex: 0xFFC7D2FE).opacity(0.18),
                            Color(hex: 0xFFFAFBFD).opacity(0)
                        ] : [
                            Color(hex: 0xFFFDA4AF).opacity(0.38),
                            Color(hex: 0xFFE0E7FF).opacity(0.20),
                            Color(hex: 0xFFFAFBFD).opacity(0)
                        ],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )

                // 2. Vertical Dashed Guidelines down to X-axis
                Path { path in
                    // Guideline at Start (Today)
                    path.move(to: CGPoint(x: startX, y: startY))
                    path.addLine(to: CGPoint(x: startX, y: bottomY))

                    // Guideline at Target Date
                    path.move(to: CGPoint(x: targetX, y: targetY))
                    path.addLine(to: CGPoint(x: targetX, y: bottomY))
                }
                .stroke(
                    Color(hex: 0xFFCBD5E1),
                    style: StrokeStyle(lineWidth: 1.2, dash: [4, 4])
                )

                // 3. Main S-Curve Line with Gradient
                Path { path in
                    path.move(to: CGPoint(x: 0, y: startY))
                    path.addLine(to: CGPoint(x: startX, y: startY))
                    path.addCurve(
                        to: CGPoint(x: targetX, y: targetY),
                        control1: CGPoint(x: startX + (targetX - startX) * 0.45, y: isGaining ? (startY - 6) : (startY + 6)),
                        control2: CGPoint(x: startX + (targetX - startX) * 0.55, y: targetY)
                    )
                    path.addLine(to: CGPoint(x: w, y: targetY))
                }
                .stroke(
                    LinearGradient(
                        colors: isGaining ? [
                            Color(hex: 0xFF3B82F6), // Blue
                            Color(hex: 0xFF6366F1), // Indigo
                            Color(hex: 0xFF0062FF), // Vibrant Blue
                            Color(hex: 0xFF38BDF8)  // Light Blue
                        ] : [
                            Color(hex: 0xFFF43F5E), // Coral / Red at Start
                            Color(hex: 0xFFA855F7), // Purple in Transition
                            Color(hex: 0xFF0062FF), // Vibrant Blue at Target
                            Color(hex: 0xFF38BDF8)  // Light Blue plateau
                        ],
                        startPoint: .leading,
                        endPoint: .trailing
                    ),
                    style: StrokeStyle(lineWidth: 4, lineCap: .round, lineJoin: .round)
                )

                // 4. Start Point Label & Dot (Today)
                Text(String(format: "%.1f kg", effectiveCurrentWeightKg))
                    .font(.system(size: 14, weight: .bold))
                    .foregroundColor(Color(hex: 0xFF0F172A))
                    .position(x: startX, y: startY - 18)

                Circle()
                    .fill(isGaining ? Color(hex: 0xFF3B82F6) : Color(hex: 0xFFF43F5E))
                    .frame(width: 10, height: 10)
                    .overlay(Circle().stroke(Color.white, lineWidth: 2.5))
                    .shadow(color: (isGaining ? Color(hex: 0xFF3B82F6) : Color(hex: 0xFFF43F5E)).opacity(0.4), radius: 3, y: 1)
                    .position(x: startX, y: startY)

                // 5. Target Point Dot (Glow ring + circle)
                ZStack {
                    Circle()
                        .fill(Color(hex: 0xFF0062FF).opacity(0.20))
                        .frame(width: 24, height: 24)

                    Circle()
                        .fill(Color(hex: 0xFF0062FF))
                        .frame(width: 13, height: 13)
                        .overlay(Circle().stroke(Color.white, lineWidth: 2.5))
                }
                .position(x: targetX, y: targetY)

                // 6. Target Weight Speech Bubble Tooltip
                TargetTooltipBubble(weightKg: effectiveTargetWeightKg)
                    .position(x: targetX, y: targetY - 40)

                // 7. X-Axis Labels
                Text("Today")
                    .font(.system(size: 14, weight: .medium))
                    .foregroundColor(Color(hex: 0xFF64748B))
                    .position(x: startX, y: h - 8)

                Text(Self.monthDayFormatter.string(from: effectiveTargetDate))
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(Color(hex: 0xFF0F172A))
                    .position(x: targetX, y: h - 8)
            }
        }
    }

    private func loadBackendData() async {
        do {
            let data = try await WorkoutOnboardingApi.shared.fetchOnboarding()
            await MainActor.run {
                if let tw = data.targetWeightKg, tw > 0 {
                    self.backendTargetWeightKg = tw
                    self.targetWeightKg = tw
                }
                if let w = data.weightKg, w > 0 {
                    self.backendCurrentWeightKg = w
                    self.currentWeightKg = w
                }
                if let tdStr = data.targetDate,
                   let parsed = Self.isoDateFormatter.date(from: tdStr) {
                    self.backendTargetDate = parsed
                }
            }
        } catch {
            print("Notice: using cached onboarding data: \(error.localizedDescription)")
        }
    }

    private func syncWeightToBackendAndHealthKit(newWeight: Double) {
        Task {
            // 1. Sync to HealthKit
            _ = await HealthKitManager.shared.writeWeight(kg: newWeight, date: Date())

            // 2. Sync to Backend
            _ = try? await WorkoutOnboardingApi.shared.syncOnboarding(
                goal: selectedGoalRaw,
                heightCm: userHeightCm,
                weightKg: newWeight,
                targetWeightKg: effectiveTargetWeightKg,
                birthYear: userBirthYear,
                workoutDaysPerWeek: userWorkoutDaysPerWeek,
                targetDate: effectiveTargetDate
            )

            // 3. Reload fresh backend confirmation
            await loadBackendData()
        }
    }
}

// MARK: - Target Weight Speech Bubble Tooltip

struct TargetTooltipBubble: View {
    let weightKg: Double

    var body: some View {
        VStack(spacing: 0) {
            HStack(alignment: .firstTextBaseline, spacing: 3) {
                Text(String(format: "%.1f", weightKg))
                    .font(.system(size: 18, weight: .bold))
                    .foregroundColor(.white)

                Text("kg")
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundColor(.white.opacity(0.92))
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 7)
            .background(Color(hex: 0xFF0062FF))
            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))

            // Speech bubble downward triangle pointer
            TooltipTriangle()
                .fill(Color(hex: 0xFF0062FF))
                .frame(width: 10, height: 6)
                .offset(y: -1)
        }
        .shadow(color: Color(hex: 0xFF0062FF).opacity(0.28), radius: 6, y: 3)
    }
}

struct TooltipTriangle: Shape {
    func path(in rect: CGRect) -> Path {
        var path = Path()
        path.move(to: CGPoint(x: rect.minX, y: rect.minY))
        path.addLine(to: CGPoint(x: rect.maxX, y: rect.minY))
        path.addLine(to: CGPoint(x: rect.midX, y: rect.maxY))
        path.closeSubpath()
        return path
    }
}

// MARK: - Record Weight Sheet

struct RecordWeightSheet: View {
    @Binding var currentWeightKg: Double
    var onWeightSaved: (Double) -> Void
    @Environment(\.dismiss) private var dismiss

    @State private var recordedWeightKg: Double = 70.0
    @State private var selectedUnit: String = "kg"
    @State private var recordDate: Date = Date()
    @State private var isSaving: Bool = false

    private var displayValue: Double {
        selectedUnit == "kg" ? recordedWeightKg : (recordedWeightKg * 2.20462)
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 24) {
                // Unit Picker (kg / lbs)
                Picker("Unit", selection: $selectedUnit) {
                    Text("kg").tag("kg")
                    Text("lbs").tag("lbs")
                }
                .pickerStyle(.segmented)
                .padding(.horizontal, 24)
                .padding(.top, 16)

                // Date display
                HStack {
                    Image(systemName: "calendar")
                        .foregroundColor(Color(hex: 0xFF0062FF))
                    DatePicker(
                        "Date",
                        selection: $recordDate,
                        in: ...Date(),
                        displayedComponents: .date
                    )
                    .labelsHidden()
                }
                .padding(.horizontal, 24)

                Spacer().frame(height: 10)

                // Large Weight Display
                VStack(spacing: 8) {
                    HStack(alignment: .firstTextBaseline, spacing: 6) {
                        Text(String(format: "%.1f", displayValue))
                            .font(.system(size: 54, weight: .bold))
                            .foregroundColor(Color(hex: 0xFF0F172A))

                        Text(selectedUnit)
                            .font(.system(size: 24, weight: .bold))
                            .foregroundColor(Color(hex: 0xFF64748B))
                    }

                    Text("Current Recorded Weight")
                        .font(.system(size: 14, weight: .medium))
                        .foregroundColor(Color(hex: 0xFF94A3B8))
                }

                // Quick Increment / Decrement Steppers
                HStack(spacing: 12) {
                    weightAdjustButton(delta: -0.5, label: "-0.5")
                    weightAdjustButton(delta: -0.1, label: "-0.1")
                    weightAdjustButton(delta: +0.1, label: "+0.1")
                    weightAdjustButton(delta: +0.5, label: "+0.5")
                }
                .padding(.horizontal, 24)

                Spacer()

                // Save Button
                Button(action: saveWeight) {
                    HStack {
                        if isSaving {
                            ProgressView()
                                .tint(.white)
                                .padding(.trailing, 6)
                        }
                        Text("Save Weight")
                            .font(.system(size: 17, weight: .bold))
                            .foregroundColor(.white)
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: 54)
                    .background(Color(hex: 0xFF0062FF))
                    .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                }
                .disabled(isSaving)
                .padding(.horizontal, 24)
                .padding(.bottom, 20)
            }
            .navigationTitle("Record Weight")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("Cancel") {
                        dismiss()
                    }
                }
            }
            .onAppear {
                recordedWeightKg = currentWeightKg
            }
        }
        .presentationDetents([.height(440)])
        .presentationDragIndicator(.visible)
    }

    private func weightAdjustButton(delta: Double, label: String) -> some View {
        Button(action: {
            if selectedUnit == "kg" {
                recordedWeightKg = max(30.0, min(250.0, recordedWeightKg + delta))
            } else {
                let currentLbs = recordedWeightKg * 2.20462
                let newLbs = max(66.0, min(550.0, currentLbs + delta))
                recordedWeightKg = newLbs / 2.20462
            }
        }) {
            Text(label)
                .font(.system(size: 15, weight: .bold))
                .foregroundColor(Color(hex: 0xFF0062FF))
                .frame(maxWidth: .infinity)
                .frame(height: 44)
                .background(Color(hex: 0xFFEFF6FF))
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        }
        .buttonStyle(.plain)
    }

    private func saveWeight() {
        isSaving = true
        currentWeightKg = recordedWeightKg
        onWeightSaved(recordedWeightKg)
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
            isSaving = false
            dismiss()
        }
    }
}
