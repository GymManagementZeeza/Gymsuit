import SwiftUI

struct WorkoutBirthYearView: View {
    @Binding var birthYear: Int
    var onBack: () -> Void
    var onNext: () -> Void

    @State private var selectedYear: Int = 2003
    private let years: [Int] = Array((1940...2012).reversed()) // Most recent first, or 1950...2012
    private let haptic = UIImpactFeedbackGenerator(style: .light)

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
                Text("What's your birth year?")
                    .font(.system(size: 28, weight: .bold))
                    .foregroundColor(Color(hex: 0xFF0F172A))
                    .multilineTextAlignment(.center)
                    .padding(.top, 14)

                // Info Message Box
                infoCard
                    .padding(.horizontal, 20)
                    .padding(.top, 24)

                Spacer()

                // Vertical Year Selection Roller
                yearPickerView
                    .frame(height: 280)

                Spacer()

                // Spacer for bottom NEXT button
                Spacer().frame(height: 80)
            }

            // Bottom NEXT Button
            bottomActionBar
        }
        .onAppear {
            haptic.prepare()
            if birthYear > 0 {
                selectedYear = birthYear
            } else {
                selectedYear = 2003 // Default matching screenshot
                birthYear = 2003
            }
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

                // Progress Bar: 4 segments, step 1 & 2 filled
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

    // MARK: - Info Card
    private var infoCard: some View {
        HStack(alignment: .center, spacing: 14) {
            Text("📋")
                .font(.system(size: 26))

            Text("It will help us to adjust the workout that best suits your age group.")
                .font(.system(size: 14, weight: .regular))
                .foregroundColor(Color(hex: 0xFF475569))
                .lineSpacing(3)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(.horizontal, 18)
        .padding(.vertical, 16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(hex: 0xFFF3F4FB))
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
    }

    // MARK: - Vertical Year Picker
    private var yearPickerView: some View {
        ScrollViewReader { proxy in
            ScrollView(showsIndicators: false) {
                LazyVStack(spacing: 12) {
                    // Top padding so selected item can center
                    Spacer().frame(height: 100)

                    ForEach(years.reversed(), id: \.self) { year in
                        let isSelected = (year == selectedYear)
                        let distance = abs(year - selectedYear)

                        Button(action: {
                            withAnimation(.spring(response: 0.3, dampingFraction: 0.8)) {
                                selectedYear = year
                                birthYear = year
                                haptic.impactOccurred()
                            }
                        }) {
                            Text("\(year)")
                                .font(.system(
                                    size: isSelected ? 34 : (distance == 1 ? 26 : 22),
                                    weight: isSelected ? .bold : .medium,
                                    design: .rounded
                                ))
                                .foregroundColor(
                                    isSelected
                                        ? Color(hex: 0xFF0066FF)
                                        : (distance == 1 ? Color(hex: 0xFF64748B) : Color(hex: 0xFFCBD5E1))
                                )
                                .frame(maxWidth: .infinity)
                                .frame(height: 52)
                                .background(
                                    isSelected
                                        ? Color(hex: 0xFFF1F4FD)
                                        : Color.clear
                                )
                                .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                                .padding(.horizontal, 40)
                        }
                        .buttonStyle(.plain)
                        .id(year)
                    }

                    // Bottom padding
                    Spacer().frame(height: 100)
                }
            }
            .onAppear {
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
                    proxy.scrollTo(selectedYear, anchor: .center)
                }
            }
        }
    }

    // MARK: - Bottom Action Bar
    private var bottomActionBar: some View {
        VStack(spacing: 0) {
            Button(action: {
                birthYear = selectedYear
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
