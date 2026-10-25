import Foundation
import HealthKit

/// HealthKit reads for the widgets. The extension queries HealthKit directly
/// (it shares the containing app's authorization), so steps and heart rate
/// never depend on the main app having run recently.
enum HealthKitWidgetReader {
    static let store = HKHealthStore()

    static func stepsToday() async -> Int64? {
        guard HKHealthStore.isHealthDataAvailable(),
              let type = HKQuantityType.quantityType(forIdentifier: .stepCount) else {
            return nil
        }
        let start = Calendar.current.startOfDay(for: Date())
        let predicate = HKQuery.predicateForSamples(
            withStart: start, end: Date(), options: .strictStartDate
        )
        return await withCheckedContinuation { continuation in
            let query = HKStatisticsQuery(
                quantityType: type,
                quantitySamplePredicate: predicate,
                options: .cumulativeSum
            ) { _, result, _ in
                let steps = result?.sumQuantity()?.doubleValue(for: HKUnit.count())
                continuation.resume(returning: steps.map { Int64($0) })
            }
            store.execute(query)
        }
    }

    /// (latest, min, max) bpm for the given day, or nil when there are no
    /// samples or the read fails.
    static func heartRateForDay(_ day: Date) async -> (latest: Int, min: Int, max: Int)? {
        guard HKHealthStore.isHealthDataAvailable(),
              let type = HKQuantityType.quantityType(forIdentifier: .heartRate) else {
            return nil
        }
        let calendar = Calendar.current
        let start = calendar.startOfDay(for: day)
        let end = calendar.date(byAdding: .day, value: 1, to: start)!
        let predicate = HKQuery.predicateForSamples(
            withStart: start, end: end, options: .strictStartDate
        )
        let sort = NSSortDescriptor(
            key: HKSampleSortIdentifierStartDate, ascending: true
        )
        return await withCheckedContinuation { continuation in
            let query = HKSampleQuery(
                sampleType: type,
                predicate: predicate,
                limit: HKObjectQueryNoLimit,
                sortDescriptors: [sort]
            ) { _, samples, _ in
                let bpmUnit = HKUnit.count().unitDivided(by: HKUnit.minute())
                let values = (samples as? [HKQuantitySample] ?? []).map {
                    Int($0.quantity.doubleValue(for: bpmUnit).rounded())
                }
                guard let latest = values.last,
                      let min = values.min(),
                      let max = values.max() else {
                    continuation.resume(returning: nil)
                    return
                }
                continuation.resume(returning: (latest: latest, min: min, max: max))
            }
            store.execute(query)
        }
    }

    static func heartRateAuthorization() -> HKAuthorizationStatus {
        guard let type = HKQuantityType.quantityType(forIdentifier: .heartRate) else {
            return .notDetermined
        }
        return store.authorizationStatus(for: type)
    }
}
