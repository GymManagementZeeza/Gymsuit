import SwiftUI

struct RegisterView: View {
    var onBack: () -> Void
    var onNavigateToLogin: () -> Void
    var onRegisterSuccess: () -> Void
    
    @State private var firstName: String = ""
    @State private var lastName: String = ""
    @State private var email: String = ""
    @State private var phone: String = ""
    @State private var otp: String = ""
    @State private var otpSent: Bool = false
    
    @State private var isLoading: Bool = false
    @State private var errorMessage: String?
    @State private var infoMessage: String?
    
    private let authApi = MobileAuthApi.shared
    private let authManager = AuthManager.shared
    
    var body: some View {
        ZStack {
            AppColors.surface.ignoresSafeArea()
            
            ScrollView {
                VStack(spacing: 0) {
                    // Header Image
                    ZStack(alignment: .topLeading) {
                        Image("light_gym_bg")
                            .resizable()
                            .scaledToFill()
                            .frame(maxWidth: .infinity)
                            .frame(height: 220)
                            .clipped()
                        
                        LinearGradient(
                            colors: [Color.black.opacity(0.4), Color.clear, AppColors.surface],
                            startPoint: .top,
                            endPoint: .bottom
                        )
                        
                        Button(action: onBack) {
                            Image(systemName: "chevron.left")
                                .font(.system(size: 16, weight: .bold))
                                .foregroundColor(.white)
                                .frame(width: 40, height: 40)
                                .background(Color.black.opacity(0.35))
                                .clipShape(Circle())
                        }
                        .padding(.leading, 20)
                        .padding(.top, 50)
                    }
                    
                    VStack(alignment: .leading, spacing: 18) {
                        VStack(alignment: .leading, spacing: 4) {
                            Text("Create Account")
                                .font(.system(size: 28, weight: .bold))
                                .foregroundColor(AppColors.textPrimary)
                            Text("Join GymSuit to track biometrics, workouts, and AI health summaries.")
                                .font(.system(size: 14))
                                .foregroundColor(AppColors.textSecondary)
                        }
                        
                        if let error = errorMessage {
                            Text(error)
                                .font(.system(size: 13, weight: .medium))
                                .foregroundColor(AppColors.danger)
                                .padding()
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .background(AppColors.danger.opacity(0.1))
                                .clipShape(RoundedRectangle(cornerRadius: 12))
                        }
                        
                        if let info = infoMessage {
                            Text(info)
                                .font(.system(size: 13, weight: .medium))
                                .foregroundColor(AppColors.success)
                                .padding()
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .background(AppColors.success.opacity(0.1))
                                .clipShape(RoundedRectangle(cornerRadius: 12))
                        }
                        
                        // First & Last Name
                        HStack(spacing: 12) {
                            VStack(alignment: .leading, spacing: 6) {
                                Text("First Name")
                                    .font(.system(size: 13, weight: .semibold))
                                    .foregroundColor(AppColors.textSecondary)
                                TextField("", text: $firstName, prompt: Text("Alex").foregroundColor(AppColors.textTertiary))
                                    .foregroundColor(AppColors.textPrimary)
                                    .tint(AppColors.primary)
                                    .padding()
                                    .background(Color.white)
                                    .clipShape(RoundedRectangle(cornerRadius: 14))
                                    .overlay(RoundedRectangle(cornerRadius: 14).stroke(AppColors.border, lineWidth: 1))
                            }
                            
                            VStack(alignment: .leading, spacing: 6) {
                                Text("Last Name")
                                    .font(.system(size: 13, weight: .semibold))
                                    .foregroundColor(AppColors.textSecondary)
                                TextField("", text: $lastName, prompt: Text("Smith").foregroundColor(AppColors.textTertiary))
                                    .foregroundColor(AppColors.textPrimary)
                                    .tint(AppColors.primary)
                                    .padding()
                                    .background(Color.white)
                                    .clipShape(RoundedRectangle(cornerRadius: 14))
                                    .overlay(RoundedRectangle(cornerRadius: 14).stroke(AppColors.border, lineWidth: 1))
                            }
                        }
                        
                        // Email
                        VStack(alignment: .leading, spacing: 6) {
                            Text("Email Address")
                                .font(.system(size: 13, weight: .semibold))
                                .foregroundColor(AppColors.textSecondary)
                            TextField("", text: $email, prompt: Text("alex@example.com").foregroundColor(AppColors.textTertiary))
                                .foregroundColor(AppColors.textPrimary)
                                .tint(AppColors.primary)
                                .autocapitalization(.none)
                                .keyboardType(.emailAddress)
                                .padding()
                                .background(Color.white)
                                .clipShape(RoundedRectangle(cornerRadius: 14))
                                .overlay(RoundedRectangle(cornerRadius: 14).stroke(AppColors.border, lineWidth: 1))
                        }
                        
                        // Phone
                        VStack(alignment: .leading, spacing: 6) {
                            Text("Phone Number")
                                .font(.system(size: 13, weight: .semibold))
                                .foregroundColor(AppColors.textSecondary)
                            TextField("", text: $phone, prompt: Text("+1 234 567 8900").foregroundColor(AppColors.textTertiary))
                                .foregroundColor(AppColors.textPrimary)
                                .tint(AppColors.primary)
                                .keyboardType(.phonePad)
                                .padding()
                                .background(Color.white)
                                .clipShape(RoundedRectangle(cornerRadius: 14))
                                .overlay(RoundedRectangle(cornerRadius: 14).stroke(AppColors.border, lineWidth: 1))
                        }
                        
                        if !otpSent {
                            Button(action: sendRegisterOtp) {
                                HStack {
                                    if isLoading {
                                        ProgressView().progressViewStyle(CircularProgressViewStyle(tint: isFormValid ? .white : AppColors.textTertiary))
                                    } else {
                                        Text("Continue & Get Code")
                                            .font(.system(size: 16, weight: .semibold))
                                    }
                                }
                                .foregroundColor(isFormValid ? .white : AppColors.textTertiary)
                                .frame(maxWidth: .infinity)
                                .frame(height: 54)
                                .background(isFormValid ? AppColors.primary : AppColors.divider)
                                .clipShape(RoundedRectangle(cornerRadius: 16))
                            }
                            .disabled(!isFormValid || isLoading)
                        } else {
                            VStack(alignment: .leading, spacing: 6) {
                                Text("6-Digit Code")
                                    .font(.system(size: 13, weight: .semibold))
                                    .foregroundColor(AppColors.textSecondary)
                                TextField("", text: $otp, prompt: Text("123456").foregroundColor(AppColors.textTertiary))
                                    .foregroundColor(AppColors.textPrimary)
                                    .tint(AppColors.primary)
                                    .keyboardType(.numberPad)
                                    .padding()
                                    .background(Color.white)
                                    .clipShape(RoundedRectangle(cornerRadius: 14))
                                    .overlay(RoundedRectangle(cornerRadius: 14).stroke(AppColors.border, lineWidth: 1))
                            }
                            
                            Button(action: completeRegistration) {
                                HStack {
                                    if isLoading {
                                        ProgressView().progressViewStyle(CircularProgressViewStyle(tint: otp.count < 4 ? AppColors.textTertiary : .white))
                                    } else {
                                        Text("Complete Registration")
                                            .font(.system(size: 16, weight: .semibold))
                                    }
                                }
                                .foregroundColor(otp.count < 4 ? AppColors.textTertiary : .white)
                                .frame(maxWidth: .infinity)
                                .frame(height: 54)
                                .background(otp.count < 4 ? AppColors.divider : AppColors.primary)
                                .clipShape(RoundedRectangle(cornerRadius: 16))
                            }
                            .disabled(otp.count < 4 || isLoading)
                        }
                        
                        HStack {
                            Spacer()
                            Text("Already have an account?")
                                .font(.system(size: 14))
                                .foregroundColor(AppColors.textSecondary)
                            Button(action: onNavigateToLogin) {
                                Text("Sign In")
                                    .font(.system(size: 14, weight: .bold))
                                    .foregroundColor(AppColors.primary)
                            }
                            Spacer()
                        }
                        .padding(.top, 10)
                    }
                    .padding(24)
                }
            }
            .ignoresSafeArea(edges: .top)
        }
    }
    
    private var isFormValid: Bool {
        !firstName.isEmpty && !lastName.isEmpty && !email.isEmpty
    }
    
    private func sendRegisterOtp() {
        errorMessage = nil
        infoMessage = nil
        isLoading = true
        
        Task {
            do {
                let msg = try await authApi.sendOtp(email: email, mode: "register")
                await MainActor.run {
                    self.isLoading = false
                    self.otpSent = true
                    self.infoMessage = msg
                }
            } catch {
                await MainActor.run {
                    self.isLoading = false
                    self.errorMessage = error.localizedDescription
                }
            }
        }
    }
    
    private func completeRegistration() {
        errorMessage = nil
        infoMessage = nil
        isLoading = true
        
        Task {
            do {
                let session = try await authApi.registerWithOtp(
                    firstName: firstName,
                    lastName: lastName,
                    email: email,
                    phone: phone,
                    otp: otp
                )
                await MainActor.run {
                    self.isLoading = false
                    self.authManager.saveSession(session)
                    self.onRegisterSuccess()
                }
            } catch {
                await MainActor.run {
                    self.isLoading = false
                    self.errorMessage = error.localizedDescription
                }
            }
        }
    }
}
