import SwiftUI

/// Tappable points pill for the top bar. The number rolls up with a springy
/// pop whenever the balance grows. Tapping opens the points screen.
struct PointsPill: View {
    @ObservedObject private var pointsStore = PointsStore.shared
    @State private var showPoints = false
    @State private var popScale: CGFloat = 1.0

    var body: some View {
        Button(action: { showPoints = true }) {
            HStack(spacing: 6) {
                Image(systemName: "star.circle.fill")
                    .font(.system(size: 18))
                    .foregroundColor(AppColors.warning)
                Text("\(pointsStore.balance)")
                    .font(.system(size: 15, weight: .bold))
                    .foregroundColor(AppColors.textPrimary)
                    .lineLimit(1)
                    .fixedSize(horizontal: true, vertical: false)
                    .contentTransition(.numericText())
                    .animation(.spring(response: 0.6, dampingFraction: 0.7), value: pointsStore.balance)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(Color.white)
            .clipShape(Capsule())
            .overlay(Capsule().stroke(AppColors.border, lineWidth: 1))
            .shadow(color: Color.black.opacity(0.06), radius: 4, y: 2)
            .scaleEffect(popScale)
        }
        .fixedSize()
        .layoutPriority(1)
        .onChange(of: pointsStore.balance) { _, _ in
            withAnimation(.spring(response: 0.35, dampingFraction: 0.45)) {
                popScale = 1.22
            }
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
                withAnimation(.spring(response: 0.4, dampingFraction: 0.6)) {
                    popScale = 1.0
                }
            }
        }
        .sheet(isPresented: $showPoints) {
            PointsView()
        }
    }
}
