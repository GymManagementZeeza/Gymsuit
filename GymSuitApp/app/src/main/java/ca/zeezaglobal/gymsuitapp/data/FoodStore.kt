package ca.zeezaglobal.gymsuitapp.data

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

enum class MealType(val label: String) { BREAKFAST("Breakfast"), LUNCH("Lunch"), DINNER("Dinner"), SNACK("Snacks") }

data class FoodEntry(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val meal: MealType,
    val calories: Int,
    val proteinG: Double = 0.0,
    val carbsG: Double = 0.0,
    val fatG: Double = 0.0,
    val timestampMillis: Long
)

/** Persists food log entries as JSON in the app's private files dir. */
class FoodStore(context: Context) {
    private val appContext = context.applicationContext
    private val file = File(context.filesDir, "food_log.json")
    private val syncPrefs = context.getSharedPreferences("food_sync_prefs", Context.MODE_PRIVATE)

    /** Entries the backend has confirmed. Logged entries never change, so each one uploads once. */
    fun uploadedIds(): Set<String> = syncPrefs.getStringSet(KEY_UPLOADED_IDS, emptySet()) ?: emptySet()

    fun markUploaded(ids: Collection<String>) {
        syncPrefs.edit().putStringSet(KEY_UPLOADED_IDS, uploadedIds() + ids).apply()
    }

    /** Remembers a local delete until the backend confirms it, so offline deletes aren't lost. */
    fun queueRemoteDelete(id: String) {
        syncPrefs.edit().putStringSet(KEY_PENDING_DELETES, pendingRemoteDeletes() + id).apply()
    }

    fun pendingRemoteDeletes(): Set<String> =
        syncPrefs.getStringSet(KEY_PENDING_DELETES, emptySet()) ?: emptySet()

    fun clearRemoteDelete(id: String) {
        syncPrefs.edit().putStringSet(KEY_PENDING_DELETES, pendingRemoteDeletes() - id).apply()
    }

    /**
     * Call on login and registration. The log and photos belong to one account:
     * a different account starts empty instead of uploading them as its own,
     * while the same account logging back in keeps anything not yet uploaded.
     */
    fun claimFor(email: String) {
        val user = email.trim().lowercase()
        if (user.isEmpty()) return
        val owner = syncPrefs.getString(KEY_OWNER, null)
        if (owner != null && owner != user) {
            file.delete()
            syncPrefs.edit().clear().apply()
            FoodPhotoStore.clearAll(appContext)
        }
        syncPrefs.edit().putString(KEY_OWNER, user).apply()
    }

    fun getAll(): List<FoodEntry> = try {
        if (!file.exists()) emptyList() else {
            val arr = JSONArray(file.readText())
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                FoodEntry(
                    id = o.optString("id"),
                    name = o.optString("name"),
                    meal = runCatching { MealType.valueOf(o.optString("meal")) }.getOrDefault(MealType.SNACK),
                    calories = o.optInt("calories"),
                    proteinG = o.optDouble("proteinG", 0.0),
                    carbsG = o.optDouble("carbsG", 0.0),
                    fatG = o.optDouble("fatG", 0.0),
                    timestampMillis = o.optLong("timestampMillis")
                )
            }.sortedBy { it.timestampMillis }
        }
    } catch (e: Exception) {
        Log.e("FoodStore", "Failed to read food log", e)
        emptyList()
    }

    fun add(entry: FoodEntry): List<FoodEntry> = persist(getAll() + entry)

    fun delete(id: String): List<FoodEntry> = persist(getAll().filter { it.id != id })

    private fun persist(entries: List<FoodEntry>): List<FoodEntry> {
        try {
            val arr = JSONArray()
            entries.forEach { e ->
                arr.put(JSONObject().apply {
                    put("id", e.id)
                    put("name", e.name)
                    put("meal", e.meal.name)
                    put("calories", e.calories)
                    put("proteinG", e.proteinG)
                    put("carbsG", e.carbsG)
                    put("fatG", e.fatG)
                    put("timestampMillis", e.timestampMillis)
                })
            }
            file.writeText(arr.toString())
        } catch (e: Exception) {
            Log.e("FoodStore", "Failed to persist food log", e)
        }
        return entries.sortedBy { it.timestampMillis }
    }

    private companion object {
        const val KEY_UPLOADED_IDS = "uploaded_entry_ids"
        const val KEY_PENDING_DELETES = "pending_delete_ids"
        const val KEY_OWNER = "owner_email"
    }
}

/**
 * Splits entries into upload batches, keeping each request well under
 * nginx's 10 MB limit and the 20 s client timeout. A photo is sent as base64,
 * about a third larger than [photoSizes] says. An entry that alone exceeds
 * [maxPhotoBytes] still gets a batch of its own.
 */
internal fun batchFoodUploads(
    entries: List<FoodEntry>,
    photoSizes: Map<String, Long>,
    maxEntries: Int = 50,
    maxPhotoBytes: Long = 2_000_000
): List<List<FoodEntry>> {
    val batches = mutableListOf<List<FoodEntry>>()
    var current = mutableListOf<FoodEntry>()
    var currentBytes = 0L
    for (entry in entries) {
        val size = photoSizes[entry.id] ?: 0L
        if (current.isNotEmpty() && (current.size >= maxEntries || currentBytes + size > maxPhotoBytes)) {
            batches += current
            current = mutableListOf()
            currentBytes = 0L
        }
        current += entry
        currentBytes += size
    }
    if (current.isNotEmpty()) {
        batches += current
    }
    return batches
}
