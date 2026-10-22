import SwiftUI

struct WorkoutTargetWeightView: View {
    var goal: WorkoutGoal
    @Binding var targetWeightKg: Double
    var currentWeightKg: Double
    var onBack: () -> Void
    var onNext: () -> Void
    @State private var selectedUnit: WeightInputUnit

    init(
        goal: WorkoutGoal = .loseWeight,
        targetWeightKg: Binding<Double>,
        currentWeightKg: Double,
        selectedUnit: WeightInputUnit = .kg,
        onBack: @escaping () -> Void,
        onNext: @escaping () -> Void
    ) {
        self.goal = goal
        self._targetWeightKg = targetWeightKg
        self.currentWeightKg = currentWeightKg
        self._selectedUnit = State(initialValue: selectedUnit)
        self.onBack = onBack
        self.onNext = onNext
    }

    @State private var displayedTarget: Double = 71.0
    @State private var dragOffset: CGFloat = 0
    @State private var lastHapticValue: Double = 71.0
    private let haptic = UIImpactFeedbackGenerator(style: .light)

    private let tickSpacing: CGFloat = 8.0 // points per 0.1 unit

    // Converted current weight in the active unit
    private var displayedCurrent: Double {
        if selectedUnit == .lbs {
            return currentWeightKg * 2.20462
        } else {
            return currentWeightKg
        }
    }

    var body: some View {
        ZStack(alignment: .bottom) {
            Color(hex: 0xFFFAFBFD)
                .ignoresSafeArea()

            VStack(spacing: 0) {
                // Header (02 ABOUT YOUR BODY)
                headerView
                    .padding(.horizontal, 20)
                    .padding(.top, 10)
                    .padding(.bottom, 12)

                ScrollView(showsIndicators: false) {
                    VStack(spacing: 24) {
                        // Title: What's your target weight?
                        titleView
                            .padding(.top, 14)

                        // kg / lbs Toggle Pill
                        unitTogglePill
                            .padding(.top, 8)

                        // Target Weight Display: 71 kg  ◀◀ 91.7 kg
                        weightDisplayRow
                            .padding(.top, 8)

                        // Horizontal Weight Ruler with Shaded Span
                        rulerView
                            .frame(height: 90)
                            .padding(.vertical, 6)

                        // Goal Feedback & Health Benefit Card
                        goalBenefitCard
                            .padding(.top, 10)

                        // Bottom spacer for floating button
                        Spacer().frame(height: 90)
                    }
                    .padding(.horizontal, 20)
                }
            }

            // Bottom NEXT Button
            bottomActionBar
        }
        .onAppear {
            haptic.prepare()
            if targetWeightKg > 0 {
                if selectedUnit == .lbs {
                    displayedTarget = targetWeightKg * 2.20462
                } else {
                    displayedTarget = targetWeightKg
                }
            } else {
                if goal == .buildMuscle {
                    let baseKg = currentWeightKg > 0 ? (currentWeightKg + 6.0) : 78.0
                    displayedTarget = (selectedUnit == .lbs) ? (baseKg * 2.20462) : baseKg
                } else {
                    // Default target for lose weight: 71 kg (matches screenshot)
                    displayedTarget = (selectedUnit == .lbs) ? (71.0 * 2.20462) : 71.0
                }
                syncTargetKg()
            }
            lastHapticValue = displayedTarget
        }
    }

    // MARK: - Header
    private var headerView: some View {
        ZStack {
            // Centered Step 02 Indicator
            VStack(spacing: 8) {
                HStack(spacing: 4) {
                    Text("02")
                        .font(.system(size: 13, weight: .bold))
                        .foregroundColor(Color(hex: 0xFF0066FF))
                    Text("ABOUT YOUR BODY")
                        .font(.system(size: 13, weight: .bold))
                        .foregroundColor(Color(hex: 0xFF1E293B))
                        .tracking(0.6)
                }

                // Progress Bar: 4 segments, step 1 & 2 fully active (end of step 02)
                HStack(spacing: 6) {
                    Capsule()
                        .fill(Color(hex: 0xFF0066FF))
                        .frame(width: 44, height: 3)
                    Capsule()
                        .fill(Color(hex: 0xFF0066FF))
                        .frame(width: 44, height: 3)
                    Capsule()
                        .fill(Color(hex: 0xFFE2E8F0))
                        .frame(width: 44, height: 3)
                    Capsule()
                        .fill(Color(hex: 0xFFE2E8F0))
                        .frame(width: 44, height: 3)
                }
            }

            // Back Button on left
            HStack {
                Button(action: onBack) {
                    Image(systemName: "arrow.backward")
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundColor(Color(hex: 0xFF0F172A))
                        .frame(width: 40, height: 40)
                        .contentShape(Rectangle())
                }
                Spacer()
            }
        }
    }

    // MARK: - Title
    private var titleView: some View {
        VStack(spacing: 2) {
            HStack(spacing: 0) {
                Text("What's your ")
                    .foregroundColor(Color(hex: 0xFF0F172A))
                Text("target")
                    .foregroundColor(Color(hex: 0xFF0066FF))
            }
            .font(.system(size: 28, weight: .bold))

            Text("weight?")
                .font(.system(size: 28, weight: .bold))
                .foregroundColor(Color(hex: 0xFF0F172A))
        }
        .multilineTextAlignment(.center)
    }

    // MARK: - Unit Toggle Pill
    private var unitTogglePill: some View {
        HStack(spacing: 0) {
            Button(action: { switchUnit(to: .kg) }) {
                Text("kg")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundColor(selectedUnit == .kg ? .white : Color(hex: 0xFF334155))
                    .frame(width: 60, height: 36)
                    .background(selectedUnit == .kg ? Color(hex: 0xFF0066FF) : Color.clear)
                    .clipShape(Capsule())
            }

            Button(action: { switchUnit(to: .lbs) }) {
                Text("lbs")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundColor(selectedUnit == .lbs ? .white : Color(hex: 0xFF334155))
                    .frame(width: 60, height: 36)
                    .background(selectedUnit == .lbs ? Color(hex: 0xFF0066FF) : Color.clear)
                    .clipShape(Capsule())
            }
        }
        .padding(3)
        .background(Color.white)
        .clipShape(Capsule())
        .overlay(
            Capsule()
                .stroke(Color(hex: 0xFF0066FF).opacity(0.85), lineWidth: 1.5)
        )
    }

    private func switchUnit(to newUnit: WeightInputUnit) {
        guard newUnit != selectedUnit else { return }
        withAnimation(.spring(response: 0.3, dampingFraction: 0.75)) {
            if newUnit == .kg {
                displayedTarget = displayedTarget / 2.20462
            } else {
                displayedTarget = displayedTarget * 2.20462
            }
            selectedUnit = newUnit
            syncTargetKg()
        }
    }

    // MARK: - Weight Display Row: 71 kg  ◀◀ 91.7 kg
    private var weightDisplayRow: some View {
        HStack(alignment: .firstTextBaseline, spacing: 6) {
            Text(displayedTarget.truncatingRemainder(dividingBy: 1) == 0 ? "\(Int(displayedTarget))" : String(format: "%.1f", displayedTarget))
                .font(.system(size: 52, weight: .bold, design: .rounded))
                .foregroundColor(Color(hex: 0xFF0F172A))

            Text(selectedUnit.rawValue)
                .font(.system(size: 17, weight: .bold))
                .foregroundColor(Color(hex: 0xFF475569))

            HStack(spacing: 4) {
                Image(systemName: isLosingWeight ? "backward.fill" : "forward.fill")
                    .font(.system(size: 12, weight: .bold))
                    .foregroundColor(Color(hex: 0xFF3B82F6))

                Text(String(format: "%.1f %@", displayedCurrent, selectedUnit.rawValue))
                    .font(.system(size: 14, weight: .medium))
                    .foregroundColor(Color(hex: 0xFF64748B))
            }
            .padding(.leading, 8)
        }
    }

    // MARK: - Ruler View with Shaded Span
    private var rulerView: some View {
        GeometryReader { geo in
            let midX = geo.size.width / 2

            ZStack {
                // Ticks and Shaded Range Canvas
                Canvas { context, size in
                    let baseCenter = displayedTarget
                    let curWeight = displayedCurrent

                    // Shaded Span between target and current weight
                    let targetX = midX
                    let currentX = midX + CGFloat(curWeight - baseCenter) * tickSpacing * 10.0
                    let minShadeX = min(targetX, currentX)
                    let shadeWidth = abs(currentX - targetX)

                    if shadeWidth > 0 {
                        let shadeRect = CGRect(x: minShadeX, y: 14, width: shadeWidth, height: 46)
                        context.fill(Path(shadeRect), with: .color(Color(hex: 0xFFE0E7FF).opacity(0.55)))
                    }

                    // Ticks
                    let minVisible = baseCenter - Double(size.width / (tickSpacing * 2)) - 1
                    let maxVisible = baseCenter + Double(size.width / (tickSpacing * 2)) + 1

                    let startTick = Int(floor(minVisible * 10))
                    let endTick = Int(ceil(maxVisible * 10))

                    for tick in startTick...endTick {
                        let value = Double(tick) / 10.0
                        let x = midX + CGFloat(value - baseCenter) * tickSpacing * 10.0

                        if x < -20 || x > size.width + 20 { continue }

                        let isTenNumber = (tick % 100 == 0) // e.g. 50, 60, 70, 80, 90
                        let isWholeNumber = (tick % 10 == 0)
                        let isHalfNumber = (tick % 5 == 0 && !isWholeNumber)

                        var path = Path()
                        let lineHeight: CGFloat = isWholeNumber ? 28 : (isHalfNumber ? 20 : 14)
                        let startY: CGFloat = 16
                        path.move(to: CGPoint(x: x, y: startY))
                        path.addLine(to: CGPoint(x: x, y: startY + lineHeight))

                        let tickColor = isWholeNumber ? Color(hex: 0xFFCBD5E1) : Color(hex: 0xFFE2E8F0)
                        context.stroke(path, with: .color(tickColor), lineWidth: 1.5)

                        // Draw major whole numbers (every 10 units: 50, 60, 70, 80, 90)
                        if isTenNumber {
                            let wholeValue = tick / 10
                            let text = Text("\(wholeValue)")
                                .font(.system(size: 13, weight: .bold))
                                .foregroundColor(Color(hex: 0xFF94A3B8))
                            context.draw(context.resolve(text), at: CGPoint(x: x, y: startY + lineHeight + 16))
                        }
                    }
                }
                .contentShape(Rectangle())
                .gesture(
                    DragGesture()
                        .onChanged { value in
                            let deltaTicks = -Double(value.translation.width - dragOffset) / Double(tickSpacing * 10)
                            let newTarget = max(20.0, min(300.0, displayedTarget + deltaTicks))
                            displayedTarget = newTarget
                            dragOffset = value.translation.width
                            syncTargetKg()

                            if abs(displayedTarget - lastHapticValue) >= 0.5 {
                                haptic.impactOccurred()
                                lastHapticValue = displayedTarget
                            }
                        }
                        .onEnded { _ in
                            dragOffset = 0
                            displayedTarget = (displayedTarget * 10).rounded() / 10.0
                            syncTargetKg()
                        }
                )

                // Center Blue Pointer Indicator
                VStack(spacing: 0) {
                    Capsule()
                        .fill(Color(hex: 0xFF0066FF))
                        .frame(width: 2.5, height: 42)
                    Spacer()
                }

                // Left and Right Fade Gradients
                HStack {
                    LinearGradient(
                        colors: [Color(hex: 0xFFFAFBFD), Color(hex: 0xFFFAFBFD).opacity(0)],
                        startPoint: .leading,
                        endPoint: .trailing
                    )
                    .frame(width: 50)

                    Spacer()

                    LinearGradient(
                        colors: [Color(hex: 0xFFFAFBFD).opacity(0), Color(hex: 0xFFFAFBFD)],
                        startPoint: .leading,
                        endPoint: .trailing
                    )
                    .frame(width: 50)
                }
                .allowsHitTesting(false)
            }
        }
    }

    private func syncTargetKg() {
        if selectedUnit == .lbs {
            targetWeightKg = displayedTarget / 2.20462
        } else {
            targetWeightKg = displayedTarget
        }
    }

    // MARK: - Goal Benefit Card
    private var percentChange: Double {
        guard currentWeightKg > 0 else { return 0 }
        let current = displayedCurrent
        let target = displayedTarget
        return (abs(current - target) / current) * 100.0
    }

    private var isLosingWeight: Bool {
        displayedTarget < displayedCurrent
    }

    private var goalBenefitCard: some View {
        VStack(alignment: .leading, spacing: 8) {
            // Headline
            Text(isLosingWeight ? (percentChange > 18 ? "Challenging goal!" : "Achievable goal!") : "Muscle & Strength goal!")
                .font(.system(size: 16, weight: .bold))
                .foregroundColor(Color(hex: 0xFFE11D48))

            // Percentage line
            HStack(spacing: 4) {
                Text(isLosingWeight ? "You will lose" : "You will gain")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(Color(hex: 0xFF0F172A))

                Text(String(format: "%.1f%%", percentChange))
                    .font(.system(size: 15, weight: .bold))
                    .foregroundColor(Color(hex: 0xFFE11D48))

                Text("of body weight")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(Color(hex: 0xFF0F172A))
            }

            // Benefits list
            VStack(alignment: .leading, spacing: 4) {
                Text("You can see more significant health benefits:")
                    .font(.system(size: 13, weight: .regular))
                    .foregroundColor(Color(hex: 0xFF475569))
                    .padding(.top, 2)

                Text("- Improve cardiovascular health")
                    .font(.system(size: 13, weight: .regular))
                    .foregroundColor(Color(hex: 0xFF475569))

                Text("- Reduce the risk of cancer")
                    .font(.system(size: 13, weight: .regular))
                    .foregroundColor(Color(hex: 0xFF475569))
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 20)
        .padding(.vertical, 18)
        .background(Color(hex: 0xFFF3F4FB))
        .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
    }

    // MARK: - Bottom Action Bar
    private var bottomActionBar: some View {
        VStack(spacing: 0) {
            Button(action: {
                syncTargetKg()
                onNext()
            }) {
                Text("NEXT")
                    .font(.system(size: 16, weight: .bold))
                    .tracking(1.0)
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: 54)
                    .background(Color(hex: 0xFF111111))
                    .clipShape(Capsule())
                    .shadow(color: Color.black.opacity(0.18), radius: 8, y: 4)
            }
            .buttonStyle(.plain)
            .padding(.horizontal, 20)
            .padding(.top, 12)
            .padding(.bottom, 16)
        }
        .background(
            LinearGradient(
                colors: [Color(hex: 0xFFFAFBFD).opacity(0), Color(hex: 0xFFFAFBFD).opacity(0.95), Color(hex: 0xFFFAFBFD)],
                startPoint: .top,
                endPoint: .bottom
            )
        )
    }
}
