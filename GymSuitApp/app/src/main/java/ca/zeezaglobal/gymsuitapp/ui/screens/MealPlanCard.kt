package ca.zeezaglobal.gymsuitapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BakeryDining
import androidx.compose.material.icons.outlined.DinnerDining
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FreeBreakfast
import androidx.compose.material.icons.outlined.LunchDining
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import ca.zeezaglobal.gymsuitapp.data.remote.MealPlanApi
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

// Material motion tokens (m3.material.io/styles/motion): emphasized easing, medium durations.
private val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
private val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
private const val DURATION_MEDIUM_4 = 400
private const val DURATION_SHORT_4 = 200

private data class PlannedMeal(val label: String, val icon: ImageVector, val text: String?)

/**
 * Top card on the Food tab: the meal plan the member's nutritionist sent (today's, or the latest earlier one).
 * Tap the header to collapse or expand the meals.
 */
@Composable
fun MealPlanCard(plan: MealPlanApi.MealPlan, modifier: Modifier = Modifier) {
    var expanded by rememberSaveable(plan.id) { mutableStateOf(true) }
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(DURATION_SHORT_4, easing = EmphasizedDecelerate),
        label = "mealPlanArrow"
    )
    val meals = listOf(
        PlannedMeal("Breakfast", Icons.Outlined.FreeBreakfast, plan.breakfast),
        PlannedMeal("Lunch", Icons.Outlined.LunchDining, plan.lunch),
        PlannedMeal("Evening snack", Icons.Outlined.BakeryDining, plan.eveningSnack),
        PlannedMeal("Dinner", Icons.Outlined.DinnerDining, plan.dinner)
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        modifier = modifier.fillMaxWidth()
    ) {
        ListItem(
            headlineContent = { Text("Your meal plan · ${dayLabel(plan.date)}") },
            supportingContent = { Text("From ${plan.nutritionistName}") },
            leadingContent = { Icon(Icons.Outlined.RestaurantMenu, contentDescription = null) },
            trailingContent = {
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        Icons.Outlined.ExpandMore,
                        contentDescription = if (expanded) "Collapse meal plan" else "Expand meal plan",
                        modifier = Modifier.rotate(arrowRotation)
                    )
                }
            },
            colors = ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                headlineColor = MaterialTheme.colorScheme.onTertiaryContainer,
                supportingColor = MaterialTheme.colorScheme.onTertiaryContainer,
                leadingIconColor = MaterialTheme.colorScheme.onTertiaryContainer,
                trailingIconColor = MaterialTheme.colorScheme.onTertiaryContainer
            )
        )
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(tween(DURATION_MEDIUM_4, easing = EmphasizedDecelerate)) +
                fadeIn(tween(DURATION_MEDIUM_4, easing = EmphasizedDecelerate)),
            exit = shrinkVertically(tween(DURATION_SHORT_4, easing = EmphasizedAccelerate)) +
                fadeOut(tween(DURATION_SHORT_4, easing = EmphasizedAccelerate))
        ) {
            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                meals.filter { it.text != null }.forEachIndexed { index, meal ->
                    if (index > 0) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.12f)
                        )
                    }
                    ListItem(
                        overlineContent = { Text(meal.label) },
                        headlineContent = { Text(meal.text.orEmpty(), style = MaterialTheme.typography.bodyLarge) },
                        leadingContent = { Icon(meal.icon, contentDescription = null) },
                        colors = ListItemDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            overlineColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            headlineColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            leadingIconColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    )
                }
                plan.note?.let { note ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = note,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }
    }
}

private fun dayLabel(isoDate: String): String {
    val date = runCatching { LocalDate.parse(isoDate) }.getOrNull() ?: return isoDate
    val today = LocalDate.now()
    return when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    }
}
