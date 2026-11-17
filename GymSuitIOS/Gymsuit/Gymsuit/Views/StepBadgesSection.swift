import SwiftUI

// MARK: - Badges section (Home tab)

/// "Daily Badges" section: 10,000 steps and 1,000 kcal burned. They reset
/// every day: a badge shows in full colour once earned today, otherwise it
/// stays greyed out with a lock.
struct StepBadgesSectionView: View {
    @StateObject private var store = StepBadgeStore.shared

    private static var todayKey: String {
        let f = DateFormatter()
        f.locale = Locale(identifier: "en_US_POSIX")
        f.dateFormat = "yyyy-MM-dd"
        return f.string(from: Date())
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("Daily Badges")
                .font(.app(size: 18, weight: .bold))
                .foregroundColor(AppColors.textPrimary)
            Text("Earn them again every day")
                .font(.app(size: 13))
                .foregroundColor(AppColors.textSecondary)

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 12) {
                    ForEach(StepBadgeConfig.daily, id: \.name) { milestone in
                        BadgeCardView(
                            milestone: milestone,
                            badge: store.badges
                                .filter { $0.threshold == milestone.threshold
                                    && ($0.kind ?? "STEPS") == milestone.kind
                                    && $0.date == Self.todayKey }
                                .max(by: { ($0.awardedAt ?? "") < ($1.awardedAt ?? "") })
                        )
                    }
                }
                .padding(.top, 8)
            }
        }
        .task {
            await store.refresh()
        }
    }
}

private struct BadgeCardView: View {
    let milestone: StepBadgeConfig.Milestone
    let badge: StepBadge?

    private var threshold: Int { milestone.threshold }
    private var name: String { milestone.name }

    private var earned: Bool { badge != nil }

    private var stepsLine: String {
        if let badge {
            return "\(badge.stepsAchieved.formatted()) \(milestone.unit)"
        }
        return "\(threshold.formatted()) \(milestone.unit)"
    }

    var body: some View {
        VStack(spacing: 6) {
            Image(systemName: earned ? "medal.fill" : "lock.fill")
                .font(.app(size: 44))
                .foregroundColor(earned ? Color(hex: 0xFFFFB300) : AppColors.textTertiary)
            Text(name)
                .font(.app(size: 14, weight: .bold))
                .foregroundColor(earned ? AppColors.textPrimary : AppColors.textTertiary)
                .multilineTextAlignment(.center)
            Text(stepsLine)
                .font(.app(size: 12))
                .foregroundColor(AppColors.textSecondary)
            if let badge, !badge.formattedDate.isEmpty {
                Text(badge.formattedDate)
                    .font(.app(size: 11))
                    .foregroundColor(AppColors.textTertiary)
            }
        }
        .frame(width: 152)
        .padding(.vertical, 14)
        .background(earned ? Color(hex: 0xFFFFF8E1) : AppColors.surface)
        .cornerRadius(18)
        .overlay(
            RoundedRectangle(cornerRadius: 18)
                .stroke(earned ? Color(hex: 0xFFFFB300) : Color.clear, lineWidth: 1.5)
        )
    }
}

// MARK: - Celebration host

/// Presents the celebration overlay exactly once per badge (seen ids are
/// persisted in UserDefaults). If several badges are new at once, the newest
/// is celebrated first; the rest follow on the next refresh.
struct BadgeCelebrationHostView: View {
    @StateObject private var store = StepBadgeStore.shared
    @State private var dismissedId: Int64 = -1

    private var newestUnseen: StepBadge? {
        store.badges
            .filter { !store.isSeen($0.id) && $0.id != dismissedId }
            .max(by: { ($0.awardedAt ?? "") < ($1.awardedAt ?? "") })
    }

    var body: some View {
        Color.clear
            .frame(width: 1, height: 1)
            .fullScreenCover(
                isPresented: Binding(
                    get: { newestUnseen != nil },
                    set: { shown in
                        if !shown, let badge = newestUnseen {
                            store.markSeen(badge.id)
                            dismissedId = badge.id
                        }
                    }
                )
            ) {
                if let badge = newestUnseen {
                    BadgeCelebrationView(badge: badge)
                }
            }
    }
}

// MARK: - Celebration overlay

private struct BadgeCelebrationView: View {
    let badge: StepBadge

    @Environment(\.dismiss) private var dismiss
    @State private var appeared = false

    var body: some View {
        ZStack {
            Color.black.opacity(0.55).ignoresSafeArea()
            ConfettiBurstView().ignoresSafeArea()

            VStack(spacing: 0) {
                Image(systemName: "medal.fill")
                    .font(.app(size: 88))
                    .foregroundColor(Color(hex: 0xFFFFB300))
                    .scaleEffect(appeared ? 1 : 0.4)

                Text("Badge Unlocked!")
                    .font(.app(size: 24, weight: .bold))
                    .foregroundColor(AppColors.textPrimary)
                    .padding(.top, 12)

                Text(badge.badgeName)
                    .font(.app(size: 18, weight: .semibold))
                    .foregroundColor(Color(hex: 0xFFD97706))
                    .padding(.top, 4)

                Text(badge.isCalories ? "\(badge.threshold.formatted()) kcal burned in a day" : "\(badge.threshold.formatted()) steps in a day")
                    .font(.app(size: 14))
                    .foregroundColor(AppColors.textSecondary)
                    .padding(.top, 8)

                Text(badge.isCalories ? "You burned \(badge.stepsAchieved.formatted()) kcal" : "You walked \(badge.stepsAchieved.formatted()) steps")
                    .font(.app(size: 14))
                    .foregroundColor(AppColors.textSecondary)
                    .padding(.top, 2)

                Button(action: { dismiss() }) {
                    Text("Keep it up")
                        .font(.app(size: 16, weight: .semibold))
                        .foregroundColor(.white)
                        .padding(.horizontal, 28)
                        .padding(.vertical, 12)
                        .background(Color(hex: 0xFFFFB300))
                        .cornerRadius(14)
                }
                .padding(.top, 20)
            }
            .padding(28)
            .background(Color.white)
            .cornerRadius(24)
            .padding(.horizontal, 32)
            .scaleEffect(appeared ? 1 : 0.8)
            .opacity(appeared ? 1 : 0)
        }
        .onAppear {
            withAnimation(.spring(response: 0.5, dampingFraction: 0.6)) {
                appeared = true
            }
        }
    }
}

// MARK: - Confetti

/// Falling confetti rendered with SwiftUI Canvas — no new packages.
private struct ConfettiBurstView: View {
    private struct Particle: Identifiable {
        let id = UUID()
        let x: Double        // 0..1 across the width
        let phase: Double    // 0..1 stagger offset
        let color: Color
        let size: Double
        let drift: Double
        let cycles: Double
        let spin: Double
    }

    private static let colors: [Color] = [
        Color(hex: 0xFFFFC72C), Color(hex: 0xFFFF6384), Color(hex: 0xFF48BB78),
        Color(hex: 0xFF4299E1), Color(hex: 0xFFED8936), Color(hex: 0xFF9F7AEA),
        Color(hex: 0xFF2DD4BF), Color(hex: 0xFFFBB6CE)
    ]

    private let particles: [Particle] = (0..<60).map { _ in
        Particle(
            x: Double.random(in: 0...1),
            phase: Double.random(in: 0...1),
            color: colors.randomElement() ?? .yellow,
            size: Double.random(in: 6...13),
            drift: Double.random(in: -0.1...0.1),
            cycles: Double.random(in: 1...3),
            spin: Double.random(in: -1...1)
        )
    }

    @State private var start = Date()

    var body: some View {
        TimelineView(.animation(minimumInterval: 1.0 / 60.0)) { timeline in
            let elapsed = timeline.date.timeIntervalSince(start).truncatingRemainder(dividingBy: 2.6) / 2.6
            Canvas { context, size in
                for p in particles {
                    let t = (elapsed + p.phase).truncatingRemainder(dividingBy: 1)
                    let y = t * (size.height + 60) - 30
                    let x = (p.x + p.drift * sin(t * .pi * 2 * p.cycles))
                        .truncatingRemainder(dividingBy: 1) * size.width
                    let alpha = 1 - t * t

                    var ctx = context
                    ctx.translateBy(x: x, y: y)
                    ctx.rotate(by: .degrees(p.spin * t * 360))
                    let rect = CGRect(x: -p.size / 2, y: -p.size / 4, width: p.size, height: p.size / 2)
                    ctx.fill(Path(rect), with: .color(p.color.opacity(alpha)))
                }
            }
        }
        .allowsHitTesting(false)
    }
}
