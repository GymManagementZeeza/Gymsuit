import SwiftUI
import WebKit

// MARK: - In-Memory Cache for Exercise GIFs

public final class ExerciseGifCache {
    public static let shared = ExerciseGifCache()
    private let cache = NSCache<NSURL, NSData>()

    private init() {
        cache.countLimit = 50
    }

    public func data(for url: URL) -> Data? {
        cache.object(forKey: url as NSURL) as Data?
    }

    public func set(data: Data, for url: URL) {
        cache.setObject(data as NSData, forKey: url as NSURL)
    }
}

// MARK: - Shimmer Loading Animation for Network Calls

public struct ShimmerLoadingView: View {
    @State private var startPoint = UnitPoint(x: -1.8, y: -1.2)
    @State private var endPoint = UnitPoint(x: 0.0, y: -0.2)

    public init() {}

    public var body: some View {
        LinearGradient(
            gradient: Gradient(colors: [
                Color(hex: 0xFFF1F5F9),
                Color.white.opacity(0.85),
                Color(hex: 0xFFE2E8F0)
            ]),
            startPoint: startPoint,
            endPoint: endPoint
        )
        .onAppear {
            withAnimation(
                .easeInOut(duration: 1.3)
                .repeatForever(autoreverses: false)
            ) {
                startPoint = UnitPoint(x: 1.0, y: 1.0)
                endPoint = UnitPoint(x: 2.8, y: 2.2)
            }
        }
    }
}

// MARK: - Async GIF View with Shimmer Loading

public struct ExerciseGifAsyncView: View {
    let url: URL?
    @State private var gifData: Data? = nil
    @State private var isLoading: Bool = true
    @State private var hasError: Bool = false

    public init(url: URL?) {
        self.url = url
    }

    public var body: some View {
        ZStack {
            Color(hex: 0xFFF8FAFC)

            if let data = gifData {
                ExerciseGifWKWebView(data: data)
                    .transition(.opacity)
            } else if isLoading {
                ShimmerLoadingView()
                    .transition(.opacity)
            } else {
                VStack(spacing: 6) {
                    Image(systemName: "figure.strengthtraining.traditional")
                        .font(.system(size: 30))
                        .foregroundColor(Color(hex: 0xFF94A3B8))
                    Text("Exercise Demo")
                        .font(.system(size: 11, weight: .medium))
                        .foregroundColor(Color(hex: 0xFF94A3B8))
                }
            }
        }
        .task(id: url) {
            await loadGif()
        }
    }

    private func loadGif() async {
        guard let url else {
            await MainActor.run { isLoading = false }
            return
        }

        if let cached = ExerciseGifCache.shared.data(for: url) {
            await MainActor.run {
                self.gifData = cached
                self.isLoading = false
            }
            return
        }

        await MainActor.run {
            self.isLoading = true
            self.hasError = false
        }

        do {
            var req = URLRequest(url: url)
            req.timeoutInterval = 15
            let (data, response) = try await URLSession.shared.data(for: req)
            if let http = response as? HTTPURLResponse, http.statusCode == 200, !data.isEmpty {
                ExerciseGifCache.shared.set(data: data, for: url)
                await MainActor.run {
                    withAnimation(.easeInOut(duration: 0.25)) {
                        self.gifData = data
                        self.isLoading = false
                    }
                }
            } else {
                await MainActor.run {
                    self.isLoading = false
                    self.hasError = true
                }
            }
        } catch {
            await MainActor.run {
                self.isLoading = false
                self.hasError = true
            }
        }
    }
}

// MARK: - WKWebView for GIF Playback

public struct ExerciseGifWKWebView: UIViewRepresentable {
    let data: Data

    public func makeUIView(context: Context) -> WKWebView {
        let config = WKWebViewConfiguration()
        let webView = WKWebView(frame: .zero, configuration: config)
        webView.scrollView.isScrollEnabled = false
        webView.isOpaque = false
        webView.backgroundColor = .clear
        webView.scrollView.backgroundColor = .clear
        return webView
    }

    public func updateUIView(_ webView: WKWebView, context: Context) {
        webView.load(
            data,
            mimeType: "image/gif",
            characterEncodingName: "UTF-8",
            baseURL: URL(string: "https://api.gymsuit.app")!
        )
    }
}

// MARK: - Body Focus Section View

public struct WorkoutBodyFocusSectionView: View {
    @ObservedObject private var workoutStore = WorkoutStore.shared
    @Environment(\.scenePhase) private var scenePhase
    @State private var selectedFocus: String = "Forearm"

    // Focus categories matching reference design ("Abs", "Arm", "Forearm", "Chest", "Leg"...)
    private let focusOptions: [String] = [
        "Abs",
        "Arm",
        "Forearm",
        "Chest",
        "Leg",
        "Back",
        "Shoulders"
    ]

    private var filteredExercises: [Exercise] {
        let matches = workoutStore.exercises.filter { ex in
            matchesFocus(ex, focus: selectedFocus)
        }
        return matches.isEmpty ? workoutStore.exercises : matches
    }

    private func matchesFocus(_ ex: Exercise, focus: String) -> Bool {
        let name = ex.name.lowercased()
        let bodyPart = ex.bodyPart.lowercased()
        let primary = ex.primaryMuscle.lowercased()
        let secondary = ex.secondaryMuscles.map { $0.lowercased() }

        switch focus {
        case "Chest":
            return bodyPart.contains("chest") || primary.contains("chest") || primary.contains("pectoral")
        case "Arm":
            return bodyPart.contains("arm") || primary.contains("biceps") || primary.contains("triceps") || primary.contains("brachii")
        case "Forearm":
            return name.contains("hammer") || primary.contains("forearm") || primary.contains("brachioradialis") || secondary.contains("forearms")
        case "Back":
            return bodyPart.contains("back") || primary.contains("lat") || primary.contains("trapezius") || name.contains("pulldown") || name.contains("deadlift")
        case "Leg":
            return bodyPart.contains("leg") || primary.contains("quad") || primary.contains("glute") || primary.contains("hamstring") || name.contains("squat") || name.contains("deadlift")
        case "Shoulders":
            return bodyPart.contains("shoulder") || primary.contains("deltoid") || name.contains("raise")
        case "Abs":
            return bodyPart.contains("waist") || primary.contains("abs") || secondary.contains("abs") || name.contains("squat")
        default:
            return true
        }
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            // Section Header matching uploaded image
            Text("Body Focus")
                .font(.system(size: 22, weight: .bold))
                .foregroundColor(Color(hex: 0xFF0F172A))
                .padding(.horizontal, 4)

            // Horizontal scrolling pills
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 10) {
                    ForEach(focusOptions, id: \.self) { option in
                        let isSelected = selectedFocus == option

                        Button(action: {
                            withAnimation(.spring(response: 0.3, dampingFraction: 0.75)) {
                                selectedFocus = option
                            }
                        }) {
                            Text(option)
                                .font(.system(size: 15, weight: isSelected ? .bold : .medium))
                                .foregroundColor(isSelected ? Color(hex: 0xFF0062FF) : Color(hex: 0xFF94A3B8))
                                .padding(.horizontal, 20)
                                .padding(.vertical, 10)
                                .background(
                                    isSelected
                                        ? Color(hex: 0xFFEFF6FF)
                                        : Color(hex: 0xFFF1F5F9)
                                )
                                .clipShape(Capsule())
                                .overlay(
                                    Capsule()
                                        .stroke(
                                            isSelected ? Color(hex: 0xFF0062FF) : Color.clear,
                                            lineWidth: 1.5
                                        )
                                )
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(.horizontal, 4)
                .padding(.vertical, 2)
            }

            // 2-Column Grid of Exercises
            LazyVGrid(
                columns: [
                    GridItem(.flexible(), spacing: 14),
                    GridItem(.flexible(), spacing: 14)
                ],
                spacing: 16
            ) {
                ForEach(filteredExercises) { exercise in
                    exerciseCard(for: exercise)
                }
            }
            .padding(.top, 4)
        }
        .task {
            await workoutStore.fetchLatestExercisesFromBackend()
        }
        .onChange(of: scenePhase) { _, newPhase in
            if newPhase == .active {
                Task { await workoutStore.fetchLatestExercisesFromBackend() }
            }
        }
    }

    private func exerciseCard(for exercise: Exercise) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            // GIF Preview with shimmer loading animation during the API call
            ExerciseGifAsyncView(url: exercise.gifURL)
                .frame(height: 135)
                .frame(maxWidth: .infinity)
                .clipped()
                .background(Color(hex: 0xFFF8FAFC))

            // Exercise Title and Target Details
            VStack(alignment: .leading, spacing: 4) {
                Text(exercise.name.capitalized)
                    .font(.system(size: 14, weight: .bold))
                    .foregroundColor(Color(hex: 0xFF0F172A))
                    .lineLimit(2)
                    .multilineTextAlignment(.leading)
                    .frame(height: 38, alignment: .topLeading)

                Text("\(exercise.muscleGroup) • \(exercise.equipment.capitalized)")
                    .font(.system(size: 11, weight: .medium))
                    .foregroundColor(Color(hex: 0xFF64748B))
                    .lineLimit(1)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 10)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color.white)
        }
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        .background(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .fill(Color.white)
        )
        .overlay(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .stroke(Color(hex: 0xFFF1F5F9), lineWidth: 1)
        )
        .shadow(color: Color.black.opacity(0.04), radius: 8, y: 3)
    }
}
