import SwiftUI

struct WorkoutProjectionGraphView: View {
    var goal: WorkoutGoal = .loseWeight
    var currentWeightKg: Double
    var targetWeightKg: Double
    var onBack: () -> Void
    var onFinish: () -> Void = {}
    var onFinishWithTargetDate: ((Date) -> Void)? = nil

    // Whether this trajectory is gaining weight (Build Muscle) or losing weight (Lose Weight)
    private var isGaining: Bool {
        (goal == .buildMuscle) || (targetWeightKg > currentWeightKg)
    }

    // 1 kg per week = 7 days per kg
    private var weightDifferenceKg: Double {
        abs(currentWeightKg - targetWeightKg)
    }

    private var acceleratedDays: Int {
        // 1 kg per week = 7 days per kg
        let days = Int(ceil(max(weightDifferenceKg, 1.0) * 7.0))
        return max(days, 14) // At least 2 weeks
    }

    private var standardDays: Int {
        // Standard slower baseline (e.g. ~0.67 kg per week -> ~10.5 days per kg)
        let days = Int(ceil(Double(acceleratedDays) * 1.5))
        return days
    }

    private var daysEarly: Int {
        max(standardDays - acceleratedDays, 7)
    }

    private var targetDate: Date {
        Calendar.current.date(byAdding: .day, value: acceleratedDays, to: Date()) ?? Date()
    }

    private var standardDate: Date {
        Calendar.current.date(byAdding: .day, value: standardDays, to: Date()) ?? Date()
    }

    private static let monthDayFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "MMM d"
        return f
    }()

    var body: some View {
        ZStack(alignment: .bottom) {
            Color(hex: 0xFFFAFBFD)
                .ignoresSafeArea()

            VStack(spacing: 0) {
                // Header (03 FITNESS ASSESSMENT)
                headerView
                    .padding(.horizontal, 20)
                    .padding(.top, 10)
                    .padding(.bottom, 12)

                ScrollView(showsIndicators: false) {
                    VStack(spacing: 20) {
                        // Title Section
                        titleSection
                            .padding(.top, 10)

                        // S-Curve Trajectory Graph (Downwards for Lose Weight, Upwards for Build Muscle)
                        graphView
                            .frame(height: 260)
                            .padding(.top, 10)

                        // Summary: 57 days early!
                        summarySection
                            .padding(.top, 24)

                        // Spacer for floating button
                        Spacer().frame(height: 90)
                    }
                    .padding(.horizontal, 20)
                }
            }

            // Bottom NEXT Button
            bottomActionBar
        }
    }

    // MARK: - Header (03 FITNESS ASSESSMENT)
    private var headerView: some View {
        ZStack {
            // Centered Step 03 Indicator
            VStack(spacing: 8) {
                HStack(spacing: 4) {
                    Text("03")
                        .font(.system(size: 13, weight: .bold))
                        .foregroundColor(Color(hex: 0xFF0066FF))
                    Text("FITNESS ASSESSMENT")
                        .font(.system(size: 13, weight: .bold))
                        .foregroundColor(Color(hex: 0xFF1E293B))
                        .tracking(0.6)
                }

                // Progress Bar: Step 1, 2, 3 fully filled (blue), step 4 gray
                HStack(spacing: 6) {
                    Capsule()
                        .fill(Color(hex: 0xFF0066FF))
                        .frame(width: 44, height: 3)
                    Capsule()
                        .fill(Color(hex: 0xFF0066FF))
                        .frame(width: 44, height: 3)
                    Capsule()
                        .fill(Color(hex: 0xFF0066FF))
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

    // MARK: - Title Section
    private var titleSection: some View {
        VStack(spacing: 6) {
            Text("A more precise outlook is here!")
                .font(.system(size: 17, weight: .medium))
                .foregroundColor(Color(hex: 0xFF475569))

            Text("You'll reach")
                .font(.system(size: 17, weight: .medium))
                .foregroundColor(Color(hex: 0xFF475569))

            HStack(spacing: 6) {
                Text(String(format: "%.1f kg on", targetWeightKg))
                    .foregroundColor(Color(hex: 0xFF0F172A))

                Text(Self.monthDayFormatter.string(from: targetDate))
                    .foregroundColor(Color(hex: 0xFF0066FF))
            }
            .font(.system(size: 32, weight: .bold))
        }
        .multilineTextAlignment(.center)
    }

    // MARK: - S-Curve Trajectory Graph
    private var graphView: some View {
        GeometryReader { geo in
            let w = geo.size.width
            let h = geo.size.height

            // Coordinates:
            // For losing weight: Start is HIGH (y=46), Target is LOW (y=h-68)
            // For gaining weight: Start is LOW (y=h-68), Target is HIGH (y=46)
            let startX: CGFloat = 36
            let startY: CGFloat = isGaining ? (h - 68) : 46

            let targetX: CGFloat = w * 0.58
            let targetY: CGFloat = isGaining ? 46 : (h - 68)

            let baselineX: CGFloat = w - 44
            let baselineY: CGFloat = targetY

            ZStack {
                // Gradient Fill Under the Accelerated Curve
                Path { path in
                    path.move(to: CGPoint(x: 0, y: isGaining ? h : startY))
                    path.addLine(to: CGPoint(x: startX, y: startY))
                    path.addCurve(
                        to: CGPoint(x: targetX, y: targetY),
                        control1: CGPoint(x: startX + (targetX - startX) * 0.45, y: isGaining ? (startY - 8) : (startY + 8)),
                        control2: CGPoint(x: startX + (targetX - startX) * 0.55, y: targetY)
                    )
                    path.addLine(to: CGPoint(x: w, y: targetY))
                    path.addLine(to: CGPoint(x: w, y: h))
                    path.addLine(to: CGPoint(x: 0, y: h))
                    path.closeSubpath()
                }
                .fill(
                    LinearGradient(
                        colors: isGaining ? [
                            Color(hex: 0xFF93C5FD).opacity(0.35),
                            Color(hex: 0xFFC7D2FE).opacity(0.2),
                            Color(hex: 0xFFFAFBFD).opacity(0)
                        ] : [
                            Color(hex: 0xFFFDA4AF).opacity(0.35),
                            Color(hex: 0xFFE0E7FF).opacity(0.2),
                            Color(hex: 0xFFFAFBFD).opacity(0)
                        ],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )

                // Vertical Dashed Lines down to X-Axis
                Path { path in
                    // Line at Today
                    path.move(to: CGPoint(x: startX, y: startY))
                    path.addLine(to: CGPoint(x: startX, y: h - 30))

                    // Line at Target Date
                    path.move(to: CGPoint(x: targetX, y: targetY))
                    path.addLine(to: CGPoint(x: targetX, y: h - 30))

                    // Line at Baseline Date
                    path.move(to: CGPoint(x: baselineX, y: baselineY))
                    path.addLine(to: CGPoint(x: baselineX, y: h - 30))
                }
                .stroke(
                    Color(hex: 0xFFCBD5E1),
                    style: StrokeStyle(lineWidth: 1.2, dash: [4, 4])
                )

                // Curve 1: Standard Baseline Pace (Dashed Curve)
                Path { path in
                    path.move(to: CGPoint(x: startX, y: startY))
                    path.addCurve(
                        to: CGPoint(x: baselineX, y: baselineY),
                        control1: CGPoint(x: startX + (baselineX - startX) * 0.5, y: isGaining ? (startY - 14) : (startY + 14)),
                        control2: CGPoint(x: startX + (baselineX - startX) * 0.6, y: baselineY)
                    )
                }
                .stroke(
                    Color(hex: 0xFFCBD5E1),
                    style: StrokeStyle(lineWidth: 2.2, dash: [6, 5])
                )

                // Curve 2: Accelerated Tailored Plan (Solid Vibrant Gradient Curve)
                Path { path in
                    path.move(to: CGPoint(x: 0, y: startY))
                    path.addLine(to: CGPoint(x: startX, y: startY))
                    path.addCurve(
                        to: CGPoint(x: targetX, y: targetY),
                        control1: CGPoint(x: startX + (targetX - startX) * 0.45, y: isGaining ? (startY - 8) : (startY + 8)),
                        control2: CGPoint(x: startX + (targetX - startX) * 0.55, y: targetY)
                    )
                    path.addLine(to: CGPoint(x: w, y: targetY))
                }
                .stroke(
                    LinearGradient(
                        colors: isGaining ? [
                            Color(hex: 0xFF3B82F6), // Blue at Start
                            Color(hex: 0xFF6366F1), // Indigo in Transition
                            Color(hex: 0xFF0066FF), // Vibrant Blue at Target
                            Color(hex: 0xFF10B981)  // Emerald plateau
                        ] : [
                            Color(hex: 0xFFF43F5E), // Coral / Red at Start
                            Color(hex: 0xFFA855F7), // Purple in Transition
                            Color(hex: 0xFF0066FF), // Vibrant Blue at Target
                            Color(hex: 0xFF38BDF8)  // Light Blue plateau
                        ],
                        startPoint: .leading,
                        endPoint: .trailing
                    ),
                    style: StrokeStyle(lineWidth: 4, lineCap: .round, lineJoin: .round)
                )

                // Double Chevron icon between target and baseline
                HStack(spacing: 2) {
                    Image(systemName: "chevron.left")
                    Image(systemName: "chevron.left")
                }
                .font(.system(size: 13, weight: .bold))
                .foregroundColor(Color(hex: 0xFF38BDF8))
                .position(x: (targetX + baselineX) / 2 - 4, y: targetY + (isGaining ? -22 : 22))

                // Dot 1: Start (Today)
                Circle()
                    .fill(isGaining ? Color(hex: 0xFF3B82F6) : Color(hex: 0xFFF43F5E))
                    .frame(width: 12, height: 12)
                    .overlay(Circle().stroke(Color.white, lineWidth: 2.5))
                    .shadow(color: (isGaining ? Color(hex: 0xFF3B82F6) : Color(hex: 0xFFF43F5E)).opacity(0.4), radius: 3, y: 1)
                    .position(x: startX, y: startY)

                // Dot 3: Baseline Standard Date
                Circle()
                    .fill(Color(hex: 0xFF38BDF8))
                    .frame(width: 11, height: 11)
                    .overlay(Circle().stroke(Color.white, lineWidth: 2))
                    .position(x: baselineX, y: baselineY)

                // Dot 2: Target Date (Glowing Halo + Center Dot)
                ZStack {
                    Circle()
                        .fill(Color(hex: 0xFF0066FF).opacity(0.18))
                        .frame(width: 26, height: 26)

                    Circle()
                        .fill(Color(hex: 0xFF0066FF))
                        .frame(width: 14, height: 14)
                        .overlay(Circle().stroke(Color.white, lineWidth: 2.5))
                }
                .position(x: targetX, y: targetY)

                // Floating Blue Tooltip Bubble above Target Dot
                tooltipBubble(days: acceleratedDays)
                    .position(x: targetX, y: targetY - 44)

                // X-Axis Labels
                Text("Today")
                    .font(.system(size: 13, weight: .medium))
                    .foregroundColor(Color(hex: 0xFF64748B))
                    .position(x: startX, y: h - 14)

                Text(Self.monthDayFormatter.string(from: targetDate))
                    .font(.system(size: 15, weight: .bold))
                    .foregroundColor(Color(hex: 0xFF0F172A))
                    .position(x: targetX, y: h - 14)

                Text(Self.monthDayFormatter.string(from: standardDate))
                    .font(.system(size: 13, weight: .medium))
                    .foregroundColor(Color(hex: 0xFF64748B))
                    .position(x: baselineX, y: h - 14)
            }
        }
    }

    // MARK: - Tooltip Bubble
    private func tooltipBubble(days: Int) -> some View {
        VStack(spacing: 0) {
            Text("\(days) Days")
                .font(.system(size: 14, weight: .bold))
                .foregroundColor(.white)
                .padding(.horizontal, 14)
                .padding(.vertical, 8)
                .background(Color(hex: 0xFF0066FF))
                .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                .shadow(color: Color(hex: 0xFF0066FF).opacity(0.35), radius: 6, y: 3)

            // Small bottom triangle pointer
            Triangle()
                .fill(Color(hex: 0xFF0066FF))
                .frame(width: 10, height: 6)
        }
    }

    // MARK: - Summary Section (e.g. 57 days early!)
    private var summarySection: some View {
        VStack(spacing: 8) {
            Text("\(daysEarly) days early!")
                .font(.system(size: 24, weight: .bold))
                .foregroundColor(Color(hex: 0xFF0F172A))

            Text(isGaining
                ? "You have great potential to build lean muscle\nand hit your target ahead of schedule!"
                : "You have great potential to achieve your\ngoals ahead of schedule!"
            )
            .font(.system(size: 15, weight: .regular))
            .foregroundColor(Color(hex: 0xFF64748B))
            .multilineTextAlignment(.center)
            .lineSpacing(3)
        }
    }

    // MARK: - Bottom Action Bar
    private var bottomActionBar: some View {
        VStack(spacing: 0) {
            Button(action: {
                onFinishWithTargetDate?(targetDate)
                onFinish()
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

// Helper Triangle Shape for Tooltip Pointer
private struct Triangle: Shape {
    func path(in rect: CGRect) -> Path {
        var path = Path()
        path.move(to: CGPoint(x: rect.minX, y: rect.minY))
        path.addLine(to: CGPoint(x: rect.maxX, y: rect.minY))
        path.addLine(to: CGPoint(x: rect.midX, y: rect.maxY))
        path.closeSubpath()
        return path
    }
}
