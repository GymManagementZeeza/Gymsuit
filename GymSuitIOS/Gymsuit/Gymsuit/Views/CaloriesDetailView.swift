import SwiftUI

struct CaloriesDetailView: View {
    let date: Date
    var onBack: () -> Void
    
    @State private var caloriesData: DetailedCaloriesData?
    @State private var targetKcal: Double = 4000.0
    @State private var showGoalDialog: Bool = false
    @State private var inputGoalText: String = "4000"
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
                    
                    Text("Calorie Burn")
                        .font(.system(size: 18, weight: .bold))
                        .foregroundColor(AppColors.textPrimary)
                    
                    Spacer()
                    
                    Button(action: {
                        inputGoalText = String(Int(targetKcal))
                        showGoalDialog = true
                    }) {
                        Image(systemName: "pencil")
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(AppColors.primary)
                            .frame(width: 44, height: 44)
                            .background(Color.white)
                            .clipShape(Circle())
                            .shadow(color: Color.black.opacity(0.04), radius: 4, y: 2)
                    }
                }
                .padding(.horizontal, 20)
                .padding(.top, 10)
                .padding(.bottom, 12)
                
                ScrollView {
                    VStack(spacing: 20) {
                        if let data = caloriesData {
                            // Donut Ring Chart Card
                            VStack(spacing: 20) {
                                ZStack {
                                    Circle()
                                        .stroke(Color(hex: 0xFFEDE9FE), lineWidth: 22)
                                        .frame(width: 200, height: 200)
                                    
                                    let progress = min(1.0, data.totalCaloriesKcal / max(1.0, data.targetKcal))
                                    Circle()
                                        .trim(from: 0.0, to: CGFloat(progress))
                                        .stroke(
                                            AngularGradient(
                                                gradient: Gradient(colors: [Color(hex: 0xFF7C3AED), Color(hex: 0xFF4F46E5)]),
                                                center: .center
                                            ),
                                            style: StrokeStyle(lineWidth: 22, lineCap: .round)
                                        )
                                        .rotationEffect(.degrees(-90))
                                        .frame(width: 200, height: 200)
                                        .animation(.easeOut(duration: 0.8), value: progress)
                                    
                                    VStack(spacing: 4) {
                                        Text("\(Int(data.totalCaloriesKcal))")
                                            .font(.system(size: 34, weight: .bold))
                                            .foregroundColor(AppColors.textPrimary)
                                        Text("/ \(Int(data.targetKcal)) kcal")
                                            .font(.system(size: 14, weight: .medium))
                                            .foregroundColor(AppColors.textSecondary)
                                    }
                                }
                                .padding(.top, 10)
                                
                                Text("Burned \(Int((data.totalCaloriesKcal / max(1, data.targetKcal)) * 100))% of your daily target")
                                    .font(.system(size: 14, weight: .medium))
                                    .foregroundColor(AppColors.textSecondary)
                            }
                            .padding(24)
                            .frame(maxWidth: .infinity)
                            .background(Color.white)
                            .clipShape(RoundedRectangle(cornerRadius: 28))
                            .shadow(color: Color.black.opacity(0.03), radius: 6, y: 3)
                            
                            // Activity Breakdown List
                            VStack(alignment: .leading, spacing: 16) {
                                Text("Activity Breakdown")
                                    .font(.system(size: 17, weight: .bold))
                                    .foregroundColor(AppColors.textPrimary)
                                
                                VStack(spacing: 12) {
                                    ForEach(data.activities) { act in
                                        HStack(spacing: 14) {
                                            Circle()
                                                .fill(Color(hex: act.colorHex))
                                                .frame(width: 12, height: 12)
                                            
                                            VStack(alignment: .leading, spacing: 2) {
                                                Text(act.name)
                                                    .font(.system(size: 15, weight: .semibold))
                                                    .foregroundColor(AppColors.textPrimary)
                                                Text(act.durationOrCount)
                                                    .font(.system(size: 12))
                                                    .foregroundColor(AppColors.textSecondary)
                                            }
                                            
                                            Spacer()
                                            
                                            VStack(alignment: .trailing, spacing: 2) {
                                                Text("\(Int(act.caloriesKcal)) kcal")
                                                    .font(.system(size: 15, weight: .bold))
                                                    .foregroundColor(AppColors.textPrimary)
                                                Text("\(act.percentage)%")
                                                    .font(.system(size: 12))
                                                    .foregroundColor(AppColors.textSecondary)
                                            }
                                        }
                                        .padding(.vertical, 6)
                                        if act.id != data.activities.last?.id {
                                            Divider()
                                        }
                                    }
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
        .alert("Set Daily Calorie Target", isPresented: $showGoalDialog) {
            TextField("4000", text: $inputGoalText)
                .keyboardType(.numberPad)
            Button("Cancel", role: .cancel) {}
            Button("Save") {
                if let val = Double(inputGoalText), val > 0 {
                    targetKcal = val
                    Task {
                        self.caloriesData = await healthKit.fetchDetailedCalories(for: date, targetKcal: targetKcal)
                    }
                }
            }
        } message: {
            Text("Enter your daily active calorie burn target in kcal.")
        }
        .task {
            self.caloriesData = await healthKit.fetchDetailedCalories(for: date, targetKcal: targetKcal)
            self.isLoading = false
        }
    }
}
