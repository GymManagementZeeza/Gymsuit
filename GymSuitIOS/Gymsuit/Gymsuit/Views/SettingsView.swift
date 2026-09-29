import SwiftUI

enum AppearanceMode: String, CaseIterable {
    case system = "System"
    case forYou = "For You"
    case dark = "Dark"
    case light = "Light"
    
    var iconName: String {
        switch self {
        case .system: return "gearshape"
        case .forYou: return "star"
        case .dark: return "moon.fill"
        case .light: return "sun.max.fill"
        }
    }
}

struct SettingsView: View {
    var onBack: () -> Void
    var onLogout: () -> Void
    
    @State private var selectedAppearance: AppearanceMode = .light
    @State private var notificationsEnabled: Bool = true
    @State private var healthSyncEnabled: Bool = true
    @State private var showingLogoutAlert: Bool = false
    
    @ObservedObject private var authManager = AuthManager.shared
    
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
                                Text("\(authManager.currentSession?.firstName ?? "Athlete") \(authManager.currentSession?.lastName ?? "")")
                                    .font(.system(size: 18, weight: .bold))
                                    .foregroundColor(AppColors.textPrimary)
                                Text(authManager.currentSession?.email ?? "member@gymsuit.app")
                                    .font(.system(size: 13))
                                    .foregroundColor(AppColors.textSecondary)
                                Text(authManager.currentSession?.gymName ?? "GymSuit Elite Club")
                                    .font(.system(size: 12, weight: .medium))
                                    .foregroundColor(AppColors.primary)
                            }
                            Spacer()
                        }
                        .padding(20)
                        .background(Color.white)
                        .clipShape(RoundedRectangle(cornerRadius: 24))
                        .shadow(color: Color.black.opacity(0.03), radius: 6, y: 3)
                        
                        // Appearance Segmented
                        VStack(alignment: .leading, spacing: 14) {
                            Text("Appearance")
                                .font(.system(size: 15, weight: .semibold))
                                .foregroundColor(AppColors.textSecondary)
                            
                            HStack(spacing: 8) {
                                ForEach(AppearanceMode.allCases, id: \.self) { mode in
                                    Button(action: { selectedAppearance = mode }) {
                                        VStack(spacing: 6) {
                                            Image(systemName: mode.iconName)
                                                .font(.system(size: 16))
                                            Text(mode.rawValue)
                                                .font(.system(size: 12, weight: .medium))
                                        }
                                        .frame(maxWidth: .infinity)
                                        .padding(.vertical, 12)
                                        .background(selectedAppearance == mode ? AppColors.primary : Color.white)
                                        .foregroundColor(selectedAppearance == mode ? .white : AppColors.textPrimary)
                                        .clipShape(RoundedRectangle(cornerRadius: 16))
                                        .overlay(RoundedRectangle(cornerRadius: 16).stroke(AppColors.border, lineWidth: 1))
                                    }
                                }
                            }
                        }
                        .padding(20)
                        .background(Color.white)
                        .clipShape(RoundedRectangle(cornerRadius: 24))
                        
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
                            
                            Divider()
                            
                            Toggle(isOn: $notificationsEnabled) {
                                HStack {
                                    Image(systemName: "bell.fill")
                                        .foregroundColor(AppColors.warning)
                                    Text("Daily Wellness Briefing")
                                        .font(.system(size: 15, weight: .medium))
                                        .foregroundColor(AppColors.textPrimary)
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
    }
}
