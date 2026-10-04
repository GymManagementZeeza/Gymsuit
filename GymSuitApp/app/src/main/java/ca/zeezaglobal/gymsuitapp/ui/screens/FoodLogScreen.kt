package ca.zeezaglobal.gymsuitapp.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.core.content.FileProvider
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import ca.zeezaglobal.gymsuitapp.notifications.MealReminders
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import ca.zeezaglobal.gymsuitapp.data.food.FoodClassifier
import ca.zeezaglobal.gymsuitapp.data.food.FoodDatabase
import ca.zeezaglobal.gymsuitapp.data.food.FoodItem
import ca.zeezaglobal.gymsuitapp.data.food.RegionalFoodRepository
import ca.zeezaglobal.gymsuitapp.data.food.FoodNutrition
import ca.zeezaglobal.gymsuitapp.data.food.FoodPrediction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ca.zeezaglobal.gymsuitapp.data.FoodEntry
import ca.zeezaglobal.gymsuitapp.data.FoodStore
import ca.zeezaglobal.gymsuitapp.data.MealType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val DAILY_CALORIE_TARGET = 2000

@Composable
fun FoodLogScreen() {
    val context = LocalContext.current
    val store = remember { FoodStore(context) }
    val zone = remember { ZoneId.systemDefault() }
    val today = remember { LocalDate.now() }

    var entries by remember { mutableStateOf(store.getAll()) }
    var date by remember { mutableStateOf(today) }
    var addForMeal by remember { mutableStateOf<MealType?>(null) }
    var prefill by remember { mutableStateOf<FoodPrefill?>(null) }
    var chooserMeal by remember { mutableStateOf<MealType?>(null) } // meal picked, choosing how to add
    var scanMeal by remember { mutableStateOf(MealType.SNACK) }      // meal the scan result will go into

    // On-device scan flow: photo -> classify -> pick result -> prefilled add sheet
    val scope = rememberCoroutineScope()
    val classifier = remember { FoodClassifier(context) }
    var scanning by remember { mutableStateOf(false) }
    var scanImage by remember { mutableStateOf<Bitmap?>(null) }
    var scanPredictions by remember { mutableStateOf<List<FoodPrediction>>(emptyList()) }
    var showScanResult by remember { mutableStateOf(false) }

    fun runScan(uri: Uri) {
        scanning = true
        scope.launch {
            val bitmap = withContext(Dispatchers.IO) { decodeBitmap(context, uri) }
            if (bitmap != null) {
                scanImage = bitmap
                scanPredictions = classifier.classify(bitmap)
                showScanResult = true
            }
            scanning = false
        }
    }

    val photoFile = remember { File(File(context.cacheDir, "food_scans").apply { mkdirs() }, "scan.jpg") }
    val photoUri = remember { FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile) }
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) runScan(photoUri)
    }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) runScan(uri)
    }

    val dayEntries = remember(entries, date) {
        entries.filter { Instant.ofEpochMilli(it.timestampMillis).atZone(zone).toLocalDate() == date }
    }
    val totalKcal = dayEntries.sumOf { it.calories }
    val progress by animateFloatAsState(
        targetValue = (totalKcal.toFloat() / DAILY_CALORIE_TARGET).coerceIn(0f, 1f),
        label = "foodProgress"
    )

    // Meal reminders (morning, noon, evening, night)
    var remindersOn by remember { mutableStateOf(MealReminders.isEnabled(context) && MealReminders.hasNotificationPermission(context)) }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        remindersOn = granted
        MealReminders.setEnabled(context, granted)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (date == today) "Today" else date.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f)
            )
            FilledTonalIconButton(onClick = { date = date.minusDays(1) }) {
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = "Previous day")
            }
            FilledTonalIconButton(onClick = { date = date.plusDays(1) }, enabled = date < today) {
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = "Next day")
            }
        }

        ListItem(
            headlineContent = { Text("Meal reminders") },
            supportingContent = { Text("Morning, noon, evening and night") },
            trailingContent = {
                Switch(
                    checked = remindersOn,
                    onCheckedChange = { on ->
                        if (on && !MealReminders.hasNotificationPermission(context)) {
                            notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            remindersOn = on
                            MealReminders.setEnabled(context, on)
                        }
                    }
                )
            },
            modifier = Modifier.clip(RoundedCornerShape(16.dp))
        )

        Spacer(Modifier.height(8.dp))

        if (scanning) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Calories eaten", style = MaterialTheme.typography.labelLarge)
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("$totalKcal", style = MaterialTheme.typography.displaySmall)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "/ $DAILY_CALORIE_TARGET kcal",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                        Text(
                            "Protein ${dayEntries.sumOf { it.proteinG }.toInt()} g  •  " +
                                "Carbs ${dayEntries.sumOf { it.carbsG }.toInt()} g  •  " +
                                "Fat ${dayEntries.sumOf { it.fatG }.toInt()} g",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            items(MealType.entries) { meal ->
                val mealEntries = dayEntries.filter { it.meal == meal }
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(meal.label, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${mealEntries.sumOf { it.calories }} kcal",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { chooserMeal = meal }) {
                                Icon(Icons.Outlined.Add, contentDescription = "Add to ${meal.label}")
                            }
                        }
                        mealEntries.forEach { entry ->
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Column {
                                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                                    ListItem(
                                        headlineContent = { Text(entry.name) },
                                        supportingContent = {
                                            if (entry.proteinG + entry.carbsG + entry.fatG > 0) {
                                                Text("P ${entry.proteinG.toInt()} • C ${entry.carbsG.toInt()} • F ${entry.fatG.toInt()} g")
                                            }
                                        },
                                        trailingContent = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("${entry.calories} kcal", style = MaterialTheme.typography.labelLarge)
                                                IconButton(onClick = { entries = store.delete(entry.id) }) {
                                                    Icon(Icons.Outlined.Delete, contentDescription = "Delete ${entry.name}")
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                        if (mealEntries.isEmpty()) {
                            Text(
                                "Nothing logged",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    chooserMeal?.let { meal ->
        AddMethodSheet(
            meal = meal,
            scanning = scanning,
            onDismiss = { chooserMeal = null },
            onCamera = {
                scanMeal = meal
                chooserMeal = null
                takePicture.launch(photoUri)
            },
            onGallery = {
                scanMeal = meal
                chooserMeal = null
                pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onManual = {
                chooserMeal = null
                prefill = null
                addForMeal = meal
            }
        )
    }

    if (showScanResult) {
        ScanResultSheet(
            image = scanImage,
            predictions = scanPredictions,
            onDismiss = { showScanResult = false },
            onContinue = { name, kcal, p, c, f ->
                showScanResult = false
                prefill = FoodPrefill(name, kcal, p, c, f)
                addForMeal = scanMeal
            }
        )
    }

    addForMeal?.let { initialMeal ->
        AddFoodSheet(
            initialMeal = initialMeal,
            prefill = prefill,
            onDismiss = {
                addForMeal = null
                prefill = null
            },
            onSave = { name, meal, kcal, p, c, f, isManual ->
                val time = if (date == today) Instant.now().toEpochMilli()
                else date.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
                entries = store.add(FoodEntry(name = name, meal = meal, calories = kcal, proteinG = p, carbsG = c, fatG = f, timestampMillis = time))
                if (isManual) {
                    // Remember it so it shows up in search next time (Room DB + custom_foods.json)
                    scope.launch {
                        RegionalFoodRepository.addCustom(
                            context,
                            FoodItem(name, "1 serving", kcal, p.toInt(), c.toInt(), f.toInt())
                        )
                    }
                }
                addForMeal = null
                prefill = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddFoodSheet(
    initialMeal: MealType,
    prefill: FoodPrefill?,
    onDismiss: () -> Unit,
    onSave: (String, MealType, Int, Double, Double, Double, Boolean) -> Unit
) {
    var name by remember { mutableStateOf(prefill?.name ?: "") }
    var calories by remember { mutableStateOf(prefill?.kcal?.toString() ?: "") }
    var protein by remember { mutableStateOf(prefill?.proteinG?.toString() ?: "") }
    var carbs by remember { mutableStateOf(prefill?.carbsG?.toString() ?: "") }
    var fat by remember { mutableStateOf(prefill?.fatG?.toString() ?: "") }
    var meal by remember { mutableStateOf(initialMeal) }
    var base by remember { mutableStateOf<FoodItem?>(null) }   // food picked from search
    var multiplier by remember { mutableStateOf(1.0) }
    var searching by remember { mutableStateOf(prefill == null) }
    val appContext = LocalContext.current
    var suggestions by remember { mutableStateOf<List<FoodItem>>(emptyList()) }
    // Kerala foods come from the Room database; the built-in Indian/international list fills the rest
    LaunchedEffect(name, searching) {
        suggestions = if (searching) {
            val kerala = RegionalFoodRepository.search(appContext, name)
            (kerala + FoodDatabase.search(name))
                .distinctBy { it.name.lowercase() }
                .take(6)
        } else emptyList()
    }
    fun applyBase() {
        val b = base ?: return
        calories = (b.kcal * multiplier).toInt().toString()
        protein = (b.proteinG * multiplier).toInt().toString()
        carbs = (b.carbsG * multiplier).toInt().toString()
        fat = (b.fatG * multiplier).toInt().toString()
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val number = KeyboardOptions(keyboardType = KeyboardType.Decimal)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Restaurant, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text("Add food", style = MaterialTheme.typography.headlineSmall)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState()).fillMaxWidth()) {
                MealType.entries.forEach {
                    FilterChip(selected = meal == it, onClick = { meal = it }, label = { Text(it.label) })
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it.take(60)
                    searching = true
                    base = null
                },
                label = { Text("Search food (e.g. dosa, biryani, paneer)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Live suggestions from the built-in Indian + international food list
            suggestions.forEach { item ->
                ListItem(
                    headlineContent = { Text(item.name) },
                    supportingContent = { Text("${item.serving} • ${item.kcal} kcal") },
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            name = item.name
                            base = item
                            multiplier = 1.0
                            searching = false
                            applyBase()
                        }
                )
            }

            base?.let { b ->
                Text("Portion (${b.serving} = 1x)", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0.5, 1.0, 1.5, 2.0, 3.0).forEach {
                        FilterChip(
                            selected = multiplier == it,
                            onClick = {
                                multiplier = it
                                applyBase()
                            },
                            label = { Text("${it}x") }
                        )
                    }
                }
            }
            OutlinedTextField(
                value = calories, onValueChange = { calories = it.filter(Char::isDigit).take(5) },
                label = { Text("Calories (kcal)") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = protein, onValueChange = { protein = it.filter { c -> c.isDigit() || c == '.' }.take(5) },
                    label = { Text("Protein g") }, singleLine = true, keyboardOptions = number, modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = carbs, onValueChange = { carbs = it.filter { c -> c.isDigit() || c == '.' }.take(5) },
                    label = { Text("Carbs g") }, singleLine = true, keyboardOptions = number, modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = fat, onValueChange = { fat = it.filter { c -> c.isDigit() || c == '.' }.take(5) },
                    label = { Text("Fat g") }, singleLine = true, keyboardOptions = number, modifier = Modifier.weight(1f)
                )
            }

            val kcal = calories.toIntOrNull()
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Button(
                    onClick = {
                        onSave(
                            name.trim(), meal, kcal ?: 0,
                            protein.toDoubleOrNull() ?: 0.0, carbs.toDoubleOrNull() ?: 0.0, fat.toDoubleOrNull() ?: 0.0,
                            base == null && prefill == null // typed in by hand, not picked from search or a scan
                        )
                    },
                    enabled = name.isNotBlank() && kcal != null && kcal > 0
                ) { Text("Save") }
            }
        }
    }
}

private data class FoodPrefill(val name: String, val kcal: Int, val proteinG: Int, val carbsG: Int, val fatG: Int)

/** Decodes a downsampled bitmap and applies the EXIF rotation. */
private fun decodeBitmap(context: android.content.Context, uri: Uri): Bitmap? = try {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (bounds.outWidth / sample > 1600 || bounds.outHeight / sample > 1600) sample *= 2
    val bitmap = context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
    }
    val orientation = context.contentResolver.openInputStream(uri)?.use {
        ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    } ?: ExifInterface.ORIENTATION_NORMAL
    val degrees = when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }
    if (bitmap != null && degrees != 0f) {
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(degrees) }, true)
    } else bitmap
} catch (e: Exception) {
    null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScanResultSheet(
    image: Bitmap?,
    predictions: List<FoodPrediction>,
    onDismiss: () -> Unit,
    onContinue: (name: String, kcal: Int, protein: Int, carbs: Int, fat: Int) -> Unit
) {
    var selected by remember { mutableStateOf(0) }
    var portion by remember { mutableStateOf(1.0) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val nutrition = predictions.getOrNull(selected)?.let { FoodNutrition.forClass(it.classId) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Scan result", style = MaterialTheme.typography.headlineSmall)

            image?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "Scanned food",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(16.dp))
                )
            }

            if (predictions.isEmpty() || nutrition == null) {
                Text(
                    "Couldn't recognise this food. You can still add it manually.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(onClick = { onContinue("", 0, 0, 0, 0) }, modifier = Modifier.fillMaxWidth()) { Text("Add manually") }
            } else {
                Text("Best matches", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    predictions.forEachIndexed { index, p ->
                        val label = FoodNutrition.forClass(p.classId)?.name ?: p.classId
                        FilterChip(
                            selected = selected == index,
                            onClick = { selected = index },
                            label = { Text("$label ${(p.probability * 100).toInt()}%") }
                        )
                    }
                }

                Text("Portion (${nutrition.serving} = 1x)", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0.5, 1.0, 1.5, 2.0).forEach {
                        FilterChip(selected = portion == it, onClick = { portion = it }, label = { Text("${it}x") })
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("${(nutrition.kcal * portion).toInt()} kcal", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            "Protein ${(nutrition.proteinG * portion).toInt()} g • Carbs ${(nutrition.carbsG * portion).toInt()} g • Fat ${(nutrition.fatG * portion).toInt()} g",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "Estimate for a typical serving. You can edit it next.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                Button(
                    onClick = {
                        onContinue(
                            nutrition.name,
                            (nutrition.kcal * portion).toInt(),
                            (nutrition.proteinG * portion).toInt(),
                            (nutrition.carbsG * portion).toInt(),
                            (nutrition.fatG * portion).toInt()
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Continue") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMethodSheet(
    meal: MealType,
    scanning: Boolean,
    onDismiss: () -> Unit,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onManual: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Text(
                "Add to ${meal.label}",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
            ListItem(
                headlineContent = { Text("Take a picture") },
                supportingContent = { Text("Identify the food and estimate calories") },
                leadingContent = { Icon(Icons.Outlined.CameraAlt, contentDescription = null) },
                modifier = Modifier.clickable(enabled = !scanning, onClick = onCamera)
            )
            ListItem(
                headlineContent = { Text("Choose from gallery") },
                leadingContent = { Icon(Icons.Outlined.PhotoLibrary, contentDescription = null) },
                modifier = Modifier.clickable(enabled = !scanning, onClick = onGallery)
            )
            ListItem(
                headlineContent = { Text("Enter manually") },
                leadingContent = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onManual)
            )
        }
    }
}
