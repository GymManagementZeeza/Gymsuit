package ca.zeezaglobal.gymsuitapp.ui.widgets

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ca.zeezaglobal.gymsuitapp.data.model.ChallengeStatus
import ca.zeezaglobal.gymsuitapp.data.model.ChallengeSummary
import ca.zeezaglobal.gymsuitapp.data.remote.ChallengeApi
import ca.zeezaglobal.gymsuitapp.ui.theme.GymSuitAppTheme

/**
 * Challenge picker for the home-screen challenges widget. Runs when the
 * widget is first placed (android:configure) and whenever the widget's
 * title is tapped, so a widget can switch challenges at any time. The
 * choice is stored per widget instance; "Automatic" clears it and the
 * widget shows the first active challenge again.
 */
class ChallengeWidgetConfigActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val widgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        setContent {
            GymSuitAppTheme {
                ChallengePickerScreen(
                    currentSelection = if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                        getChallengeSelection(this, widgetId)
                    } else {
                        null
                    },
                    onChoose = { challengeId ->
                        if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                            saveChallengeSelection(this, widgetId, challengeId)
                            WidgetUpdateWorker.enqueueOneShot(this)
                        }
                        setResult(
                            RESULT_OK,
                            Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                        )
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
private fun ChallengePickerScreen(
    currentSelection: String?,
    onChoose: (String?) -> Unit
) {
    val context = LocalContext.current
    var challenges by remember { mutableStateOf<List<ChallengeSummary>?>(null) }
    var error by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val result = ChallengeApi(context).listChallenges()
        result.fold(
            onSuccess = { list ->
                challenges = list.sortedWith(
                    compareBy<ChallengeSummary> { it.status != ChallengeStatus.ACTIVE }
                        .thenBy { it.name }
                )
            },
            onFailure = { error = true }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text(
            text = "Choose a challenge",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "This widget will show its members and their progress.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(20.dp))

        when {
            error -> {
                Text(
                    text = "Couldn't load challenges. Check your connection and try again.",
                    color = MaterialTheme.colorScheme.error
                )
            }
            challenges == null -> {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            else -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    item {
                        ChallengeRow(
                            title = "Automatic",
                            subtitle = "First active challenge",
                            selected = currentSelection == null,
                            onClick = { onChoose(null) }
                        )
                    }
                    items(challenges!!) { challenge ->
                        ChallengeRow(
                            title = challenge.name,
                            subtitle = "${challenge.status.label} · ${challenge.metric.label} · ${challenge.participantCount} members",
                            selected = currentSelection == challenge.id,
                            onClick = { onChoose(challenge.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChallengeRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (selected) {
            Text(
                text = "✓",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
