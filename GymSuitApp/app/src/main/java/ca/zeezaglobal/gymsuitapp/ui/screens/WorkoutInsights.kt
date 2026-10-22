package ca.zeezaglobal.gymsuitapp.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ca.zeezaglobal.gymsuitapp.data.WeightUnit
import ca.zeezaglobal.gymsuitapp.data.WeightUnits
import ca.zeezaglobal.gymsuitapp.data.WorkoutStore
import ca.zeezaglobal.gymsuitapp.data.model.Exercise
import ca.zeezaglobal.gymsuitapp.data.model.LoggedWorkout
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// MARK: - Shared colors (match WorkoutStatsCards)

private val InsightInk = Color(0xFF0F172A)
private val InsightMuted = Color(0xFF64748B)

// MARK: - Weight unit toggle

/** Reads the persisted lb/kg preference and returns it with a setter that also persists. */
@Composable
fun rememberWeightUnit(): Pair<WeightUnit, (WeightUnit) -> Unit> {
    val context = LocalContext.current
    var unit by remember { mutableStateOf(WeightUnits.load(context)) }
    val setUnit: (WeightUnit) -> Unit = { newUnit ->
        unit = newUnit
        WeightUnits.save(context, newUnit)
    }
    return unit to setUnit
}

/** Small segmented lb | kg switch used by the workout logging UI. */
@Composable
fun WeightUnitToggle(
    unit: WeightUnit,
    onSelect: (WeightUnit) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF1F5F9))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        WeightUnit.values().forEach { option ->
            val selected = option == unit
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected) Color.White else Color.Transparent)
                    .clickable { onSelect(option) }
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option.symbol,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) InsightInk else InsightMuted
                )
            }
        }
    }
}

// MARK: - Muscle regions + dataset mapping

/** Silhouette regions the body figure is partitioned into. */
enum class MuscleRegion(val label: String) {
    CHEST("Chest"),
    DELTS("Shoulders"),
    BICEPS("Biceps"),
    TRICEPS("Triceps"),
    FOREARMS("Forearms"),
    ABS("Abs & obliques"),
    QUADS("Quadriceps"),
    CALVES("Calves"),
    TRAPS("Traps"),
    BACK("Back & lats"),
    LOWER_BACK("Lower back"),
    GLUTES("Glutes"),
    HAMSTRINGS("Hamstrings")
}

data class MuscleHits(
    val primary: Set<MuscleRegion>,
    val secondary: Set<MuscleRegion>
)

/**
 * Maps the bundled EXDB dataset's muscle strings onto silhouette regions.
 * Covers every distinct body_part / primary_muscle / secondary_muscles value
 * in exercises.json; values with no visible region (cardiovascular system,
 * ankles, feet, shins, ankle stabilizers) intentionally map to null.
 */
object MuscleMap {

    fun regionFor(name: String): MuscleRegion? = when (name.trim().lowercase()) {
        "pectorals", "chest", "upper chest", "serratus anterior" -> MuscleRegion.CHEST
        "delts", "deltoids", "shoulders", "rear deltoids", "rotator cuff" -> MuscleRegion.DELTS
        "biceps", "brachialis" -> MuscleRegion.BICEPS
        "triceps" -> MuscleRegion.TRICEPS
        "forearms", "wrist flexors", "wrist extensors", "wrists", "grip muscles", "hands" ->
            MuscleRegion.FOREARMS
        "abs", "abdominals", "lower abs", "obliques", "core" -> MuscleRegion.ABS
        "quads", "quadriceps", "adductors", "abductors", "inner thighs", "groin",
        "hip flexors" -> MuscleRegion.QUADS
        "calves", "soleus" -> MuscleRegion.CALVES
        "traps", "trapezius", "levator scapulae", "sternocleidomastoid" -> MuscleRegion.TRAPS
        "lats", "latissimus dorsi", "upper back", "rhomboids", "back" -> MuscleRegion.BACK
        "lower back", "spine" -> MuscleRegion.LOWER_BACK
        "glutes" -> MuscleRegion.GLUTES
        "hamstrings" -> MuscleRegion.HAMSTRINGS
        else -> null
    }

    /** Fallback used only when an exercise has no primary muscle string. */
    fun regionForBodyPart(bodyPart: String): MuscleRegion? = when (bodyPart.trim().lowercase()) {
        "chest" -> MuscleRegion.CHEST
        "back" -> MuscleRegion.BACK
        "shoulders" -> MuscleRegion.DELTS
        "upper arms" -> MuscleRegion.BICEPS
        "lower arms" -> MuscleRegion.FOREARMS
        "waist" -> MuscleRegion.ABS
        "upper legs" -> MuscleRegion.QUADS
        "lower legs" -> MuscleRegion.CALVES
        "neck" -> MuscleRegion.TRAPS
        else -> null
    }

    /** Primary = solid red regions, secondary = light red regions, for a set of logged workouts. */
    fun hits(workouts: List<LoggedWorkout>, exercises: List<Exercise>): MuscleHits {
        val byId = exercises.associateBy { it.id }
        val byName = exercises.associateBy { it.name.lowercase() }
        val primary = mutableSetOf<MuscleRegion>()
        val secondary = mutableSetOf<MuscleRegion>()
        for (workout in workouts) {
            val exercise = byId[workout.exerciseId]
                ?: byName[workout.exerciseName.lowercase()]
                ?: continue
            (regionFor(exercise.primaryMuscle) ?: regionForBodyPart(exercise.bodyPart))
                ?.let { primary.add(it) }
            for (muscle in exercise.secondaryMuscles) {
                regionFor(muscle)?.let { secondary.add(it) }
            }
        }
        secondary.removeAll(primary)
        return MuscleHits(primary, secondary)
    }
}

// MARK: - Body silhouette (vector-drawn, no image assets)
//
// The figure lives in a 200 x 400 coordinate space. Left-half shapes are
// mirrored to the right half (x -> 200 - x) so both sides stay symmetric.

private sealed interface BodyShape {
    data class Poly(val points: List<Offset>) : BodyShape
    data class Oval(val cx: Float, val cy: Float, val rx: Float, val ry: Float) : BodyShape
}

private fun poly(vararg coords: Float): BodyShape.Poly {
    val points = mutableListOf<Offset>()
    var i = 0
    while (i < coords.size) {
        points.add(Offset(coords[i], coords[i + 1]))
        i += 2
    }
    return BodyShape.Poly(points)
}

private fun oval(cx: Float, cy: Float, rx: Float, ry: Float) = BodyShape.Oval(cx, cy, rx, ry)

private fun mirrored(shape: BodyShape): BodyShape = when (shape) {
    is BodyShape.Poly -> BodyShape.Poly(shape.points.map { Offset(200f - it.x, it.y) })
    is BodyShape.Oval -> BodyShape.Oval(200f - shape.cx, shape.cy, shape.rx, shape.ry)
}

private fun both(shape: BodyShape): List<BodyShape> = listOf(shape, mirrored(shape))

private val BicepsShape = poly(37f, 84f, 53f, 82f, 55f, 116f, 43f, 120f, 36f, 102f)
private val ForearmShape = poly(36f, 122f, 52f, 120f, 50f, 168f, 44f, 230f, 35f, 226f, 34f, 160f)
private val QuadShape = poly(59f, 180f, 95f, 178f, 93f, 236f, 87f, 260f, 68f, 260f, 61f, 234f, 57f, 204f)
private val CalfFrontShape = poly(63f, 266f, 89f, 264f, 87f, 346f, 67f, 346f, 62f, 300f)
private val HamstringShape = poly(61f, 246f, 93f, 244f, 91f, 294f, 85f, 308f, 69f, 308f, 62f, 292f)
private val CalfBackShape = poly(64f, 314f, 89f, 312f, 87f, 350f, 67f, 350f, 63f, 332f)

private val FrontShapes: Map<MuscleRegion, List<BodyShape>> = mapOf(
    MuscleRegion.CHEST to listOf(
        poly(68f, 64f, 132f, 64f, 128f, 100f, 120f, 118f, 80f, 118f, 72f, 100f)
    ),
    MuscleRegion.DELTS to both(oval(44f, 72f, 13f, 13f)),
    MuscleRegion.BICEPS to both(BicepsShape),
    MuscleRegion.FOREARMS to both(ForearmShape),
    MuscleRegion.ABS to listOf(
        poly(79f, 122f, 121f, 122f, 119f, 166f, 115f, 198f, 85f, 198f, 81f, 166f)
    ),
    MuscleRegion.QUADS to both(QuadShape),
    MuscleRegion.CALVES to both(CalfFrontShape)
)

private val BackShapes: Map<MuscleRegion, List<BodyShape>> = mapOf(
    MuscleRegion.TRAPS to listOf(
        poly(80f, 54f, 120f, 54f, 134f, 82f, 100f, 102f, 66f, 82f)
    ),
    MuscleRegion.BACK to listOf(
        poly(63f, 86f, 137f, 86f, 129f, 148f, 119f, 184f, 81f, 184f, 71f, 148f)
    ),
    MuscleRegion.LOWER_BACK to listOf(
        poly(83f, 186f, 117f, 186f, 115f, 216f, 85f, 216f)
    ),
    MuscleRegion.DELTS to both(oval(44f, 72f, 13f, 13f)),
    MuscleRegion.TRICEPS to both(BicepsShape),
    MuscleRegion.FOREARMS to both(ForearmShape),
    MuscleRegion.GLUTES to both(oval(77f, 226f, 16f, 14f)),
    MuscleRegion.HAMSTRINGS to both(HamstringShape),
    MuscleRegion.CALVES to both(CalfBackShape)
)

/** Left half of the body outline, top (neck) to bottom (inner ankle), crotch last. */
private val OutlineLeft: List<Offset> = listOf(
    Offset(88f, 46f), Offset(72f, 52f), Offset(54f, 58f), Offset(42f, 64f),
    Offset(37f, 72f), Offset(35f, 108f), Offset(37f, 148f), Offset(39f, 192f),
    Offset(34f, 198f), Offset(33f, 228f), Offset(37f, 236f), Offset(45f, 232f),
    Offset(49f, 200f), Offset(51f, 156f), Offset(54f, 118f), Offset(58f, 124f),
    Offset(63f, 130f), Offset(64f, 166f), Offset(58f, 178f), Offset(59f, 238f),
    Offset(61f, 296f), Offset(63f, 348f), Offset(59f, 378f), Offset(59f, 390f),
    Offset(74f, 393f), Offset(86f, 390f), Offset(87f, 352f), Offset(91f, 298f),
    Offset(95f, 244f), Offset(100f, 254f)
)

private val BodyOutline: List<Offset> =
    OutlineLeft + OutlineLeft.dropLast(1).reversed().map { Offset(200f - it.x, it.y) }

private fun polyPath(points: List<Offset>, sx: Float, sy: Float): Path {
    val path = Path()
    points.forEachIndexed { index, point ->
        if (index == 0) path.moveTo(point.x * sx, point.y * sy)
        else path.lineTo(point.x * sx, point.y * sy)
    }
    path.close()
    return path
}

/** Closed path smoothed by running quadratic curves through edge midpoints. */
private fun smoothClosedPath(points: List<Offset>, sx: Float, sy: Float): Path {
    val path = Path()
    val n = points.size
    fun pt(i: Int): Offset {
        val p = points[((i % n) + n) % n]
        return Offset(p.x * sx, p.y * sy)
    }
    fun mid(a: Offset, b: Offset) = Offset((a.x + b.x) / 2f, (a.y + b.y) / 2f)
    val start = mid(pt(n - 1), pt(0))
    path.moveTo(start.x, start.y)
    for (i in 0 until n) {
        val control = pt(i)
        val end = mid(pt(i), pt(i + 1))
        path.quadraticBezierTo(control.x, control.y, end.x, end.y)
    }
    path.close()
    return path
}

private val SilhouetteBase = Color(0xFFE2E8F0)
private val SilhouetteOutline = Color(0xFF94A3B8)
private val SilhouetteSeparator = Color(0xFFCBD5E1)
private val MusclePrimaryFill = Color(0xFFDC2626)
private val MuscleSecondaryFill = Color(0xFFFCA5A5)

@Composable
private fun BodyFigure(
    front: Boolean,
    primary: Set<MuscleRegion>,
    secondary: Set<MuscleRegion>,
    modifier: Modifier = Modifier
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(modifier = modifier.aspectRatio(0.5f)) {
            val sx = size.width / 200f
            val sy = size.height / 400f
            val shapes = if (front) FrontShapes else BackShapes

            // Neutral body: outline, neck, head.
            drawPath(smoothClosedPath(BodyOutline, sx, sy), color = SilhouetteBase)
            drawRect(
                color = SilhouetteBase,
                topLeft = Offset(90f * sx, 40f * sy),
                size = Size(20f * sx, 18f * sy)
            )
            drawOval(
                color = SilhouetteBase,
                topLeft = Offset(84f * sx, 9f * sy),
                size = Size(32f * sx, 40f * sy)
            )

            // Trained regions.
            for ((region, regionShapes) in shapes) {
                val fill = when {
                    region in primary -> MusclePrimaryFill
                    region in secondary -> MuscleSecondaryFill
                    else -> null
                } ?: continue
                for (shape in regionShapes) {
                    when (shape) {
                        is BodyShape.Poly -> drawPath(polyPath(shape.points, sx, sy), color = fill)
                        is BodyShape.Oval -> drawOval(
                            color = fill,
                            topLeft = Offset((shape.cx - shape.rx) * sx, (shape.cy - shape.ry) * sy),
                            size = Size(2f * shape.rx * sx, 2f * shape.ry * sy)
                        )
                    }
                }
            }

            // Subtle separators between regions, then the outline on top.
            for ((_, regionShapes) in shapes) {
                for (shape in regionShapes) {
                    when (shape) {
                        is BodyShape.Poly -> drawPath(
                            polyPath(shape.points, sx, sy),
                            color = SilhouetteSeparator,
                            style = Stroke(width = 1f)
                        )
                        is BodyShape.Oval -> drawOval(
                            color = SilhouetteSeparator,
                            topLeft = Offset((shape.cx - shape.rx) * sx, (shape.cy - shape.ry) * sy),
                            size = Size(2f * shape.rx * sx, 2f * shape.ry * sy),
                            style = Stroke(width = 1f)
                        )
                    }
                }
            }
            drawPath(
                smoothClosedPath(BodyOutline, sx, sy),
                color = SilhouetteOutline,
                style = Stroke(width = 1.5f)
            )
        }
        Text(
            text = if (front) "FRONT" else "BACK",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = InsightMuted
        )
    }
}

/** Front + back silhouettes with the given regions highlighted. */
@Composable
fun BodySilhouette(
    primary: Set<MuscleRegion>,
    secondary: Set<MuscleRegion>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center
    ) {
        BodyFigure(front = true, primary = primary, secondary = secondary, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        BodyFigure(front = false, primary = primary, secondary = secondary, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun MuscleLegend() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendSwatch(MusclePrimaryFill, "Primary")
        LegendSwatch(MuscleSecondaryFill, "Secondary")
        LegendSwatch(SilhouetteBase, "Not trained")
    }
}

@Composable
private fun LegendSwatch(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
        Spacer(Modifier.width(5.dp))
        Text(text = label, fontSize = 12.sp, color = InsightMuted)
    }
}

private fun LoggedWorkout.insightLocalDate(): LocalDate =
    Instant.ofEpochMilli(timestampMillis).atZone(ZoneId.systemDefault()).toLocalDate()

/**
 * "Muscles trained" card for the main Workouts page: front + back silhouettes
 * for the most recent day with logged workouts (all of that day's exercises
 * count — a training day is usually split across several logged exercises).
 */
@Composable
fun MusclesTrainedCard(workoutStore: WorkoutStore) {
    val workouts = remember { workoutStore.getLoggedWorkouts() }
    val exercises = remember { workoutStore.exercises }
    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Muscles trained",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = InsightInk
            )
            if (workouts.isEmpty()) {
                BodySilhouette(emptySet(), emptySet(), modifier = Modifier.fillMaxWidth())
                Text(
                    text = "Complete a workout to see which muscles you trained",
                    style = MaterialTheme.typography.bodySmall,
                    color = InsightMuted
                )
            } else {
                val latestDay = workouts.first().insightLocalDate()
                val dayWorkouts = workouts.filter { it.insightLocalDate() == latestDay }
                val hits = MuscleMap.hits(dayWorkouts, exercises)
                Text(
                    text = "Latest workout day · " +
                        latestDay.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())) +
                        " · ${dayWorkouts.size} exercise${if (dayWorkouts.size == 1) "" else "s"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = InsightMuted
                )
                BodySilhouette(hits.primary, hits.secondary, modifier = Modifier.fillMaxWidth())
                MuscleLegend()
                if (hits.primary.isNotEmpty()) {
                    Text(
                        text = "Primary: " + hits.primary.joinToString(", ") { it.label },
                        style = MaterialTheme.typography.bodySmall,
                        color = InsightInk
                    )
                }
                if (hits.secondary.isNotEmpty()) {
                    Text(
                        text = "Secondary: " + hits.secondary.joinToString(", ") { it.label },
                        style = MaterialTheme.typography.bodySmall,
                        color = InsightMuted
                    )
                }
            }
        }
    }
}

/** Emits the muscles-trained card + recent-workouts card as LazyColumn items. */
fun LazyListScope.workoutInsightItems(workoutStore: WorkoutStore) {
    item(key = "muscles_trained_card") {
        MusclesTrainedCard(workoutStore)
    }
    item(key = "recent_workouts_card") {
        RecentWorkoutsCard(workoutStore)
    }
}

// MARK: - Per-workout radar chart

data class RadarMetric(
    val label: String,
    val shortLabel: String,
    val fraction: Float,
    val valueLabel: String
)

/**
 * Radar metrics for one workout. Each axis is normalized 0-100 against the
 * user's personal best for that metric across ALL logged workouts, so a
 * single-workout history reads full scale.
 */
fun workoutRadarMetrics(
    workout: LoggedWorkout,
    allWorkouts: List<LoggedWorkout>,
    unit: WeightUnit
): List<RadarMetric> {
    fun best(selector: (LoggedWorkout) -> Double): Double =
        allWorkouts.maxOfOrNull(selector) ?: 0.0
    fun fraction(value: Double, bestValue: Double): Float =
        if (bestValue <= 0.0) 0f else (value / bestValue).toFloat().coerceIn(0f, 1f)

    val maxWeight = workout.maxWeightKg
    val bestMaxWeight = best { it.maxWeightKg }
    val volume = workout.totalVolumeKg
    val bestVolume = best { it.totalVolumeKg }
    val duration = workout.durationMinutes.toDouble()
    val bestDuration = best { it.durationMinutes.toDouble() }
    val sets = workout.sets.size.toDouble()
    val bestSets = best { it.sets.size.toDouble() }
    val reps = workout.totalReps.toDouble()
    val bestReps = best { it.totalReps.toDouble() }

    return listOf(
        RadarMetric("Max weight", "Max", fraction(maxWeight, bestMaxWeight), WeightUnits.label(maxWeight, unit)),
        RadarMetric("Volume", "Volume", fraction(volume, bestVolume), WeightUnits.label(volume, unit)),
        RadarMetric("Time", "Time", fraction(duration, bestDuration), "${workout.durationMinutes} min"),
        RadarMetric("Sets", "Sets", fraction(sets, bestSets), "${workout.sets.size}"),
        RadarMetric("Reps", "Reps", fraction(reps, bestReps), "${workout.totalReps}")
    )
}

/** Spider/radar chart drawn with Compose Canvas only — polygon, rings, spokes, labels. */
@Composable
fun WorkoutRadarChart(
    metrics: List<RadarMetric>,
    modifier: Modifier = Modifier
) {
    val accent = Color(0xFF2563EB)
    val grid = Color(0xFFE2E8F0)
    val labelPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.rgb(0x47, 0x55, 0x69)
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.create(
                android.graphics.Typeface.DEFAULT,
                android.graphics.Typeface.BOLD
            )
            isAntiAlias = true
        }
    }
    Canvas(modifier = modifier.aspectRatio(1f)) {
        if (metrics.isEmpty()) return@Canvas
        labelPaint.textSize = 11.sp.toPx()
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = min(size.width, size.height) / 2f * 0.76f
        val n = metrics.size
        fun vertex(index: Int, fraction: Float): Offset {
            val angle = -PI / 2 + index * (2 * PI / n)
            return Offset(
                cx + (radius * fraction * cos(angle)).toFloat(),
                cy + (radius * fraction * sin(angle)).toFloat()
            )
        }
        // Grid rings at 25/50/75/100%.
        for (ring in listOf(0.25f, 0.5f, 0.75f, 1f)) {
            val ringPath = Path()
            for (i in 0 until n) {
                val v = vertex(i, ring)
                if (i == 0) ringPath.moveTo(v.x, v.y) else ringPath.lineTo(v.x, v.y)
            }
            ringPath.close()
            drawPath(ringPath, color = grid, style = Stroke(width = 1f))
        }
        // Spokes.
        for (i in 0 until n) {
            val end = vertex(i, 1f)
            drawLine(color = grid, start = Offset(cx, cy), end = end, strokeWidth = 1f)
        }
        // Data polygon.
        val dataPath = Path()
        metrics.forEachIndexed { index, metric ->
            val v = vertex(index, metric.fraction)
            if (index == 0) dataPath.moveTo(v.x, v.y) else dataPath.lineTo(v.x, v.y)
        }
        dataPath.close()
        drawPath(dataPath, color = accent.copy(alpha = 0.28f))
        drawPath(dataPath, color = accent, style = Stroke(width = 2.5f))
        metrics.forEachIndexed { index, metric ->
            drawCircle(color = accent, radius = 4f, center = vertex(index, metric.fraction))
        }
        // Axis labels just outside the ring.
        metrics.forEachIndexed { index, metric ->
            val labelPos = vertex(index, 1.18f)
            drawContext.canvas.nativeCanvas.drawText(
                metric.shortLabel,
                labelPos.x,
                labelPos.y + labelPaint.textSize / 3f,
                labelPaint
            )
        }
    }
}

@Composable
private fun RadarValuesLine(metrics: List<RadarMetric>) {
    Text(
        text = metrics.joinToString("  ·  ") { "${it.label}: ${it.valueLabel}" },
        style = MaterialTheme.typography.bodySmall,
        color = InsightMuted
    )
}

// MARK: - Recent workouts + per-workout detail

@Composable
fun RecentWorkoutsCard(workoutStore: WorkoutStore) {
    val workouts = remember { workoutStore.getLoggedWorkouts() }
    if (workouts.isEmpty()) return
    val exercises = remember { workoutStore.exercises }
    val (unit, _) = rememberWeightUnit()
    var detailWorkout by remember { mutableStateOf<LoggedWorkout?>(null) }

    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Recent workouts",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = InsightInk
            )
            Text(
                text = "Tap a workout for its radar chart",
                style = MaterialTheme.typography.bodySmall,
                color = InsightMuted
            )
            workouts.take(5).forEach { workout ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { detailWorkout = workout }
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = workout.exerciseName.replaceFirstChar { it.uppercaseChar() },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = InsightInk
                        )
                        Text(
                            text = workout.insightLocalDate().format(
                                DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = InsightMuted
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = WeightUnits.label(workout.totalVolumeKg, unit),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2563EB)
                        )
                        Text(
                            text = "${workout.sets.size} sets",
                            style = MaterialTheme.typography.bodySmall,
                            color = InsightMuted
                        )
                    }
                }
            }
        }
    }

    detailWorkout?.let { workout ->
        WorkoutDetailDialog(
            workout = workout,
            allWorkouts = workouts,
            exercises = exercises,
            unit = unit,
            onDismiss = { detailWorkout = null }
        )
    }
}

/** Full per-workout detail: sets (unit-aware), radar chart and muscles hit. */
@Composable
fun WorkoutDetailDialog(
    workout: LoggedWorkout,
    allWorkouts: List<LoggedWorkout>,
    exercises: List<Exercise>,
    unit: WeightUnit,
    onDismiss: () -> Unit
) {
    val metrics = workoutRadarMetrics(workout, allWorkouts, unit)
    val hits = MuscleMap.hits(listOf(workout), exercises)
    val otherUnit = WeightUnits.other(unit)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFF8F9FA)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Surface(color = Color.White, shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = workout.exerciseName.replaceFirstChar { it.uppercaseChar() },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = InsightInk,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 12.dp)
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close",
                                tint = InsightInk
                            )
                        }
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = workout.insightLocalDate().format(
                            DateTimeFormatter.ofPattern("EEEE, MMM d yyyy", Locale.getDefault())
                        ) + " · ${workout.durationMinutes} min · ${workout.sets.size} sets · ${workout.totalReps} reps",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InsightMuted
                    )

                    ElevatedCard(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Performance radar",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = InsightInk
                            )
                            Text(
                                text = "Each axis is scored against your personal best",
                                style = MaterialTheme.typography.bodySmall,
                                color = InsightMuted
                            )
                            WorkoutRadarChart(metrics, modifier = Modifier.fillMaxWidth())
                            RadarValuesLine(metrics)
                        }
                    }

                    ElevatedCard(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Muscles hit",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = InsightInk
                            )
                            BodySilhouette(hits.primary, hits.secondary, modifier = Modifier.fillMaxWidth())
                            MuscleLegend()
                        }
                    }

                    ElevatedCard(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Sets",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = InsightInk
                            )
                            workout.sets.forEachIndexed { index, set ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Set ${index + 1}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = InsightInk,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${set.reps} reps × ${WeightUnits.label(set.weightKg, unit)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = InsightInk
                                        )
                                        Text(
                                            text = "≈ ${WeightUnits.label(set.weightKg, otherUnit)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = InsightMuted
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(60.dp))
                }
            }
        }
    }
}
