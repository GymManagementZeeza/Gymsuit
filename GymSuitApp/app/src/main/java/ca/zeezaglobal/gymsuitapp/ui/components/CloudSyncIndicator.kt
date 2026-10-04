package ca.zeezaglobal.gymsuitapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ca.zeezaglobal.gymsuitapp.data.SyncStatus
import ca.zeezaglobal.gymsuitapp.ui.theme.PoppinsFontFamily

/**
 * Top cloud synchronization indicator:
 * 1. Material 3 LinearProgressIndicator line loading bar on top when sync is in progress.
 * 2. Animated green "All data synced" message when synchronization succeeds.
 */
@Composable
fun CloudSyncIndicator(
    syncStatus: SyncStatus,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Material 3 linear line loading bar right on top
        AnimatedVisibility(
            visible = syncStatus is SyncStatus.Syncing,
            enter = expandVertically(animationSpec = tween(200)) + fadeIn(animationSpec = tween(200)),
            exit = shrinkVertically(animationSpec = tween(250)) + fadeOut(animationSpec = tween(250))
        ) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.5.dp),
                color = Color(0xFF16A34A), // M3 Emerald Green
                trackColor = Color(0x3316A34A)
            )
        }

        // Floating message badge under status bar
        AnimatedVisibility(
            visible = syncStatus !is SyncStatus.Idle,
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = spring(
                    stiffness = Spring.StiffnessMediumLow,
                    dampingRatio = Spring.DampingRatioLowBouncy
                )
            ) + fadeIn(animationSpec = tween(250)),
            exit = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(250))
        ) {
            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                when (syncStatus) {
                    is SyncStatus.Syncing -> {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFFF0FDF4),
                            border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                            shadowElevation = 4.dp
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(13.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF16A34A)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Syncing to cloud…",
                                    fontFamily = PoppinsFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.5.sp,
                                    color = Color(0xFF15803D)
                                )
                            }
                        }
                    }

                    is SyncStatus.Success -> {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFFDCFCE7), // Green container
                            border = BorderStroke(1.dp, Color(0xFF86EFAC)), // Green border
                            shadowElevation = 5.dp
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "Synced",
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = syncStatus.message, // "All data synced"
                                    fontFamily = PoppinsFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF15803D)
                                )
                            }
                        }
                    }

                    is SyncStatus.Error -> {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFFFEF2F2),
                            border = BorderStroke(1.dp, Color(0xFFFECACA)),
                            shadowElevation = 4.dp
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Warning,
                                    contentDescription = "Error",
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = syncStatus.message,
                                    fontFamily = PoppinsFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp,
                                    color = Color(0xFFB91C1C)
                                )
                            }
                        }
                    }

                    SyncStatus.Idle -> Unit
                }
            }
        }
    }
}
