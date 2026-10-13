//
//  GymsuitApp.swift
//  Gymsuit
//
//  Created by Athul Babu on 2026-09-28.
//

import SwiftUI

@main
struct GymsuitApp: App {
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
