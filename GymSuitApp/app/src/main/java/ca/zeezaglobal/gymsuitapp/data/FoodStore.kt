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
    private val file = File(context.filesDir, "food_log.json")

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
}
