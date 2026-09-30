import Foundation
import Combine
import SwiftUI

public final class AuthManager: ObservableObject {
    public static let shared = AuthManager()

    private let defaults = UserDefaults.standard
    private let keyToken = "access_token"
    private let keyRefreshToken = "refresh_token"
    private let keyEmail = "user_email"
    private let keyFirstName = "first_name"
    private let keyLastName = "last_name"
    private let keyRole = "user_role"
    private let keyMemberId = "member_id"
    private let keyGymId = "gym_id"
    private let keyGymName = "gym_name"

    @Published public var currentSession: UserSession?

    public init() {
        migrateTokensToKeychainIfNeeded()
        self.currentSession = getSession()
    }

    /// One-time migration: move tokens previously stored in plaintext
    /// UserDefaults into the Keychain, then delete the old copies.
    private func migrateTokensToKeychainIfNeeded() {
        if KeychainHelper.get(forKey: keyToken) == nil,
           let legacy = defaults.string(forKey: keyToken), !legacy.isEmpty {
            KeychainHelper.set(legacy, forKey: keyToken)
        }
        if KeychainHelper.get(forKey: keyRefreshToken) == nil,
           let legacy = defaults.string(forKey: keyRefreshToken), !legacy.isEmpty {
            KeychainHelper.set(legacy, forKey: keyRefreshToken)
        }
        defaults.removeObject(forKey: keyToken)
        defaults.removeObject(forKey: keyRefreshToken)
    }

    public func saveSession(_ session: UserSession) {
        KeychainHelper.set(session.token, forKey: keyToken)
        if session.refreshToken.isEmpty {
            KeychainHelper.delete(forKey: keyRefreshToken)
        } else {
            KeychainHelper.set(session.refreshToken, forKey: keyRefreshToken)
        }
        defaults.set(session.email, forKey: keyEmail)
        defaults.set(session.firstName, forKey: keyFirstName)
        defaults.set(session.lastName, forKey: keyLastName)
        defaults.set(session.role, forKey: keyRole)

        if let memberId = session.memberId {
            defaults.set(memberId, forKey: keyMemberId)
        } else {
            defaults.removeObject(forKey: keyMemberId)
        }

        if let gymId = session.gymId {
            defaults.set(gymId, forKey: keyGymId)
        } else {
            defaults.removeObject(forKey: keyGymId)
        }

        if let gymName = session.gymName {
            defaults.set(gymName, forKey: keyGymName)
        } else {
            defaults.removeObject(forKey: keyGymName)
        }

        self.currentSession = session
    }

    public func getAccessToken() -> String? {
        return KeychainHelper.get(forKey: keyToken)
    }

    public func getRefreshToken() -> String? {
        return KeychainHelper.get(forKey: keyRefreshToken)
    }

    public var isLoggedIn: Bool {
        guard let token = getAccessToken() else { return false }
        return !token.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    public func getSession() -> UserSession? {
        guard let token = getAccessToken(), !token.isEmpty else { return nil }
        let refreshToken = getRefreshToken() ?? ""
        let email = defaults.string(forKey: keyEmail) ?? ""
        let firstName = defaults.string(forKey: keyFirstName) ?? ""
        let lastName = defaults.string(forKey: keyLastName) ?? ""
        let role = defaults.string(forKey: keyRole) ?? "MEMBER"
        let memberId = defaults.object(forKey: keyMemberId) as? Int64
        let gymId = defaults.object(forKey: keyGymId) as? Int64
        let gymName = defaults.string(forKey: keyGymName)

        return UserSession(
            token: token,
            refreshToken: refreshToken,
            email: email,
            firstName: firstName,
            lastName: lastName,
            role: role,
            memberId: memberId,
            gymId: gymId,
            gymName: gymName
        )
    }

    /// Silently refreshes the access token using the stored refresh token.
    /// Returns true when the session was renewed. On failure the existing
    /// session is left untouched (the access token may still be valid);
    /// callers should only clear the session on explicit logout or a 401.
    @discardableResult
    public func refreshSession() async -> Bool {
        guard let refreshToken = getRefreshToken(), !refreshToken.isEmpty else {
            return false
        }
        do {
            let session = try await MobileAuthApi.shared.refreshToken(refreshToken: refreshToken)
            saveSession(session)
            return true
        } catch {
            return false
        }
    }

    public func updateName(firstName: String, lastName: String) {
        defaults.set(firstName.trimmingCharacters(in: .whitespacesAndNewlines), forKey: keyFirstName)
        defaults.set(lastName.trimmingCharacters(in: .whitespacesAndNewlines), forKey: keyLastName)
        if var session = currentSession {
            session.firstName = firstName.trimmingCharacters(in: .whitespacesAndNewlines)
            session.lastName = lastName.trimmingCharacters(in: .whitespacesAndNewlines)
            self.currentSession = session
        }
    }

    public func clear() {
        KeychainHelper.delete(forKey: keyToken)
        KeychainHelper.delete(forKey: keyRefreshToken)
        defaults.removeObject(forKey: keyEmail)
        defaults.removeObject(forKey: keyFirstName)
        defaults.removeObject(forKey: keyLastName)
        defaults.removeObject(forKey: keyRole)
        defaults.removeObject(forKey: keyMemberId)
        defaults.removeObject(forKey: keyGymId)
        defaults.removeObject(forKey: keyGymName)
        self.currentSession = nil
    }
}
