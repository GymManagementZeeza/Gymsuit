package ca.zeezaglobal.gymsuitapp.data.model

import androidx.annotation.DrawableRes
import ca.zeezaglobal.gymsuitapp.R

/**
 * The two goals offered by the workout onboarding.
 *
 * Mirror of the iOS `WorkoutGoal` enum (Models/WorkoutGoal.swift). The drawable
 * resources are the exact PNG files from the iOS asset catalog
 * (goal_lose_weight / goal_build_muscle / goal_lose_weight_blue imagesets),
 * copied unchanged into res/drawable-nodpi.
 */
enum class WorkoutGoal(
    val rawValue: String,
    val title: String,
    val emoji: String,
    val bannerTitle: String,
    val bannerDescription: String,
    @DrawableRes val imageRes: Int,
    @DrawableRes val blueImageRes: Int
) {
    LOSE_WEIGHT(
        rawValue = "lose_weight",
        title = "Lose Weight",
        emoji = "🤗",
        bannerTitle = "Slim & fit ahead!",
        bannerDescription = "Get ready to see a healthier, lighter you. Follow our tailored plan to conquer your fitness goal!",
        imageRes = R.drawable.goal_lose_weight,
        blueImageRes = R.drawable.goal_lose_weight_blue
    ),
    BUILD_MUSCLE(
        rawValue = "build_muscle",
        title = "Gain Weight",
        emoji = "💪",
        bannerTitle = "Gain weight & strength ahead!",
        bannerDescription = "Get ready to build lean mass, boost power, and reach your target weight with progressive overload training!",
        imageRes = R.drawable.goal_build_muscle,
        blueImageRes = R.drawable.goal_build_muscle
    );

    companion object {
        fun fromRawValue(rawValue: String?): WorkoutGoal =
            values().firstOrNull { it.rawValue == rawValue } ?: LOSE_WEIGHT
    }
}
