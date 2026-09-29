import SwiftUI

struct HealthPermissionView: View {
    var onContinue: () -> Void
    var onSkip: () -> Void
    
    @State private var isRequesting: Bool = false
    private let healthKit = HealthKitManager.shared
    
    var body: some View {
        ZStack {
            AppColors.surface.ignoresSafeArea()
            
            VStack(spacing: 32) {
                Spacer()
                
                // Pulsing Icon
                ZStack {
                    Circle()
                        .fill(AppColors.primaryLight)
                        .frame(width: 120, height: 120)
                    
                    Image(systemName: "heart.fill")
                        .font(.system(size: 54))
                        .foregroundColor(AppColors.primary)
                }
                
                VStack(spacing: 12) {
                    Text("Sync Apple Health")
                        .font(.system(size: 28, weight: .bold))
                        .foregroundColor(AppColors.textPrimary)
                    
                    Text("Connect with Apple Health to automatically sync your steps, heart rate, calories burned, and sleep sessions in real-time.")
                        .font(.system(size: 15))
                        .foregroundColor(AppColors.textSecondary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 24)
                }
                
                // Permission features list
                VStack(spacing: 16) {
                    FeatureRow(icon: "flame.fill", title: "Active Burn & Calories", subtitle: "Track daily energy output & resting metabolic rate", tint: AppColors.calories)
                    FeatureRow(icon: "figure.walk", title: "Steps & Distance", subtitle: "Record walking and running distances accurately", tint: AppColors.steps)
                    FeatureRow(icon: "bed.double.fill", title: "Sleep Analysis", subtitle: "Awake, Light, Deep, and REM hypnogram breakdown", tint: AppColors.sleep)
                    FeatureRow(icon: "heart.fill", title: "Heart Rate & HRV", subtitle: "Continuous pulse and heart rate variability tracking", tint: AppColors.heartRate)
                }
                .padding(.horizontal, 24)
                
                Spacer()
                
                VStack(spacing: 12) {
                    Button(action: requestPermissions) {
                        HStack {
                            if isRequesting {
                                ProgressView().progressViewStyle(CircularProgressViewStyle(tint: .white))
                            } else {
                                Text("Connect Apple Health")
                                    .font(.system(size: 16, weight: .semibold))
                            }
                        }
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .frame(height: 54)
                        .background(AppColors.primary)
                        .clipShape(Capsule())
                    }
                    .disabled(isRequesting)
                    
                    Button(action: onSkip) {
                        Text("Maybe Later")
                            .font(.system(size: 15, weight: .medium))
                            .foregroundColor(AppColors.textSecondary)
                    }
                    .padding(.top, 4)
                }
                .padding(.horizontal, 24)
                .padding(.bottom, 24)
            }
        }
    }
    
    private func requestPermissions() {
        isRequesting = true
        Task {
            _ = await healthKit.requestAuthorization()
            await MainActor.run {
                self.isRequesting = false
                self.onContinue()
            }
        }
    }
}

private struct FeatureRow: View {
    let icon: String
    let title: String
    let subtitle: String
    let tint: Color
    
    var body: some View {
        HStack(spacing: 16) {
            ZStack {
                Circle()
                    .fill(tint.opacity(0.12))
                    .frame(width: 44, height: 44)
                Image(systemName: icon)
                    .font(.system(size: 18))
                    .foregroundColor(tint)
            }
            
            VStack(alignment: .leading, spacing: 3) {
                Text(title)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundColor(AppColors.textPrimary)
                Text(subtitle)
                    .font(.system(size: 12))
                    .foregroundColor(AppColors.textSecondary)
            }
            Spacer()
        }
        .padding(14)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }
}
