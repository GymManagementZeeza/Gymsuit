package ca.zeezaglobal.gymsuitapp.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Local persistence for the workout onboarding (goal & body setup) flow.
 *
 * Mirror of the iOS `@AppStorage` keys used by WorkoutsView and
 * WorkoutOnboardingApi — same key names, same defaults:
 *
 *  - `workout_goal_setup_completed`   false
 *  - `workout_selected_goal`          "lose_weight"
 *  - `workout_user_height_cm`         172.72  (5 ft 8 in)
 *  - `workout_user_weight_kg`         74.526  (~164.3 lbs)
 *  - `workout_user_target_weight_kg`  71.0
 *  - `workout_user_birth_year`        2003
 *  - `workout_user_days_per_week`     2
 *
 * Doubles are stored as raw long bits because SharedPreferences has no
 * putDouble; the values round-trip exactly.
 */
class WorkoutOnboardingStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "gymsuit_workout_onboarding"

        const val KEY_SETUP_COMPLETED = "workout_goal_setup_completed"
        const val KEY_SELECTED_GOAL = "workout_selected_goal"
        const val KEY_HEIGHT_CM = "workout_user_height_cm"
        const val KEY_WEIGHT_KG = "workout_user_weight_kg"
        const val KEY_TARGET_WEIGHT_KG = "workout_user_target_weight_kg"
        const val KEY_BIRTH_YEAR = "workout_user_birth_year"
        const val KEY_DAYS_PER_WEEK = "workout_user_days_per_week"

        const val DEFAULT_GOAL = "lose_weight"
        const val DEFAULT_HEIGHT_CM = 172.72
        const val DEFAULT_WEIGHT_KG = 74.526
        const val DEFAULT_TARGET_WEIGHT_KG = 71.0
        const val DEFAULT_BIRTH_YEAR = 2003
        const val DEFAULT_DAYS_PER_WEEK = 2
    }

    var setupCompleted: Boolean
        get() = prefs.getBoolean(KEY_SETUP_COMPLETED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_SETUP_COMPLETED, value).apply()
        }

    var selectedGoalRaw: String
        get() = prefs.getString(KEY_SELECTED_GOAL, DEFAULT_GOAL) ?: DEFAULT_GOAL
        set(value) {
            prefs.edit().putString(KEY_SELECTED_GOAL, value).apply()
        }

    var heightCm: Double
        get() = getDouble(KEY_HEIGHT_CM, DEFAULT_HEIGHT_CM)
        set(value) {
            putDouble(KEY_HEIGHT_CM, value)
        }

    var weightKg: Double
        get() = getDouble(KEY_WEIGHT_KG, DEFAULT_WEIGHT_KG)
        set(value) {
            putDouble(KEY_WEIGHT_KG, value)
        }

    var targetWeightKg: Double
        get() = getDouble(KEY_TARGET_WEIGHT_KG, DEFAULT_TARGET_WEIGHT_KG)
        set(value) {
            putDouble(KEY_TARGET_WEIGHT_KG, value)
        }

    var birthYear: Int
        get() = prefs.getInt(KEY_BIRTH_YEAR, DEFAULT_BIRTH_YEAR)
        set(value) {
            prefs.edit().putInt(KEY_BIRTH_YEAR, value).apply()
        }

    var daysPerWeek: Int
        get() = prefs.getInt(KEY_DAYS_PER_WEEK, DEFAULT_DAYS_PER_WEEK)
        set(value) {
            prefs.edit().putInt(KEY_DAYS_PER_WEEK, value).apply()
        }

    /**
     * Mirror of the local cleanup in the iOS `deleteOnboardingData()`:
     * completion flag back to false, every other key removed.
     */
    fun clearOnboardingData() {
        prefs.edit()
            .putBoolean(KEY_SETUP_COMPLETED, false)
            .remove(KEY_SELECTED_GOAL)
            .remove(KEY_HEIGHT_CM)
            .remove(KEY_WEIGHT_KG)
            .remove(KEY_TARGET_WEIGHT_KG)
            .remove(KEY_BIRTH_YEAR)
            .remove(KEY_DAYS_PER_WEEK)
            .apply()
    }

    private fun getDouble(key: String, defaultValue: Double): Double =
        if (prefs.contains(key)) Double.fromBits(prefs.getLong(key, 0L)) else defaultValue

    private fun putDouble(key: String, value: Double) {
        prefs.edit().putLong(key, value.toRawBits()).apply()
    }
}
