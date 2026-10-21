package ca.zeezaglobal.gymsuitapp.data

import android.content.Context
import java.util.Locale

/** Display unit for weights. Storage is ALWAYS kilograms (see below). */
enum class WeightUnit(val symbol: String) {
    KG("kg"),
    LB("lb")
}

/**
 * Weight unit preference + conversion helpers.
 *
 * Canonical storage unit is kilograms: `WorkoutSet.weightKg` has always been
 * entered and stored as kg (every weight field in the app was labelled
 * "Weight (kg)"), and volume totals / health sync are computed from it.
 * The lb/kg preference therefore only changes what the UI shows and what the
 * user types — entered values are converted to kg before they are stored,
 * and stored values are converted for display. Toggling the unit never
 * rewrites stored history.
 */
object WeightUnits {
    /** 1 kg = 2.20462 lb. */
    const val LB_PER_KG = 2.20462

    private const val PREFS_NAME = "gymsuit_workout_prefs"
    private const val KEY_WEIGHT_UNIT = "weight_unit"

    fun load(context: Context): WeightUnit {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_WEIGHT_UNIT, null)
        return if (raw == "lb") WeightUnit.LB else WeightUnit.KG
    }

    fun save(context: Context, unit: WeightUnit) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_WEIGHT_UNIT, unit.symbol)
            .apply()
    }

    fun other(unit: WeightUnit): WeightUnit =
        if (unit == WeightUnit.KG) WeightUnit.LB else WeightUnit.KG

    /** Stored kg -> value in the display unit. */
    fun kgToDisplay(kg: Double, unit: WeightUnit): Double =
        if (unit == WeightUnit.LB) kg * LB_PER_KG else kg

    /** Value typed in the display unit -> stored kg. */
    fun displayToKg(value: Double, unit: WeightUnit): Double =
        if (unit == WeightUnit.LB) value / LB_PER_KG else value

    /** One decimal place, trailing ".0" trimmed (100.0 -> "100", 45.36 -> "45.4"). */
    fun format(value: Double): String {
        val rounded = String.format(Locale.US, "%.1f", value)
        return if (rounded.endsWith(".0")) rounded.dropLast(2) else rounded
    }

    /** "100 kg" / "45.4 kg" style label for a stored kg value in the given unit. */
    fun label(kg: Double, unit: WeightUnit): String =
        "${format(kgToDisplay(kg, unit))} ${unit.symbol}"
}
