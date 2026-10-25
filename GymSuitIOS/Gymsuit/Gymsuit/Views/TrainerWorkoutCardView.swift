import SwiftUI

/// Top card on the workouts page: the workout the member's trainer sent for today.
/// Renders nothing when no workout was sent today.
struct TrainerWorkoutCardView: View {
    @State private var suggestion: TrainerWorkoutSuggestion?

    var body: some View {
        Group {
            if let suggestion {
                VStack(alignment: .leading, spacing: 12) {
                    HStack(spacing: 10) {
                        Image(systemName: "figure.strengthtraining.traditional")
                            .font(.system(size: 20, weight: .semibold))
                            .foregroundColor(Color(hex: 0xFF395CBD))
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Today's workout")
                                .font(.system(size: 16, weight: .bold))
                                .foregroundColor(AppColors.textPrimary)
                            Text("From \(suggestion.trainerName)")
                                .font(.system(size: 12))
                                .foregroundColor(AppColors.textSecondary)
                        }
                        Spacer()
                    }

                    if let message = suggestion.message, !message.isEmpty {
                        Text(message)
                            .font(.system(size: 13))
                            .foregroundColor(AppColors.textSecondary)
                    }

                    ForEach(suggestion.exercises) { exercise in
                        HStack {
                            Text(exercise.exerciseName)
                                .font(.system(size: 14, weight: .medium))
                                .foregroundColor(AppColors.textPrimary)
                            Spacer()
                            Text("\(exercise.sets) × \(exercise.reps)")
                                .font(.system(size: 13))
                                .foregroundColor(AppColors.textSecondary)
                                .padding(.trailing, 12)
                            Text(weightText(exercise.weightKg))
                                .font(.system(size: 13, weight: .semibold))
                                .foregroundColor(Color(hex: 0xFF395CBD))
                        }
                    }
                }
                .padding(16)
                .background(Color.white)
                .clipShape(RoundedRectangle(cornerRadius: 20))
                .shadow(color: Color.black.opacity(0.06), radius: 4, y: 2)
            }
        }
        .task {
            suggestion = try? await TrainerWorkoutApi.shared.fetchToday()
        }
    }

    private func weightText(_ weightKg: Double) -> String {
        if weightKg <= 0 {
            return "Bodyweight"
        }
        if weightKg.truncatingRemainder(dividingBy: 1) == 0 {
            return "\(Int(weightKg)) kg"
        }
        return "\(weightKg) kg"
    }
}
