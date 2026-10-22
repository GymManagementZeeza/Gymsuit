//
//  WatchMetricsManager.swift
//  GymsuitKit Watch App
//

import Foundation
import WatchConnectivity
import HealthKit
import Combine

public final class WatchMetricsManager: NSObject, ObservableObject, WCSessionDelegate {
    public static let shared = WatchMetricsManager()

    @Published public var steps: Int64 = 0
    @Published public var calories: Double = 0.0
    @Published public var heartRate: Int = 0
    @Published public var sleepHours: Double = 0.0
    @Published public var stepGoal: Int64 = 10_000
    @Published public var lastUpdated: Date? = nil
    @Published public var isSyncing: Bool = false

    private let healthStore = HKHealthStore()
    private let defaults = UserDefaults.standard

    private override init() {
        super.init()
        loadCachedMetrics()
        activateSession()
        queryLocalHealthKitIfAvailable()
    }

    public func activateSession() {
        guard WCSession.isSupported() else { return }
        WCSession.default.delegate = self
        WCSession.default.activate()
    }

    public func requestSync() {
        isSyncing = true

        // 1. Try Live Session message if iPhone is reachable
        if WCSession.default.isReachable {
            WCSession.default.sendMessage(["action": "requestSync"], replyHandler: nil) { [weak self] _ in
                DispatchQueue.main.async { self?.isSyncing = false }
            }
        } else if let context = WCSession.default.receivedApplicationContext as [String: Any]?, !context.isEmpty {
            // Apply last received context
            applyPayload(context)
            isSyncing = false
        } else {
            isSyncing = false
        }

        // 2. Also query local watch HealthKit directly
        queryLocalHealthKitIfAvailable()
    }

    private func applyPayload(_ payload: [String: Any]) {
        DispatchQueue.main.async {
            if let steps = payload["steps"] as? Int64 {
                self.steps = steps
            } else if let steps = payload["steps"] as? Int {
                self.steps = Int64(steps)
            }

            if let calories = payload["calories"] as? Double {
                self.calories = calories
            }

            if let heartRate = payload["heartRate"] as? Int {
                self.heartRate = heartRate
            }

            if let sleepHours = payload["sleepHours"] as? Double {
                self.sleepHours = sleepHours
            }

            if let stepGoal = payload["stepGoal"] as? Int64 {
                self.stepGoal = stepGoal
            } else if let stepGoal = payload["stepGoal"] as? Int {
                self.stepGoal = Int64(stepGoal)
            }

            if let timestamp = payload["timestamp"] as? TimeInterval {
                self.lastUpdated = Date(timeIntervalSince1970: timestamp)
            } else {
                self.lastUpdated = Date()
            }

            self.isSyncing = false
            self.persistMetrics()
        }
    }

    // MARK: - Local Persistence
    private func persistMetrics() {
        defaults.set(steps, forKey: "cached_steps")
        defaults.set(calories, forKey: "cached_calories")
        defaults.set(heartRate, forKey: "cached_heartRate")
        defaults.set(sleepHours, forKey: "cached_sleepHours")
        defaults.set(stepGoal, forKey: "cached_stepGoal")
        if let lastUpdated = lastUpdated {
            defaults.set(lastUpdated.timeIntervalSince1970, forKey: "cached_lastUpdated")
        }
    }

    private func loadCachedMetrics() {
        let savedSteps = defaults.object(forKey: "cached_steps") as? Int64 ?? 0
        let savedCal = defaults.double(forKey: "cached_calories")
        let savedHr = defaults.integer(forKey: "cached_heartRate")
        let savedSleep = defaults.double(forKey: "cached_sleepHours")
        let savedGoal = defaults.object(forKey: "cached_stepGoal") as? Int64 ?? 10_000
        let savedTimestamp = defaults.double(forKey: "cached_lastUpdated")

        self.steps = savedSteps
        self.calories = savedCal
        self.heartRate = savedHr
        self.sleepHours = savedSleep
        self.stepGoal = savedGoal > 0 ? savedGoal : 10_000
        if savedTimestamp > 0 {
            self.lastUpdated = Date(timeIntervalSince1970: savedTimestamp)
        }
    }

    // MARK: - Local HealthKit Query Fallback
    private func queryLocalHealthKitIfAvailable() {
        guard HKHealthStore.isHealthDataAvailable() else { return }
        guard let stepType = HKQuantityType.quantityType(forIdentifier: .stepCount) else { return }

        let calendar = Calendar.current
        let startOfDay = calendar.startOfDay(for: Date())
        let predicate = HKQuery.predicateForSamples(withStart: startOfDay, end: Date(), options: .strictStartDate)

        let query = HKStatisticsQuery(quantityType: stepType, quantitySamplePredicate: predicate, options: .cumulativeSum) { [weak self] _, stats, _ in
            guard let self = self, let sum = stats?.sumQuantity() else { return }
            let count = Int64(sum.doubleValue(for: HKUnit.count()))
            DispatchQueue.main.async {
                if count > self.steps {
                    self.steps = count
                    self.lastUpdated = Date()
                }
            }
        }
        healthStore.execute(query)
    }

    // MARK: - WCSessionDelegate
    public func session(_ session: WCSession, activationDidCompleteWith activationState: WCSessionActivationState, error: Error?) {
        if activationState == .activated {
            if !session.receivedApplicationContext.isEmpty {
                applyPayload(session.receivedApplicationContext)
            }
            requestSync()
        }
    }

    public func session(_ session: WCSession, didReceiveApplicationContext applicationContext: [String : Any]) {
        applyPayload(applicationContext)
    }

    public func session(_ session: WCSession, didReceiveMessage message: [String : Any]) {
        applyPayload(message)
    }
}
