package ca.zeezaglobal.gymsuitapp.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class PointsEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestampMillis: Long = System.currentTimeMillis(),
    val points: Int,
    val reason: String,
    val rupeeValue: Double? = null
) {
    val isRedemption: Boolean get() = points < 0
}

object PointsConfig {
    /** Points awarded for simply logging a workout. */
    const val POINTS_PER_WORKOUT = 50
    /** Points awarded per logged set. */
    const val POINTS_PER_SET = 5
    /** Points awarded per minute of workout duration. */
    const val POINTS_PER_MINUTE = 2
    /** How many points equal one Indian rupee. */
    const val POINTS_PER_RUPEE = 100

    fun rupeesFor(points: Int): Double = points / POINTS_PER_RUPEE.toDouble()

    fun formattedRupees(points: Int): String = "₹%.2f".format(rupeesFor(points))

    fun pointsForWorkout(setCount: Int, durationMinutes: Int): Int =
        POINTS_PER_WORKOUT + POINTS_PER_SET * setCount + POINTS_PER_MINUTE * durationMinutes
}

/**
 * Persists the user's points balance and history as JSON in the app's private
 * files dir. Points are earned by logging workouts. Exposes balance/history as
 * StateFlows so the top-bar pill animates whenever they change.
 */
object PointsStore {
    private var pointsFile: File? = null

    private val _balance = MutableStateFlow(0)
    val balance: StateFlow<Int> = _balance.asStateFlow()

    private val _history = MutableStateFlow<List<PointsEntry>>(emptyList())
    val history: StateFlow<List<PointsEntry>> = _history.asStateFlow()

    @Synchronized
    fun init(context: Context) {
        if (pointsFile != null) return
        pointsFile = File(context.applicationContext.filesDir, "gymsuit_points.json")
        load()
    }

    fun redeemableRupees(): Double = PointsConfig.rupeesFor(_balance.value)

    /** Awards points for a logged workout and returns the amount earned. */
    @Synchronized
    fun awardForWorkout(exerciseName: String, setCount: Int, durationMinutes: Int): Int {
        val earned = PointsConfig.pointsForWorkout(setCount, durationMinutes)
        _balance.value = _balance.value + earned
        _history.value = listOf(
            PointsEntry(points = earned, reason = "Workout: $exerciseName")
        ) + _history.value
        persist()
        return earned
    }

    /**
     * Redeems points for rupees. Returns the rupee amount, or null when the
     * amount is invalid or exceeds the balance.
     */
    @Synchronized
    fun redeem(points: Int): Double? {
        if (points <= 0 || points > _balance.value) return null
        val rupees = PointsConfig.rupeesFor(points)
        _balance.value = _balance.value - points
        _history.value = listOf(
            PointsEntry(points = -points, reason = "Redeemed for cash", rupeeValue = rupees)
        ) + _history.value
        persist()
        return rupees
    }

    @Synchronized
    fun mergeRemote(remoteBalance: Int?, remoteTransactions: List<ca.zeezaglobal.gymsuitapp.data.model.PointsTransactionDto>?) {
        if (remoteTransactions == null && remoteBalance == null) return
        val currentMap = _history.value.associateBy { it.id }.toMutableMap()
        remoteTransactions?.forEach { r ->
            if (!currentMap.containsKey(r.id)) {
                val epoch = try {
                    if (r.createdAt != null) {
                        java.time.LocalDateTime.parse(r.createdAt).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                    } else System.currentTimeMillis()
                } catch (e: Exception) {
                    System.currentTimeMillis()
                }
                currentMap[r.id] = PointsEntry(
                    id = r.id,
                    timestampMillis = epoch,
                    points = r.points,
                    reason = r.reason,
                    rupeeValue = r.rupeeValue
                )
            }
        }
        val mergedList = currentMap.values.sortedByDescending { it.timestampMillis }
        _history.value = mergedList
        _balance.value = remoteBalance ?: mergedList.sumOf { it.points }
        persist()
    }

    private fun load() {
        val file = pointsFile ?: return
        try {
            if (!file.exists()) return
            val o = JSONObject(file.readText())
            _balance.value = o.optInt("balance")
            val arr = o.optJSONArray("history") ?: JSONArray()
            _history.value = List(arr.length()) { i ->
                val e = arr.getJSONObject(i)
                PointsEntry(
                    id = e.optString("id", UUID.randomUUID().toString()),
                    timestampMillis = e.optLong("timestampMillis", System.currentTimeMillis()),
                    points = e.optInt("points"),
                    reason = e.optString("reason"),
                    rupeeValue = if (e.has("rupeeValue") && !e.isNull("rupeeValue")) e.optDouble("rupeeValue") else null
                )
            }.sortedByDescending { it.timestampMillis }
        } catch (e: Exception) {
            Log.e("PointsStore", "Failed to load points", e)
        }
    }

    private fun persist() {
        val file = pointsFile ?: return
        try {
            val historyArr = JSONArray()
            _history.value.forEach { e ->
                historyArr.put(
                    JSONObject()
                        .put("id", e.id)
                        .put("timestampMillis", e.timestampMillis)
                        .put("points", e.points)
                        .put("reason", e.reason)
                        .put("rupeeValue", e.rupeeValue ?: JSONObject.NULL)
                )
            }
            file.writeText(
                JSONObject()
                    .put("balance", _balance.value)
                    .put("history", historyArr)
                    .toString()
            )
        } catch (e: Exception) {
            Log.e("PointsStore", "Failed to save points", e)
        }
    }
}
