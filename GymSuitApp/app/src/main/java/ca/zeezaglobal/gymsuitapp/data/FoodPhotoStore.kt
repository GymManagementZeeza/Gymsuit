package ca.zeezaglobal.gymsuitapp.data

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import java.io.File
import java.io.FileOutputStream

/**
 * Keeps the compressed meal photo for each scanned food entry on-device
 * until the backend confirms it received it. Photos are stored as JPEG
 * (longest side 1024px, quality 75) under filesDir/food_photos/<entryId>.jpg
 * — never inside food_log.json, so the log itself stays small.
 *
 * Upload tracking lives in SharedPreferences: an entry id is marked
 * uploaded only after a successful sync, so a failed upload is retried by
 * the next sync instead of being lost, and already-uploaded photos are
 * never re-sent on every periodic sync.
 */
object FoodPhotoStore {

    private const val TAG = "FoodPhotoStore"
    private const val PREFS = "food_photo_prefs"
    private const val KEY_UPLOADED = "uploaded_photo_ids"
    private const val MAX_DIMENSION = 1024

    private fun dir(context: Context): File =
        File(context.filesDir, "food_photos").apply { mkdirs() }

    private fun fileFor(context: Context, entryId: String): File =
        File(dir(context), "$entryId.jpg")

    fun save(context: Context, entryId: String, bitmap: Bitmap): Boolean {
        return try {
            val scaled = scaleDown(bitmap)
            // Written to a temp file first: a sync must never pick up a half-written photo.
            val temp = File(dir(context), "$entryId.tmp")
            val written = FileOutputStream(temp).use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 75, out)
            }
            if (scaled !== bitmap) {
                scaled.recycle()
            }
            val saved = written && temp.renameTo(fileFor(context, entryId))
            if (!saved) {
                temp.delete()
            }
            saved
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save food photo: ${e.message}")
            false
        }
    }

    fun read(context: Context, entryId: String): ByteArray? {
        return try {
            val file = fileFor(context, entryId)
            if (file.exists()) file.readBytes() else null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read food photo: ${e.message}")
            null
        }
    }

    fun delete(context: Context, entryId: String) {
        try {
            fileFor(context, entryId).delete()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete food photo: ${e.message}")
        }
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val uploaded = prefs.getStringSet(KEY_UPLOADED, emptySet())?.toMutableSet() ?: mutableSetOf()
        if (uploaded.remove(entryId)) {
            prefs.edit().putStringSet(KEY_UPLOADED, uploaded).apply()
        }
    }

    /**
     * Sizes in bytes of the photos that exist locally but have not been
     * confirmed uploaded yet, keyed by entry id. Sizes rather than bytes, so a
     * backlog is only read into memory one upload batch at a time.
     */
    fun pendingPhotoSizes(context: Context): Map<String, Long> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val uploaded = prefs.getStringSet(KEY_UPLOADED, emptySet()) ?: emptySet()
        return dir(context).listFiles().orEmpty()
            .filter { it.extension == "jpg" && it.nameWithoutExtension !in uploaded }
            .associate { it.nameWithoutExtension to it.length() }
    }

    /** Removes every cached photo and its upload tracking, when the log changes owner. */
    fun clearAll(context: Context) {
        dir(context).deleteRecursively()
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }

    fun markUploaded(context: Context, entryIds: Collection<String>) {
        if (entryIds.isEmpty()) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val uploaded = prefs.getStringSet(KEY_UPLOADED, emptySet())?.toMutableSet() ?: mutableSetOf()
        uploaded.addAll(entryIds)
        prefs.edit().putStringSet(KEY_UPLOADED, uploaded).apply()
    }

    private fun scaleDown(bitmap: Bitmap): Bitmap {
        val maxSide = maxOf(bitmap.width, bitmap.height)
        if (maxSide <= MAX_DIMENSION) {
            return bitmap
        }
        val ratio = MAX_DIMENSION.toFloat() / maxSide
        val width = (bitmap.width * ratio).toInt().coerceAtLeast(1)
        val height = (bitmap.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }
}
