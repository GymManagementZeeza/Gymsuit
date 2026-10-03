import SwiftUI

struct WorkoutsView: View {
    // Session state is owned by DashboardView (the tab container) and injected
    // here so an active guided session survives tab switches and locks navigation.
    @EnvironmentObject var session: WorkoutSessionState
    @ObservedObject private var store = WorkoutStore.shared

    @State private var isCatalogMode = false
    @State private var sessionPart: WorkoutBodyPart? = nil
    @State private var setLogExercise: Exercise? = nil
    @State private var searchQuery = ""
    @State private var selectedGroup: String? = nil
    @State private var showingHistory = false

    @State private var selectedCalendarDate = Date()
    @State private var aiRecommendation: WorkoutRecommendation? = nil
    @State private var isAiLoading = true
    @State private var healthSignals = HealthWorkoutSignals()
    @State private var selectedBodyParts: [WorkoutBodyPart] = []
    @State private var selectedSplitIndex = 0
    @State private var loggingTarget: LoggingTarget? = nil
    @State private var recommendGlow = false

    private let sessionTimer = Timer.publish(every: 1, on: .main, in: .common).autoconnect()

    private struct LoggingTarget: Identifiable {
        let id: String
        let exercise: Exercise
        let targetReps: Int
        let targetWeightKg: Double
        let setCount: Int
    }

    private static let dayNameFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "EE"
        return f
    }()

    private static let dayNumberFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "d"
        return f
    }()

    // MARK: - Router

    var body: some View {
        Group {
            if session.active, let exercise = setLogExercise {
                SetBySetLogPage(
                    exercise: exercise,
                    onBack: { setLogExercise = nil },
                    onFinished: { workout in
                        onWorkoutSaved(workout)
                        if let part = OnDeviceWorkoutAiEngine.bodyPartName(for: exercise) ?? sessionPart {
                            session.record(exercise: exercise, part: part, sets: workout.sets.count)
                        }
                        setLogExercise = nil
                    }
                )
                .id(exercise.id)
            } else if session.active, let part = sessionPart {
                BodyPartWorkoutPickerPage(
                    part: part,
                    onBack: { sessionPart = nil },
                    onExerciseClick: { setLogExercise = $0 }
                )
            } else if session.active {
                WorkoutSessionPage(
                    onFinish: {
                        session.end()
                        sessionPart = nil
                        setLogExercise = nil
                    },
                    onSelectBodyPart: { sessionPart = $0 }
                )
            } else if isCatalogMode {
                VStack(spacing: 0) {
                    workoutsHeader
                    catalogContent
                }
            } else {
                VStack(spacing: 0) {
                    workoutsHeader
                    splitContent
                }
            }
        }
        .background(AppColors.surface)
        .onReceive(sessionTimer) { _ in
            if session.active && session.running { session.tick() }
        }
        .task(id: aiTaskId) { await loadAiRecommendation() }
        .sheet(item: $loggingTarget) { target in
            NavigationStack {
                WorkoutLogView(
                    exercise: target.exercise,
                    targetReps: target.targetReps,
                    targetWeightKg: target.targetWeightKg,
                    setCount: target.setCount
                )
            }
        }
        .sheet(isPresented: $showingHistory) { historySheet }
    }

    // MARK: - Header

    private var workoutsHeader: some View {
        HStack(spacing: 12) {
            Text("Workouts")
                .font(.system(size: 28, weight: .bold))
                .foregroundColor(AppColors.textPrimary)
            Spacer()
            Button(action: { showingHistory = true }) {
                HStack(spacing: 6) {
                    Image(systemName: "clock.arrow.circlepath")
                        .font(.system(size: 14))
                    Text("History")
                        .font(.system(size: 14, weight: .semibold))
                }
                .foregroundColor(AppColors.primary)
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(AppColors.primaryLight)
                .clipShape(Capsule())
            }
            PointsPill()
        }
        .padding(.horizontal, 20)
        .padding(.top, 4)
    }

    // MARK: - Split content (home)

    private var splitContent: some View {
        ScrollView {
            VStack(spacing: 16) {
                weeklyCalendarStrip
                aiRecommendationCard
                bodyPartsPickerCard
                splitPresetsSection
                browseAllButton
            }
            .padding(.horizontal, 20)
            .padding(.top, 12)
            .padding(.bottom, 24)
        }
    }

    // MARK: Weekly calendar strip

    private var weekDays: [Date] {
        let cal = Calendar.current
        let startOfDay = cal.startOfDay(for: selectedCalendarDate)
        let weekday = cal.component(.weekday, from: selectedCalendarDate) // 1 = Sunday
        let daysFromMonday = (weekday + 5) % 7
        guard let monday = cal.date(byAdding: .day, value: -daysFromMonday, to: startOfDay) else { return [] }
        return (0..<7).compactMap { cal.date(byAdding: .day, value: $0, to: monday) }
    }

    private var completedDayStarts: Set<Date> {
        let cal = Calendar.current
        var days = Set(store.loggedWorkouts.map { cal.startOfDay(for: $0.date) })
        for hsession in healthSignals.sessions {
            days.insert(cal.startOfDay(for: hsession.date))
        }
        return days
    }

    private var weeklyCalendarStrip: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(weekDays, id: \.self) { day in
                    calendarDayCard(day)
                }
            }
        }
    }

    private func calendarDayCard(_ day: Date) -> some View {
        let cal = Calendar.current
        let isSelected = cal.isDate(day, inSameDayAs: selectedCalendarDate)
        let isCompleted = completedDayStarts.contains(cal.startOfDay(for: day))
        return Button(action: { selectedCalendarDate = day }) {
            VStack(spacing: 6) {
                Text(Self.dayNameFormatter.string(from: day))
                    .font(.system(size: 12, weight: .medium))
                    .foregroundColor(isSelected ? Color(hex: 0xFF94A3B8) : AppColors.textSecondary)
                Text(Self.dayNumberFormatter.string(from: day))
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(isSelected ? .white : AppColors.textPrimary)
                if isCompleted {
                    Image(systemName: "checkmark")
                        .font(.system(size: 10, weight: .bold))
                        .foregroundColor(.white)
                        .frame(width: 18, height: 18)
                        .background(AppColors.success)
                        .clipShape(Circle())
                } else {
                    Spacer().frame(width: 18, height: 18)
                }
            }
            .frame(width: 52, height: 84)
            .background(isSelected ? Color(hex: 0xFF0F172A) : Color.white)
            .clipShape(RoundedRectangle(cornerRadius: 20))
            .overlay(
                RoundedRectangle(cornerRadius: 20)
                    .stroke(isSelected ? Color.clear : AppColors.border, lineWidth: 1)
            )
            .shadow(color: isSelected ? .black.opacity(0.15) : .clear, radius: 4, y: 2)
        }
        .buttonStyle(.plain)
    }

    // MARK: AI recommendation card + shimmer

    private var aiRecommendationCard: some View {
        Group {
            if let rec = aiRecommendation, !isAiLoading {
                aiCard(rec)
            } else {
                aiShimmerCard
            }
        }
    }

    private func aiCard(_ rec: WorkoutRecommendation) -> some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack {
                Text(rec.headline)
                    .font(.system(size: 20, weight: .bold))
                    .foregroundColor(.white)
                Spacer()
                Text(rec.splitBadge)
                    .font(.system(size: 11, weight: .bold))
                    .foregroundColor(Color(hex: 0xFFCBD5E1))
                    .padding(.horizontal, 10)
                    .padding(.vertical, 5)
                    .background(Color(hex: 0xFF1E293B))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
            }
            Text(rec.description + (rec.recoveryNote.map { " " + $0 } ?? ""))
                .font(.system(size: 14))
                .foregroundColor(Color(hex: 0xFF94A3B8))
                .lineSpacing(4)

            VStack(alignment: .leading, spacing: 8) {
                HStack {
                    Text("Body parts")
                        .font(.system(size: 12, weight: .bold))
                        .foregroundColor(Color(hex: 0xFFE2E8F0))
                    Spacer()
                    Text("\(selectedBodyParts.count)/3 part limit")
                        .font(.system(size: 11))
                        .foregroundColor(Color(hex: 0xFF94A3B8))
                }
                FlowLayout(spacing: 8) {
                    ForEach(selectedBodyParts, id: \.self) { part in
                        Button(action: { removeBodyPart(part) }) {
                            HStack(spacing: 6) {
                                Image(systemName: "sparkles")
                                    .font(.system(size: 11))
                                Text(part.displayName)
                                    .font(.system(size: 13, weight: .bold))
                                Image(systemName: "xmark")
                                    .font(.system(size: 10, weight: .bold))
                            }
                            .foregroundColor(.white)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 7)
                            .background(Color(hex: 0xFF1E293B))
                            .clipShape(RoundedRectangle(cornerRadius: 14))
                            .overlay(
                                RoundedRectangle(cornerRadius: 14)
                                    .stroke(Color(hex: 0xFF334155), lineWidth: 1)
                            )
                        }
                        .transition(.scale.combined(with: .opacity))
                    }
                }
                .animation(.easeInOut(duration: 0.25), value: selectedBodyParts)
            }

            Button(action: startGuidedSession) {
                Text(selectedBodyParts.isEmpty ? "Select Body Part" : "Start Workout")
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(Color(hex: 0xFF0F172A))
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
                    .background(Color.white.opacity(selectedBodyParts.isEmpty ? 0.6 : 1.0))
                    .clipShape(Capsule())
            }
            .disabled(selectedBodyParts.isEmpty)
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 22)
        .background(Color(hex: 0xFF0F172A))
        .clipShape(RoundedRectangle(cornerRadius: 24))
        .shadow(color: .black.opacity(0.15), radius: 8, y: 4)
    }

    private var aiShimmerCard: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack {
                RoundedRectangle(cornerRadius: 8)
                    .fill(Color(hex: 0xFF334155))
                    .frame(width: 180, height: 22)
                Spacer()
                RoundedRectangle(cornerRadius: 12)
                    .fill(Color(hex: 0xFF334155))
                    .frame(width: 90, height: 26)
            }
            RoundedRectangle(cornerRadius: 8)
                .fill(Color(hex: 0xFF1E293B))
                .frame(height: 14)
            RoundedRectangle(cornerRadius: 8)
                .fill(Color(hex: 0xFF1E293B))
                .frame(width: 240, height: 14)
            HStack(spacing: 8) {
                ForEach(0..<3, id: \.self) { _ in
                    RoundedRectangle(cornerRadius: 14)
                        .fill(Color(hex: 0xFF1E293B))
                        .frame(width: 90, height: 30)
                }
            }
            RoundedRectangle(cornerRadius: 25)
                .fill(Color(hex: 0xFF1E293B))
                .frame(height: 50)
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 22)
        .background(Color(hex: 0xFF0F172A))
        .clipShape(RoundedRectangle(cornerRadius: 24))
        .redacted(reason: .placeholder)
    }

    // MARK: Body parts picker

    private var bodyPartsPickerCard: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack {
                Text("Body parts")
                    .font(.system(size: 20, weight: .bold))
                    .foregroundColor(Color(hex: 0xFF0F172A))
                Spacer()
                Text("\(OnDeviceWorkoutAiEngine.defaultBodyParts.count) parts")
                    .font(.system(size: 11, weight: .bold))
                    .foregroundColor(AppColors.primary)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 5)
                    .background(AppColors.primaryLight)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
            }
            Text("Choose the focus for your next session")
                .font(.system(size: 14))
                .foregroundColor(AppColors.textSecondary)
            VStack(spacing: 10) {
                ForEach(OnDeviceWorkoutAiEngine.defaultBodyParts, id: \.self) { part in
                    bodyPartRow(part)
                }
            }
        }
        .padding(20)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 28))
        .shadow(color: .black.opacity(0.04), radius: 6, y: 2)
        .onAppear {
            withAnimation(.easeInOut(duration: 1.1).repeatForever(autoreverses: true)) {
                recommendGlow = true
            }
        }
    }

    private func bodyPartRow(_ part: WorkoutBodyPart) -> some View {
        let isSelected = selectedBodyParts.contains(part)
        let isRecommended = aiRecommendation?.recommendedParts.contains(part) ?? false
        let canAdd = selectedBodyParts.count < 3
        return Button(action: { toggleBodyPart(part) }) {
            HStack(spacing: 12) {
                VStack(alignment: .leading, spacing: 4) {
                    HStack(spacing: 8) {
                        Text(part.displayName)
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(Color(hex: 0xFF0F172A))
                        if isRecommended {
                            HStack(spacing: 3) {
                                Image(systemName: "sparkles")
                                    .font(.system(size: 10))
                                Text("Recommended")
                                    .font(.system(size: 11, weight: .bold))
                            }
                            .foregroundColor(AppColors.purple)
                            .padding(.horizontal, 7)
                            .padding(.vertical, 3)
                            .background(AppColors.purple.opacity(0.12))
                            .clipShape(RoundedRectangle(cornerRadius: 8))
                        }
                    }
                    Text(bodyPartSubtitle(part))
                        .font(.system(size: 13))
                        .foregroundColor(AppColors.textSecondary)
                }
                Spacer()
                Image(systemName: isSelected ? "checkmark" : "plus")
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(.white)
                    .frame(width: 38, height: 38)
                    .background(isSelected ? AppColors.success : (canAdd ? AppColors.primary : AppColors.textTertiary))
                    .clipShape(Circle())
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .background(Color.white)
            .clipShape(RoundedRectangle(cornerRadius: 18))
            .overlay(
                RoundedRectangle(cornerRadius: 18)
                    .stroke(
                        isRecommended
                            ? AppColors.purple.opacity(recommendGlow ? 0.9 : 0.3)
                            : (isSelected ? AppColors.primary.opacity(0.4) : AppColors.border),
                        lineWidth: isRecommended ? 2 : 1
                    )
            )
        }
        .buttonStyle(.plain)
    }

    private func bodyPartSubtitle(_ part: WorkoutBodyPart) -> String {
        switch part {
        case .chest: return "Pecs, push-ups, dumbbells"
        case .back: return "Lats, rows, pulldowns"
        case .shoulders: return "Delts, presses, raises"
        case .biceps: return "Curls, preacher, hammer"
        case .triceps: return "Dips, extensions, pressdowns"
        case .legs: return "Quads, hamstrings, glutes"
        case .core: return "Abs, obliques, planks"
        case .calves: return "Gastrocnemius, raises"
        }
    }

    // MARK: Split presets

    private var splitPresetsSection: some View {
        let splits = OnDeviceWorkoutAiEngine.workoutSplits
        let split = splits[selectedSplitIndex]
        return VStack(alignment: .leading, spacing: 12) {
            Text("Workout splits")
                .font(.system(size: 20, weight: .bold))
                .foregroundColor(AppColors.textPrimary)
            Picker("", selection: $selectedSplitIndex) {
                ForEach(splits.indices, id: \.self) { index in
                    Text(splits[index].name).tag(index)
                }
            }
            .pickerStyle(.segmented)
            VStack(alignment: .leading, spacing: 10) {
                Text("\(split.name) Day")
                    .font(.system(size: 17, weight: .bold))
                    .foregroundColor(AppColors.textPrimary)
                Text(split.parts.map(\.displayName).joined(separator: " • "))
                    .font(.system(size: 13))
                    .foregroundColor(AppColors.textSecondary)
                ForEach(split.exercises.indices, id: \.self) { index in
                    splitExerciseRow(split.exercises[index], split: split)
                }
            }
            .padding(18)
            .background(Color.white)
            .clipShape(RoundedRectangle(cornerRadius: 20))
            .shadow(color: .black.opacity(0.04), radius: 6, y: 2)
        }
    }

    private func splitExerciseRow(_ item: WorkoutSplitExercise, split: WorkoutSplit) -> some View {
        let catalog = store.exercises.first(where: { $0.id == item.catalogExerciseId })
        let name = catalog?.name ?? item.exerciseName
        let subtitle = catalog.map { "\($0.primaryMuscleLabel) • \($0.equipment.capitalized)" }
            ?? split.parts.map(\.displayName).joined(separator: " • ")
        return Button(action: {
            let exercise = resolveSplitExercise(item, split: split)
            loggingTarget = LoggingTarget(
                id: exercise.id,
                exercise: exercise,
                targetReps: item.targetReps,
                targetWeightKg: item.targetWeightKg,
                setCount: item.targetSets
            )
        }) {
            VStack(alignment: .leading, spacing: 8) {
                HStack {
                    Text(name)
                        .font(.system(size: 15, weight: .bold))
                        .foregroundColor(AppColors.textPrimary)
                    Spacer()
                    Text("\(item.targetSets) Sets")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundColor(AppColors.textSecondary)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 4)
                        .background(AppColors.surface)
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                }
                Text(subtitle)
                    .font(.system(size: 13))
                    .foregroundColor(AppColors.textSecondary)
                    .lineLimit(2)
                HStack(spacing: 8) {
                    targetPill("\(item.targetReps) Reps")
                    targetPill(item.targetWeightKg > 0 ? "\(formatKg(item.targetWeightKg)) kg" : "Bodyweight")
                }
            }
            .padding(16)
            .background(Color.white)
            .clipShape(RoundedRectangle(cornerRadius: 16))
            .overlay(
                RoundedRectangle(cornerRadius: 16)
                    .stroke(AppColors.border, lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
    }

    private func targetPill(_ text: String) -> some View {
        Text(text)
            .font(.system(size: 12, weight: .semibold))
            .foregroundColor(AppColors.primary)
            .padding(.horizontal, 10)
            .padding(.vertical, 5)
            .background(AppColors.primaryLight)
            .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    private func resolveSplitExercise(_ item: WorkoutSplitExercise, split: WorkoutSplit) -> Exercise {
        if let exercise = store.exercises.first(where: { $0.id == item.catalogExerciseId }) {
            return exercise
        }
        if let exercise = store.exercises.first(where: {
            $0.name.localizedCaseInsensitiveContains(item.exerciseName)
        }) {
            return exercise
        }
        return Exercise(
            id: item.catalogExerciseId,
            name: item.exerciseName,
            bodyPart: catalogBodyPart(for: split.parts.first ?? .chest),
            equipment: "Gym Equipment",
            primaryMuscle: split.parts.first?.displayName ?? "Muscles",
            secondaryMuscles: [],
            instructions: ["Perform \(item.exerciseName) with proper form for \(item.targetReps) reps."],
            gif: "",
            img: ""
        )
    }

    private func formatKg(_ value: Double) -> String {
        value.truncatingRemainder(dividingBy: 1) == 0 ? String(Int(value)) : String(format: "%g", value)
    }

    private var browseAllButton: some View {
        Button(action: { isCatalogMode = true }) {
            HStack {
                Text("Browse all exercises")
                    .font(.system(size: 15, weight: .semibold))
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.system(size: 13, weight: .semibold))
            }
            .foregroundColor(AppColors.primary)
            .padding(16)
            .background(Color.white)
            .clipShape(RoundedRectangle(cornerRadius: 16))
            .overlay(
                RoundedRectangle(cornerRadius: 16)
                    .stroke(AppColors.border, lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
    }

    // MARK: - Catalog mode

    private var catalogContent: some View {
        VStack(spacing: 0) {
            HStack {
                Text("All Exercises")
                    .font(.system(size: 20, weight: .bold))
                    .foregroundColor(AppColors.textPrimary)
                Spacer()
                Button(action: { isCatalogMode = false }) {
                    Text("Back to Splits")
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundColor(AppColors.primary)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 7)
                        .background(AppColors.primaryLight)
                        .clipShape(Capsule())
                }
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 10)

            HStack {
                Image(systemName: "magnifyingglass")
                    .foregroundColor(AppColors.textTertiary)
                TextField("Search exercises", text: $searchQuery)
                    .font(.system(size: 15))
                if !searchQuery.isEmpty {
                    Button(action: { searchQuery = "" }) {
                        Image(systemName: "xmark.circle.fill")
                            .foregroundColor(AppColors.textTertiary)
                    }
                }
            }
            .padding(12)
            .background(Color.white)
            .clipShape(RoundedRectangle(cornerRadius: 16))
            .padding(.horizontal, 20)
            .padding(.bottom, 10)

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
                            Button(action: {
                                loggingTarget = LoggingTarget(
                                    id: exercise.id,
                                    exercise: exercise,
                                    targetReps: 10,
                                    targetWeightKg: 0,
                                    setCount: 3
                                )
                            }) {
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
                .font(.system(size: 13, weight: .semibold))
                .foregroundColor(selected ? .white : AppColors.textSecondary)
                .padding(.horizontal, 14)
                .padding(.vertical, 8)
                .background(selected ? AppColors.primary : Color.white)
                .clipShape(Capsule())
                .overlay(
                    Capsule().stroke(selected ? Color.clear : AppColors.border, lineWidth: 1)
                )
        }
        .buttonStyle(.plain)
    }

    private func exerciseRow(_ exercise: Exercise) -> some View {
        HStack(spacing: 12) {
            VStack(alignment: .leading, spacing: 4) {
                Text(exercise.name)
                    .font(.system(size: 15, weight: .bold))
                    .foregroundColor(AppColors.textPrimary)
                    .lineLimit(1)
                Text("\(exercise.primaryMuscleLabel) • \(exercise.equipment.capitalized)")
                    .font(.system(size: 13))
                    .foregroundColor(AppColors.textSecondary)
                    .lineLimit(1)
            }
            Spacer()
            Image(systemName: "chevron.right")
                .font(.system(size: 14, weight: .semibold))
                .foregroundColor(AppColors.textTertiary)
        }
        .padding(14)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .shadow(color: .black.opacity(0.04), radius: 4, y: 2)
    }

    // MARK: - History

    private var historySheet: some View {
        VStack(spacing: 0) {
            HStack {
                Text("Workout History")
                    .font(.system(size: 20, weight: .bold))
                    .foregroundColor(AppColors.textPrimary)
                Spacer()
                Button("Done") { showingHistory = false }
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundColor(AppColors.primary)
            }
            .padding(.horizontal, 20)
            .padding(.top, 16)
            .padding(.bottom, 12)

            historyContent
        }
        .background(AppColors.surface)
    }

    private var historyContent: some View {
        Group {
            if store.loggedWorkouts.isEmpty {
                VStack(spacing: 12) {
                    Image(systemName: "dumbbell")
                        .font(.system(size: 44))
                        .foregroundColor(AppColors.textTertiary)
                    Text("No workouts logged yet")
                        .font(.system(size: 17, weight: .bold))
                        .foregroundColor(AppColors.textPrimary)
                    Text("Your completed workouts will appear here.")
                        .font(.system(size: 14))
                        .foregroundColor(AppColors.textSecondary)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
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
                    .font(.system(size: 15, weight: .bold))
                    .foregroundColor(AppColors.textPrimary)
                    .lineLimit(1)
                Text(workout.date, style: .date)
                    .font(.system(size: 12))
                    .foregroundColor(AppColors.textSecondary)
            }
            Spacer()
            VStack(alignment: .trailing, spacing: 4) {
                Text("\(workout.totalVolumeKg, specifier: "%g") kg")
                    .font(.system(size: 15, weight: .bold))
                    .foregroundColor(AppColors.primary)
                HStack(spacing: 6) {
                    Text("\(workout.sets.count) sets")
                        .font(.system(size: 12))
                        .foregroundColor(AppColors.textSecondary)
                    if workout.syncedToHealthKit {
                        Image(systemName: "checkmark.circle.fill")
                            .font(.system(size: 12))
                            .foregroundColor(AppColors.success)
                    }
                }
            }
            Button(action: { store.deleteWorkout(id: workout.id) }) {
                Image(systemName: "trash")
                    .font(.system(size: 15))
                    .foregroundColor(AppColors.danger)
                    .frame(width: 36, height: 36)
                    .background(AppColors.danger.opacity(0.1))
                    .clipShape(Circle())
            }
        }
        .padding(14)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .shadow(color: .black.opacity(0.04), radius: 4, y: 2)
    }

    // MARK: - AI recommendation

    private var aiTaskId: String {
        let day = Calendar.current.startOfDay(for: selectedCalendarDate)
        return "\(day.timeIntervalSince1970)-\(store.loggedWorkouts.count)-\(store.exercises.count)"
    }

    private func loadAiRecommendation() async {
        let date = selectedCalendarDate
        let exercises = store.exercises
        let logged = store.loggedWorkouts
        let signals = await HealthKitManager.shared.readWorkoutSignals()
        try? await Task.sleep(nanoseconds: 650_000_000)
        guard !Task.isCancelled else { return }
        let rec = OnDeviceWorkoutAiEngine.generateRecommendation(
            selectedDate: date,
            exercises: exercises,
            loggedWorkouts: logged,
            signals: signals
        )
        await MainActor.run {
            guard !Task.isCancelled else { return }
            healthSignals = signals
            aiRecommendation = rec
            isAiLoading = false
            if selectedBodyParts.isEmpty {
                selectedBodyParts = rec.recommendedParts
            }
        }
    }

    // MARK: - Actions

    private func toggleBodyPart(_ part: WorkoutBodyPart) {
        if selectedBodyParts.contains(part) {
            selectedBodyParts.removeAll { $0 == part }
        } else if selectedBodyParts.count < 3 {
            selectedBodyParts.append(part)
        }
    }

    private func removeBodyPart(_ part: WorkoutBodyPart) {
        selectedBodyParts.removeAll { $0 == part }
    }

    private func startGuidedSession() {
        guard !selectedBodyParts.isEmpty else { return }
        sessionPart = nil
        setLogExercise = nil
        session.start(bodyParts: selectedBodyParts)
    }

    private func onWorkoutSaved(_ workout: LoggedWorkout) {
        store.logWorkout(workout)
        PointsStore.shared.awardForWorkout(exerciseName: workout.exerciseName, setCount: workout.sets.count, durationMinutes: workout.durationMinutes)
        let end = workout.date
        let start = end.addingTimeInterval(TimeInterval(-max(workout.durationMinutes, 1) * 60))
        Task {
            let ok = await HealthKitManager.shared.saveWorkout(
                exerciseName: workout.exerciseName,
                start: start,
                end: end,
                setCount: workout.sets.count,
                totalReps: workout.totalReps,
                totalVolumeKg: workout.totalVolumeKg
            )
            if ok {
                await MainActor.run { store.markSynced(id: workout.id) }
            }
        }
    }
}
