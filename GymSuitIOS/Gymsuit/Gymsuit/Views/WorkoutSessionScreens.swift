import SwiftUI
import Combine

// MARK: - Helpers shared by the guided session screens

/// Formats elapsed seconds as H:MM:SS when >= 1 hour, otherwise MM:SS.
func formatWorkoutClock(_ totalSeconds: Int) -> String {
    let h = totalSeconds / 3600
    let m = (totalSeconds % 3600) / 60
    let s = totalSeconds % 60
    if h > 0 {
        return String(format: "%d:%02d:%02d", h, m, s)
    }
    return String(format: "%02d:%02d", m, s)
}

/// Rounded progress bar used by the session screens and the Workouts screen.
struct BarProgress: View {
    var progress: Float
    var track: Color = Color(hex: 0xFFE2E8F0)
    var fill: Color = AppColors.primary
    var height: CGFloat = 8

    var body: some View {
        GeometryReader { geo in
            ZStack(alignment: .leading) {
                Capsule().fill(track)
                Capsule().fill(fill)
                    .frame(width: geo.size.width * CGFloat(min(max(progress, 0), 1)))
                    .animation(.easeInOut(duration: 0.3), value: progress)
            }
        }
        .frame(height: height)
    }
}

/// Maps a WorkoutBodyPart to the openGym dataset's `bodyPart` value.
func catalogBodyPart(for part: WorkoutBodyPart) -> String {
    switch part {
    case .chest: return "chest"
    case .back: return "back"
    case .shoulders: return "shoulders"
    case .biceps, .triceps: return "upper arms"
    case .legs: return "upper legs"
    case .core: return "waist"
    case .calves: return "lower legs"
    }
}

/// Wrapping horizontal layout for pill rows (body-part chips, etc.).
struct FlowLayout: Layout {
    var spacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let width = proposal.replacingUnspecifiedDimensions().width
        return arrange(in: width, subviews: subviews).size
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        let result = arrange(in: bounds.width, subviews: subviews)
        for (index, subview) in subviews.enumerated() {
            let point = result.positions[index]
            subview.place(
                at: CGPoint(x: bounds.minX + point.x, y: bounds.minY + point.y),
                proposal: .unspecified
            )
        }
    }

    private func arrange(in maxWidth: CGFloat, subviews: Subviews) -> (size: CGSize, positions: [CGPoint]) {
        var positions: [CGPoint] = []
        var x: CGFloat = 0
        var y: CGFloat = 0
        var rowHeight: CGFloat = 0
        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if x > 0 && x + size.width > maxWidth {
                x = 0
                y += rowHeight + spacing
                rowHeight = 0
            }
            positions.append(CGPoint(x: x, y: y))
            rowHeight = max(rowHeight, size.height)
            x += size.width + spacing
        }
        return (CGSize(width: maxWidth, height: y + rowHeight), positions)
    }
}

// MARK: - WorkoutSessionPage (guided session home)

/// Session home: live stopwatch card, pause/resume/finish, and per-body-part progress cards.
/// Expects `WorkoutSessionState` as an environment object.
struct WorkoutSessionPage: View {
    @EnvironmentObject var session: WorkoutSessionState
    let onFinish: () -> Void
    let onSelectBodyPart: (WorkoutBodyPart) -> Void

    @State private var confirmFinish = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                Text("Workout in progress")
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundColor(AppColors.textSecondary)

                timerCard

                Text("Body parts")
                    .font(.system(size: 20, weight: .bold))
                    .foregroundColor(AppColors.textPrimary)
                Text("Tap a part to log an exercise. 3 exercises per part completes the part.")
                    .font(.system(size: 14))
                    .foregroundColor(AppColors.textSecondary)

                ForEach(session.parts, id: \.self) { part in
                    bodyPartCard(part)
                }
            }
            .padding(20)
        }
        .background(AppColors.surface)
        .interactiveDismissDisabled(session.active)
        .alert("End this workout?", isPresented: $confirmFinish) {
            Button("End workout", role: .destructive) { onFinish() }
            Button("Keep going", role: .cancel) { }
        } message: {
            Text("Timer and progress will be cleared.")
        }
    }

    private var timerCard: some View {
        VStack(spacing: 14) {
            Text(session.running ? "ELAPSED" : "PAUSED")
                .font(.system(size: 12, weight: .bold))
                .foregroundColor(Color(hex: 0xFF94A3B8))
                .tracking(2)
            Text(formatWorkoutClock(session.elapsedSeconds))
                .font(.system(size: 54, weight: .bold))
                .foregroundColor(.white)
                .monospacedDigit()
            BarProgress(
                progress: session.overallProgress(),
                track: Color(hex: 0xFF1E293B),
                fill: AppColors.success,
                height: 6
            )
            HStack(spacing: 10) {
                Button(action: { session.running.toggle() }) {
                    HStack(spacing: 6) {
                        Image(systemName: session.running ? "pause.fill" : "play.fill")
                            .font(.system(size: 15, weight: .bold))
                        Text(session.running ? "Pause" : "Resume")
                            .font(.system(size: 15, weight: .bold))
                    }
                    .foregroundColor(Color(hex: 0xFF0F172A))
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 13)
                    .background(Color.white)
                    .clipShape(Capsule())
                }
                Button(action: { confirmFinish = true }) {
                    Text("Finish")
                        .font(.system(size: 15, weight: .bold))
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 13)
                        .overlay(Capsule().stroke(Color(hex: 0xFF475569), lineWidth: 1))
                }
            }
        }
        .padding(20)
        .background(Color(hex: 0xFF0F172A))
        .clipShape(RoundedRectangle(cornerRadius: 24))
        .shadow(color: .black.opacity(0.15), radius: 8, y: 4)
    }

    private func bodyPartCard(_ part: WorkoutBodyPart) -> some View {
        let progress = session.progress(part: part)
        let isDone = progress >= 1
        return Button(action: { onSelectBodyPart(part) }) {
            VStack(alignment: .leading, spacing: 10) {
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(part.displayName)
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(AppColors.textPrimary)
                        Text("\(session.exercisesDone(part: part))/\(WorkoutSessionState.exercisesPerPartTarget) exercises • \(session.setsDone(part: part)) sets")
                            .font(.system(size: 13))
                            .foregroundColor(AppColors.textSecondary)
                    }
                    Spacer()
                    Image(systemName: isDone ? "checkmark" : "chevron.right")
                        .font(.system(size: 15, weight: .bold))
                        .foregroundColor(isDone ? AppColors.success : AppColors.textTertiary)
                }
                BarProgress(
                    progress: progress,
                    track: Color(hex: 0xFFE2E8F0),
                    fill: isDone ? AppColors.success : AppColors.primary,
                    height: 8
                )
            }
            .padding(16)
            .background(Color.white)
            .clipShape(RoundedRectangle(cornerRadius: 18))
            .shadow(color: .black.opacity(0.04), radius: 4, y: 2)
        }
        .buttonStyle(.plain)
    }
}

// MARK: - BodyPartWorkoutPickerPage

/// Exercises filtered by body part for the guided session; selecting one opens set-by-set logging.
struct BodyPartWorkoutPickerPage: View {
    let part: WorkoutBodyPart
    let onBack: () -> Void
    let onExerciseClick: (Exercise) -> Void

    @ObservedObject private var store = WorkoutStore.shared

    private var exercises: [Exercise] {
        let key = catalogBodyPart(for: part)
        return store.exercises.filter { $0.bodyPart.lowercased() == key }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 12) {
                Button(action: onBack) {
                    Image(systemName: "chevron.left")
                        .font(.system(size: 17, weight: .semibold))
                        .foregroundColor(AppColors.textPrimary)
                        .frame(width: 36, height: 36)
                        .background(Color.white)
                        .clipShape(Circle())
                }
                Text("\(part.displayName) exercises")
                    .font(.system(size: 22, weight: .bold))
                    .foregroundColor(AppColors.textPrimary)
                Spacer()
            }
            .padding(.horizontal, 20)
            .padding(.top, 4)

            if exercises.isEmpty {
                Spacer()
                Text(store.exercises.isEmpty ? "Exercise catalog failed to load." : "No exercises found for this body part.")
                    .font(.system(size: 14))
                    .foregroundColor(AppColors.textSecondary)
                    .frame(maxWidth: .infinity, alignment: .center)
                Spacer()
            } else {
                ScrollView {
                    LazyVStack(spacing: 10) {
                        ForEach(exercises) { exercise in
                            Button(action: { onExerciseClick(exercise) }) {
                                pickerRow(exercise)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    .padding(.horizontal, 20)
                    .padding(.bottom, 20)
                }
            }
        }
        .background(AppColors.surface)
    }

    private func pickerRow(_ exercise: Exercise) -> some View {
        HStack(spacing: 12) {
            if let gifURL = exercise.gifURL {
                GIFView(url: gifURL)
                    .frame(width: 84, height: 84)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
            }
            VStack(alignment: .leading, spacing: 4) {
                Text(exercise.name)
                    .font(.system(size: 15, weight: .bold))
                    .foregroundColor(AppColors.textPrimary)
                    .lineLimit(2)
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
        .padding(12)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 18))
        .shadow(color: .black.opacity(0.04), radius: 4, y: 2)
    }
}

// MARK: - SetBySetLogPage

private enum SetPhase { case working, resting }

/// Set-by-set logging inside a guided session: working / resting phases with a 60s rest countdown.
/// Expects `WorkoutSessionState` as an environment object.
struct SetBySetLogPage: View {
    @EnvironmentObject var session: WorkoutSessionState
    let exercise: Exercise
    let onBack: () -> Void
    let onFinished: (LoggedWorkout) -> Void

    @State private var totalSets = 3
    @State private var completed: [WorkoutSet] = []
    @State private var phase: SetPhase = .working
    @State private var repsText = "10"
    @State private var weightText = "0"
    @State private var setSeconds = 0
    @State private var restTotal = 60
    @State private var restLeft = 60
    @State private var totalSeconds = 0
    @State private var startDate = Date()

    private let tick = Timer.publish(every: 1, on: .main, in: .common).autoconnect()

    private var allDone: Bool { completed.count >= totalSets }
    private var displayName: String {
        exercise.name.prefix(1).uppercased() + String(exercise.name.dropFirst())
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 14) {
                headerRow

                if let gifURL = exercise.gifURL {
                    GIFView(url: gifURL)
                        .frame(height: 200)
                        .clipShape(RoundedRectangle(cornerRadius: 16))
                }

                setProgressSection

                if !allDone && phase == .working {
                    workingCard
                } else if !allDone {
                    restingCard
                }

                if !completed.isEmpty {
                    completedSetsCard
                    footerButtons
                }
            }
            .padding(20)
        }
        .background(AppColors.surface)
        .onReceive(tick) { _ in
            guard session.running else { return }
            totalSeconds += 1
            switch phase {
            case .working:
                setSeconds += 1
            case .resting:
                if restLeft > 0 {
                    restLeft -= 1
                    if restLeft == 0 {
                        phase = .working
                        setSeconds = 0
                    }
                }
            }
        }
    }

    // MARK: Header

    private var headerRow: some View {
        HStack(spacing: 12) {
            Button(action: onBack) {
                Image(systemName: "chevron.left")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundColor(AppColors.textPrimary)
                    .frame(width: 36, height: 36)
                    .background(Color.white)
                    .clipShape(Circle())
            }
            VStack(alignment: .leading, spacing: 2) {
                Text(displayName)
                    .font(.system(size: 22, weight: .bold))
                    .foregroundColor(AppColors.textPrimary)
                Text("Set \(min(completed.count + 1, totalSets)) of \(totalSets)")
                    .font(.system(size: 13))
                    .foregroundColor(AppColors.textSecondary)
            }
            Spacer()
            Text(formatWorkoutClock(totalSeconds))
                .font(.system(size: 18, weight: .bold))
                .foregroundColor(AppColors.primary)
                .monospacedDigit()
        }
    }

    private var setProgressSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text("Sets")
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(AppColors.textPrimary)
                Spacer()
                Text("\(completed.count)/\(totalSets)")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(AppColors.textSecondary)
            }
            BarProgress(
                progress: totalSets > 0 ? Float(completed.count) / Float(totalSets) : 0,
                track: Color(hex: 0xFFE2E8F0),
                fill: AppColors.success,
                height: 8
            )
        }
        .padding(16)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 18))
        .shadow(color: .black.opacity(0.04), radius: 4, y: 2)
    }

    // MARK: Working card

    private var workingCard: some View {
        VStack(spacing: 14) {
            Text("Set \(completed.count + 1) of \(totalSets)")
                .font(.system(size: 13, weight: .bold))
                .foregroundColor(AppColors.primary)
            Text(formatWorkoutClock(setSeconds))
                .font(.system(size: 40, weight: .bold))
                .foregroundColor(AppColors.textPrimary)
                .monospacedDigit()
            HStack(spacing: 12) {
                numberField(title: "Reps", text: $repsText, allowDecimal: false)
                numberField(title: "Weight (kg)", text: $weightText, allowDecimal: true)
            }
            Button(action: completeCurrentSet) {
                HStack(spacing: 8) {
                    Image(systemName: "checkmark")
                        .font(.system(size: 15, weight: .bold))
                    Text("Done")
                        .font(.system(size: 16, weight: .bold))
                }
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 14)
                .background(AppColors.workout)
                .clipShape(Capsule())
            }
        }
        .padding(18)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 18))
        .shadow(color: .black.opacity(0.04), radius: 4, y: 2)
    }

    private func numberField(title: String, text: Binding<String>, allowDecimal: Bool) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(title)
                .font(.system(size: 13, weight: .semibold))
                .foregroundColor(AppColors.textSecondary)
            TextField(title, text: text)
                .keyboardType(.decimalPad)
                .font(.system(size: 18, weight: .semibold))
                .foregroundColor(AppColors.textPrimary)
                .padding(12)
                .background(AppColors.surface)
                .clipShape(RoundedRectangle(cornerRadius: 12))
                .onChange(of: text.wrappedValue) { _, newValue in
                    let filtered: String
                    if allowDecimal {
                        filtered = String(newValue.filter { $0.isNumber || $0 == "." }.prefix(6))
                    } else {
                        filtered = String(newValue.filter(\.isNumber).prefix(3))
                    }
                    if filtered != newValue { text.wrappedValue = filtered }
                }
        }
        .frame(maxWidth: .infinity)
    }

    // MARK: Resting card

    private var restingCard: some View {
        VStack(spacing: 12) {
            Text("REST")
                .font(.system(size: 12, weight: .bold))
                .foregroundColor(AppColors.textSecondary)
                .tracking(2)
            Text(formatWorkoutClock(restLeft))
                .font(.system(size: 52, weight: .bold))
                .foregroundColor(AppColors.primary)
                .monospacedDigit()
            BarProgress(
                progress: restTotal > 0 ? Float(restTotal - restLeft) / Float(restTotal) : 0,
                track: Color(hex: 0xFFE2E8F0),
                fill: AppColors.primary,
                height: 8
            )
            HStack(spacing: 10) {
                Button(action: {
                    restTotal += 15
                    restLeft += 15
                }) {
                    Text("+15s")
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundColor(AppColors.primary)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 13)
                        .overlay(Capsule().stroke(AppColors.primary, lineWidth: 1))
                }
                Button(action: {
                    restLeft = 0
                    phase = .working
                    setSeconds = 0
                }) {
                    Text("Skip")
                        .font(.system(size: 15, weight: .bold))
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 13)
                        .background(AppColors.primary)
                        .clipShape(Capsule())
                }
            }
        }
        .padding(18)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 18))
        .shadow(color: .black.opacity(0.04), radius: 4, y: 2)
    }

    // MARK: Completed sets + footer

    private var completedSetsCard: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Completed sets")
                .font(.system(size: 14, weight: .bold))
                .foregroundColor(AppColors.textSecondary)
            ForEach(completed.indices, id: \.self) { index in
                HStack {
                    Text("Set \(index + 1)")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(AppColors.textPrimary)
                    Spacer()
                    Text("\(completed[index].reps) reps × \(formatSetWeight(completed[index].weightKg)) kg")
                        .font(.system(size: 14))
                        .foregroundColor(AppColors.textSecondary)
                    Image(systemName: "checkmark.circle.fill")
                        .foregroundColor(AppColors.success)
                        .font(.system(size: 16))
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 9)
                .background(AppColors.surface)
                .clipShape(RoundedRectangle(cornerRadius: 12))
            }
        }
        .padding(16)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 18))
        .shadow(color: .black.opacity(0.04), radius: 4, y: 2)
    }

    private var footerButtons: some View {
        HStack(spacing: 10) {
            if !allDone {
                Button(action: finish) {
                    Text("Finish early")
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundColor(AppColors.textPrimary)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 13)
                        .overlay(Capsule().stroke(AppColors.border, lineWidth: 1))
                }
                Button(action: { totalSets += 1 }) {
                    Text("Add a set")
                        .font(.system(size: 15, weight: .bold))
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 13)
                        .background(AppColors.workout)
                        .clipShape(Capsule())
                }
            } else {
                Button(action: finish) {
                    Text("Save exercise")
                        .font(.system(size: 15, weight: .bold))
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 13)
                        .background(AppColors.workout)
                        .clipShape(Capsule())
                }
                Button(action: {
                    totalSets += 1
                    phase = .working
                    setSeconds = 0
                }) {
                    Text("Add a set")
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundColor(AppColors.textPrimary)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 13)
                        .overlay(Capsule().stroke(AppColors.border, lineWidth: 1))
                }
            }
        }
    }

    // MARK: Actions

    private func completeCurrentSet() {
        completed.append(WorkoutSet(reps: Int(repsText) ?? 0, weightKg: Double(weightText) ?? 0.0))
        if completed.count < totalSets {
            restLeft = restTotal
            phase = .resting
        }
    }

    private func finish() {
        guard !completed.isEmpty else { return }
        let end = min(startDate.addingTimeInterval(TimeInterval(totalSeconds)), Date())
        let workout = LoggedWorkout(
            exerciseId: exercise.id,
            exerciseName: exercise.name,
            date: end,
            sets: completed,
            durationMinutes: max(1, Int(ceil(Double(totalSeconds) / 60.0))),
            notes: "",
            syncedToHealthKit: false
        )
        onFinished(workout)
    }

    private func formatSetWeight(_ value: Double) -> String {
        value.truncatingRemainder(dividingBy: 1) == 0 ? String(Int(value)) : String(format: "%g", value)
    }
}
