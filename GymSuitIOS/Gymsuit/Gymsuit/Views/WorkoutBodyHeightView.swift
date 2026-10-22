import SwiftUI

public enum HeightInputUnit: String, CaseIterable {
    case cm = "cm"
    case ft = "ft"
}

struct WorkoutBodyHeightView: View {
    @Binding var heightCm: Double
    var onBack: () -> Void
    var onNext: () -> Void
    @State private var selectedUnit: HeightInputUnit

    init(
        heightCm: Binding<Double>,
        selectedUnit: HeightInputUnit = .ft,
        onBack: @escaping () -> Void,
        onNext: @escaping () -> Void
    ) {
        self._heightCm = heightCm
        self._selectedUnit = State(initialValue: selectedUnit)
        self.onBack = onBack
        self.onNext = onNext
    }

    // 172.72 cm = 5 ft 8 in (matches screenshot)
    @State private var dragOffset: CGFloat = 0
    @State private var lastHapticInches: Int = 68
    private let haptic = UIImpactFeedbackGenerator(style: .light)

    // Points per unit on the vertical ruler:
    // When in ft: 12 points per inch (144 points per foot)
    // When in cm: 5 points per cm
    private var pointsPerUnit: CGFloat {
        selectedUnit == .ft ? 12.0 : 5.0
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

                // Title
                Text("What's your height?")
                    .font(.system(size: 28, weight: .bold))
                    .foregroundColor(Color(hex: 0xFF0F172A))
                    .multilineTextAlignment(.center)
                    .padding(.top, 14)

                // Unit Toggle [ cm | ft ]
                unitTogglePill
                    .padding(.top, 24)

                Spacer()

                // Main Display with Horizontal Pointer Line and Vertical Ruler
                interactiveRulerArea
                    .frame(height: 380)

                Spacer()

                // Bottom spacer for floating button
                Spacer().frame(height: 80)
            }

            // Bottom NEXT Button
            bottomActionBar
        }
        .onAppear {
            haptic.prepare()
            if heightCm <= 0 {
                heightCm = 172.72 // 5 ft 8 in
            }
            lastHapticInches = totalInches
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

                // Progress Bar: Step 1 fully blue, Step 2 half blue (indicating height is sub-step 1 of 02)
                HStack(spacing: 6) {
                    Capsule()
                        .fill(Color(hex: 0xFF0066FF))
                        .frame(width: 44, height: 3)

                    // Half-filled capsule: 20pt blue + 24pt gray
                    HStack(spacing: 0) {
                        Capsule()
                            .fill(Color(hex: 0xFF0066FF))
                            .frame(width: 20, height: 3)
                        Capsule()
                            .fill(Color(hex: 0xFFE2E8F0))
                            .frame(width: 24, height: 3)
                    }
                    .frame(width: 44, height: 3)
                    .clipShape(Capsule())

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

    // MARK: - Unit Toggle Pill [ cm | ft ]
    private var unitTogglePill: some View {
        HStack(spacing: 0) {
            Button(action: {
                withAnimation(.spring(response: 0.3, dampingFraction: 0.75)) {
                    selectedUnit = .cm
                }
            }) {
                Text("cm")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundColor(selectedUnit == .cm ? .white : Color(hex: 0xFF334155))
                    .frame(width: 60, height: 36)
                    .background(selectedUnit == .cm ? Color(hex: 0xFF0066FF) : Color.clear)
                    .clipShape(Capsule())
            }

            Button(action: {
                withAnimation(.spring(response: 0.3, dampingFraction: 0.75)) {
                    selectedUnit = .ft
                }
            }) {
                Text("ft")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundColor(selectedUnit == .ft ? .white : Color(hex: 0xFF334155))
                    .frame(width: 60, height: 36)
                    .background(selectedUnit == .ft ? Color(hex: 0xFF0066FF) : Color.clear)
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

    // MARK: - Calculations for feet / inches
    private var totalInches: Int {
        Int((heightCm / 2.54).rounded())
    }

    private var feetPart: Int {
        totalInches / 12
    }

    private var inchesPart: Int {
        totalInches % 12
    }

    // MARK: - Interactive Ruler Area
    private var interactiveRulerArea: some View {
        GeometryReader { geo in
            let midY = geo.size.height / 2
            let rulerWidth: CGFloat = 64
            let lineEndX = geo.size.width - 2

            ZStack(alignment: .leading) {
                // Large Value Display (aligned horizontally with the blue pointer line)
                VStack(alignment: .leading, spacing: 6) {
                    if selectedUnit == .ft {
                        HStack(alignment: .firstTextBaseline, spacing: 4) {
                            Text("\(feetPart)")
                                .font(.system(size: 56, weight: .bold, design: .rounded))
                                .foregroundColor(Color(hex: 0xFF0F172A))
                            Text("ft")
                                .font(.system(size: 17, weight: .bold))
                                .foregroundColor(Color(hex: 0xFF0F172A))
                                .padding(.trailing, 8)

                            Text("\(inchesPart)")
                                .font(.system(size: 56, weight: .bold, design: .rounded))
                                .foregroundColor(Color(hex: 0xFF0F172A))
                            Text("in")
                                .font(.system(size: 17, weight: .bold))
                                .foregroundColor(Color(hex: 0xFF0F172A))
                        }
                    } else {
                        HStack(alignment: .firstTextBaseline, spacing: 6) {
                            Text("\(Int(heightCm.rounded()))")
                                .font(.system(size: 56, weight: .bold, design: .rounded))
                                .foregroundColor(Color(hex: 0xFF0F172A))
                            Text("cm")
                                .font(.system(size: 17, weight: .bold))
                                .foregroundColor(Color(hex: 0xFF0F172A))
                        }
                    }

                    // Blue Horizontal Pointer Line extending under the value to the ruler
                    Capsule()
                        .fill(Color(hex: 0xFF0066FF))
                        .frame(width: lineEndX - 80, height: 2.5)
                }
                .padding(.leading, 80)
                .position(x: 180, y: midY)

                // Vertical Ruler on Right Edge
                verticalRuler(size: CGSize(width: rulerWidth, height: geo.size.height), midY: midY)
                    .frame(width: rulerWidth)
                    .frame(maxWidth: .infinity, alignment: .trailing)
            }
            .contentShape(Rectangle())
            .gesture(
                DragGesture()
                    .onChanged { value in
                        let deltaY = value.translation.height - dragOffset
                        dragOffset = value.translation.height

                        // Moving finger down (deltaY > 0) pulls ruler down -> increases height
                        // Moving finger up (deltaY < 0) pushes ruler up -> decreases height
                        if selectedUnit == .ft {
                            let deltaInches = Double(deltaY) / Double(pointsPerUnit)
                            let currentInches = heightCm / 2.54
                            let newInches = currentInches + deltaInches
                            let clampedInches = max(36.0, min(96.0, newInches))
                            heightCm = clampedInches * 2.54

                            let currentWholeInches = Int(clampedInches.rounded())
                            if currentWholeInches != lastHapticInches {
                                haptic.impactOccurred()
                                lastHapticInches = currentWholeInches
                            }
                        } else {
                            let deltaCm = Double(deltaY) / Double(pointsPerUnit)
                            let newCm = heightCm + deltaCm
                            heightCm = max(90.0, min(240.0, newCm))

                            let currentWholeCm = Int(heightCm.rounded())
                            if currentWholeCm != lastHapticInches {
                                haptic.impactOccurred()
                                lastHapticInches = currentWholeCm
                            }
                        }
                    }
                    .onEnded { _ in
                        dragOffset = 0
                        // Snap to clean integer inch or cm
                        if selectedUnit == .ft {
                            heightCm = Double(totalInches) * 2.54
                        } else {
                            heightCm = heightCm.rounded()
                        }
                    }
            )
        }
    }

    // MARK: - Canvas for Vertical Ruler
    private func verticalRuler(size: CGSize, midY: CGFloat) -> some View {
        ZStack {
            Canvas { context, canvasSize in
                let isFeet = (selectedUnit == .ft)

                if isFeet {
                    // Current value in inches
                    let currentVal = heightCm / 2.54
                    let minInch = Int(floor(currentVal - Double(canvasSize.height / (pointsPerUnit * 2)))) - 2
                    let maxInch = Int(ceil(currentVal + Double(canvasSize.height / (pointsPerUnit * 2)))) + 2

                    for inch in minInch...maxInch {
                        // Position y on screen: higher inch is further up (smaller y)
                        let y = midY - CGFloat(Double(inch) - currentVal) * pointsPerUnit
                        if y < -20 || y > canvasSize.height + 20 { continue }

                        let isFoot = (inch % 12 == 0)
                        let isHalfFoot = (inch % 6 == 0 && !isFoot)

                        let tickLength: CGFloat = isFoot ? 24 : (isHalfFoot ? 16 : 10)
                        let startX = canvasSize.width - tickLength

                        var path = Path()
                        path.move(to: CGPoint(x: startX, y: y))
                        path.addLine(to: CGPoint(x: canvasSize.width, y: y))

                        let color = isFoot ? Color(hex: 0xFFCBD5E1) : Color(hex: 0xFFE2E8F0)
                        context.stroke(path, with: .color(color), lineWidth: 1.5)

                        // Label for whole feet (4, 5, 6, 7, 8)
                        if isFoot {
                            let feetNum = inch / 12
                            let text = Text("\(feetNum)")
                                .font(.system(size: 14, weight: .bold))
                                .foregroundColor(Color(hex: 0xFF94A3B8))
                            context.draw(context.resolve(text), at: CGPoint(x: startX - 14, y: y))
                        }
                    }
                } else {
                    // CM mode
                    let currentVal = heightCm
                    let minCm = Int(floor(currentVal - Double(canvasSize.height / (pointsPerUnit * 2)))) - 5
                    let maxCm = Int(ceil(currentVal + Double(canvasSize.height / (pointsPerUnit * 2)))) + 5

                    for cm in minCm...maxCm {
                        let y = midY - CGFloat(Double(cm) - currentVal) * pointsPerUnit
                        if y < -20 || y > canvasSize.height + 20 { continue }

                        let isMajor = (cm % 10 == 0)
                        let isHalf = (cm % 5 == 0 && !isMajor)

                        let tickLength: CGFloat = isMajor ? 24 : (isHalf ? 16 : 10)
                        let startX = canvasSize.width - tickLength

                        var path = Path()
                        path.move(to: CGPoint(x: startX, y: y))
                        path.addLine(to: CGPoint(x: canvasSize.width, y: y))

                        let color = isMajor ? Color(hex: 0xFFCBD5E1) : Color(hex: 0xFFE2E8F0)
                        context.stroke(path, with: .color(color), lineWidth: 1.5)

                        if isMajor {
                            let text = Text("\(cm)")
                                .font(.system(size: 13, weight: .bold))
                                .foregroundColor(Color(hex: 0xFF94A3B8))
                            context.draw(context.resolve(text), at: CGPoint(x: startX - 16, y: y))
                        }
                    }
                }
            }

            // Top and Bottom Soft Fade Gradients
            VStack {
                LinearGradient(
                    colors: [Color(hex: 0xFFFAFBFD), Color(hex: 0xFFFAFBFD).opacity(0)],
                    startPoint: .top,
                    endPoint: .bottom
                )
                .frame(height: 70)

                Spacer()

                LinearGradient(
                    colors: [Color(hex: 0xFFFAFBFD).opacity(0), Color(hex: 0xFFFAFBFD)],
                    startPoint: .top,
                    endPoint: .bottom
                )
                .frame(height: 70)
            }
            .allowsHitTesting(false)
        }
    }

    // MARK: - Bottom Action Bar
    private var bottomActionBar: some View {
        VStack(spacing: 0) {
            Button(action: onNext) {
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
