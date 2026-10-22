import SwiftUI

struct WorkoutGoalSelectionView: View {
    @Binding var selectedGoal: WorkoutGoal
    var onBack: () -> Void
    var onNext: () -> Void

    var body: some View {
        ZStack(alignment: .bottom) {
            Color(hex: 0xFFFAFBFD)
                .ignoresSafeArea()

            VStack(spacing: 0) {
                // Top Navigation Bar
                headerView
                    .padding(.horizontal, 20)
                    .padding(.top, 10)
                    .padding(.bottom, 12)

                ScrollView(showsIndicators: false) {
                    VStack(spacing: 18) {
                        // Title
                        Text("What's your main goal?")
                            .font(.system(size: 28, weight: .bold))
                            .foregroundColor(Color(hex: 0xFF0F172A))
                            .frame(maxWidth: .infinity, alignment: .center)
                            .padding(.top, 16)
                            .padding(.bottom, 6)

                        // Three Goal Cards
                        ForEach(WorkoutGoal.allCases) { goal in
                            goalCard(for: goal)
                        }

                        // Bottom breathing room for floating NEXT button
                        Spacer().frame(height: 90)
                    }
                    .padding(.horizontal, 20)
                }
            }

            // Bottom NEXT Button
            bottomActionBar
        }
    }

    // MARK: - Header
    private var headerView: some View {
        ZStack {
            // Centered Step Indicator & Title
            VStack(spacing: 8) {
                HStack(spacing: 4) {
                    Text("01")
                        .font(.system(size: 13, weight: .bold))
                        .foregroundColor(Color(hex: 0xFF0066FF))
                    Text("GOAL & FOCUS")
                        .font(.system(size: 13, weight: .bold))
                        .foregroundColor(Color(hex: 0xFF1E293B))
                        .tracking(0.6)
                }

                // Progress Bar: Step 1 active (blue), others gray
                HStack(spacing: 6) {
                    Capsule()
                        .fill(Color(hex: 0xFF0066FF))
                        .frame(width: 44, height: 3)
                    Capsule()
                        .fill(Color(hex: 0xFFE2E8F0))
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

    // MARK: - Goal Card
    private func goalCard(for goal: WorkoutGoal) -> some View {
        let isSelected = selectedGoal == goal

        return VStack(spacing: 12) {
            Button(action: {
                withAnimation(.spring(response: 0.35, dampingFraction: 0.82)) {
                    selectedGoal = goal
                }
            }) {
                ZStack(alignment: .trailing) {
                    // Card background
                    RoundedRectangle(cornerRadius: 24, style: .continuous)
                        .fill(isSelected ? Color(hex: 0xFF0066FF) : Color.white)
                        .overlay(
                            RoundedRectangle(cornerRadius: 24, style: .continuous)
                                .stroke(isSelected ? Color.clear : Color(hex: 0xFFECEEF2), lineWidth: 1)
                        )
                        .shadow(
                            color: isSelected
                                ? Color(hex: 0xFF0066FF).opacity(0.22)
                                : Color.black.opacity(0.04),
                            radius: isSelected ? 12 : 8,
                            y: isSelected ? 6 : 2
                        )

                    // Right-side Person Cutout / Graphic
                    goalCardGraphic(for: goal, isSelected: isSelected)

                    // Left-side Goal Title
                    HStack {
                        Text(goal.title)
                            .font(.system(size: 23, weight: .bold))
                            .foregroundColor(isSelected ? .white : Color(hex: 0xFF0F172A))
                            .lineLimit(2)
                            .multilineTextAlignment(.leading)
                            .padding(.leading, 22)
                            .frame(maxWidth: .infinity, alignment: .leading)

                        Spacer()
                    }
                }
                .frame(height: 126)
                .contentShape(RoundedRectangle(cornerRadius: 24))
            }
            .buttonStyle(.plain)

            // Info bubble below selected card
            if isSelected {
                VStack(alignment: .leading, spacing: 6) {
                    HStack(spacing: 6) {
                        Text(goal.emoji)
                            .font(.system(size: 16))
                        Text(goal.bannerTitle)
                            .font(.system(size: 15, weight: .bold))
                            .foregroundColor(Color(hex: 0xFF0F172A))
                    }

                    Text(goal.bannerDescription)
                        .font(.system(size: 13, weight: .regular))
                        .foregroundColor(Color(hex: 0xFF475569))
                        .lineSpacing(3)
                        .fixedSize(horizontal: false, vertical: true)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, 20)
                .padding(.vertical, 16)
                .background(Color(hex: 0xFFF3F4FB))
                .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
                .transition(.asymmetric(
                    insertion: .opacity.combined(with: .move(edge: .top)),
                    removal: .opacity
                ))
            }
        }
    }

    // MARK: - Card Graphics
    @ViewBuilder
    private func goalCardGraphic(for goal: WorkoutGoal, isSelected: Bool) -> some View {
        Image(goal.imageName)
            .resizable()
            .scaledToFit()
            .frame(height: 122)
            .padding(.trailing, 10)
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
