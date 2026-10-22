//
//  GymsuitApp.swift
//  Gymsuit
//
//  Created by Athul Babu on 2026-09-28.
//

import SwiftUI
import UIKit

class AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        PushNotificationManager.shared.configure(application: application)
        return true
    }

    func application(
        _ application: UIApplication,
        didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
    ) {
        PushNotificationManager.shared.handleDeviceToken(deviceToken)
    }

    func application(
        _ application: UIApplication,
        didFailToRegisterForRemoteNotificationsWithError error: Error
    ) {
        print("Failed to register for remote notifications: \(error.localizedDescription)")
    }
}

@main
struct GymsuitApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate
    @Environment(\.scenePhase) private var scenePhase

    init() {
        BackgroundHealthSync.register()
        BackgroundHealthSync.schedule()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .preferredColorScheme(.light)
        }
        .onChange(of: scenePhase) { _, newPhase in
            // Re-arm the background refresh every time the app backgrounds.
            if newPhase == .background {
                BackgroundHealthSync.schedule()
            }
        }
    }
}
