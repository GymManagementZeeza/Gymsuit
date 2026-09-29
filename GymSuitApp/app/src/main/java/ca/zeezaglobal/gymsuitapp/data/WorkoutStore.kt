package ca.zeezaglobal.gymsuitapp.data

import android.content.Context
import android.util.Log
import ca.zeezaglobal.gymsuitapp.data.model.Exercise
import ca.zeezaglobal.gymsuitapp.data.model.LoggedWorkout
import ca.zeezaglobal.gymsuitapp.data.model.WorkoutSet
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Loads the bundled exercise catalog and persists the user's logged workouts
 * as JSON in the app's private files dir. Logged workouts are also written to
 * Health Connect (see HealthConnectManager.insertExerciseSession) so they
 * appear in the user's health apps alongside everything else.
 */
class WorkoutStore(private val context: Context) {

    val exercises: List<Exercise> = loadExercises()
    private val logFile = File(context.filesDir, "logged_workouts.json")

    private fun loadExercises(): List<Exercise> {
        return try {
            val json = context.assets.open("exercises.json").bufferedReader().use { it.readText() }
            val arr = JSONArray(json)
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                Exercise(
                    id = o.optString("id"),
                    name = o.optString("name"),
                    description = o.optString("description"),
                    difficulty = o.optInt("difficulty", 3),
                    equipment = o.optJSONArray("equipment")?.toStringList() ?: emptyList(),
                    primaryMuscle = o.optString("primary_muscle"),
                    secondaryMuscles = o.optJSONArray("secondary_muscles")?.toStringList() ?: emptyList(),
                    typicalSetsReps = o.optString("typical_sets_reps").takeIf { it.isNotEmpty() },
                    executionTips = o.optJSONArray("execution_tips")?.toStringList() ?: emptyList()
                )
            }.sortedBy { it.name }
        } catch (e: Exception) {
            Log.e("WorkoutStore", "Failed to load exercises.json", e)
            emptyList()
        }
    }

    fun filteredExercises(query: String, muscleGroup: String?): List<Exercise> {
        return exercises.filter { e ->
            val matchesQuery = query.isBlank() ||
                e.name.contains(query, ignoreCase = true) ||
                e.primaryMuscle.contains(query, ignoreCase = true)
            val matchesGroup = muscleGroup == null || e.muscleGroup == muscleGroup
            matchesQuery && matchesGroup
        }
    }

    fun getLoggedWorkouts(): List<LoggedWorkout> {
        return try {
            if (!logFile.exists()) return emptyList()
            val arr = JSONArray(logFile.readText())
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                LoggedWorkout(
                    id = o.optString("id"),
                    exerciseId = o.optString("exerciseId"),
                    exerciseName = o.optString("exerciseName"),
                    timestampMillis = o.optLong("timestampMillis"),
                    sets = o.optJSONArray("sets")?.let { sa ->
                        List(sa.length()) { j ->
                            val s = sa.getJSONObject(j)
                            WorkoutSet(
                                reps = s.optInt("reps"),
                                weightKg = s.optDouble("weightKg")
                            )
                        }
                    } ?: emptyList(),
                    durationMinutes = o.optInt("durationMinutes"),
                    notes = o.optString("notes"),
                    syncedToHealth = o.optBoolean("syncedToHealth")
                )
            }.sortedByDescending { it.timestampMillis }
        } catch (e: Exception) {
            Log.e("WorkoutStore", "Failed to read workout log", e)
            emptyList()
        }
    }

    fun logWorkout(workout: LoggedWorkout): List<LoggedWorkout> {
        val updated = (getLoggedWorkouts() + workout).sortedByDescending { it.timestampMillis }
        persist(updated)
        return updated
    }

    fun markSynced(id: String): List<LoggedWorkout> {
        val updated = getLoggedWorkouts().map {
            if (it.id == id) it.copy(syncedToHealth = true) else it
        }
        persist(updated)
        return updated
    }

    fun deleteWorkout(id: String): List<LoggedWorkout> {
        val updated = getLoggedWorkouts().filter { it.id != id }
        persist(updated)
        return updated
    }

    private fun persist(workouts: List<LoggedWorkout>) {
        try {
            val arr = JSONArray()
            workouts.forEach { w ->
                val setsArr = JSONArray()
                w.sets.forEach { s ->
                    setsArr.put(JSONObject().apply {
                        put("reps", s.reps)
                        put("weightKg", s.weightKg)
                    })
                }
                arr.put(JSONObject().apply {
                    put("id", w.id)
                    put("exerciseId", w.exerciseId)
                    put("exerciseName", w.exerciseName)
                    put("timestampMillis", w.timestampMillis)
                    put("sets", setsArr)
                    put("durationMinutes", w.durationMinutes)
                    put("notes", w.notes)
                    put("syncedToHealth", w.syncedToHealth)
                })
            }
            logFile.writeText(arr.toString())
        } catch (e: Exception) {
            Log.e("WorkoutStore", "Failed to persist workout log", e)
        }
    }

    private fun JSONArray.toStringList(): List<String> =
        List(length()) { i -> optString(i) }
}
