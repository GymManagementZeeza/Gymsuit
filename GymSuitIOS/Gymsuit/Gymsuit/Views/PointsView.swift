import SwiftUI

/// Points screen: balance, earning rules, and history.
struct PointsView: View {
    @Environment(\.dismiss) private var dismiss
    @ObservedObject private var pointsStore = PointsStore.shared

    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 16) {
                    // Balance hero
                    VStack(spacing: 8) {
                        Image(systemName: "star.circle.fill")
                            .font(.system(size: 54))
                            .foregroundColor(AppColors.warning)
                        Text("\(pointsStore.balance)")
                            .font(.system(size: 44, weight: .bold))
                            .foregroundColor(AppColors.textPrimary)
                            .contentTransition(.numericText())
                            .animation(.spring(response: 0.6, dampingFraction: 0.7), value: pointsStore.balance)
                        Text("Total Points")
                            .font(.system(size: 15, weight: .medium))
                            .foregroundColor(AppColors.textSecondary)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 28)
                    .background(Color.white)
                    .clipShape(RoundedRectangle(cornerRadius: 20))
                    .overlay(RoundedRectangle(cornerRadius: 20).stroke(AppColors.border, lineWidth: 1))

                    // How to earn
                    VStack(alignment: .leading, spacing: 12) {
                        Text("How to earn")
                            .font(.system(size: 15, weight: .bold))
                            .foregroundColor(AppColors.textPrimary)
                        earnRow("+50 pts", "for every workout you log")
                        earnRow("+5 pts", "for every set")
                        earnRow("+2 pts", "for every minute of workout")
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(16)
                    .background(Color.white)
                    .clipShape(RoundedRectangle(cornerRadius: 20))
                    .overlay(RoundedRectangle(cornerRadius: 20).stroke(AppColors.border, lineWidth: 1))

                    // History
                    VStack(alignment: .leading, spacing: 8) {
                        Text("History")
                            .font(.system(size: 15, weight: .bold))
                            .foregroundColor(AppColors.textPrimary)
                            .padding(.horizontal, 4)
                            .padding(.bottom, 2)
                        if pointsStore.history.isEmpty {
                            Text("No points yet. Log a workout to start earning!")
                                .font(.system(size: 14))
                                .foregroundColor(AppColors.textSecondary)
                                .frame(maxWidth: .infinity, alignment: .center)
                                .padding(.vertical, 20)
                                .background(Color.white)
                                .clipShape(RoundedRectangle(cornerRadius: 20))
                                .overlay(RoundedRectangle(cornerRadius: 20).stroke(AppColors.border, lineWidth: 1))
                        } else {
                            ForEach(pointsStore.history) { entry in
                                historyRow(entry)
                            }
                        }
                    }
                }
                .padding(20)
            }
            .background(AppColors.surface.ignoresSafeArea())
            .navigationTitle("My Points")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") { dismiss() }
                }
            }
        }
    }

    private func earnRow(_ points: String, _ label: String) -> some View {
        HStack(spacing: 8) {
            Text(points)
                .font(.system(size: 14, weight: .bold))
                .foregroundColor(AppColors.workout)
            Text(label)
                .font(.system(size: 14))
                .foregroundColor(AppColors.textSecondary)
        }
    }

    private func historyRow(_ entry: PointsEntry) -> some View {
        HStack(spacing: 12) {
            Image(systemName: entry.points < 0 ? "arrow.down.circle.fill" : "star.circle.fill")
                .font(.system(size: 22))
                .foregroundColor(entry.points < 0 ? AppColors.danger : AppColors.workout)
            VStack(alignment: .leading, spacing: 2) {
                Text(entry.reason)
                    .font(.system(size: 14, weight: .medium))
                    .foregroundColor(AppColors.textPrimary)
                Text(entry.date, style: .date)
                    .font(.system(size: 12))
                    .foregroundColor(AppColors.textSecondary)
            }
            Spacer()
            Text("\(entry.points > 0 ? "+" : "")\(entry.points) pts")
                .font(.system(size: 15, weight: .bold))
                .foregroundColor(entry.points < 0 ? AppColors.danger : AppColors.workout)
        }
        .padding(12)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 14))
        .overlay(RoundedRectangle(cornerRadius: 14).stroke(AppColors.border, lineWidth: 1))
    }
}
