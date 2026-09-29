package ca.zeezaglobal.gymsuitapp.data.model

import java.util.UUID

/**
 * Exercise catalog bundled in assets/exercises.json, sourced from the
 * open-source Glowupp-app/open-exercisedb dataset (MIT licensed).
 */
data class Exercise(
    val id: String,
    val name: String,
    val description: String,
    val difficulty: Int,
    val equipment: List<String>,
    val primaryMuscle: String,
    val secondaryMuscles: List<String>,
    val typicalSetsReps: String?,
    val executionTips: List<String>
) {
    val muscleGroup: String get() = ExerciseMuscleGroups.groupFor(primaryMuscle)
    val equipmentLabel: String get() = equipment.joinToString(", ") {
        it.replaceFirstChar { c -> c.uppercaseChar() }
    }
}

object ExerciseMuscleGroups {
    val all = listOf("Chest", "Back", "Shoulders", "Arms", "Legs", "Core", "Full Body", "Cardio", "Other")

    fun groupFor(muscle: String): String = when (muscle) {
        "Chest", "Pectoralis Major" -> "Chest"
        "Back", "Back (Full)", "Latissimus Dorsi", "Rhomboids", "Erector Spinae" -> "Back"
        "Shoulders", "Deltoids" -> "Shoulders"
        "Biceps", "Biceps Brachii", "Brachialis", "Brachioradialis", "Triceps", "Forearms (Front)" -> "Arms"
        "Quadriceps", "Hamstrings", "Glutes", "Gluteus Medius", "Calves", "Gastrocnemius (Calves)" -> "Legs"
        "Core", "Rectus Abdominis", "Rectus Abdominis (Lower)", "Lower Rectus Abdominis", "Obliques" -> "Core"
        "Full Body" -> "Full Body"
        "Cardio" -> "Cardio"
        else -> "Other"
    }
}

data class WorkoutSet(
    var reps: Int = 10,
    var weightKg: Double = 0.0
)

data class LoggedWorkout(
    val id: String = UUID.randomUUID().toString(),
    val exerciseId: String,
    val exerciseName: String,
    val timestampMillis: Long,
    val sets: List<WorkoutSet>,
    val durationMinutes: Int,
    val notes: String,
    val syncedToHealth: Boolean = false
) {
    val totalReps: Int get() = sets.sumOf { it.reps }
    val totalVolumeKg: Double get() = sets.sumOf { it.reps * it.weightKg }
}

/** Parses "3x16-20" / "4x8-12" style typical set schemes into (sets, reps). */
fun parseTypicalSetsReps(value: String?): Pair<Int, Int> {
    if (value == null) return 3 to 10
    val match = Regex("""(\d+)\s*x\s*(\d+)""").find(value) ?: return 3 to 10
    val sets = match.groupValues[1].toIntOrNull() ?: 3
    val reps = match.groupValues[2].toIntOrNull() ?: 10
    return sets.coerceIn(1, 10) to reps.coerceIn(1, 100)
}
