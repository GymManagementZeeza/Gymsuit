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
    private val syncPrefs = context.getSharedPreferences("workout_sync_prefs", Context.MODE_PRIVATE)

    private fun loadExercises(): List<Exercise> {
        return try {
            val json = context.assets.open("exercises.json").bufferedReader().use { it.readText() }
            val arr = JSONArray(json)
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                Exercise(
                    id = o.optString("id"),
                    name = o.optString("name"),
                    bodyPart = o.optString("body_part"),
                    equipment = o.optString("equipment"),
                    primaryMuscle = o.optString("primary_muscle"),
                    secondaryMuscles = o.optJSONArray("secondary_muscles")?.toStringList() ?: emptyList(),
                    instructions = o.optJSONArray("instructions")?.toStringList() ?: emptyList(),
                    gif = o.optString("gif"),
                    img = o.optString("img")
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
                    syncedToHealth = o.optBoolean("syncedToHealth"),
                    caloriesBurned = if (o.has("caloriesBurned") && !o.isNull("caloriesBurned"))
                        o.optInt("caloriesBurned") else null
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

    fun upsertWorkout(workout: LoggedWorkout): List<LoggedWorkout> {
        val current = getLoggedWorkouts().toMutableList()
        val idx = current.indexOfFirst { it.id == workout.id }
        if (idx >= 0) {
            current[idx] = workout
        } else {
            current.add(0, workout)
        }
        val updated = current.sortedByDescending { it.timestampMillis }
        persist(updated)
        return updated
    }

    /** Ids of workouts pulled from the backend (logged on another device or an
     *  MCP client). They are excluded from the health-sync upload so re-sending
     *  them can never overwrite the server's stored calorie estimate. */
    fun serverOriginIds(): Set<String> =
        syncPrefs.getStringSet(KEY_SERVER_ORIGIN_IDS, emptySet()) ?: emptySet()

    private fun markServerOrigin(ids: Collection<String>) {
        syncPrefs.edit().putStringSet(KEY_SERVER_ORIGIN_IDS, serverOriginIds() + ids).apply()
    }

    /**
     * Merges workouts pulled from the backend into the local log. Only sessions
     * not already present (by id) are added, so a locally-logged workout is
     * never overwritten. Added workouts are recorded as server-origin so the
     * upload skips them. Returns the merged list (unchanged if nothing new).
     */
    fun mergeFromServer(serverWorkouts: List<LoggedWorkout>): List<LoggedWorkout> {
        val current = getLoggedWorkouts()
        val serverIds = serverOriginIds()
        val byId = current.associateBy { it.id }
        // New sessions are added; sessions already pulled earlier are refreshed
        // (so calories/edits from the server show). Locally-logged ones are
        // never overwritten.
        val fresh = serverWorkouts.filter { it.id.isNotBlank() && it.id !in byId }
        val refreshed = serverWorkouts.filter { it.id in serverIds && it.id in byId }
            .associateBy { it.id }
        if (fresh.isEmpty() && refreshed.isEmpty()) return current
        markServerOrigin(fresh.map { it.id })
        val updated = (current.map { refreshed[it.id] ?: it } + fresh)
            .sortedByDescending { it.timestampMillis }
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

    /** Removes every logged workout (iOS `WorkoutStore.clearAllWorkouts()`). */
    fun clearAllWorkouts() {
        persist(emptyList())
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
                    w.caloriesBurned?.let { put("caloriesBurned", it) }
                })
            }
            logFile.writeText(arr.toString())
        } catch (e: Exception) {
            Log.e("WorkoutStore", "Failed to persist workout log", e)
        }
    }

    private fun JSONArray.toStringList(): List<String> =
        List(length()) { i -> optString(i) }

    private companion object {
        const val KEY_SERVER_ORIGIN_IDS = "server_origin_workout_ids"
    }
}
