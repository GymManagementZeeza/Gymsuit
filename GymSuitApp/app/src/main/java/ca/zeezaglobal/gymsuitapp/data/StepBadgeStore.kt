package ca.zeezaglobal.gymsuitapp.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import ca.zeezaglobal.gymsuitapp.data.model.StepBadge
import ca.zeezaglobal.gymsuitapp.data.remote.StepBadgeApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds the user's step milestone badges from the backend and tracks which
 * badge celebrations have been seen (SharedPreferences, so each badge's
 * celebration overlay shows exactly once).
 */
object StepBadgeStore {
    private val _badges = MutableStateFlow<List<StepBadge>>(emptyList())
    val badges: StateFlow<List<StepBadge>> = _badges.asStateFlow()

    private var prefs: SharedPreferences? = null

    private fun getPrefs(context: Context): SharedPreferences {
        var p = prefs
        if (p == null) {
            p = context.applicationContext.getSharedPreferences("gymsuit_step_badges", Context.MODE_PRIVATE)
            prefs = p
        }
        return p
    }

    /** Refreshes the badge list from the backend. Safe to call repeatedly. */
    suspend fun refresh(context: Context) {
        try {
            getPrefs(context) // ensure prefs are ready for seen tracking
            val result = StepBadgeApi(context).listBadges()
            result.fold(
                onSuccess = { _badges.value = it },
                onFailure = { e -> Log.w("StepBadgeStore", "refresh failed: ${e.message}") }
            )
        } catch (e: Exception) {
            Log.w("StepBadgeStore", "refresh failed: ${e.message}")
        }
    }

    fun isSeen(context: Context, id: Long): Boolean =
        getPrefs(context).getBoolean("seen_$id", false)

    fun markSeen(context: Context, id: Long) {
        getPrefs(context).edit().putBoolean("seen_$id", true).apply()
    }
}
