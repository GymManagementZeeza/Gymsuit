import Foundation
import Combine

public enum AiSummaryUiState: Equatable {
    case idle
    case loading
    case success(String)
    case error(String)
}

@MainActor
public final class DashboardViewModel: ObservableObject {
    @Published public var summaryState: AiSummaryUiState = .idle
    @Published public var selectedDate: Date = Date()
    @Published public var stepsCount: Int64 = 8420
    @Published public var caloriesBreakdown: CaloriesBreakdown = CaloriesBreakdown(totalKcal: 2350, stepsKcal: 420, workoutKcal: 680, moveKcal: 1250, hasData: true)
    @Published public var sleepSession: SleepSessionData?
    @Published public var heartRateSummary: HeartRateSummaryData?
    @Published public var isLoadingMetrics: Bool = false
    
    private var activeTask: Task<Void, Never>?
    private let repository: AiSummaryRepository
    private let healthKitManager: HealthKitManager
    
    public init(
        repository: AiSummaryRepository = .shared,
        healthKitManager: HealthKitManager = .shared
    ) {
        self.repository = repository
        self.healthKitManager = healthKitManager
        onDateSelected(Date())
    }
    
    public func onDateSelected(_ date: Date, forceRefresh: Bool = false) {
        self.selectedDate = date
        activeTask?.cancel()
        
        // 1. Immediately display cached summary if present
        if !forceRefresh, let cached = repository.getCachedSummary(for: date) {
            self.summaryState = .success(cached)
        } else {
            // Start loading state
            self.summaryState = .loading
            
            // 3-second hold before generating AI insight (matching Android implementation)
            activeTask = Task {
                do {
                    try await Task.sleep(nanoseconds: 3_000_000_000)
                    guard !Task.isCancelled else { return }
                    let summary = try await self.repository.getSummary(for: date, forceRefresh: forceRefresh)
                    guard !Task.isCancelled else { return }
                    if !summary.isEmpty {
                        self.summaryState = .success(summary)
                    } else {
                        self.summaryState = .error("AI summary is not available now, please try again later.")
                    }
                } catch {
                    guard !Task.isCancelled else { return }
                    self.summaryState = .error("AI summary is not available now, please try again later.")
                }
            }
        }
        
        // 2. Fetch Day's Biometrics
        Task {
            self.isLoadingMetrics = true
            async let s = healthKitManager.fetchSteps(for: date)
            async let c = healthKitManager.fetchCaloriesBreakdown(for: date)
            async let sl = healthKitManager.fetchSleepSession(for: date)
            async let hr = healthKitManager.fetchHeartRateSummary(for: date)
            
            let (steps, calories, sleep, heartRate) = await (s, c, sl, hr)
            self.stepsCount = steps
            self.caloriesBreakdown = calories
            self.sleepSession = sleep
            self.heartRateSummary = heartRate
            self.isLoadingMetrics = false
        }
    }
    
    public func retryCurrentDate() {
        onDateSelected(selectedDate, forceRefresh: true)
    }
}
