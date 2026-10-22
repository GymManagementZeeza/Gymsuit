import SwiftUI

struct WorkoutFrequencyView: View {
    @Binding var workoutDaysPerWeek: Int
    var onBack: () -> Void
    var onNext: () -> Void

    @State private var selectedDays: Int = 2
    private let availableDays: [Int] = [1, 2, 3, 4, 5]
    private let haptic = UIImpactFeedbackGenerator(style: .light)

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

                // Title
                Text("How often would you like\nto work out?")
                    .font(.system(size: 26, weight: .bold))
                    .foregroundColor(Color(hex: 0xFF0F172A))
                    .multilineTextAlignment(.center)
                    .padding(.top, 14)
                    .padding(.horizontal, 20)

                Spacer()

                // Calendar Card Graphic
                calendarCard
                    .padding(.top, 10)

                // Dynamic descriptive text below calendar
                Text(frequencyDescription)
                    .font(.system(size: 16, weight: .regular))
                    .foregroundColor(Color(hex: 0xFF64748B))
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 40)
                    .padding(.top, 24)
                    .frame(height: 54)

                Spacer()

                // Frequency Stepper Control ("2 times/week", track with dots, "Less" / "More")
                stepperControl
                    .padding(.horizontal, 28)
                    .padding(.bottom, 20)

                // Spacer for floating bottom button
                Spacer().frame(height: 80)
            }

            // Bottom NEXT Button
            bottomActionBar
        }
        .onAppear {
            haptic.prepare()
            if workoutDaysPerWeek > 0 {
                selectedDays = workoutDaysPerWeek
            } else {
                selectedDays = 2 // Default matching screenshot
                workoutDaysPerWeek = 2
            }
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

                // Progress Bar: 4 segments, steps 1, 2, 3 active (blue), step 4 gray
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

    // MARK: - Calendar Card Graphic
    private var calendarCard: some View {
        ZStack(alignment: .top) {
            // Main white calendar page
            ZStack(alignment: .bottomTrailing) {
                RoundedRectangle(cornerRadius: 22, style: .continuous)
                    .fill(Color.white)
                    .overlay(
                        RoundedRectangle(cornerRadius: 22, style: .continuous)
                            .stroke(Color(hex: 0xFFEEF2F6), lineWidth: 1)
                    )
                    .shadow(color: Color.black.opacity(0.08), radius: 16, y: 8)

                // Top Blue Header Strip
                VStack {
                    LinearGradient(
                        colors: [Color(hex: 0xFF3B82F6), Color(hex: 0xFF2563EB)],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                    .frame(height: 38)
                    .clipShape(
                        CustomRoundedCorner(radius: 22, corners: [.topLeft, .topRight])
                    )

                    Spacer()
                }

                // Big Centered Day Count
                Text("\(selectedDays)")
                    .font(.system(size: 72, weight: .bold, design: .rounded))
                    .foregroundColor(Color(hex: 0xFF0F172A))
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .padding(.top, 30)

                // Page Fold / Curl Effect on bottom right
                Path { path in
                    path.move(to: CGPoint(x: 18, y: 0))
                    path.addLine(to: CGPoint(x: 0, y: 18))
                    path.addLine(to: CGPoint(x: 18, y: 18))
                    path.closeSubpath()
                }
                .fill(Color(hex: 0xFFE2E8F0))
                .frame(width: 18, height: 18)
                .padding(.trailing, 4)
                .padding(.bottom, 4)
            }
            .frame(width: 160, height: 160)

            // Binder Rings across top
            HStack(spacing: 12) {
                ForEach(0..<6, id: \.self) { _ in
                    Capsule()
                        .fill(
                            LinearGradient(
                                colors: [Color(hex: 0xFF93C5FD), Color(hex: 0xFF3B82F6), Color(hex: 0xFF1D4ED8)],
                                startPoint: .top,
                                endPoint: .bottom
                            )
                        )
                        .frame(width: 7, height: 16)
                        .shadow(color: Color.black.opacity(0.12), radius: 1, y: 1)
                }
            }
            .offset(y: -8)
        }
    }

    // MARK: - Dynamic Description Text
    private var frequencyDescription: String {
        switch selectedDays {
        case 1:
            return "A gentle start to build your routine and keep moving."
        case 2:
            return "I have a bit of free time, but not too much."
        case 3:
            return "Balanced and sustainable. A great sweet spot."
        case 4:
            return "Dedicated to seeing rapid, powerful progress."
        case 5:
            return "Full commitment to peak conditioning and strength."
        default:
            return "Consistency is the key to conquering your fitness goal!"
        }
    }

    // MARK: - Stepper Control
    private var stepperControl: some View {
        VStack(spacing: 14) {
            // Title: "2 times/week"
            Text("\(selectedDays) times/week")
                .font(.system(size: 16, weight: .bold))
                .foregroundColor(Color(hex: 0xFF0F172A))

            // Dot Track
            GeometryReader { geo in
                let trackWidth = geo.size.width
                let stepCount = availableDays.count
                let spacing = trackWidth / CGFloat(stepCount - 1)

                ZStack(alignment: .leading) {
                    // Gray background track
                    Capsule()
                        .fill(Color(hex: 0xFFF1F5F9))
                        .frame(height: 28)

                    // Dots for stops
                    HStack(spacing: 0) {
                        ForEach(availableDays.indices, id: \.self) { index in
                            let day = availableDays[index]
                            let isSelected = (day == selectedDays)

                            ZStack {
                                if isSelected {
                                    // Active ring indicator
                                    Circle()
                                        .stroke(Color(hex: 0xFF0066FF), lineWidth: 4)
                                        .background(Circle().fill(Color.white))
                                        .frame(width: 22, height: 22)
                                        .shadow(color: Color(hex: 0xFF0066FF).opacity(0.3), radius: 4, y: 2)
                                } else {
                                    Circle()
                                        .fill(Color(hex: 0xFFCBD5E1))
                                        .frame(width: 7, height: 7)
                                }
                            }
                            .frame(maxWidth: .infinity)
                            .contentShape(Rectangle())
                            .onTapGesture {
                                withAnimation(.spring(response: 0.3, dampingFraction: 0.75)) {
                                    selectedDays = day
                                    workoutDaysPerWeek = day
                                    haptic.impactOccurred()
                                }
                            }
                        }
                    }
                    .frame(height: 28)
                }
                .gesture(
                    DragGesture(minimumDistance: 0)
                        .onChanged { value in
                            let clampedX = max(0, min(trackWidth, value.location.x))
                            let fraction = clampedX / trackWidth
                            let index = Int((fraction * CGFloat(stepCount - 1)).rounded())
                            let safeIndex = max(0, min(stepCount - 1, index))
                            let newDay = availableDays[safeIndex]
                            if newDay != selectedDays {
                                selectedDays = newDay
                                workoutDaysPerWeek = newDay
                                haptic.impactOccurred()
                            }
                        }
                )
            }
            .frame(height: 28)

            // Less / More Labels
            HStack {
                Text("Less")
                    .font(.system(size: 14, weight: .bold))
                    .foregroundColor(Color(hex: 0xFF0F172A))

                Spacer()

                Text("More")
                    .font(.system(size: 14, weight: .bold))
                    .foregroundColor(Color(hex: 0xFF0F172A))
            }
        }
    }

    // MARK: - Bottom Action Bar
    private var bottomActionBar: some View {
        VStack(spacing: 0) {
            Button(action: {
                workoutDaysPerWeek = selectedDays
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

// Helper for top-rounded corners
struct CustomRoundedCorner: Shape {
    var radius: CGFloat = .infinity
    var corners: UIRectCorner = .allCorners

    func path(in rect: CGRect) -> Path {
        let path = UIBezierPath(
            roundedRect: rect,
            byRoundingCorners: corners,
            cornerRadii: CGSize(width: radius, height: radius)
        )
        return Path(path.cgPath)
    }
}
