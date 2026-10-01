import SwiftUI

struct WorkoutLogView: View {
    let exercise: Exercise
    @Environment(\.dismiss) private var dismiss

    @State private var sets: [WorkoutSet]
    @State private var durationMinutes: Int
    @State private var logDate: Date = Date()
    @State private var notes: String = ""
    @State private var isSaving: Bool = false
    @State private var saveMessage: String? = nil
    @State private var repsText: [String]
    @State private var weightText: [String]

    init(exercise: Exercise, targetReps: Int = 10, targetWeightKg: Double = 0, setCount: Int = 3) {
        self.exercise = exercise
        let count = min(max(setCount, 1), 8)
        let initial = (0..<count).map { _ in WorkoutSet(reps: targetReps, weightKg: targetWeightKg) }
        _sets = State(initialValue: initial)
        _repsText = State(initialValue: initial.map { String($0.reps) })
        _weightText = State(initialValue: initial.map { String(format: "%g", $0.weightKg) })
        _durationMinutes = State(initialValue: 30)
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    // Exercise info
                    VStack(alignment: .leading, spacing: 8) {
                        Text(exercise.name)
                            .font(.system(size: 22, weight: .bold))
                            .foregroundColor(AppColors.textPrimary)
                        Text("\(exercise.primaryMuscleLabel) • \(exercise.equipment.capitalized)")
                            .font(.system(size: 13))
                            .foregroundColor(AppColors.textSecondary)
                        if let gifURL = exercise.gifURL {
                            GIFView(url: gifURL)
                                .frame(height: 220)
                                .clipShape(RoundedRectangle(cornerRadius: 12))
                        }
                        if !exercise.instructions.isEmpty {
                            Text("How to perform")
                                .font(.system(size: 15, weight: .bold))
                                .foregroundColor(AppColors.textPrimary)
                                .padding(.top, 4)
                            ForEach(Array(exercise.instructions.enumerated()), id: \.offset) { index, step in
                                HStack(alignment: .top, spacing: 8) {
                                    Text("\(index + 1).")
                                        .font(.system(size: 14, weight: .semibold))
                                        .foregroundColor(AppColors.workout)
                                    Text(step)
                                        .font(.system(size: 14))
                                        .foregroundColor(AppColors.textSecondary)
                                }
                            }
                        }
                    }
                    .padding(16)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color.white)
                    .clipShape(RoundedRectangle(cornerRadius: 18))

                    // Sets
                    VStack(alignment: .leading, spacing: 8) {
                        HStack {
                            Text("Sets")
                                .font(.system(size: 16, weight: .bold))
                                .foregroundColor(AppColors.textPrimary)
                            Spacer()
                            Button(action: addSet) {
                                Label("Add set", systemImage: "plus.circle.fill")
                                    .font(.system(size: 13, weight: .semibold))
                                    .foregroundColor(AppColors.workout)
                            }
                        }
                        ForEach(sets.indices, id: \.self) { index in
                            HStack(spacing: 10) {
                                Text("Set \(index + 1)")
                                    .font(.system(size: 14, weight: .semibold))
                                    .foregroundColor(AppColors.textPrimary)
                                    .frame(width: 52, alignment: .leading)
                                VStack(alignment: .leading, spacing: 2) {
                                    Text("Reps")
                                        .font(.system(size: 11))
                                        .foregroundColor(AppColors.textTertiary)
                                    TextField("0", text: $repsText[index])
                                        .keyboardType(.numberPad)
                                        .textFieldStyle(.roundedBorder)
                                        .frame(width: 70)
                                        .onChange(of: repsText[index]) { _, newValue in
                                            sets[index].reps = Int(newValue) ?? 0
                                        }
                                }
                                VStack(alignment: .leading, spacing: 2) {
                                    Text("Weight (kg)")
                                        .font(.system(size: 11))
                                        .foregroundColor(AppColors.textTertiary)
                                    TextField("0", text: $weightText[index])
                                        .keyboardType(.decimalPad)
                                        .textFieldStyle(.roundedBorder)
                                        .frame(width: 80)
                                        .onChange(of: weightText[index]) { _, newValue in
                                            sets[index].weightKg = Double(newValue) ?? 0
                                        }
                                }
                                Spacer()
                                if sets.count > 1 {
                                    Button(action: { removeSet(at: index) }) {
                                        Image(systemName: "minus.circle.fill")
                                            .foregroundColor(AppColors.danger.opacity(0.7))
                                    }
                                }
                            }
                            .padding(10)
                            .background(AppColors.surface)
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                        }
                    }
                    .padding(16)
                    .background(Color.white)
                    .clipShape(RoundedRectangle(cornerRadius: 18))

                    // Details
                    VStack(spacing: 12) {
                        HStack {
                            Text("Duration")
                                .font(.system(size: 14, weight: .semibold))
                                .foregroundColor(AppColors.textPrimary)
                            Spacer()
                            Stepper("\(durationMinutes) min", value: $durationMinutes, in: 1...300)
                                .font(.system(size: 14))
                        }
                        DatePicker("Date", selection: $logDate, in: ...Date(), displayedComponents: [.date, .hourAndMinute])
                            .font(.system(size: 14))
                        TextField("Notes (optional)", text: $notes)
                            .textFieldStyle(.roundedBorder)
                            .font(.system(size: 14))
                    }
                    .padding(16)
                    .background(Color.white)
                    .clipShape(RoundedRectangle(cornerRadius: 18))

                    if let saveMessage {
                        Text(saveMessage)
                            .font(.system(size: 13))
                            .foregroundColor(AppColors.textSecondary)
                            .frame(maxWidth: .infinity)
                    }

                    Button(action: saveWorkout) {
                        HStack {
                            if isSaving { ProgressView().tint(.white) }
                            Text(isSaving ? "Saving..." : "Log Workout")
                                .font(.system(size: 16, weight: .bold))
                        }
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .background(sets.isEmpty ? AppColors.textTertiary : AppColors.workout)
                        .clipShape(RoundedRectangle(cornerRadius: 16))
                    }
                    .disabled(isSaving || sets.isEmpty)
                }
                .padding(20)
            }
            .background(AppColors.surface)
            .navigationTitle("Log Workout")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
            }
        }
    }

    private func addSet() {
        let last = sets.last
        sets.append(WorkoutSet(reps: last?.reps ?? 10, weightKg: last?.weightKg ?? 0))
        repsText.append(String(last?.reps ?? 10))
        weightText.append(String(format: "%g", last?.weightKg ?? 0))
    }

    private func removeSet(at index: Int) {
        sets.remove(at: index)
        repsText.remove(at: index)
        weightText.remove(at: index)
    }

    private func saveWorkout() {
        guard !isSaving else { return }
        isSaving = true
        let workout = LoggedWorkout(
            exerciseId: exercise.id,
            exerciseName: exercise.name,
            date: logDate,
            sets: sets,
            durationMinutes: durationMinutes,
            notes: notes,
            syncedToHealthKit: false
        )
        WorkoutStore.shared.logWorkout(workout)
        let earned = PointsStore.shared.awardForWorkout(
            exerciseName: exercise.name,
            setCount: sets.count,
            durationMinutes: durationMinutes
        )

        let end = logDate
        let start = end.addingTimeInterval(TimeInterval(-durationMinutes * 60))
        Task {
            let ok = await HealthKitManager.shared.saveWorkout(
                exerciseName: exercise.name,
                start: start,
                end: end,
                setCount: sets.count,
                totalReps: workout.totalReps,
                totalVolumeKg: workout.totalVolumeKg
            )
            await MainActor.run {
                if ok {
                    WorkoutStore.shared.markSynced(id: workout.id)
                    saveMessage = "Saved and synced to Apple Health. +\(earned) pts earned!"
                } else {
                    saveMessage = "Saved in GymSuit (+\(earned) pts). Apple Health sync needs workout write permission - grant it in the Health app."
                }
                isSaving = false
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.9) {
                    dismiss()
                }
            }
        }
    }
}
