import SwiftUI

enum AppScreen {
    case onboarding
    case login
    case register
    case healthPermission
    case dashboard
    case sleepDetail(Date)
    case heartRateDetail(Date)
    case caloriesDetail(Date)
}

struct ContentView: View {
    @State private var currentScreen: AppScreen = .onboarding
    @ObservedObject private var authManager = AuthManager.shared
    
    var body: some View {
        Group {
            switch currentScreen {
            case .onboarding:
                OnboardingView(
                    onSkip: { currentScreen = .login },
                    onContinue: { currentScreen = .login }
                )
                
            case .login:
                LoginView(
                    onBack: { currentScreen = .onboarding },
                    onNavigateToRegister: { currentScreen = .register },
                    onLoginSuccess: { navigateAfterLogin() }
                )
                
            case .register:
                RegisterView(
                    onBack: { currentScreen = .login },
                    onNavigateToLogin: { currentScreen = .login },
                    onRegisterSuccess: { navigateAfterLogin() }
                )
                
            case .healthPermission:
                HealthPermissionView(
                    onContinue: { currentScreen = .dashboard },
                    onSkip: { currentScreen = .dashboard }
                )
                
            case .dashboard:
                DashboardView(
                    onLogout: {
                        currentScreen = .login
                    },
                    onNavigateToSleepDetail: { date in
                        currentScreen = .sleepDetail(date)
                    },
                    onNavigateToHeartRateDetail: { date in
                        currentScreen = .heartRateDetail(date)
                    },
                    onNavigateToCaloriesDetail: { date in
                        currentScreen = .caloriesDetail(date)
                    }
                )
                
            case .sleepDetail(let date):
                SleepDetailView(date: date, onBack: { currentScreen = .dashboard })
                
            case .heartRateDetail(let date):
                HeartRateDetailView(date: date, onBack: { currentScreen = .dashboard })
                
            case .caloriesDetail(let date):
                CaloriesDetailView(date: date, onBack: { currentScreen = .dashboard })
            }
        }
        .onAppear {
            if authManager.isLoggedIn {
                currentScreen = .dashboard
            } else {
                currentScreen = .onboarding
            }
        }
    }
    
    private func navigateAfterLogin() {
        if HealthKitManager.shared.isAuthorized {
            currentScreen = .dashboard
        } else {
            currentScreen = .healthPermission
        }
    }
}
