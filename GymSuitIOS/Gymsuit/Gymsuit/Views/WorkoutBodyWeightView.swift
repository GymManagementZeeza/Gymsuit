import SwiftUI

public enum WeightInputUnit: String, CaseIterable {
    case kg = "kg"
    case lbs = "lbs"
}

struct WorkoutBodyWeightView: View {
    @Binding var weightKg: Double
    var heightCm: Double
    var onBack: () -> Void
    var onNext: () -> Void
    @State private var selectedUnit: WeightInputUnit

    init(
        weightKg: Binding<Double>,
        heightCm: Double = 172.6,
        selectedUnit: WeightInputUnit = .lbs,
        onBack: @escaping () -> Void,
        onNext: @escaping () -> Void
    ) {
        self._weightKg = weightKg
        self.heightCm = heightCm
        self._selectedUnit = State(initialValue: selectedUnit)
        self.onBack = onBack
        self.onNext = onNext
    }
    
    // Internal weight state in selected unit
    @State private var displayedWeight: Double = 164.3
    @State private var dragOffset: CGFloat = 0
    @State private var lastHapticValue: Double = 164.3
    private let haptic = UIImpactFeedbackGenerator(style: .light)

    private let tickSpacing: CGFloat = 8.0 // points per 0.1 unit (80 points per whole unit)

    var body: some View {
        ZStack(alignment: .bottom) {
            Color(hex: 0xFFFAFBFD)
                .ignoresSafeArea()

            VStack(spacing: 0) {
                // Top Navigation Bar (02 ABOUT YOUR BODY)
                headerView
                    .padding(.horizontal, 20)
                    .padding(.top, 10)
                    .padding(.bottom, 12)

                ScrollView(showsIndicators: false) {
                    VStack(spacing: 24) {
                        // Title
                        Text("What's your current\nweight?")
                            .font(.system(size: 28, weight: .bold))
                            .foregroundColor(Color(hex: 0xFF0F172A))
                            .multilineTextAlignment(.center)
                            .padding(.top, 14)

                        // kg / lbs Toggle Pill
                        unitTogglePill
                            .padding(.top, 8)

                        // Big Weight Display (e.g. 164.3 lbs)
                        HStack(alignment: .firstTextBaseline, spacing: 6) {
                            Text(String(format: "%.1f", displayedWeight))
                                .font(.system(size: 52, weight: .bold, design: .rounded))
                                .foregroundColor(Color(hex: 0xFF0F172A))

                            Text(selectedUnit.rawValue)
                                .font(.system(size: 17, weight: .bold))
                                .foregroundColor(Color(hex: 0xFF475569))
                        }
                        .padding(.top, 8)

                        // Horizontal Weight Ruler
                        rulerView
                            .frame(height: 90)
                            .padding(.vertical, 6)

                        // BMI Insight Card
                        bmiCard
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
            if weightKg > 0 {
                if selectedUnit == .lbs {
                    displayedWeight = weightKg * 2.20462
                } else {
                    displayedWeight = weightKg
                }
            } else {
                // Default to 164.3 lbs (matches screenshot)
                displayedWeight = 164.3
                weightKg = 164.3 / 2.20462
            }
            lastHapticValue = displayedWeight
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

                // Progress Bar: 4 segments, step 1 & 2 active
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

    // MARK: - Unit Toggle Pill [ kg | lbs ]
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
                displayedWeight = displayedWeight / 2.20462
            } else {
                displayedWeight = displayedWeight * 2.20462
            }
            selectedUnit = newUnit
            syncWeightKg()
        }
    }

    // MARK: - Ruler View
    private var rulerView: some View {
        GeometryReader { geo in
            let midX = geo.size.width / 2

            ZStack {
                // Ticks Canvas
                Canvas { context, size in
                    let baseCenter = displayedWeight
                    let minVisible = baseCenter - Double(size.width / (tickSpacing * 2)) - 1
                    let maxVisible = baseCenter + Double(size.width / (tickSpacing * 2)) + 1

                    let startTick = Int(floor(minVisible * 10))
                    let endTick = Int(ceil(maxVisible * 10))

                    for tick in startTick...endTick {
                        let value = Double(tick) / 10.0
                        let x = midX + CGFloat(value - baseCenter) * tickSpacing * 10.0

                        if x < -20 || x > size.width + 20 { continue }

                        let isWholeNumber = (tick % 10 == 0)
                        let isHalfNumber = (tick % 5 == 0 && !isWholeNumber)

                        var path = Path()
                        let lineHeight: CGFloat = isWholeNumber ? 28 : (isHalfNumber ? 20 : 14)
                        let startY: CGFloat = 16
                        path.move(to: CGPoint(x: x, y: startY))
                        path.addLine(to: CGPoint(x: x, y: startY + lineHeight))

                        let tickColor = isWholeNumber ? Color(hex: 0xFFCBD5E1) : Color(hex: 0xFFE2E8F0)
                        context.stroke(path, with: .color(tickColor), lineWidth: 1.5)

                        // Draw Whole Number text label below major ticks
                        if isWholeNumber {
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
                            let newWeight = max(20.0, min(500.0, displayedWeight + deltaTicks))
                            displayedWeight = newWeight
                            dragOffset = value.translation.width
                            syncWeightKg()

                            // Haptic on every 0.2 increment
                            if abs(displayedWeight - lastHapticValue) >= 0.2 {
                                haptic.impactOccurred()
                                lastHapticValue = displayedWeight
                            }
                        }
                        .onEnded { _ in
                            dragOffset = 0
                            // Round to 1 decimal place
                            displayedWeight = (displayedWeight * 10).rounded() / 10.0
                            syncWeightKg()
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
                    .frame(width: 60)

                    Spacer()

                    LinearGradient(
                        colors: [Color(hex: 0xFFFAFBFD).opacity(0), Color(hex: 0xFFFAFBFD)],
                        startPoint: .leading,
                        endPoint: .trailing
                    )
                    .frame(width: 60)
                }
                .allowsHitTesting(false)
            }
        }
    }

    private func syncWeightKg() {
        if selectedUnit == .lbs {
            weightKg = displayedWeight / 2.20462
        } else {
            weightKg = displayedWeight
        }
    }

    // MARK: - BMI Calculation and Insight Card
    private var currentBMI: Double {
        let heightM = heightCm / 100.0
        guard heightM > 0 else { return 25.0 }
        let currentKg = (selectedUnit == .lbs) ? (displayedWeight / 2.20462) : displayedWeight
        return currentKg / (heightM * heightM)
    }

    private var bmiStatusInfo: (status: String, color: Color, message: String) {
        let bmi = currentBMI
        if bmi < 18.5 {
            return ("Underweight", Color(hex: 0xFF3B82F6), "A balanced diet and strength building will help you reach a healthy weight.")
        } else if bmi < 25.0 {
            return ("Normal", Color(hex: 0xFF16A34A), "You are in a healthy, prime range! Keep maintaining your great habits.")
        } else if bmi < 30.0 {
            return ("Overweight", Color(hex: 0xFFEA580C), "You only need a bit more sweat exercises to see a fitter you!")
        } else {
            return ("Obese", Color(hex: 0xFFDC2626), "Targeted cardiovascular & nutrition plans will guide your transformation!")
        }
    }

    private var bmiCard: some View {
        let info = bmiStatusInfo

        return VStack(alignment: .leading, spacing: 10) {
            // Header: Your BMI (Overweight) ℹ️
            HStack(spacing: 5) {
                Text("Your BMI")
                    .font(.system(size: 15, weight: .bold))
                    .foregroundColor(Color(hex: 0xFF0F172A))

                Text("(\(info.status))")
                    .font(.system(size: 15, weight: .bold))
                    .foregroundColor(info.color)

                Image(systemName: "info.circle.fill")
                    .font(.system(size: 13))
                    .foregroundColor(Color(hex: 0xFFCBD5E1))
            }

            // BMI value + message
            HStack(alignment: .top, spacing: 14) {
                Text(String(format: "%.1f", currentBMI))
                    .font(.system(size: 32, weight: .bold, design: .rounded))
                    .foregroundColor(info.color)

                Text(info.message)
                    .font(.system(size: 13, weight: .regular))
                    .foregroundColor(Color(hex: 0xFF475569))
                    .lineSpacing(3)
                    .fixedSize(horizontal: false, vertical: true)
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
                syncWeightKg()
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
