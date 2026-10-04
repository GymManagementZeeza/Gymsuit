package ca.zeezaglobal.gymsuitapp.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ca.zeezaglobal.gymsuitapp.data.PointsConfig
import ca.zeezaglobal.gymsuitapp.data.PointsEntry
import ca.zeezaglobal.gymsuitapp.data.PointsStore
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val PointsGold = Color(0xFFF59E0B)
private val PointsGreen = Color(0xFF059669)
private val PointsRed = Color(0xFFEF4444)
private val PInkPrimary = Color(0xFF111827)
private val PInkSecondary = Color(0xFF6B7280)
private val PDivider = Color(0xFFE5E7EB)
private val PSurfaceBg = Color(0xFFF9FAFB)

/**
 * Tappable points pill for the top bar. The number counts up with a springy
 * pop whenever the balance grows. Tapping opens the points screen.
 */
@Composable
fun PointsPill() {
    val context = LocalContext.current
    LaunchedEffect(Unit) { PointsStore.init(context) }
    val balance by PointsStore.balance.collectAsState()
    var showPoints by remember { mutableStateOf(false) }

    val animatedBalance by animateIntAsState(
        targetValue = balance,
        animationSpec = tween(durationMillis = 700),
        label = "pointsCount"
    )

    // Springy pop on balance change (skipped on first composition).
    var first by remember { mutableStateOf(true) }
    var popped by remember { mutableStateOf(false) }
    LaunchedEffect(balance) {
        if (first) {
            first = false
            return@LaunchedEffect
        }
        popped = true
        delay(350)
        popped = false
    }
    val scale by animateFloatAsState(
        targetValue = if (popped) 1.22f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "pointsPop"
    )

    // M3 filled tonal button with an icon, matching the 40dp height of the neighbouring icon button
    FilledTonalButton(
        onClick = { showPoints = true },
        contentPadding = PaddingValues(start = 12.dp, end = 16.dp),
        modifier = Modifier
            .height(40.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("$animatedBalance", style = MaterialTheme.typography.labelLarge)
    }

    if (showPoints) {
        Dialog(
            onDismissRequest = { showPoints = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            PointsScreenContent(onClose = { showPoints = false })
        }
    }
}

@Composable
private fun PointsScreenContent(onClose: () -> Unit) {
    val balance by PointsStore.balance.collectAsState()
    val history by PointsStore.history.collectAsState()
    val animatedBalance by animateIntAsState(
        targetValue = balance,
        animationSpec = tween(durationMillis = 700),
        label = "pointsHeroCount"
    )
    var showRedeem by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PSurfaceBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = "Close", tint = PInkPrimary)
            }
            Text("My Points", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PInkPrimary)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Balance hero
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, PDivider),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.Star, contentDescription = null, tint = PointsGold, modifier = Modifier.size(44.dp))
                        Text("$animatedBalance", fontSize = 44.sp, fontWeight = FontWeight.Bold, color = PInkPrimary)
                        Text("points", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = PInkSecondary)
                    }
                }
            }

            item {
                // Redeemable value
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, PDivider),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Redeemable value", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = PInkPrimary)
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                PointsConfig.formattedRupees(balance),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = PointsGreen
                            )
                            Spacer(Modifier.weight(1f))
                            Text("100 pts = ₹1", fontSize = 13.sp, color = PInkSecondary)
                        }
                        Text(
                            "Your $balance points can be redeemed for ${PointsConfig.formattedRupees(balance)} in real money.",
                            fontSize = 13.sp,
                            color = PInkSecondary
                        )
                        Button(
                            onClick = { showRedeem = true },
                            enabled = balance > 0,
                            colors = ButtonDefaults.buttonColors(containerColor = PointsGreen),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Redeem points", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                // How to earn
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, PDivider),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("How to earn", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = PInkPrimary)
                        EarnRow("+50 pts", "for every workout you log")
                        EarnRow("+5 pts", "for every set")
                        EarnRow("+2 pts", "for every minute of workout")
                    }
                }
            }

            item {
                Text("History", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = PInkPrimary)
            }

            if (history.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, PDivider),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "No points yet. Log a workout to start earning!",
                            fontSize = 14.sp,
                            color = PInkSecondary,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                }
            } else {
                items(history, key = { it.id }) { entry ->
                    PointsHistoryRow(entry)
                }
            }
        }
    }

    if (showRedeem) {
        RedeemDialog(
            balance = balance,
            onDismiss = { showRedeem = false }
        )
    }
}

@Composable
private fun EarnRow(points: String, label: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(points, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PointsGreen)
        Text(label, fontSize = 14.sp, color = PInkSecondary)
    }
}

@Composable
private fun PointsHistoryRow(entry: PointsEntry) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM d, yyyy") }
    val date = remember(entry.timestampMillis) {
        Instant.ofEpochMilli(entry.timestampMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    }
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, PDivider),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Filled.Star,
                contentDescription = null,
                tint = if (entry.isRedemption) PointsRed else PointsGreen,
                modifier = Modifier.size(22.dp)
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(entry.reason, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = PInkPrimary)
                Text(date.format(dateFormatter), fontSize = 12.sp, color = PInkSecondary)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    (if (entry.points > 0) "+" else "") + entry.points.toString(),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (entry.isRedemption) PointsRed else PointsGreen
                )
                entry.rupeeValue?.let {
                    Text("₹%.2f".format(it), fontSize = 12.sp, color = PInkSecondary)
                }
            }
        }
    }
}

@Composable
private fun RedeemDialog(balance: Int, onDismiss: () -> Unit) {
    var amountText by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    val amount = amountText.toIntOrNull() ?: 0
    val valid = amount > 0 && amount <= balance

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Redeem points", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PInkPrimary)
                Text(
                    "Enter the points you want to redeem. 100 points = ₹1.",
                    fontSize = 14.sp,
                    color = PInkSecondary
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter(Char::isDigit).take(7) },
                    label = { Text("Points") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "You will receive ${PointsConfig.formattedRupees(amount)}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PInkPrimary
                )
                message?.let {
                    Text(it, fontSize = 14.sp, color = PInkSecondary)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    androidx.compose.material3.TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val rupees = PointsStore.redeem(amount)
                            if (rupees != null) {
                                message = "Redeemed $amount points for ₹%.2f.".format(rupees)
                                amountText = ""
                            } else {
                                message = "Not enough points for that amount."
                            }
                        },
                        enabled = valid,
                        colors = ButtonDefaults.buttonColors(containerColor = PointsGreen),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Confirm", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
