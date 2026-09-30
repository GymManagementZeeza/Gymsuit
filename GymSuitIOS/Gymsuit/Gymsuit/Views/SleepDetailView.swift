import SwiftUI

struct SleepDetailView: View {
    let date: Date
    var onBack: () -> Void
    
    @State private var detailedSleep: DetailedSleepData?
    @State private var isLoading: Bool = true
    
    private let healthKit = HealthKitManager.shared
    
    var body: some View {
        ZStack {
            Color(hex: 0xFFF3F2F8).ignoresSafeArea()
            
            VStack(spacing: 0) {
                // Top App Bar
                HStack {
                    Button(action: onBack) {
                        Image(systemName: "chevron.left")
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(AppColors.textPrimary)
                            .frame(width: 44, height: 44)
                            .background(Color.white)
                            .clipShape(Circle())
                            .shadow(color: Color.black.opacity(0.04), radius: 4, y: 2)
                    }
                    
                    Spacer()
                    
                    Text("Sleep Analysis")
                        .font(.system(size: 18, weight: .bold))
                        .foregroundColor(AppColors.textPrimary)
                    
                    Spacer()
                    
                    Image("avatar_gaze_1")
                        .resizable()
                        .scaledToFill()
                        .frame(width: 44, height: 44)
                        .clipShape(Circle())
                }
                .padding(.horizontal, 20)
                .padding(.top, 10)
                .padding(.bottom, 12)
                
                ScrollView {
                    VStack(spacing: 20) {
                        if let sleep = detailedSleep {
                            let insight = SleepAiSummaryEngine.generateInsight(from: sleep)
                            
                            // AI Insight Card
                            VStack(alignment: .leading, spacing: 10) {
                                HStack {
                                    Text(insight.badgeText.uppercased())
                                        .font(.system(size: 11, weight: .bold))
                                        .foregroundColor(AppColors.sleep)
                                        .padding(.horizontal, 10)
                                        .padding(.vertical, 4)
                                        .background(AppColors.purpleLight)
                                        .clipShape(Capsule())
                                    Spacer()
                                    Image(systemName: "sparkles")
                                        .foregroundColor(AppColors.sleep)
                                }
                                
                                Text(insight.headline)
                                    .font(.system(size: 20, weight: .bold))
                                    .foregroundColor(AppColors.textPrimary)
                                
                                Text(insight.description)
                                    .font(.system(size: 14))
                                    .foregroundColor(AppColors.textSecondary)
                                    .lineSpacing(4)
                            }
                            .padding(20)
                            .background(Color.white)
                            .clipShape(RoundedRectangle(cornerRadius: 24))
                            .shadow(color: Color.black.opacity(0.03), radius: 6, y: 3)
                            
                            // Sleep Duration Header
                            HStack {
                                VStack(alignment: .leading, spacing: 4) {
                                    Text("Total Sleep")
                                        .font(.system(size: 14))
                                        .foregroundColor(AppColors.textSecondary)
                                    Text("\(sleep.totalSleepMinutes / 60)h \(sleep.totalSleepMinutes % 60)m")
                                        .font(.system(size: 32, weight: .bold))
                                        .foregroundColor(AppColors.textPrimary)
                                }
                                Spacer()
                                VStack(alignment: .trailing, spacing: 4) {
                                    Text("Schedule")
                                        .font(.system(size: 14))
                                        .foregroundColor(AppColors.textSecondary)
                                    Text("\(sleep.startTimeFormatted) – \(sleep.endTimeFormatted)")
                                        .font(.system(size: 15, weight: .semibold))
                                        .foregroundColor(AppColors.textPrimary)
                                }
                            }
                            .padding(20)
                            .background(Color.white)
                            .clipShape(RoundedRectangle(cornerRadius: 24))
                            
                            // Hypnogram / Sleep Stages Timeline
                            VStack(alignment: .leading, spacing: 16) {
                                Text("Sleep Stages")
                                    .font(.system(size: 17, weight: .bold))
                                    .foregroundColor(AppColors.textPrimary)
                                
                                // Color Stage Bar
                                GeometryReader { geo in
                                    HStack(spacing: 2) {
                                        ForEach(sleep.stages) { stage in
                                            let width = geo.size.width * CGFloat(stage.endFraction - stage.startFraction)
                                            stageColor(stage.stage)
                                                .frame(width: max(4, width), height: 32)
                                                .clipShape(RoundedRectangle(cornerRadius: 4))
                                        }
                                    }
                                }
                                .frame(height: 32)
                                
                                // Time markers
                                HStack {
                                    Text(sleep.startTimeFormatted)
                                    Spacer()
                                    Text(sleep.midTimeFormatted)
                                    Spacer()
                                    Text(sleep.endTimeFormatted)
                                }
                                .font(.system(size: 12, weight: .medium))
                                .foregroundColor(AppColors.textTertiary)
                                
                                Divider()
                                
                                // Breakdown Rows
                                VStack(spacing: 12) {
                                    SleepMetricRow(label: "Deep Sleep", minutes: sleep.deepMinutes, color: Color(hex: 0xFF3B2F7E), total: sleep.totalSleepMinutes)
                                    SleepMetricRow(label: "REM Sleep", minutes: sleep.remMinutes, color: Color(hex: 0xFF6366F1), total: sleep.totalSleepMinutes)
                                    SleepMetricRow(label: "Light Sleep", minutes: sleep.lightMinutes, color: Color(hex: 0xFFA5B4FC), total: sleep.totalSleepMinutes)
                                    SleepMetricRow(label: "Awake / Restless", minutes: sleep.awakeMinutes, color: Color(hex: 0xFFFCA5A5), total: sleep.totalSleepMinutes)
                                }
                            }
                            .padding(20)
                            .background(Color.white)
                            .clipShape(RoundedRectangle(cornerRadius: 24))
                            .shadow(color: Color.black.opacity(0.03), radius: 6, y: 3)
                        } else if isLoading {
                            ProgressView()
                                .padding(.top, 60)
                        } else {
                            Text("No sleep data for this day yet.")
                                .font(.system(size: 14))
                                .foregroundColor(AppColors.textSecondary)
                                .padding(.top, 60)
                        }
                    }
                    .padding(20)
                }
            }
        }
        .task {
            self.detailedSleep = await healthKit.fetchDetailedSleep(for: date)
            self.isLoading = false
        }
    }
    
    private func stageColor(_ stage: SleepStageType) -> Color {
        switch stage {
        case .deep: return Color(hex: 0xFF3B2F7E)
        case .rem: return Color(hex: 0xFF6366F1)
        case .light: return Color(hex: 0xFFA5B4FC)
        case .awake: return Color(hex: 0xFFFCA5A5)
        }
    }
}

private struct SleepMetricRow: View {
    let label: String
    let minutes: Int64
    let color: Color
    let total: Int64
    
    var body: some View {
        let pct = total > 0 ? Int((Double(minutes) / Double(total)) * 100) : 0
        HStack {
            Circle()
                .fill(color)
                .frame(width: 10, height: 10)
            Text(label)
                .font(.system(size: 14, weight: .medium))
                .foregroundColor(AppColors.textPrimary)
            Spacer()
            Text("\(minutes / 60)h \(minutes % 60)m")
                .font(.system(size: 14, weight: .semibold))
                .foregroundColor(AppColors.textPrimary)
            Text("(\(pct)%)")
                .font(.system(size: 13))
                .foregroundColor(AppColors.textSecondary)
                .frame(width: 45, alignment: .trailing)
        }
    }
}
