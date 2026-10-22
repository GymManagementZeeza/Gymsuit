import SwiftUI

struct SettingsView: View {
    var onBack: () -> Void
    var onLogout: () -> Void
    var onRestartWorkoutOnboarding: (() -> Void)? = nil

    @State private var healthSyncEnabled: Bool = BackgroundHealthSync.isEnabled
    @State private var showingLogoutAlert: Bool = false
    @State private var showingDeleteAccountAlert: Bool = false
    @State private var showingDeleteWorkoutAlert: Bool = false
    @State private var deleteError: String? = nil
    @State private var isDeletingAccount: Bool = false
    @State private var isDeletingWorkout: Bool = false

    @ObservedObject private var authManager = AuthManager.shared

    /// Display name from the signed-in session; falls back to a generic
    /// label instead of a fake name when the session has no name.
    private var displayName: String {
        guard let session = authManager.currentSession else { return "Athlete" }
        let fullName = [session.firstName, session.lastName]
            .filter { !$0.isEmpty }
            .joined(separator: " ")
        return fullName.isEmpty ? "Athlete" : fullName
    }

    var body: some View {
        ZStack {
            AppColors.surface.ignoresSafeArea()

            VStack(spacing: 0) {
                // Top Header
                HStack {
                    Button(action: onBack) {
                        Image(systemName: "chevron.left")
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(AppColors.textPrimary)
                            .frame(width: 44, height: 44)
                            .background(Color.white)
                            .clipShape(Circle())
                            .shadow(color: Color.black.opacity(0.04), radius: 4, y: 2)
                    }

                    Spacer()

                    Text("Settings")
                        .font(.system(size: 18, weight: .bold))
                        .foregroundColor(AppColors.textPrimary)

                    Spacer()

                    Color.clear.frame(width: 44, height: 44)
                }
                .padding(.horizontal, 20)
                .padding(.top, 10)
                .padding(.bottom, 12)

                ScrollView {
                    VStack(spacing: 20) {
                        // Profile Banner Card
                        HStack(spacing: 16) {
                            Image("avatar_gaze_1")
                                .resizable()
                                .scaledToFill()
                                .frame(width: 60, height: 60)
                                .clipShape(Circle())

                            VStack(alignment: .leading, spacing: 4) {
                                Text(displayName)
                                    .font(.system(size: 18, weight: .bold))
                                    .foregroundColor(AppColors.textPrimary)
                                if let session = authManager.currentSession, !session.email.isEmpty {
                                    Text(session.email)
                                        .font(.system(size: 13))
                                        .foregroundColor(AppColors.textSecondary)
                                }
                                if let gymName = authManager.currentSession?.gymName, !gymName.isEmpty {
                                    Text(gymName)
                                        .font(.system(size: 12, weight: .medium))
                                        .foregroundColor(AppColors.primary)
                                }
                            }
                            Spacer()
                        }
                        .padding(20)
                        .background(Color.white)
                        .clipShape(RoundedRectangle(cornerRadius: 24))
                        .shadow(color: Color.black.opacity(0.03), radius: 6, y: 3)

                        // Preferences Section
                        VStack(alignment: .leading, spacing: 14) {
                            Text("Preferences")
                                .font(.system(size: 15, weight: .semibold))
                                .foregroundColor(AppColors.textSecondary)

                            Toggle(isOn: $healthSyncEnabled) {
                                HStack {
                                    Image(systemName: "heart.fill")
                                        .foregroundColor(AppColors.heartRate)
                                    Text("Apple Health Auto-Sync")
                                        .font(.system(size: 15, weight: .medium))
                                        .foregroundColor(AppColors.textPrimary)
                                }
                            }
                        }
                        .padding(20)
                        .background(Color.white)
                        .clipShape(RoundedRectangle(cornerRadius: 24))
                        .onChange(of: healthSyncEnabled) { _, newValue in
                            BackgroundHealthSync.isEnabled = newValue
                            if newValue {
                                BackgroundHealthSync.schedule()
                            } else {
                                BackgroundHealthSync.cancel()
                            }
                        }

                        // Workout Section
                        VStack(alignment: .leading, spacing: 14) {
                            Text("Workout")
                                .font(.system(size: 15, weight: .semibold))
                                .foregroundColor(AppColors.textSecondary)

                            Button(action: { showingDeleteWorkoutAlert = true }) {
                                HStack(spacing: 14) {
                                    Image(systemName: "figure.run.circle.fill")
                                        .font(.system(size: 24))
                                        .foregroundColor(Color(hex: 0xFFEF4444))

                                    VStack(alignment: .leading, spacing: 3) {
                                        Text("Delete Workout Data")
                                            .font(.system(size: 15, weight: .semibold))
                                            .foregroundColor(Color(hex: 0xFFEF4444))
                                        Text("Clears fitness goals, body metrics & restarts onboarding")
                                            .font(.system(size: 12))
                                            .foregroundColor(AppColors.textSecondary)
                                    }

                                    Spacer()

                                    if isDeletingWorkout {
                                        ProgressView()
                                            .scaleEffect(0.85)
                                    } else {
                                        Image(systemName: "trash")
                                            .font(.system(size: 15, weight: .semibold))
                                            .foregroundColor(Color(hex: 0xFFEF4444))
                                    }
                                }
                                .padding(.vertical, 4)
                            }
                            .buttonStyle(.plain)
                            .disabled(isDeletingWorkout)
                        }
                        .padding(20)
                        .background(Color.white)
                        .clipShape(RoundedRectangle(cornerRadius: 24))
                        .shadow(color: Color.black.opacity(0.03), radius: 6, y: 3)

                        // Legal Section
                        VStack(alignment: .leading, spacing: 14) {
                            Text("Legal")
                                .font(.system(size: 15, weight: .semibold))
                                .foregroundColor(AppColors.textSecondary)

                            if let privacyURL = URL(string: "https://gymsuit.app/privacy") {
                                Link(destination: privacyURL) {
                                    HStack {
                                        Image(systemName: "lock.shield.fill")
                                            .foregroundColor(AppColors.primary)
                                        Text("Privacy Policy")
                                            .font(.system(size: 15, weight: .medium))
                                            .foregroundColor(AppColors.textPrimary)
                                        Spacer()
                                        Image(systemName: "chevron.right")
                                            .font(.system(size: 12, weight: .semibold))
                                            .foregroundColor(AppColors.textSecondary)
                                    }
                                }
                            }
                        }
                        .padding(20)
                        .background(Color.white)
                        .clipShape(RoundedRectangle(cornerRadius: 24))

                        // Logout Button
                        Button(action: { showingLogoutAlert = true }) {
                            HStack {
                                Image(systemName: "arrow.right.square")
                                    .font(.system(size: 16, weight: .semibold))
                                Text("Log Out")
                                    .font(.system(size: 16, weight: .semibold))
                            }
                            .foregroundColor(AppColors.danger)
                            .frame(maxWidth: .infinity)
                            .frame(height: 52)
                            .background(AppColors.danger.opacity(0.1))
                            .clipShape(RoundedRectangle(cornerRadius: 16))
                        }
                        .padding(.top, 10)

                        // Delete Account Button
                        Button(action: { showingDeleteAccountAlert = true }) {
                            HStack {
                                Image(systemName: "trash")
                                    .font(.system(size: 16, weight: .semibold))
                                Text(isDeletingAccount ? "Deleting Account…" : "Delete Account")
                                    .font(.system(size: 16, weight: .semibold))
                            }
                            .foregroundColor(AppColors.danger)
                            .frame(maxWidth: .infinity)
                            .frame(height: 52)
                            .background(AppColors.danger.opacity(0.1))
                            .clipShape(RoundedRectangle(cornerRadius: 16))
                        }
                        .disabled(isDeletingAccount)

                        if let deleteError {
                            Text(deleteError)
                                .font(.system(size: 13))
                                .foregroundColor(AppColors.danger)
                                .multilineTextAlignment(.center)
                        }
                    }
                    .padding(20)
                }
            }
        }
        .alert("Log Out", isPresented: $showingLogoutAlert) {
            Button("Cancel", role: .cancel) {}
            Button("Log Out", role: .destructive) {
                authManager.clear()
                onLogout()
            }
        } message: {
            Text("Are you sure you want to log out of GymSuit?")
        }
        .alert("Delete Account", isPresented: $showingDeleteAccountAlert) {
            Button("Cancel", role: .cancel) {}
            Button("Delete", role: .destructive) {
                Task { await deleteAccount() }
            }
        } message: {
            Text("This permanently deletes your account and your synced health data. This cannot be undone.")
        }
        .alert("Delete Workout Data", isPresented: $showingDeleteWorkoutAlert) {
            Button("Cancel", role: .cancel) {}
            Button("Delete & Restart", role: .destructive) {
                Task { await deleteWorkoutData() }
            }
        } message: {
            Text("This will permanently delete your workout goals, target weight, body metrics, and workout logs from the cloud database. Workout onboarding will restart immediately.")
        }
    }

    private func deleteWorkoutData() async {
        isDeletingWorkout = true
        deleteError = nil
        do {
            try await WorkoutOnboardingApi.shared.deleteOnboardingData()
            await MainActor.run {
                isDeletingWorkout = false
                onRestartWorkoutOnboarding?()
            }
        } catch {
            await MainActor.run {
                isDeletingWorkout = false
                deleteError = error.localizedDescription
            }
        }
    }

    private func deleteAccount() async {
        guard let token = authManager.getAccessToken() else { return }
        isDeletingAccount = true
        deleteError = nil
        do {
            try await MobileAuthApi.shared.deleteAccount(accessToken: token)
            authManager.clear()
            onLogout()
        } catch {
            deleteError = error.localizedDescription
        }
        isDeletingAccount = false
    }
}
