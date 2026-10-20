package ca.zeezaglobal.gymsuitapp.data.model

import java.util.UUID

/**
 * Exercise catalog bundled in assets/exercises.json. Exercise metadata (names,
 * muscle data, instructions) sourced from DuarteSantos8/openGym's EXDB dataset.
 * Exercise GIFs are our own dataset, served by the backend on demand
 * (see ExerciseMedia) — the old Gym Visual remote GIFs are no longer used.
 */
data class Exercise(
    val id: String,
    val name: String,
    val bodyPart: String,
    val equipment: String,
    val primaryMuscle: String,
    val secondaryMuscles: List<String>,
    val instructions: List<String>,
    val gif: String,
    val img: String
) {
    val muscleGroup: String get() = ExerciseMuscleGroups.groupFor(bodyPart)
    /**
     * Bundled exercise GIF (own dataset), served by the backend on demand.
     * Null when this exercise has no GIF yet — the old Gym Visual remote
     * GIFs are no longer used.
     */
    val gifUrl: String?
        get() = ExerciseMedia.localGifAsset(name)
            ?.let { ExerciseMedia.GIF_BASE_URL + it }
    val imageUrl: String? get() = img.takeIf { it.isNotEmpty() }?.let { ExerciseMedia.IMAGE_BASE_URL + it }
    val primaryMuscleLabel: String get() =
        if (primaryMuscle.isNotEmpty()) primaryMuscle.replaceFirstChar { it.uppercaseChar() }
        else bodyPart.replaceFirstChar { it.uppercaseChar() }
}

object ExerciseMedia {
    const val IMAGE_BASE_URL = "https://raw.githubusercontent.com/hasaneyldrm/exercises-dataset/main/images/"

    /**
     * Backend endpoint serving the exercise GIFs on demand, so the app
     * doesn't bundle them. Same hardcoded host as the other API clients —
     * repoint it together with them when moving to the self-hosted server.
     */
    const val GIF_BASE_URL = "https://api.gymsuit.app/api/exercise-gifs/"

    /**
     * Exercise GIF dataset index (own GIFs, served by the backend).
     * An exercise shows a GIF only if its exact (lowercased) name is in
     * this map. To add a GIF: drop the file in the backend's
     * src/main/resources/exercise-gifs/ and add one line here.
     * NOTE: squat.gif is on the backend but unmapped — the catalog has no
     * plain bodyweight-squat entry. Map it when one is added.
     */
    private val LOCAL_GIFS = mapOf(
        "barbell bench press" to "bench-press.gif",
        "barbell full squat" to "weighted-squat.gif",
        "barbell deadlift" to "deadlift.gif",
        "dumbbell biceps curl" to "dumbbell-curl.gif",
        "dumbbell hammer curl" to "hammer-curl.gif",
        "lever seated fly" to "machine-chest-fly.gif",
        "dumbbell front raise" to "dumbbell-front-raise.gif",
        "cable pulldown" to "cable-pulldown.gif"
    )

    fun localGifAsset(exerciseName: String): String? =
        LOCAL_GIFS[exerciseName.lowercase().trim()]
}

object ExerciseMuscleGroups {
    val all = listOf("Chest", "Back", "Shoulders", "Arms", "Legs", "Core", "Cardio", "Other")

    fun groupFor(bodyPart: String): String = when (bodyPart) {
        "chest" -> "Chest"
        "back" -> "Back"
        "shoulders" -> "Shoulders"
        "upper arms", "lower arms" -> "Arms"
        "upper legs", "lower legs" -> "Legs"
        "waist" -> "Core"
        "cardio" -> "Cardio"
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

