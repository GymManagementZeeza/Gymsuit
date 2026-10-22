//
//  WatchConnectivityManager.swift
//  Gymsuit
//

import Foundation
import WatchConnectivity

public final class WatchConnectivityManager: NSObject, WCSessionDelegate {
    public static let shared = WatchConnectivityManager()

    private var session: WCSession?

    private override init() {
        super.init()
        activateSession()
    }

    public func activateSession() {
        guard WCSession.isSupported() else { return }
        session = WCSession.default
        session?.delegate = self
        session?.activate()
    }

    public func syncMetricsToWatch(
        steps: Int64,
        calories: Double,
        heartRate: Int,
        sleepHours: Double,
        goal: Int64 = 10_000
    ) {
        guard let session = session, session.activationState == .activated else { return }

        let payload: [String: Any] = [
            "steps": steps,
            "calories": calories,
            "heartRate": heartRate,
            "sleepHours": sleepHours,
            "stepGoal": goal,
            "timestamp": Date().timeIntervalSince1970
        ]

        do {
            try session.updateApplicationContext(payload)
        } catch {
            print("Failed to update Watch application context: \(error.localizedDescription)")
        }

        if session.isReachable {
            session.sendMessage(payload, replyHandler: nil) { err in
                print("Failed to send live message to Watch: \(err.localizedDescription)")
            }
        }
    }

    // MARK: - WCSessionDelegate
    public func session(_ session: WCSession, activationDidCompleteWith activationState: WCSessionActivationState, error: Error?) {
        if let error = error {
            print("WCSession on iOS failed to activate: \(error.localizedDescription)")
        }
    }

    public func sessionDidBecomeInactive(_ session: WCSession) {}

    public func sessionDidDeactivate(_ session: WCSession) {
        session.activate()
    }

    public func session(_ session: WCSession, didReceiveMessage message: [String : Any]) {
        if message["action"] as? String == "requestSync" {
            NotificationCenter.default.post(name: NSNotification.Name("GymSuitRequestWatchSync"), object: nil)
        }
    }
}
