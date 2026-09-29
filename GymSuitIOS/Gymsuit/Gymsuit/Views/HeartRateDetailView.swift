import SwiftUI

struct HeartRateDetailView: View {
    let date: Date
    var onBack: () -> Void
    
    @State private var hrvData: DetailedHrvData?
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
                    
                    Text("Heart Rate & HRV")
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
                    VStack(spacing: 18) {
                        if let hrv = hrvData {
                            // HRV Bead / Scatter Graph Card
                            VStack(alignment: .leading, spacing: 16) {
                                HStack {
                                    VStack(alignment: .leading, spacing: 4) {
                                        Text("Heart Rate Variability")
                                            .font(.system(size: 17, weight: .bold))
                                            .foregroundColor(AppColors.textPrimary)
                                        Text("Daily autonomic nervous system balance")
                                            .font(.system(size: 13))
                                            .foregroundColor(AppColors.textSecondary)
                                    }
                                    Spacer()
                                    Text("\(hrv.latestHrvMs) ms")
                                        .font(.system(size: 28, weight: .bold))
                                        .foregroundColor(Color(hex: 0xFFDC2626))
                                }
                                
                                // Vertical Bead Scatter Columns
                                HStack(alignment: .bottom, spacing: 12) {
                                    ForEach(hrv.buckets) { bucket in
                                        VStack(spacing: 8) {
                                            ZStack(alignment: .bottom) {
                                                Capsule()
                                                    .fill(bucket.isHighlighted ? Color(hex: 0xFFFEE2E2) : Color(hex: 0xFFF1F5F9))
                                                    .frame(width: 32, height: 140)
                                                
                                                VStack(spacing: 6) {
                                                    ForEach(0..<bucket.samples.count, id: \.self) { idx in
                                                        Circle()
                                                            .fill(bucket.isHighlighted ? Color(hex: 0xFFEF4444) : Color(hex: 0xFF94A3B8))
                                                            .frame(width: 8, height: 8)
                                                    }
                                                }
                                                .padding(.bottom, 16)
                                            }
                                            
                                            Text(bucket.hourLabel)
                                                .font(.system(size: 11, weight: bucket.isHighlighted ? .bold : .medium))
                                                .foregroundColor(bucket.isHighlighted ? Color(hex: 0xFFDC2626) : AppColors.textTertiary)
                                        }
                                        .frame(maxWidth: .infinity)
                                    }
                                }
                                .padding(.vertical, 10)
                            }
                            .padding(20)
                            .background(Color.white)
                            .clipShape(RoundedRectangle(cornerRadius: 24))
                            .shadow(color: Color.black.opacity(0.03), radius: 6, y: 3)
                            
                            // Metrics Pair Cards
                            HStack(spacing: 14) {
                                MetricCard(title: "Stress Level", value: hrv.stressLevel, subtitle: "Normal baseline", icon: "waveform.path.ecg", color: Color(hex: 0xFF10B981))
                                MetricCard(title: "Ave. Variability", value: "\(hrv.aveVariabilityMs) ms", subtitle: "Resting average", icon: "heart.fill", color: Color(hex: 0xFF6366F1))
                            }
                            
                            // Pulse Range Card
                            VStack(alignment: .leading, spacing: 14) {
                                Text("Heart Rate Summary")
                                    .font(.system(size: 17, weight: .bold))
                                    .foregroundColor(AppColors.textPrimary)
                                
                                HStack(spacing: 20) {
                                    PulseItem(label: "Current", bpm: "\(hrv.latestBpm) BPM", color: Color(hex: 0xFFEF4444))
                                    PulseItem(label: "Min Resting", bpm: "\(hrv.minBpm) BPM", color: Color(hex: 0xFF3B82F6))
                                    PulseItem(label: "Max Peak", bpm: "\(hrv.maxBpm) BPM", color: Color(hex: 0xFFF59E0B))
                                }
                            }
                            .padding(20)
                            .background(Color.white)
                            .clipShape(RoundedRectangle(cornerRadius: 24))
                            .shadow(color: Color.black.opacity(0.03), radius: 6, y: 3)
                        } else {
                            ProgressView()
                                .padding(.top, 60)
                        }
                    }
                    .padding(20)
                }
            }
        }
        .task {
            self.hrvData = await healthKit.fetchDetailedHrv(for: date)
            self.isLoading = false
        }
    }
}

private struct MetricCard: View {
    let title: String
    let value: String
    let subtitle: String
    let icon: String
    let color: Color
    
    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Image(systemName: icon)
                    .foregroundColor(color)
                Spacer()
            }
            Text(title)
                .font(.system(size: 13, weight: .medium))
                .foregroundColor(AppColors.textSecondary)
            Text(value)
                .font(.system(size: 22, weight: .bold))
                .foregroundColor(AppColors.textPrimary)
            Text(subtitle)
                .font(.system(size: 11))
                .foregroundColor(AppColors.textTertiary)
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 20))
        .shadow(color: Color.black.opacity(0.03), radius: 6, y: 3)
    }
}

private struct PulseItem: View {
    let label: String
    let bpm: String
    let color: Color
    
    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label)
                .font(.system(size: 12))
                .foregroundColor(AppColors.textSecondary)
            Text(bpm)
                .font(.system(size: 18, weight: .bold))
                .foregroundColor(color)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}
