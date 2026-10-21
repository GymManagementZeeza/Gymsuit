import SwiftUI

// MARK: - Weight unit toggle

/// Small segmented lb | kg switch used by the workout logging UI.
/// Bind it to the persisted preference, e.g.
/// `@AppStorage(WeightUnits.storageKey) var weightUnitRaw = WeightUnit.kg.rawValue`.
struct WeightUnitToggle: View {
    @Binding var unit: WeightUnit

    var body: some View {
        Picker("Weight unit", selection: $unit) {
            Text("kg").tag(WeightUnit.kg)
            Text("lb").tag(WeightUnit.lb)
        }
        .pickerStyle(.segmented)
        .frame(width: 132)
    }
}

/// Convenience accessor for the persisted weight unit in views.
struct WeightUnitPreference {
    static func unit(from raw: String) -> WeightUnit {
        WeightUnit(rawValue: raw) ?? .kg
    }
}

// MARK: - Muscle regions + dataset mapping

/// Silhouette regions the body figure is partitioned into.
enum MuscleRegion: String, CaseIterable {
    case chest
    case delts
    case biceps
    case triceps
    case forearms
    case abs
    case quads
    case calves
    case traps
    case back
    case lowerBack
    case glutes
    case hamstrings

    var label: String {
        switch self {
        case .chest: return "Chest"
        case .delts: return "Shoulders"
        case .biceps: return "Biceps"
        case .triceps: return "Triceps"
        case .forearms: return "Forearms"
        case .abs: return "Abs & obliques"
        case .quads: return "Quadriceps"
        case .calves: return "Calves"
        case .traps: return "Traps"
        case .back: return "Back & lats"
        case .lowerBack: return "Lower back"
        case .glutes: return "Glutes"
        case .hamstrings: return "Hamstrings"
        }
    }
}

struct MuscleHits {
    var primary: Set<MuscleRegion>
    var secondary: Set<MuscleRegion>
}

/// Maps the bundled EXDB dataset's muscle strings onto silhouette regions.
/// Covers every distinct body_part / primary_muscle / secondary_muscles value
/// in exercises.json; values with no visible region (cardiovascular system,
/// ankles, feet, shins, ankle stabilizers) intentionally map to nil.
enum MuscleMap {
    static func region(for name: String) -> MuscleRegion? {
        switch name.trimmingCharacters(in: .whitespaces).lowercased() {
        case "pectorals", "chest", "upper chest", "serratus anterior":
            return .chest
        case "delts", "deltoids", "shoulders", "rear deltoids", "rotator cuff":
            return .delts
        case "biceps", "brachialis":
            return .biceps
        case "triceps":
            return .triceps
        case "forearms", "wrist flexors", "wrist extensors", "wrists", "grip muscles", "hands":
            return .forearms
        case "abs", "abdominals", "lower abs", "obliques", "core":
            return .abs
        case "quads", "quadriceps", "adductors", "abductors", "inner thighs", "groin", "hip flexors":
            return .quads
        case "calves", "soleus":
            return .calves
        case "traps", "trapezius", "levator scapulae", "sternocleidomastoid":
            return .traps
        case "lats", "latissimus dorsi", "upper back", "rhomboids", "back":
            return .back
        case "lower back", "spine":
            return .lowerBack
        case "glutes":
            return .glutes
        case "hamstrings":
            return .hamstrings
        default:
            return nil
        }
    }

    /// Fallback used only when an exercise has no primary muscle string.
    static func region(forBodyPart bodyPart: String) -> MuscleRegion? {
        switch bodyPart.trimmingCharacters(in: .whitespaces).lowercased() {
        case "chest": return .chest
        case "back": return .back
        case "shoulders": return .delts
        case "upper arms": return .biceps
        case "lower arms": return .forearms
        case "waist": return .abs
        case "upper legs": return .quads
        case "lower legs": return .calves
        case "neck": return .traps
        default: return nil
        }
    }

    /// Primary = solid red regions, secondary = light red regions, for a set of logged workouts.
    static func hits(workouts: [LoggedWorkout], exercises: [Exercise]) -> MuscleHits {
        let byId = Dictionary(uniqueKeysWithValues: exercises.map { ($0.id, $0) })
        let byName = Dictionary(exercises.map { ($0.name.lowercased(), $0) }, uniquingKeysWith: { first, _ in first })
        var primary = Set<MuscleRegion>()
        var secondary = Set<MuscleRegion>()
        for workout in workouts {
            guard let exercise = byId[workout.exerciseId] ?? byName[workout.exerciseName.lowercased()] else {
                continue
            }
            if let region = region(for: exercise.primaryMuscle) ?? region(forBodyPart: exercise.bodyPart) {
                primary.insert(region)
            }
            for muscle in exercise.secondaryMuscles {
                if let region = region(for: muscle) {
                    secondary.insert(region)
                }
            }
        }
        secondary.subtract(primary)
        return MuscleHits(primary: primary, secondary: secondary)
    }
}

// MARK: - Body silhouette (vector-drawn, no image assets)
//
// The figure lives in a 200 x 400 coordinate space. Left-half shapes are
// mirrored to the right half (x -> 200 - x) so both sides stay symmetric.

private enum SilShape {
    case poly([CGPoint])
    case oval(cx: CGFloat, cy: CGFloat, rx: CGFloat, ry: CGFloat)

    var mirrored: SilShape {
        switch self {
        case .poly(let points):
            return .poly(points.map { CGPoint(x: 200 - $0.x, y: $0.y) })
        case .oval(let cx, let cy, let rx, let ry):
            return .oval(cx: 200 - cx, cy: cy, rx: rx, ry: ry)
        }
    }
}

private func both(_ shape: SilShape) -> [SilShape] { [shape, shape.mirrored] }

private let bicepsShape = SilShape.poly([
    CGPoint(x: 37, y: 84), CGPoint(x: 53, y: 82), CGPoint(x: 55, y: 116),
    CGPoint(x: 43, y: 120), CGPoint(x: 36, y: 102)
])
private let forearmShape = SilShape.poly([
    CGPoint(x: 36, y: 122), CGPoint(x: 52, y: 120), CGPoint(x: 50, y: 168),
    CGPoint(x: 44, y: 230), CGPoint(x: 35, y: 226), CGPoint(x: 34, y: 160)
])
private let quadShape = SilShape.poly([
    CGPoint(x: 59, y: 180), CGPoint(x: 95, y: 178), CGPoint(x: 93, y: 236),
    CGPoint(x: 87, y: 260), CGPoint(x: 68, y: 260), CGPoint(x: 61, y: 234),
    CGPoint(x: 57, y: 204)
])
private let calfFrontShape = SilShape.poly([
    CGPoint(x: 63, y: 266), CGPoint(x: 89, y: 264), CGPoint(x: 87, y: 346),
    CGPoint(x: 67, y: 346), CGPoint(x: 62, y: 300)
])
private let hamstringShape = SilShape.poly([
    CGPoint(x: 61, y: 246), CGPoint(x: 93, y: 244), CGPoint(x: 91, y: 294),
    CGPoint(x: 85, y: 308), CGPoint(x: 69, y: 308), CGPoint(x: 62, y: 292)
])
private let calfBackShape = SilShape.poly([
    CGPoint(x: 64, y: 314), CGPoint(x: 89, y: 312), CGPoint(x: 87, y: 350),
    CGPoint(x: 67, y: 350), CGPoint(x: 63, y: 332)
])

private let frontShapes: [MuscleRegion: [SilShape]] = [
    .chest: [.poly([
        CGPoint(x: 68, y: 64), CGPoint(x: 132, y: 64), CGPoint(x: 128, y: 100),
        CGPoint(x: 120, y: 118), CGPoint(x: 80, y: 118), CGPoint(x: 72, y: 100)
    ])],
    .delts: both(.oval(cx: 44, cy: 72, rx: 13, ry: 13)),
    .biceps: both(bicepsShape),
    .forearms: both(forearmShape),
    .abs: [.poly([
        CGPoint(x: 79, y: 122), CGPoint(x: 121, y: 122), CGPoint(x: 119, y: 166),
        CGPoint(x: 115, y: 198), CGPoint(x: 85, y: 198), CGPoint(x: 81, y: 166)
    ])],
    .quads: both(quadShape),
    .calves: both(calfFrontShape)
]

private let backShapes: [MuscleRegion: [SilShape]] = [
    .traps: [.poly([
        CGPoint(x: 80, y: 54), CGPoint(x: 120, y: 54), CGPoint(x: 134, y: 82),
        CGPoint(x: 100, y: 102), CGPoint(x: 66, y: 82)
    ])],
    .back: [.poly([
        CGPoint(x: 63, y: 86), CGPoint(x: 137, y: 86), CGPoint(x: 129, y: 148),
        CGPoint(x: 119, y: 184), CGPoint(x: 81, y: 184), CGPoint(x: 71, y: 148)
    ])],
    .lowerBack: [.poly([
        CGPoint(x: 83, y: 186), CGPoint(x: 117, y: 186), CGPoint(x: 115, y: 216),
        CGPoint(x: 85, y: 216)
    ])],
    .delts: both(.oval(cx: 44, cy: 72, rx: 13, ry: 13)),
    .triceps: both(bicepsShape),
    .forearms: both(forearmShape),
    .glutes: both(.oval(cx: 77, cy: 226, rx: 16, ry: 14)),
    .hamstrings: both(hamstringShape),
    .calves: both(calfBackShape)
]

/// Left half of the body outline, top (neck) to bottom (inner ankle), crotch last.
private let outlineLeft: [CGPoint] = [
    CGPoint(x: 88, y: 46), CGPoint(x: 72, y: 52), CGPoint(x: 54, y: 58), CGPoint(x: 42, y: 64),
    CGPoint(x: 37, y: 72), CGPoint(x: 35, y: 108), CGPoint(x: 37, y: 148), CGPoint(x: 39, y: 192),
    CGPoint(x: 34, y: 198), CGPoint(x: 33, y: 228), CGPoint(x: 37, y: 236), CGPoint(x: 45, y: 232),
    CGPoint(x: 49, y: 200), CGPoint(x: 51, y: 156), CGPoint(x: 54, y: 118), CGPoint(x: 58, y: 124),
    CGPoint(x: 63, y: 130), CGPoint(x: 64, y: 166), CGPoint(x: 58, y: 178), CGPoint(x: 59, y: 238),
    CGPoint(x: 61, y: 296), CGPoint(x: 63, y: 348), CGPoint(x: 59, y: 378), CGPoint(x: 59, y: 390),
    CGPoint(x: 74, y: 393), CGPoint(x: 86, y: 390), CGPoint(x: 87, y: 352), CGPoint(x: 91, y: 298),
    CGPoint(x: 95, y: 244), CGPoint(x: 100, y: 254)
]

private let bodyOutline: [CGPoint] =
    outlineLeft + outlineLeft.dropLast().reversed().map { CGPoint(x: 200 - $0.x, y: $0.y) }

private func scaledPolyPath(_ points: [CGPoint], sx: CGFloat, sy: CGFloat) -> Path {
    var path = Path()
    for (index, point) in points.enumerated() {
        let scaled = CGPoint(x: point.x * sx, y: point.y * sy)
        if index == 0 { path.move(to: scaled) } else { path.addLine(to: scaled) }
    }
    path.closeSubpath()
    return path
}

/// Closed path smoothed by running quadratic curves through edge midpoints.
private func smoothClosedPath(_ points: [CGPoint], sx: CGFloat, sy: CGFloat) -> Path {
    var path = Path()
    let n = points.count
    func pt(_ i: Int) -> CGPoint {
        let p = points[((i % n) + n) % n]
        return CGPoint(x: p.x * sx, y: p.y * sy)
    }
    func mid(_ a: CGPoint, _ b: CGPoint) -> CGPoint {
        CGPoint(x: (a.x + b.x) / 2, y: (a.y + b.y) / 2)
    }
    path.move(to: mid(pt(n - 1), pt(0)))
    for i in 0..<n {
        path.addQuadCurve(to: mid(pt(i), pt(i + 1)), control: pt(i))
    }
    path.closeSubpath()
    return path
}

private func shapePath(_ shape: SilShape, sx: CGFloat, sy: CGFloat) -> Path {
    switch shape {
    case .poly(let points):
        return scaledPolyPath(points, sx: sx, sy: sy)
    case .oval(let cx, let cy, let rx, let ry):
        return Path(ellipseIn: CGRect(
            x: (cx - rx) * sx, y: (cy - ry) * sy,
            width: 2 * rx * sx, height: 2 * ry * sy
        ))
    }
}

private let silhouetteBase = Color(hex: 0xFFE2E8F0)
private let silhouetteOutline = Color(hex: 0xFF94A3B8)
private let silhouetteSeparator = Color(hex: 0xFFCBD5E1)
private let musclePrimaryFill = Color(hex: 0xFFDC2626)
private let muscleSecondaryFill = Color(hex: 0xFFFCA5A5)

private struct BodyFigureView: View {
    let front: Bool
    let primary: Set<MuscleRegion>
    let secondary: Set<MuscleRegion>

    var body: some View {
        VStack(spacing: 4) {
            Canvas { context, size in
                let sx = size.width / 200
                let sy = size.height / 400
                let shapes = front ? frontShapes : backShapes

                // Neutral body: outline, neck, head.
                context.fill(smoothClosedPath(bodyOutline, sx: sx, sy: sy), with: .color(silhouetteBase))
                context.fill(
                    Path(CGRect(x: 90 * sx, y: 40 * sy, width: 20 * sx, height: 18 * sy)),
                    with: .color(silhouetteBase)
                )
                context.fill(
                    Path(ellipseIn: CGRect(x: 84 * sx, y: 9 * sy, width: 32 * sx, height: 40 * sy)),
                    with: .color(silhouetteBase)
                )

                // Trained regions.
                for (region, regionShapes) in shapes {
                    let fill: Color?
                    if primary.contains(region) {
                        fill = musclePrimaryFill
                    } else if secondary.contains(region) {
                        fill = muscleSecondaryFill
                    } else {
                        fill = nil
                    }
                    guard let fill else { continue }
                    for shape in regionShapes {
                        context.fill(shapePath(shape, sx: sx, sy: sy), with: .color(fill))
                    }
                }

                // Subtle separators between regions, then the outline on top.
                for (_, regionShapes) in shapes {
                    for shape in regionShapes {
                        context.stroke(
                            shapePath(shape, sx: sx, sy: sy),
                            with: .color(silhouetteSeparator),
                            lineWidth: 1
                        )
                    }
                }
                context.stroke(
                    smoothClosedPath(bodyOutline, sx: sx, sy: sy),
                    with: .color(silhouetteOutline),
                    lineWidth: 1.5
                )
            }
            .aspectRatio(0.5, contentMode: .fit)
            Text(front ? "FRONT" : "BACK")
                .font(.system(size: 11, weight: .bold))
                .foregroundColor(AppColors.textSecondary)
        }
    }
}

/// Front + back silhouettes with the given regions highlighted.
struct BodySilhouetteView: View {
    let primary: Set<MuscleRegion>
    let secondary: Set<MuscleRegion>

    var body: some View {
        HStack(spacing: 12) {
            BodyFigureView(front: true, primary: primary, secondary: secondary)
            BodyFigureView(front: false, primary: primary, secondary: secondary)
        }
    }
}

struct MuscleLegendView: View {
    var body: some View {
        HStack(spacing: 14) {
            swatch(musclePrimaryFill, "Primary")
            swatch(muscleSecondaryFill, "Secondary")
            swatch(silhouetteBase, "Not trained")
        }
    }

    private func swatch(_ color: Color, _ label: String) -> some View {
        HStack(spacing: 5) {
            RoundedRectangle(cornerRadius: 3)
                .fill(color)
                .frame(width: 12, height: 12)
            Text(label)
                .font(.system(size: 12))
                .foregroundColor(AppColors.textSecondary)
        }
    }
}

/// "Muscles trained" card for the main Workouts page: front + back silhouettes
/// for the most recent day with logged workouts (all of that day's exercises
/// count — a training day is usually split across several logged exercises).
struct MusclesTrainedCard: View {
    @ObservedObject private var store = WorkoutStore.shared

    private static let dayFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "EEE, MMM d"
        return f
    }()

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Muscles trained")
                .font(.system(size: 17, weight: .bold))
                .foregroundColor(AppColors.textPrimary)
            if store.loggedWorkouts.isEmpty {
                BodySilhouetteView(primary: [], secondary: [])
                Text("Complete a workout to see which muscles you trained")
                    .font(.system(size: 13))
                    .foregroundColor(AppColors.textSecondary)
            } else {
                let cal = Calendar.current
                let latestDay = cal.startOfDay(for: store.loggedWorkouts[0].date)
                let dayWorkouts = store.loggedWorkouts.filter {
                    cal.isDate($0.date, inSameDayAs: latestDay)
                }
                let hits = MuscleMap.hits(workouts: dayWorkouts, exercises: store.exercises)
                Text("Latest workout day · \(Self.dayFormatter.string(from: latestDay)) · \(dayWorkouts.count) exercise\(dayWorkouts.count == 1 ? "" : "s")")
                    .font(.system(size: 13))
                    .foregroundColor(AppColors.textSecondary)
                BodySilhouetteView(primary: hits.primary, secondary: hits.secondary)
                MuscleLegendView()
                if !hits.primary.isEmpty {
                    Text("Primary: " + hits.primary.map(\.label).sorted().joined(separator: ", "))
                        .font(.system(size: 13))
                        .foregroundColor(AppColors.textPrimary)
                }
                if !hits.secondary.isEmpty {
                    Text("Secondary: " + hits.secondary.map(\.label).sorted().joined(separator: ", "))
                        .font(.system(size: 13))
                        .foregroundColor(AppColors.textSecondary)
                }
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 20))
        .shadow(color: .black.opacity(0.04), radius: 4, y: 2)
    }
}

// MARK: - Per-workout radar chart

struct RadarMetric {
    let label: String
    let shortLabel: String
    let fraction: Double
    let valueLabel: String
}

/// Radar metrics for one workout. Each axis is normalized 0-100 against the
/// user's personal best for that metric across ALL logged workouts, so a
/// single-workout history reads full scale.
func workoutRadarMetrics(
    workout: LoggedWorkout,
    allWorkouts: [LoggedWorkout],
    unit: WeightUnit
) -> [RadarMetric] {
    func best(_ selector: (LoggedWorkout) -> Double) -> Double {
        allWorkouts.map(selector).max() ?? 0
    }
    func fraction(_ value: Double, _ bestValue: Double) -> Double {
        bestValue <= 0 ? 0 : min(max(value / bestValue, 0), 1)
    }

    let maxWeight = workout.maxWeightKg
    let volume = workout.totalVolumeKg
    let duration = Double(workout.durationMinutes)
    let sets = Double(workout.sets.count)
    let reps = Double(workout.totalReps)

    return [
        RadarMetric(
            label: "Max weight", shortLabel: "Max",
            fraction: fraction(maxWeight, best { $0.maxWeightKg }),
            valueLabel: WeightUnits.label(kg: maxWeight, unit: unit)
        ),
        RadarMetric(
            label: "Volume", shortLabel: "Volume",
            fraction: fraction(volume, best { $0.totalVolumeKg }),
            valueLabel: WeightUnits.label(kg: volume, unit: unit)
        ),
        RadarMetric(
            label: "Time", shortLabel: "Time",
            fraction: fraction(duration, best { Double($0.durationMinutes) }),
            valueLabel: "\(workout.durationMinutes) min"
        ),
        RadarMetric(
            label: "Sets", shortLabel: "Sets",
            fraction: fraction(sets, best { Double($0.sets.count) }),
            valueLabel: "\(workout.sets.count)"
        ),
        RadarMetric(
            label: "Reps", shortLabel: "Reps",
            fraction: fraction(reps, best { Double($0.totalReps) }),
            valueLabel: "\(workout.totalReps)"
        )
    ]
}

/// Spider/radar chart drawn with SwiftUI Canvas only — polygon, rings, spokes, labels.
struct WorkoutRadarChartView: View {
    let metrics: [RadarMetric]

    var body: some View {
        Canvas { context, size in
            guard !metrics.isEmpty else { return }
            let center = CGPoint(x: size.width / 2, y: size.height / 2)
            let radius = min(size.width, size.height) / 2 * 0.76
            let n = metrics.count
            let gridColor = Color(hex: 0xFFE2E8F0)
            let accent = Color(hex: 0xFF2563EB)

            func vertex(_ index: Int, _ fraction: Double) -> CGPoint {
                let angle = -Double.pi / 2 + Double(index) * (2 * Double.pi / Double(n))
                return CGPoint(
                    x: center.x + radius * fraction * cos(angle),
                    y: center.y + radius * fraction * sin(angle)
                )
            }
            func polygon(_ fraction: (Int) -> Double) -> Path {
                var path = Path()
                for i in 0..<n {
                    let v = vertex(i, fraction(i))
                    if i == 0 { path.move(to: v) } else { path.addLine(to: v) }
                }
                path.closeSubpath()
                return path
            }

            // Grid rings at 25/50/75/100%.
            for ring in [0.25, 0.5, 0.75, 1.0] {
                context.stroke(polygon { _ in ring }, with: .color(gridColor), lineWidth: 1)
            }
            // Spokes.
            for i in 0..<n {
                var spoke = Path()
                spoke.move(to: center)
                spoke.addLine(to: vertex(i, 1))
                context.stroke(spoke, with: .color(gridColor), lineWidth: 1)
            }
            // Data polygon.
            let data = polygon { metrics[$0].fraction }
            context.fill(data, with: .color(accent.opacity(0.28)))
            context.stroke(data, with: .color(accent), lineWidth: 2.5)
            for (index, metric) in metrics.enumerated() {
                let v = vertex(index, metric.fraction)
                context.fill(
                    Path(ellipseIn: CGRect(x: v.x - 4, y: v.y - 4, width: 8, height: 8)),
                    with: .color(accent)
                )
            }
            // Axis labels just outside the ring.
            for (index, metric) in metrics.enumerated() {
                let labelPos = vertex(index, 1.18)
                context.draw(
                    Text(metric.shortLabel)
                        .font(.system(size: 11, weight: .bold))
                        .foregroundColor(Color(hex: 0xFF475569)),
                    at: labelPos
                )
            }
        }
        .aspectRatio(1, contentMode: .fit)
    }
}

// MARK: - Per-workout detail

/// Full per-workout detail: sets (unit-aware), radar chart and muscles hit.
struct WorkoutDetailView: View {
    let workout: LoggedWorkout
    let allWorkouts: [LoggedWorkout]
    let exercises: [Exercise]
    let unit: WeightUnit

    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    Text("\(workout.date, style: .date) · \(workout.durationMinutes) min · \(workout.sets.count) sets · \(workout.totalReps) reps")
                        .font(.system(size: 14))
                        .foregroundColor(AppColors.textSecondary)

                    radarCard
                    musclesCard
                    setsCard
                }
                .padding(16)
            }
            .background(AppColors.surface)
            .navigationTitle(workout.exerciseName)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { dismiss() }
                }
            }
        }
    }

    private var metrics: [RadarMetric] {
        workoutRadarMetrics(workout: workout, allWorkouts: allWorkouts, unit: unit)
    }

    private var radarCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Performance radar")
                .font(.system(size: 16, weight: .bold))
                .foregroundColor(AppColors.textPrimary)
            Text("Each axis is scored against your personal best")
                .font(.system(size: 13))
                .foregroundColor(AppColors.textSecondary)
            WorkoutRadarChartView(metrics: metrics)
            Text(metrics.map { "\($0.label): \($0.valueLabel)" }.joined(separator: "  ·  "))
                .font(.system(size: 13))
                .foregroundColor(AppColors.textSecondary)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 20))
        .shadow(color: .black.opacity(0.04), radius: 4, y: 2)
    }

    private var musclesCard: some View {
        let hits = MuscleMap.hits(workouts: [workout], exercises: exercises)
        return VStack(alignment: .leading, spacing: 10) {
            Text("Muscles hit")
                .font(.system(size: 16, weight: .bold))
                .foregroundColor(AppColors.textPrimary)
            BodySilhouetteView(primary: hits.primary, secondary: hits.secondary)
            MuscleLegendView()
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 20))
        .shadow(color: .black.opacity(0.04), radius: 4, y: 2)
    }

    private var setsCard: some View {
        let otherUnit = WeightUnits.other(unit)
        return VStack(alignment: .leading, spacing: 8) {
            Text("Sets")
                .font(.system(size: 16, weight: .bold))
                .foregroundColor(AppColors.textPrimary)
            ForEach(workout.sets.indices, id: \.self) { index in
                let set = workout.sets[index]
                HStack {
                    Text("Set \(index + 1)")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(AppColors.textPrimary)
                    Spacer()
                    VStack(alignment: .trailing, spacing: 1) {
                        Text("\(set.reps) reps × \(WeightUnits.label(kg: set.weightKg, unit: unit))")
                            .font(.system(size: 14))
                            .foregroundColor(AppColors.textPrimary)
                        if set.weightKg > 0 {
                            Text("≈ \(WeightUnits.label(kg: set.weightKg, unit: otherUnit))")
                                .font(.system(size: 12))
                                .foregroundColor(AppColors.textSecondary)
                        }
                    }
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 9)
                .background(AppColors.surface)
                .clipShape(RoundedRectangle(cornerRadius: 12))
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 20))
        .shadow(color: .black.opacity(0.04), radius: 4, y: 2)
    }
}
