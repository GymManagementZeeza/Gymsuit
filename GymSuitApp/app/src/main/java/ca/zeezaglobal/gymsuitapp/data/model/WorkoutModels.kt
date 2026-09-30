package ca.zeezaglobal.gymsuitapp.data.model

import java.util.UUID

/**
 * Exercise catalog bundled in assets/exercises.json. Exercise metadata (names,
 * muscle data, instructions) sourced from DuarteSantos8/openGym's EXDB dataset.
 * The GIF/JPG media is (c) Gym Visual - see NOTICE in PR #22.
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
    val gifUrl: String? get() = gif.takeIf { it.isNotEmpty() }?.let { ExerciseMedia.GIF_BASE_URL + it }
    val imageUrl: String? get() = img.takeIf { it.isNotEmpty() }?.let { ExerciseMedia.IMAGE_BASE_URL + it }
    val primaryMuscleLabel: String get() =
        if (primaryMuscle.isNotEmpty()) primaryMuscle.replaceFirstChar { it.uppercaseChar() }
        else bodyPart.replaceFirstChar { it.uppercaseChar() }
}

object ExerciseMedia {
    const val GIF_BASE_URL = "https://raw.githubusercontent.com/hasaneyldrm/exercises-dataset/main/videos/"
    const val IMAGE_BASE_URL = "https://raw.githubusercontent.com/hasaneyldrm/exercises-dataset/main/images/"
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
