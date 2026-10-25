package ca.zeezaglobal.gymsuitapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ca.zeezaglobal.gymsuitapp.data.remote.TrainerWorkoutApi

/** Top card on the workouts page: the workout the member's trainer sent for today. */
@Composable
fun TrainerWorkoutCard(suggestion: TrainerWorkoutApi.TrainerWorkoutSuggestion) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.FitnessCenter,
                    contentDescription = null,
                    tint = Color(0xFF395CBD),
                    modifier = Modifier.size(24.dp)
                )
                Column(modifier = Modifier.padding(start = 10.dp)) {
                    Text(
                        text = "Today's workout",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827)
                    )
                    Text(
                        text = "From ${suggestion.trainerName}",
                        fontSize = 12.sp,
                        color = Color(0xFF6B7280)
                    )
                }
            }
            if (!suggestion.message.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = suggestion.message,
                    fontSize = 13.sp,
                    color = Color(0xFF374151)
                )
            }
            Spacer(Modifier.height(12.dp))
            suggestion.exercises.forEach { exercise ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = exercise.exerciseName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF111827),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${exercise.sets} × ${exercise.reps}",
                        fontSize = 13.sp,
                        color = Color(0xFF374151),
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Text(
                        text = if (exercise.weightKg > 0) {
                            if (exercise.weightKg % 1.0 == 0.0) {
                                "${exercise.weightKg.toInt()} kg"
                            } else {
                                "${exercise.weightKg} kg"
                            }
                        } else {
                            "Bodyweight"
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF395CBD)
                    )
                }
            }
        }
    }
}
