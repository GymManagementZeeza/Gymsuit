import SwiftUI

struct WorkoutsView: View {
    @ObservedObject private var store = WorkoutStore.shared

    @State private var segment: WorkoutSegment = .exercises
    @State private var searchQuery: String = ""
    @State private var selectedGroup: String? = nil
    @State private var loggingExercise: Exercise? = nil

    enum WorkoutSegment: String, CaseIterable {
        case exercises = "Exercises"
        case history = "History"
    }

    var body: some View {
        VStack(spacing: 0) {
            // Header
            HStack {
                Text("Workouts")
                    .font(.system(size: 24, weight: .bold))
                    .foregroundColor(AppColors.textPrimary)
                Spacer()
            }
            .padding(.horizontal, 20)
            .padding(.top, 10)
            .padding(.bottom, 12)

            // Segment control
            Picker("", selection: $segment) {
                ForEach(WorkoutSegment.allCases, id: \.self) { seg in
                    Text(seg.rawValue).tag(seg)
                }
            }
            .pickerStyle(.segmented)
            .padding(.horizontal, 20)
            .padding(.bottom, 12)

            if segment == .exercises {
                exercisesContent
            } else {
                historyContent
            }
        }
        .sheet(item: $loggingExercise) { exercise in
            WorkoutLogView(exercise: exercise)
        }
    }

    // MARK: - Exercises

    private var exercisesContent: some View {
        VStack(spacing: 0) {
            // Search
            HStack {
                Image(systemName: "magnifyingglass")
                    .foregroundColor(AppColors.textTertiary)
                TextField("Search exercises", text: $searchQuery)
                    .font(.system(size: 15))
            }
            .padding(12)
            .background(Color.white)
            .clipShape(RoundedRectangle(cornerRadius: 16))
            .padding(.horizontal, 20)
            .padding(.bottom, 10)

            // Muscle group chips
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    muscleChip(label: "All", selected: selectedGroup == nil) {
                        selectedGroup = nil
                    }
                    ForEach(ExerciseMuscleGroups.all, id: \.self) { group in
                        muscleChip(label: group, selected: selectedGroup == group) {
                            selectedGroup = (selectedGroup == group) ? nil : group
                        }
                    }
                }
                .padding(.horizontal, 20)
            }
            .padding(.bottom, 10)

            // Exercise list
            let results = store.filteredExercises(query: searchQuery, muscleGroup: selectedGroup)
            if results.isEmpty {
                Spacer()
                Text(store.exercises.isEmpty ? "Exercise catalog failed to load." : "No exercises match your search.")
                    .font(.system(size: 14))
                    .foregroundColor(AppColors.textSecondary)
                Spacer()
            } else {
                ScrollView {
                    LazyVStack(spacing: 10) {
                        ForEach(results) { exercise in
                            Button(action: { loggingExercise = exercise }) {
                                exerciseRow(exercise)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    .padding(.horizontal, 20)
                    .padding(.bottom, 20)
                }
            }
        }
    }

    private func muscleChip(label: String, selected: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(label)
                .font(.system(size: 13, weight: selected ? .bold : .medium))
                .foregroundColor(selected ? .white : AppColors.textPrimary)
                .padding(.horizontal, 14)
                .padding(.vertical, 8)
                .background(selected ? AppColors.primary : Color.white)
                .clipShape(Capsule())
                .shadow(color: Color.black.opacity(0.04), radius: 3, y: 1)
        }
    }

    private func exerciseRow(_ exercise: Exercise) -> some View {
        HStack(spacing: 12) {
            ZStack {
                RoundedRectangle(cornerRadius: 14)
                    .fill(AppColors.workout.opacity(0.12))
                    .frame(width: 48, height: 48)
                Image(systemName: "dumbbell.fill")
                    .foregroundColor(AppColors.workout)
            }
            VStack(alignment: .leading, spacing: 4) {
                Text(exercise.name)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundColor(AppColors.textPrimary)
                    .lineLimit(1)
                Text("\(exercise.primaryMuscle) • \(exercise.equipmentLabel)")
                    .font(.system(size: 12))
                    .foregroundColor(AppColors.textSecondary)
                    .lineLimit(1)
            }
            Spacer()
            difficultyDots(exercise.difficulty)
            Image(systemName: "chevron.right")
                .font(.system(size: 13, weight: .semibold))
                .foregroundColor(AppColors.textTertiary)
        }
        .padding(12)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 18))
        .shadow(color: Color.black.opacity(0.03), radius: 5, y: 2)
    }

    private func difficultyDots(_ level: Int) -> some View {
        HStack(spacing: 3) {
            ForEach(1...5, id: \.self) { i in
                Circle()
                    .fill(i <= level ? AppColors.workout : AppColors.divider)
                    .frame(width: 6, height: 6)
            }
        }
    }

    // MARK: - History

    private var historyContent: some View {
        Group {
            if store.loggedWorkouts.isEmpty {
                VStack(spacing: 12) {
                    Spacer()
                    Image(systemName: "dumbbell.fill")
                        .font(.system(size: 48))
                        .foregroundColor(AppColors.workout.opacity(0.4))
                    Text("No workouts logged yet")
                        .font(.system(size: 17, weight: .bold))
                        .foregroundColor(AppColors.textPrimary)
                    Text("Pick an exercise and log your sets to start tracking.")
                        .font(.system(size: 14))
                        .foregroundColor(AppColors.textSecondary)
                    Spacer()
                }
            } else {
                ScrollView {
                    LazyVStack(spacing: 10) {
                        ForEach(store.loggedWorkouts) { workout in
                            historyRow(workout)
                        }
                    }
                    .padding(.horizontal, 20)
                    .padding(.bottom, 20)
                }
            }
        }
    }

    private func historyRow(_ workout: LoggedWorkout) -> some View {
        HStack(spacing: 12) {
            VStack(alignment: .leading, spacing: 4) {
                Text(workout.exerciseName)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundColor(AppColors.textPrimary)
                Text("\(workout.sets.count) sets • \(workout.totalReps) reps • \(workout.durationMinutes) min")
                    .font(.system(size: 12))
                    .foregroundColor(AppColors.textSecondary)
                Text(workout.date, style: .date)
                    .font(.system(size: 12))
                    .foregroundColor(AppColors.textTertiary)
            }
            Spacer()
            VStack(alignment: .trailing, spacing: 4) {
                Text("\(Int(workout.totalVolumeKg)) kg")
                    .font(.system(size: 14, weight: .bold))
                    .foregroundColor(AppColors.textPrimary)
                HStack(spacing: 4) {
                    Image(systemName: workout.syncedToHealthKit ? "checkmark.circle.fill" : "exclamationmark.circle.fill")
                        .font(.system(size: 11))
                    Text(workout.syncedToHealthKit ? "Synced" : "Not synced")
                        .font(.system(size: 11))
                }
                .foregroundColor(workout.syncedToHealthKit ? AppColors.success : AppColors.warning)
            }
            Button(action: { store.deleteWorkout(id: workout.id) }) {
                Image(systemName: "trash")
                    .font(.system(size: 14))
                    .foregroundColor(AppColors.danger.opacity(0.7))
            }
            .buttonStyle(.plain)
        }
        .padding(14)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 18))
        .shadow(color: Color.black.opacity(0.03), radius: 5, y: 2)
    }
}
