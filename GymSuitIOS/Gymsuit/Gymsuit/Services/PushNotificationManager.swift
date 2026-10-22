//
//  PushNotificationManager.swift
//  Gymsuit
//

import Foundation
import UIKit
import UserNotifications
#if canImport(FirebaseCore)
import FirebaseCore
#endif
#if canImport(FirebaseMessaging)
import FirebaseMessaging
#endif

public final class PushNotificationManager: NSObject, UNUserNotificationCenterDelegate {
    public static let shared = PushNotificationManager()

    public func configure(application: UIApplication) {
        #if canImport(FirebaseCore)
        if FirebaseApp.app() == nil {
            FirebaseApp.configure()
        }
        #endif

        UNUserNotificationCenter.current().delegate = self

        let authOptions: UNAuthorizationOptions = [.alert, .badge, .sound]
        UNUserNotificationCenter.current().requestAuthorization(options: authOptions) { granted, error in
            if granted {
                DispatchQueue.main.async {
                    application.registerForRemoteNotifications()
                }
            }
        }

        #if canImport(FirebaseMessaging)
        Messaging.messaging().delegate = self
        #endif
    }

    public func handleDeviceToken(_ deviceToken: Data) {
        #if canImport(FirebaseMessaging)
        Messaging.messaging().apnsToken = deviceToken
        #endif
    }
}

#if canImport(FirebaseMessaging)
extension PushNotificationManager: MessagingDelegate {
    public func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        guard let token = fcmToken, !token.isEmpty else { return }
        AuthManager.shared.saveFcmToken(token)
        if AuthManager.shared.isLoggedIn {
            Task {
                await HealthSyncApi.shared.updateFcmToken(token)
            }
        }
    }
}
#endif

// MARK: - UNUserNotificationCenterDelegate
extension PushNotificationManager {
    public func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        if #available(iOS 14.0, *) {
            completionHandler([.banner, .sound, .badge])
        } else {
            completionHandler([.alert, .sound, .badge])
        }
    }

    public func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let userInfo = response.notification.request.content.userInfo
        let notificationType = userInfo["type"] as? String
        let challengeId = userInfo["challengeId"] as? String

        NotificationCenter.default.post(
            name: NSNotification.Name("GymSuitNotificationTapped"),
            object: nil,
            userInfo: [
                "type": notificationType ?? "",
                "challengeId": challengeId ?? ""
            ]
        )
        completionHandler()
    }
}
