import SwiftUI
import PhotosUI
import UIKit

/// Food log tab — iOS mirror of Android's FoodLogScreen.
///
/// Day-by-day food diary: calories summary vs the 2000 kcal target, one card
/// per meal (Breakfast / Lunch / Dinner / Snacks) with entries and totals.
/// Foods are added three ways, like Android: take a picture or pick a photo
/// (server-side recognition via FoodRecognitionApi), or enter manually with
/// offline search suggestions. Entries persist locally via FoodStore.
struct FoodLogView: View {
    @StateObject private var store = FoodStore.shared

    @State private var date: Date = Calendar.current.startOfDay(for: Date())
    @State private var scanMeal: MealType = .snack
    @State private var activeSheet: FoodSheet?
    @State private var prefill: FoodPrefill?
    @State private var pendingCamera = false
    @State private var pendingGallery = false
    @State private var showCamera = false
    @State private var showPhotoPicker = false
    @State private var pickedItem: PhotosPickerItem?

    @State private var scanning = false
    @State private var scanImage: UIImage?
    @State private var scanCandidates: [DishCandidate] = []
    @State private var scanError: String?

    @State private var remindersOn = false

    private let dailyTarget = 2000

    private var calendar: Calendar { Calendar.current }
    private var today: Date { calendar.startOfDay(for: Date()) }
    private var isToday: Bool { calendar.isDate(date, inSameDayAs: today) }
    private var dayEntries: [FoodEntry] { store.entries(onDayOf: date) }
    private var totalKcal: Int { dayEntries.reduce(0) { $0 + $1.calories } }

    private var dateLabel: String {
        let formatter = DateFormatter()
        formatter.dateFormat = "EEE, MMM d"
        return formatter.string(from: date)
    }

    var body: some View {
        ZStack {
            mainContent
            if scanning {
                ScanningOverlay()
            }
        }
        .animation(.easeInOut(duration: 0.2), value: scanning)
    }

    private var mainContent: some View {
        VStack(spacing: 0) {
            dateHeader
            remindersRow
                .padding(.top, 4)

            if let scanError {
                HStack(alignment: .top) {
                    Text(scanError)
                        .font(.system(size: 14))
                        .foregroundColor(AppColors.textPrimary)
                    Spacer()
                    Button("Dismiss") { self.scanError = nil }
                        .font(.system(size: 14, weight: .semibold))
                }
                .padding(.horizontal, 20)
                .padding(.top, 8)
            }

            ScrollView {
                VStack(spacing: 12) {
                    summaryCard
                    ForEach(MealType.allCases) { meal in
                        mealCard(meal)
                    }
                }
                .padding(.horizontal, 20)
                .padding(.top, 12)
                .padding(.bottom, 20)
            }
        }
        .sheet(item: $activeSheet, onDismiss: handleSheetDismiss) { sheet in
            switch sheet {
            case .chooser(let meal):
                AddMethodSheet(
                    meal: meal,
                    scanning: scanning,
                    onCamera: {
                        scanMeal = meal
                        pendingCamera = true
                        activeSheet = nil
                    },
                    onGallery: {
                        scanMeal = meal
                        pendingGallery = true
                        activeSheet = nil
                    },
                    onManual: {
                        prefill = nil
                        activeSheet = .add(meal)
                    }
                )
            case .scanResult:
                ScanResultSheet(
                    image: scanImage,
                    candidates: scanCandidates,
                    onContinue: { result in
                        prefill = result
                        activeSheet = .add(scanMeal)
                    }
                )
            case .add(let meal):
                AddFoodSheet(
                    initialMeal: meal,
                    prefill: prefill,
                    onSave: saveEntry,
                    onCancel: {
                        activeSheet = nil
                        prefill = nil
                    }
                )
            }
        }
        .fullScreenCover(isPresented: $showCamera) {
            CameraImagePicker(
                onImage: { image in
                    showCamera = false
                    runScan(image)
                },
                onCancel: { showCamera = false }
            )
            .ignoresSafeArea()
        }
        .photosPicker(isPresented: $showPhotoPicker, selection: $pickedItem, matching: .images)
        .onChange(of: pickedItem) { _, newItem in
            guard let newItem else { return }
            pickedItem = nil
            Task {
                if let data = try? await newItem.loadTransferable(type: Data.self),
                   let image = UIImage(data: data) {
                    runScan(image)
                } else {
                    scanError = "Couldn't read that photo. Try another one."
                }
            }
        }
        .task {
            if MealReminders.isEnabled {
                remindersOn = await MealReminders.hasAuthorization()
            } else {
                remindersOn = false
            }
            MealReminders.restore()
        }
    }

    // MARK: - Header

    private var dateHeader: some View {
        HStack {
            Text(isToday ? "Today" : dateLabel)
                .font(.system(size: 22, weight: .bold))
                .foregroundColor(AppColors.textPrimary)
            Spacer()
            Button {
                date = calendar.date(byAdding: .day, value: -1, to: date) ?? date
            } label: {
                Image(systemName: "chevron.left")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(AppColors.primary)
                    .frame(width: 38, height: 38)
                    .background(AppColors.primaryLight)
                    .clipShape(Circle())
            }
            Button {
                date = calendar.date(byAdding: .day, value: 1, to: date) ?? date
            } label: {
                Image(systemName: "chevron.right")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(AppColors.primary)
                    .frame(width: 38, height: 38)
                    .background(AppColors.primaryLight)
                    .clipShape(Circle())
            }
            .disabled(isToday)
            .opacity(isToday ? 0.4 : 1)
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 8)
    }

    private var remindersRow: some View {
        HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text("Meal reminders")
                    .font(.system(size: 15, weight: .medium))
                    .foregroundColor(AppColors.textPrimary)
                Text("Morning, noon, evening and night")
                    .font(.system(size: 12))
                    .foregroundColor(AppColors.textSecondary)
            }
            Spacer()
            Toggle("", isOn: Binding(
                get: { remindersOn },
                set: { handleReminderToggle($0) }
            ))
            .labelsHidden()
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .padding(.horizontal, 20)
    }

    private func handleReminderToggle(_ on: Bool) {
        if on {
            Task {
                let granted = await MealReminders.requestAuthorization()
                remindersOn = granted
                MealReminders.setEnabled(granted)
            }
        } else {
            remindersOn = false
            MealReminders.setEnabled(false)
        }
    }

    // MARK: - Summary + meal cards

    private var summaryCard: some View {
        let protein = dayEntries.reduce(0.0) { $0 + $1.proteinG }
        let carbs = dayEntries.reduce(0.0) { $0 + $1.carbsG }
        let fat = dayEntries.reduce(0.0) { $0 + $1.fatG }
        return VStack(alignment: .leading, spacing: 10) {
            Text("Calories eaten")
                .font(.system(size: 13, weight: .semibold))
                .foregroundColor(AppColors.textSecondary)
            HStack(alignment: .lastTextBaseline, spacing: 6) {
                Text("\(totalKcal)")
                    .font(.system(size: 34, weight: .bold))
                    .foregroundColor(AppColors.textPrimary)
                Text("/ \(dailyTarget) kcal")
                    .font(.system(size: 16, weight: .medium))
                    .foregroundColor(AppColors.textSecondary)
            }
            ProgressView(value: Double(min(totalKcal, dailyTarget)), total: Double(dailyTarget))
                .tint(AppColors.calories)
                .animation(.easeInOut, value: totalKcal)
            Text("Protein \(Int(protein)) g  •  Carbs \(Int(carbs)) g  •  Fat \(Int(fat)) g")
                .font(.system(size: 14))
                .foregroundColor(AppColors.textSecondary)
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 24))
        .shadow(color: Color.black.opacity(0.04), radius: 6, y: 3)
    }

    private func mealCard(_ meal: MealType) -> some View {
        let items = dayEntries.filter { $0.meal == meal }
        return VStack(spacing: 0) {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(meal.label)
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundColor(AppColors.textPrimary)
                    Text("\(items.reduce(0) { $0 + $1.calories }) kcal")
                        .font(.system(size: 12))
                        .foregroundColor(AppColors.textSecondary)
                }
                Spacer()
                Button {
                    activeSheet = .chooser(meal)
                } label: {
                    Image(systemName: "plus")
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundColor(AppColors.primary)
                        .frame(width: 34, height: 34)
                        .background(AppColors.primaryLight)
                        .clipShape(Circle())
                }
            }
            .padding(.horizontal, 16)
            .padding(.top, 12)
            .padding(.bottom, 6)

            if items.isEmpty {
                Text("Nothing logged")
                    .font(.system(size: 14))
                    .foregroundColor(AppColors.textSecondary)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.horizontal, 16)
                    .padding(.bottom, 12)
            } else {
                ForEach(items) { entry in
                    Divider().padding(.horizontal, 16)
                    HStack {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(entry.name)
                                .font(.system(size: 15, weight: .medium))
                                .foregroundColor(AppColors.textPrimary)
                            if entry.proteinG + entry.carbsG + entry.fatG > 0 {
                                Text("P \(Int(entry.proteinG)) • C \(Int(entry.carbsG)) • F \(Int(entry.fatG)) g")
                                    .font(.system(size: 12))
                                    .foregroundColor(AppColors.textSecondary)
                            }
                        }
                        Spacer()
                        Text("\(entry.calories) kcal")
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundColor(AppColors.textPrimary)
                        Button {
                            store.delete(id: entry.id)
                        } label: {
                            Image(systemName: "trash")
                                .font(.system(size: 14))
                                .foregroundColor(AppColors.danger)
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 10)
                }
            }
        }
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 22))
        .shadow(color: Color.black.opacity(0.03), radius: 6, y: 3)
    }

    // MARK: - Scan flow

    private func handleSheetDismiss() {
        if pendingCamera {
            pendingCamera = false
            showCamera = true
        } else if pendingGallery {
            pendingGallery = false
            showPhotoPicker = true
        }
    }

    private func runScan(_ image: UIImage) {
        scanning = true
        scanError = nil
        Task {
            print("[FoodLogView] Starting food photo scan, image size: \(image.size)")
            guard let jpeg = image.foodScanJPEGData() else {
                print("[FoodLogView] Failed to convert image to JPEG data")
                scanError = "Couldn't read that photo. Try another one."
                scanning = false
                return
            }
            print("[FoodLogView] Generated JPEG data: \(jpeg.count) bytes")
            scanImage = image
            do {
                let result = try await FoodRecognitionApi.shared.recognize(jpegData: jpeg)
                print("[FoodLogView] Received recognition result: dish='\(result.dish)', confidence=\(result.confidence)")
                let ranked = result.probabilities
                    .sorted { $0.value > $1.value }
                    .prefix(3)
                var candidates = ranked.map { pair in
                    let isTopMatch = (pair.key.caseInsensitiveCompare(result.dish) == .orderedSame) || (pair.key == ranked.first?.key)
                    let apiNutrition = isTopMatch ? result.nutrition?.toFoodItem() : nil
                    let finalNutrition = apiNutrition ?? FoodSearch.resolveNutrition(forDish: pair.key)
                    let name = (isTopMatch ? result.nutrition?.name : nil) ?? dishDisplayName(pair.key)
                    return DishCandidate(
                        dish: pair.key,
                        displayName: name,
                        confidence: pair.value,
                        nutrition: finalNutrition
                    )
                }
                // Ensure the primary recognized dish is present as the first candidate
                let trimmedDish = result.dish.trimmingCharacters(in: .whitespaces)
                if !trimmedDish.isEmpty {
                    let hasTopDish = candidates.contains { $0.dish.caseInsensitiveCompare(trimmedDish) == .orderedSame }
                    if !hasTopDish {
                        candidates.insert(
                            DishCandidate(
                                dish: trimmedDish,
                                displayName: result.nutrition?.name ?? dishDisplayName(trimmedDish),
                                confidence: result.confidence,
                                nutrition: result.nutrition?.toFoodItem() ?? FoodSearch.resolveNutrition(forDish: trimmedDish)
                            ),
                            at: 0
                        )
                    }
                }
                print("[FoodLogView] Resolved \(candidates.count) dish candidates: \(candidates.map { "\($0.displayName) (\(String(format: "%.1f%%", $0.confidence * 100)))" })")
                scanCandidates = candidates
                activeSheet = .scanResult
            } catch FoodRecognitionError.unavailable {
                print("[FoodLogView] Recognition failed: Server unavailable (503)")
                scanError = "Food recognition is unavailable right now — the server is down. You can still add it manually."
            } catch FoodRecognitionError.unauthorized {
                print("[FoodLogView] Recognition failed: Unauthorized (401)")
                scanError = "Your session has expired. Please log in again to use food scanning."
            } catch FoodRecognitionError.failed(let message) {
                print("[FoodLogView] Recognition failed: \(message)")
                scanError = message
            } catch {
                print("[FoodLogView] Recognition failed with unexpected error: \(error.localizedDescription) (\(error))")
                scanError = "Couldn't reach the food recognition server. You can still add it manually."
            }
            scanning = false
        }
    }

    // MARK: - Saving

    private func saveEntry(
        name: String,
        meal: MealType,
        calories: Int,
        protein: Double,
        carbs: Double,
        fat: Double,
        isManual: Bool
    ) {
        let timestampMillis: Int64
        if isToday {
            timestampMillis = Int64(Date().timeIntervalSince1970 * 1000)
        } else {
            // Entries added to a past day are stamped at noon of that day (Android behaviour).
            let noon = calendar.date(bySettingHour: 12, minute: 0, second: 0, of: date) ?? date
            timestampMillis = Int64(noon.timeIntervalSince1970 * 1000)
        }
        store.add(FoodEntry(
            name: name,
            meal: meal,
            calories: calories,
            proteinG: protein,
            carbsG: carbs,
            fatG: fat,
            timestampMillis: timestampMillis
        ))
        if isManual {
            // Remember it so it shows up in search next time.
            RegionalFoodStore.addCustom(FoodItem(
                name: name,
                serving: "1 serving",
                kcal: calories,
                proteinG: Int(protein),
                carbsG: Int(carbs),
                fatG: Int(fat)
            ))
        }
        activeSheet = nil
        prefill = nil
        // Re-arm reminders: the just-logged meal's slot is now skipped today.
        MealReminders.restore()
    }
}

/// Full-screen blocking overlay shown while the photo is uploaded to
/// FoodRecognitionApi and a result comes back.
private struct ScanningOverlay: View {
    var body: some View {
        ZStack {
            Color.black.opacity(0.35)
                .ignoresSafeArea()
            VStack(spacing: 16) {
                ProgressView()
                    .progressViewStyle(.circular)
                    .tint(.white)
                    .scaleEffect(1.4)
                Text("Analyzing your photo…")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundColor(.white)
            }
            .padding(28)
            .background(Color.black.opacity(0.55))
            .clipShape(RoundedRectangle(cornerRadius: 20))
        }
        .transition(.opacity)
    }
}

// MARK: - Sheets

private enum FoodSheet: Identifiable {
    case chooser(MealType)
    case scanResult
    case add(MealType)

    var id: String {
        switch self {
        case .chooser(let meal): return "chooser-\(meal.rawValue)"
        case .scanResult: return "scan-result"
        case .add(let meal): return "add-\(meal.rawValue)"
        }
    }
}

/// "Add to <meal>" chooser: camera / gallery / manual (Android's AddMethodSheet).
private struct AddMethodSheet: View {
    let meal: MealType
    let scanning: Bool
    var onCamera: () -> Void
    var onGallery: () -> Void
    var onManual: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("Add to \(meal.label)")
                .font(.system(size: 20, weight: .bold))
                .foregroundColor(AppColors.textPrimary)
                .padding(.horizontal, 24)
                .padding(.top, 20)
                .padding(.bottom, 8)

            row(icon: "camera.fill", title: "Take a picture",
                subtitle: "Identify the food and estimate calories", action: onCamera)
                .disabled(scanning)
            row(icon: "photo.on.rectangle", title: "Choose from gallery",
                subtitle: nil, action: onGallery)
                .disabled(scanning)
            row(icon: "square.and.pencil", title: "Enter manually",
                subtitle: nil, action: onManual)

            Spacer(minLength: 0)
        }
        .presentationDetents([.height(320)])
    }

    private func row(icon: String, title: String, subtitle: String?, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 14) {
                Image(systemName: icon)
                    .font(.system(size: 17))
                    .foregroundColor(AppColors.primary)
                    .frame(width: 28)
                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .font(.system(size: 16, weight: .medium))
                        .foregroundColor(AppColors.textPrimary)
                    if let subtitle {
                        Text(subtitle)
                            .font(.system(size: 13))
                            .foregroundColor(AppColors.textSecondary)
                    }
                }
                Spacer()
            }
            .padding(.horizontal, 24)
            .padding(.vertical, 12)
            .contentShape(Rectangle())
        }
    }
}

/// Recognition result: photo, best matches with confidence, portion picker
/// and the resolved nutrition estimate (Android's ScanResultSheet).
private struct ScanResultSheet: View {
    let image: UIImage?
    let candidates: [DishCandidate]
    var onContinue: (FoodPrefill) -> Void

    @State private var selected = 0
    @State private var portion = 1.0

    private var candidate: DishCandidate? {
        candidates.indices.contains(selected) ? candidates[selected] : nil
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                Text("Scan result")
                    .font(.system(size: 22, weight: .bold))
                    .foregroundColor(AppColors.textPrimary)

                if let image {
                    Image(uiImage: image)
                        .resizable()
                        .scaledToFill()
                        .frame(maxWidth: .infinity)
                        .frame(height: 180)
                        .clipped()
                        .clipShape(RoundedRectangle(cornerRadius: 16))
                }

                if let candidate {
                    Text("Best matches")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(AppColors.textPrimary)

                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ForEach(Array(candidates.enumerated()), id: \.offset) { index, item in
                                FoodChip(
                                    label: "\(item.displayName) \(Int(item.confidence * 100))%",
                                    selected: selected == index
                                ) {
                                    selected = index
                                }
                            }
                        }
                    }

                    if let nutrition = candidate.nutrition {
                        Text("Portion (\(nutrition.serving) = 1x)")
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundColor(AppColors.textPrimary)

                        HStack(spacing: 8) {
                            ForEach([0.5, 1.0, 1.5, 2.0], id: \.self) { value in
                                FoodChip(label: "\(value)x", selected: portion == value) {
                                    portion = value
                                }
                            }
                        }

                        VStack(alignment: .leading, spacing: 4) {
                            Text("\(Int(Double(nutrition.kcal) * portion)) kcal")
                                .font(.system(size: 24, weight: .bold))
                                .foregroundColor(AppColors.textPrimary)
                            Text("Protein \(Int(Double(nutrition.proteinG) * portion)) g • Carbs \(Int(Double(nutrition.carbsG) * portion)) g • Fat \(Int(Double(nutrition.fatG) * portion)) g")
                                .font(.system(size: 14))
                                .foregroundColor(AppColors.textSecondary)
                            Text("Estimate for a typical serving. You can edit it next.")
                                .font(.system(size: 12))
                                .foregroundColor(AppColors.textSecondary)
                        }
                        .padding(16)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(AppColors.primaryLight)
                        .clipShape(RoundedRectangle(cornerRadius: 16))

                        Button {
                            onContinue(FoodPrefill(
                                name: nutrition.name,
                                kcal: Int(Double(nutrition.kcal) * portion),
                                proteinG: Int(Double(nutrition.proteinG) * portion),
                                carbsG: Int(Double(nutrition.carbsG) * portion),
                                fatG: Int(Double(nutrition.fatG) * portion)
                            ))
                        } label: {
                            Text("Continue")
                                .font(.system(size: 16, weight: .semibold))
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 12)
                        }
                        .buttonStyle(.borderedProminent)
                        .tint(AppColors.primary)
                    } else {
                        Text("No nutrition data for \(candidate.displayName) yet — enter it next.")
                            .font(.system(size: 14))
                            .foregroundColor(AppColors.textSecondary)
                        Button {
                            onContinue(FoodPrefill(name: candidate.displayName, kcal: 0, proteinG: 0, carbsG: 0, fatG: 0))
                        } label: {
                            Text("Continue")
                                .font(.system(size: 16, weight: .semibold))
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 12)
                        }
                        .buttonStyle(.borderedProminent)
                        .tint(AppColors.primary)
                    }
                } else {
                    Text("Couldn't recognise this food. You can still add it manually.")
                        .font(.system(size: 14))
                        .foregroundColor(AppColors.textSecondary)
                    Button {
                        onContinue(FoodPrefill(name: "", kcal: 0, proteinG: 0, carbsG: 0, fatG: 0))
                    } label: {
                        Text("Add manually")
                            .font(.system(size: 16, weight: .semibold))
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 12)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(AppColors.primary)
                }
            }
            .padding(.horizontal, 24)
            .padding(.vertical, 20)
        }
    }
}

/// Manual add / edit-before-save sheet with offline search suggestions and
/// portion multipliers (Android's AddFoodSheet).
private struct AddFoodSheet: View {
    let initialMeal: MealType
    let prefill: FoodPrefill?
    var onSave: (String, MealType, Int, Double, Double, Double, Bool) -> Void
    var onCancel: () -> Void

    @State private var name: String
    @State private var calories: String
    @State private var protein: String
    @State private var carbs: String
    @State private var fat: String
    @State private var meal: MealType
    @State private var base: FoodItem?
    @State private var multiplier: Double = 1.0
    @State private var searching: Bool

    init(
        initialMeal: MealType,
        prefill: FoodPrefill?,
        onSave: @escaping (String, MealType, Int, Double, Double, Double, Bool) -> Void,
        onCancel: @escaping () -> Void
    ) {
        self.initialMeal = initialMeal
        self.prefill = prefill
        self.onSave = onSave
        self.onCancel = onCancel
        _name = State(initialValue: prefill?.name ?? "")
        _calories = State(initialValue: prefill.map { String($0.kcal) } ?? "")
        _protein = State(initialValue: prefill.map { String($0.proteinG) } ?? "")
        _carbs = State(initialValue: prefill.map { String($0.carbsG) } ?? "")
        _fat = State(initialValue: prefill.map { String($0.fatG) } ?? "")
        _meal = State(initialValue: initialMeal)
        _searching = State(initialValue: prefill == nil)
    }

    private var suggestions: [FoodItem] {
        searching ? FoodSearch.search(name) : []
    }

    private var canSave: Bool {
        !name.trimmingCharacters(in: .whitespaces).isEmpty && (Int(calories) ?? 0) > 0
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                HStack(spacing: 10) {
                    Image(systemName: "fork.knife")
                        .foregroundColor(AppColors.primary)
                    Text("Add food")
                        .font(.system(size: 22, weight: .bold))
                        .foregroundColor(AppColors.textPrimary)
                }

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(MealType.allCases) { type in
                            FoodChip(label: type.label, selected: meal == type) {
                                meal = type
                            }
                        }
                    }
                }

                TextField("Search food (e.g. dosa, biryani, paneer)", text: nameBinding)
                    .textFieldStyle(.roundedBorder)

                ForEach(suggestions) { item in
                    Button {
                        name = item.name
                        base = item
                        multiplier = 1.0
                        searching = false
                        applyBase()
                    } label: {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(item.name)
                                .font(.system(size: 15, weight: .medium))
                                .foregroundColor(AppColors.textPrimary)
                            Text("\(item.serving) • \(item.kcal) kcal")
                                .font(.system(size: 12))
                                .foregroundColor(AppColors.textSecondary)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.vertical, 4)
                    }
                }

                if let base {
                    Text("Portion (\(base.serving) = 1x)")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(AppColors.textPrimary)
                    HStack(spacing: 8) {
                        ForEach([0.5, 1.0, 1.5, 2.0, 3.0], id: \.self) { value in
                            FoodChip(label: "\(value)x", selected: multiplier == value) {
                                multiplier = value
                                applyBase()
                            }
                        }
                    }
                }

                TextField("Calories (kcal)", text: caloriesBinding)
                    .keyboardType(.numberPad)
                    .textFieldStyle(.roundedBorder)

                HStack(spacing: 8) {
                    TextField("Protein g", text: macroBinding($protein))
                        .keyboardType(.decimalPad)
                        .textFieldStyle(.roundedBorder)
                    TextField("Carbs g", text: macroBinding($carbs))
                        .keyboardType(.decimalPad)
                        .textFieldStyle(.roundedBorder)
                    TextField("Fat g", text: macroBinding($fat))
                        .keyboardType(.decimalPad)
                        .textFieldStyle(.roundedBorder)
                }

                HStack {
                    Spacer()
                    Button("Cancel", action: onCancel)
                    Button("Save") {
                        onSave(
                            name.trimmingCharacters(in: .whitespaces),
                            meal,
                            Int(calories) ?? 0,
                            Double(protein) ?? 0,
                            Double(carbs) ?? 0,
                            Double(fat) ?? 0,
                            base == nil && prefill == nil // typed in by hand, not picked or scanned
                        )
                    }
                    .font(.system(size: 15, weight: .semibold))
                    .disabled(!canSave)
                }
            }
            .padding(.horizontal, 24)
            .padding(.vertical, 20)
        }
    }

    // MARK: Field bindings (filtering lives in the setters, like Android's
    // onValueChange — programmatic sets from applyBase bypass the filters)

    private var nameBinding: Binding<String> {
        Binding(
            get: { name },
            set: { newValue in
                name = String(newValue.prefix(60))
                searching = true
                base = nil
            }
        )
    }

    private var caloriesBinding: Binding<String> {
        Binding(
            get: { calories },
            set: { newValue in
                calories = String(newValue.filter { $0.isNumber }.prefix(5))
            }
        )
    }

    private func macroBinding(_ value: Binding<String>) -> Binding<String> {
        Binding(
            get: { value.wrappedValue },
            set: { newValue in
                value.wrappedValue = String(newValue.filter { $0.isNumber || $0 == "." }.prefix(5))
            }
        )
    }

    private func applyBase() {
        guard let base else { return }
        calories = String(Int(Double(base.kcal) * multiplier))
        protein = String(Int(Double(base.proteinG) * multiplier))
        carbs = String(Int(Double(base.carbsG) * multiplier))
        fat = String(Int(Double(base.fatG) * multiplier))
    }
}

/// Pill chip used for meals, candidates and portions (FilterChip equivalent).
private struct FoodChip: View {
    let label: String
    let selected: Bool
    var action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(label)
                .font(.system(size: 13, weight: .semibold))
                .foregroundColor(selected ? .white : AppColors.primary)
                .padding(.horizontal, 12)
                .padding(.vertical, 7)
                .background(selected ? AppColors.primary : AppColors.primaryLight)
                .clipShape(Capsule())
        }
    }
}

// MARK: - Image helpers

extension UIImage {
    /// Downscales the image so its longest pixel dimension is at most 1024px
    /// (matching Android's decodeBitmap sample sizing) and encodes as JPEG at quality 0.8.
    /// Uses UIGraphicsImageRenderer with format.scale = 1.0 to ensure predictable pixel
    /// dimensions regardless of device screen scale, and bakes in EXIF orientation upright.
    func foodScanJPEGData() -> Data? {
        let maxDimension: CGFloat = 1024
        let pixelWidth = size.width * scale
        let pixelHeight = size.height * scale

        let targetSize: CGSize
        if pixelWidth > maxDimension || pixelHeight > maxDimension {
            let ratio = min(maxDimension / pixelWidth, maxDimension / pixelHeight)
            targetSize = CGSize(width: max(1, round(pixelWidth * ratio)), height: max(1, round(pixelHeight * ratio)))
        } else {
            targetSize = CGSize(width: max(1, round(pixelWidth)), height: max(1, round(pixelHeight)))
        }

        let format = UIGraphicsImageRendererFormat.default()
        format.scale = 1.0
        let renderer = UIGraphicsImageRenderer(size: targetSize, format: format)
        let resized = renderer.image { _ in
            draw(in: CGRect(origin: .zero, size: targetSize))
        }
        return resized.jpegData(compressionQuality: 0.8)
    }
}

/// Camera capture via UIImagePickerController (the library side uses the
/// system PhotosPicker, which needs no permission string).
private struct CameraImagePicker: UIViewControllerRepresentable {
    var onImage: (UIImage) -> Void
    var onCancel: () -> Void

    func makeUIViewController(context: Context) -> UIImagePickerController {
        let picker = UIImagePickerController()
        picker.sourceType = UIImagePickerController.isSourceTypeAvailable(.camera) ? .camera : .photoLibrary
        picker.delegate = context.coordinator
        return picker
    }

    func updateUIViewController(_ uiViewController: UIImagePickerController, context: Context) {}

    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }

    final class Coordinator: NSObject, UIImagePickerControllerDelegate, UINavigationControllerDelegate {
        private let parent: CameraImagePicker

        init(_ parent: CameraImagePicker) {
            self.parent = parent
        }

        func imagePickerController(
            _ picker: UIImagePickerController,
            didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey: Any]
        ) {
            if let image = info[.originalImage] as? UIImage {
                parent.onImage(image)
            } else {
                parent.onCancel()
            }
        }

        func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
            parent.onCancel()
        }
    }
}
