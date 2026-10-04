import SwiftUI

/// Points screen: balance, redeemable rupee value, redemption and history.
struct PointsView: View {
    @Environment(\.dismiss) private var dismiss
    @ObservedObject private var pointsStore = PointsStore.shared

    @State private var showRedeem = false
    @State private var redeemAmount = ""
    @State private var redeemMessage: String? = nil

    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 16) {
                    // Balance hero
                    VStack(spacing: 8) {
                        Image(systemName: "indianrupeesign.circle.fill")
                            .font(.system(size: 44))
                            .foregroundColor(AppColors.warning)
                        Text("\(pointsStore.balance)")
                            .font(.system(size: 44, weight: .bold))
                            .foregroundColor(AppColors.textPrimary)
                            .contentTransition(.numericText())
                            .animation(.spring(response: 0.6, dampingFraction: 0.7), value: pointsStore.balance)
                        Text("points")
                            .font(.system(size: 14, weight: .medium))
                            .foregroundColor(AppColors.textSecondary)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 24)
                    .background(Color.white)
                    .clipShape(RoundedRectangle(cornerRadius: 20))
                    .overlay(RoundedRectangle(cornerRadius: 20).stroke(AppColors.border, lineWidth: 1))

                    // Redeemable value
                    VStack(alignment: .leading, spacing: 10) {
                        Text("Redeemable value")
                            .font(.system(size: 15, weight: .bold))
                            .foregroundColor(AppColors.textPrimary)
                        HStack(alignment: .firstTextBaseline) {
                            Text(PointsConfig.formattedRupees(for: pointsStore.balance))
                                .font(.system(size: 32, weight: .bold))
                                .foregroundColor(AppColors.workout)
                            Spacer()
                            Text("100 pts = \u{20B9}1")
                                .font(.system(size: 13))
                                .foregroundColor(AppColors.textSecondary)
                        }
                        Text("Your \(pointsStore.balance) points can be redeemed for \(PointsConfig.formattedRupees(for: pointsStore.balance)) in real money.")
                            .font(.system(size: 13))
                            .foregroundColor(AppColors.textSecondary)
                        Button(action: {
                            redeemAmount = ""
                            redeemMessage = nil
                            showRedeem = true
                        }) {
                            Text("Redeem points")
                                .font(.system(size: 16, weight: .bold))
                                .foregroundColor(.white)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 12)
                                .background(AppColors.workout)
                                .clipShape(RoundedRectangle(cornerRadius: 14))
                        }
                        .disabled(pointsStore.balance <= 0)
                        .opacity(pointsStore.balance <= 0 ? 0.5 : 1.0)
                    }
                    .padding(16)
                    .background(Color.white)
                    .clipShape(RoundedRectangle(cornerRadius: 20))
                    .overlay(RoundedRectangle(cornerRadius: 20).stroke(AppColors.border, lineWidth: 1))

                    // How to earn
                    VStack(alignment: .leading, spacing: 8) {
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
                    VStack(alignment: .leading, spacing: 4) {
                        Text("History")
                            .font(.system(size: 15, weight: .bold))
                            .foregroundColor(AppColors.textPrimary)
                            .padding(.horizontal, 4)
                            .padding(.bottom, 4)
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
            .sheet(isPresented: $showRedeem) {
                redeemSheet
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
            Image(systemName: entry.isRedemption ? "arrow.down.circle.fill" : "arrow.up.circle.fill")
                .font(.system(size: 22))
                .foregroundColor(entry.isRedemption ? AppColors.danger : AppColors.workout)
            VStack(alignment: .leading, spacing: 2) {
                Text(entry.reason)
                    .font(.system(size: 14, weight: .medium))
                    .foregroundColor(AppColors.textPrimary)
                Text(entry.date, style: .date)
                    .font(.system(size: 12))
                    .foregroundColor(AppColors.textSecondary)
            }
            Spacer()
            VStack(alignment: .trailing, spacing: 2) {
                Text("\(entry.points > 0 ? "+" : "")\(entry.points)")
                    .font(.system(size: 15, weight: .bold))
                    .foregroundColor(entry.isRedemption ? AppColors.danger : AppColors.workout)
                if let rupees = entry.rupeeValue {
                    Text(String(format: "\u{20B9}%.2f", rupees))
                        .font(.system(size: 12))
                        .foregroundColor(AppColors.textSecondary)
                }
            }
        }
        .padding(12)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 14))
        .overlay(RoundedRectangle(cornerRadius: 14).stroke(AppColors.border, lineWidth: 1))
    }

    private var redeemSheet: some View {
        NavigationView {
            VStack(spacing: 16) {
                Text("Enter the points you want to redeem. 100 points = \u{20B9}1.")
                    .font(.system(size: 14))
                    .foregroundColor(AppColors.textSecondary)
                    .frame(maxWidth: .infinity, alignment: .leading)

                TextField("Points", text: $redeemAmount)
                    .keyboardType(.numberPad)
                    .font(.system(size: 20, weight: .bold))
                    .padding(14)
                    .background(Color.white)
                    .clipShape(RoundedRectangle(cornerRadius: 14))
                    .overlay(RoundedRectangle(cornerRadius: 14).stroke(AppColors.border, lineWidth: 1))

                let amount = Int(redeemAmount) ?? 0
                Text("You will receive \(PointsConfig.formattedRupees(for: amount))")
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundColor(AppColors.textPrimary)

                if let message = redeemMessage {
                    Text(message)
                        .font(.system(size: 14))
                        .foregroundColor(AppColors.textSecondary)
                }

                Button(action: confirmRedeem) {
                    Text("Confirm redemption")
                        .font(.system(size: 16, weight: .bold))
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .background(AppColors.workout)
                        .clipShape(RoundedRectangle(cornerRadius: 14))
                }
                .disabled(amount <= 0 || amount > pointsStore.balance)
                .opacity(amount <= 0 || amount > pointsStore.balance ? 0.5 : 1.0)

                Spacer()
            }
            .padding(20)
            .background(AppColors.surface.ignoresSafeArea())
            .navigationTitle("Redeem points")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Cancel") { showRedeem = false }
                }
            }
        }
    }

    private func confirmRedeem() {
        guard let amount = Int(redeemAmount), amount > 0 else { return }
        if let rupees = pointsStore.redeem(points: amount) {
            redeemMessage = String(format: "Redeemed %d points for \u{20B9}%.2f.", amount, rupees)
            redeemAmount = ""
        } else {
            redeemMessage = "Not enough points for that amount."
        }
    }
}
