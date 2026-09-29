import Foundation

public final class AiSummaryRepository {
    public static let shared = AiSummaryRepository()
    
    private let defaults = UserDefaults.standard
    private let keySummaryPrefix = "summary_"
    private let keyTimestampPrefix = "timestamp_"
    private let oneHourMillis: Double = 60 * 60 * 1000.0
    
    private let healthKitManager: HealthKitManager
    private let localAiEngine: LocalAiEngine
    
    public init(healthKitManager: HealthKitManager = .shared, localAiEngine: LocalAiEngine = OnDeviceRuleInsightEngine()) {
        self.healthKitManager = healthKitManager
        self.localAiEngine = localAiEngine
    }
    
    private func dateKey(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter.string(from: date)
    }
    
    public func getCachedSummary(for date: Date) -> String? {
        let key = dateKey(date)
        guard let cachedText = defaults.string(forKey: "\(keySummaryPrefix)\(key)") else { return nil }
        
        if Calendar.current.isDateInToday(date) {
            let lastFetchTime = defaults.double(forKey: "\(keyTimestampPrefix)\(key)")
            let now = Date().timeIntervalSince1970 * 1000.0
            if now - lastFetchTime > oneHourMillis {
                return nil
            }
        }
        return cachedText
    }
    
    public func getSummary(for date: Date, forceRefresh: Bool = false) async throws -> String {
        let key = dateKey(date)
        
        if !forceRefresh, let cached = getCachedSummary(for: date) {
            return cached
        }
        
        let request = await healthKitManager.buildAiSummarizeRequest(for: date)
        let summary = await localAiEngine.generateSummary(request: request)
        
        if !summary.isEmpty {
            defaults.set(summary, forKey: "\(keySummaryPrefix)\(key)")
            defaults.set(Date().timeIntervalSince1970 * 1000.0, forKey: "\(keyTimestampPrefix)\(key)")
        }
        
        return summary
    }
}
