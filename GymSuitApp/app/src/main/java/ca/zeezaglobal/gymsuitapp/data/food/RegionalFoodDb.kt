package ca.zeezaglobal.gymsuitapp.data.food

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import org.json.JSONObject

@Entity(tableName = "regional_foods")
data class RegionalFoodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val region: String,
    val name: String,
    val serving: String,
    val kcal: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
    val aliases: String
) {
    fun toFoodItem() = FoodItem(name, serving, kcal, proteinG, carbsG, fatG, aliases)
}

@Dao
interface RegionalFoodDao {
    @Query("SELECT COUNT(*) FROM regional_foods")
    suspend fun count(): Int

    @Insert
    suspend fun insertAll(foods: List<RegionalFoodEntity>)

    @Insert
    suspend fun insert(food: RegionalFoodEntity)

    @Query("SELECT COUNT(*) FROM regional_foods WHERE LOWER(name) = LOWER(:name)")
    suspend fun countByName(name: String): Int

    @Query("DELETE FROM regional_foods WHERE region = :region")
    suspend fun deleteRegion(region: String)

    /** Every word must appear in the name or aliases. Prefix matches on the name come first. */
    @Query(
        """
        SELECT * FROM regional_foods
        WHERE (name LIKE '%' || :q || '%' OR aliases LIKE '%' || :q || '%')
        ORDER BY (name LIKE :q || '%') DESC, LENGTH(name)
        LIMIT :limit
        """
    )
    suspend fun search(q: String, limit: Int): List<RegionalFoodEntity>
}

@Database(entities = [RegionalFoodEntity::class], version = 1, exportSchema = false)
abstract class RegionalFoodDatabase : RoomDatabase() {
    abstract fun dao(): RegionalFoodDao

    companion object {
        @Volatile private var instance: RegionalFoodDatabase? = null

        fun get(context: Context): RegionalFoodDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext, RegionalFoodDatabase::class.java, "regional_foods.db"
            ).build().also { instance = it }
        }
    }
}

/**
 * Seeds the Room database from `assets/kerala_foods.json` and serves searches.
 * Bump "version" in the JSON file to replace the stored rows with the new file contents.
 */
object RegionalFoodRepository {
    private const val ASSET = "kerala_foods.json"
    private const val PREFS = "regional_food_seed"

    @Volatile private var seeded = false

    private suspend fun ensureSeeded(context: Context, dao: RegionalFoodDao) {
        if (seeded) return
        val json = JSONObject(context.assets.open(ASSET).bufferedReader().use { it.readText() })
        val version = json.optInt("version", 1)
        val region = json.optString("region", "Kerala")
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (dao.count() == 0 || prefs.getInt(ASSET, 0) != version) {
            val arr = json.getJSONArray("foods")
            val rows = List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                RegionalFoodEntity(
                    region = region,
                    name = o.getString("name"),
                    serving = o.optString("serving"),
                    kcal = o.optInt("kcal"),
                    proteinG = o.optInt("proteinG"),
                    carbsG = o.optInt("carbsG"),
                    fatG = o.optInt("fatG"),
                    aliases = o.optString("aliases")
                )
            }
            dao.deleteRegion(region)
            dao.insertAll(rows)
            prefs.edit().putInt(ASSET, version).apply()
        }
        restoreCustom(context, dao)
        seeded = true
    }

    private const val CUSTOM_REGION = "Custom"
    private const val CUSTOM_FILE = "custom_foods.json"

    /** Re-inserts user-added foods from their JSON file if the database lost them. */
    private suspend fun restoreCustom(context: Context, dao: RegionalFoodDao) {
        val file = java.io.File(context.filesDir, CUSTOM_FILE)
        if (!file.exists()) return
        val arr = runCatching { JSONObject(file.readText()).getJSONArray("foods") }.getOrNull() ?: return
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val name = o.getString("name")
            if (dao.countByName(name) == 0) {
                dao.insert(
                    RegionalFoodEntity(
                        region = CUSTOM_REGION, name = name, serving = o.optString("serving"),
                        kcal = o.optInt("kcal"), proteinG = o.optInt("proteinG"), carbsG = o.optInt("carbsG"),
                        fatG = o.optInt("fatG"), aliases = o.optString("aliases")
                    )
                )
            }
        }
    }

    /**
     * Saves a manually entered food so it is searchable next time: inserted into the Room
     * database and appended to `custom_foods.json` in the app's files directory.
     * (The bundled asset file is read-only, so user additions live in their own file.)
     */
    suspend fun addCustom(context: Context, food: FoodItem) {
        val dao = RegionalFoodDatabase.get(context).dao()
        ensureSeeded(context, dao)
        if (dao.countByName(food.name) > 0) return
        dao.insert(
            RegionalFoodEntity(
                region = CUSTOM_REGION, name = food.name, serving = food.serving, kcal = food.kcal,
                proteinG = food.proteinG, carbsG = food.carbsG, fatG = food.fatG, aliases = food.aliases
            )
        )
        val file = java.io.File(context.filesDir, CUSTOM_FILE)
        val root = runCatching { JSONObject(file.readText()) }.getOrNull()
            ?: JSONObject().put("version", 1).put("region", CUSTOM_REGION).put("foods", org.json.JSONArray())
        root.getJSONArray("foods").put(
            JSONObject()
                .put("name", food.name).put("serving", food.serving).put("kcal", food.kcal)
                .put("proteinG", food.proteinG).put("carbsG", food.carbsG).put("fatG", food.fatG)
                .put("aliases", food.aliases)
        )
        file.writeText(root.toString(1))
    }

    suspend fun search(context: Context, query: String, limit: Int = 6): List<FoodItem> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        val dao = RegionalFoodDatabase.get(context).dao()
        ensureSeeded(context, dao)
        // Match on the longest typed word first, then require every word to be present
        val words = q.lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
        return dao.search(words.maxBy { it.length }, 50)
            .filter { e -> words.all { it in "${e.name} ${e.aliases}".lowercase() } }
            .take(limit)
            .map { it.toFoodItem() }
    }
}
