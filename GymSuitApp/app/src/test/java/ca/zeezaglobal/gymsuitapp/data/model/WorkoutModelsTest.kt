package ca.zeezaglobal.gymsuitapp.data.model

import org.junit.Assert.*
import org.junit.Test

class WorkoutModelsTest {

    @Test
    fun muscleGroups_mapBodyPartsCorrectly() {
        assertEquals("Chest", ExerciseMuscleGroups.groupFor("chest"))
        assertEquals("Back", ExerciseMuscleGroups.groupFor("back"))
        assertEquals("Shoulders", ExerciseMuscleGroups.groupFor("shoulders"))
        assertEquals("Arms", ExerciseMuscleGroups.groupFor("upper arms"))
        assertEquals("Arms", ExerciseMuscleGroups.groupFor("lower arms"))
        assertEquals("Legs", ExerciseMuscleGroups.groupFor("upper legs"))
        assertEquals("Legs", ExerciseMuscleGroups.groupFor("lower legs"))
        assertEquals("Core", ExerciseMuscleGroups.groupFor("waist"))
        assertEquals("Cardio", ExerciseMuscleGroups.groupFor("cardio"))
        assertEquals("Other", ExerciseMuscleGroups.groupFor("neck"))
        assertEquals("Other", ExerciseMuscleGroups.groupFor("unknown_part"))
    }

    @Test
    fun exercise_mediaUrlsAndLabels() {
        val exercise = Exercise(
            id = "ex-1",
            name = "Barbell Bench Press",
            bodyPart = "chest",
            equipment = "barbell",
            primaryMuscle = "pectorals",
            secondaryMuscles = listOf("triceps", "deltoids"),
            instructions = listOf("Lie on bench", "Press barbell"),
            gif = "bench.gif",
            img = "bench.jpg"
        )

        assertEquals("Chest", exercise.muscleGroup)
        assertEquals("Pectorals", exercise.primaryMuscleLabel)
        assertEquals(ExerciseMedia.GIF_BASE_URL + "bench.gif", exercise.gifUrl)
        assertEquals(ExerciseMedia.IMAGE_BASE_URL + "bench.jpg", exercise.imageUrl)
    }

    @Test
    fun loggedWorkout_computesTotalRepsAndVolume() {
        val sets = listOf(
            WorkoutSet(reps = 10, weightKg = 60.0), // 600 kg
            WorkoutSet(reps = 8, weightKg = 70.0),  // 560 kg
            WorkoutSet(reps = 6, weightKg = 80.0)   // 480 kg
        )

        val workout = LoggedWorkout(
            exerciseId = "bench_press",
            exerciseName = "Bench Press",
            timestampMillis = System.currentTimeMillis(),
            sets = sets,
            durationMinutes = 45,
            notes = "Good session"
        )

        assertEquals(24, workout.totalReps)
        assertEquals(1640.0, workout.totalVolumeKg, 0.001)
    }
}
