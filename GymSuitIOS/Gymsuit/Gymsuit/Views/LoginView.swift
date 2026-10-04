import SwiftUI

struct LoginView: View {
    var onBack: () -> Void
    var onNavigateToRegister: () -> Void
    var onLoginSuccess: () -> Void
    
    @State private var email: String = ""
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
                    // Header Image with Gradient
                    ZStack(alignment: .topLeading) {
                        Image("light_gym_bg")
                            .resizable()
                            .scaledToFill()
                            .frame(maxWidth: .infinity)
                            .frame(height: 240)
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
                    
                    VStack(alignment: .leading, spacing: 20) {
                        VStack(alignment: .leading, spacing: 6) {
                            Text("Welcome Back")
                                .font(.system(size: 28, weight: .bold))
                                .foregroundColor(AppColors.textPrimary)
                            Text("Sign in using your email address and one-time verification passcode.")
                                .font(.system(size: 14))
                                .foregroundColor(AppColors.textSecondary)
                        }
                        
                        if let error = errorMessage {
                            HStack {
                                Image(systemName: "exclamationmark.triangle.fill")
                                    .foregroundColor(AppColors.danger)
                                Text(error)
                                    .font(.system(size: 13, weight: .medium))
                                    .foregroundColor(AppColors.danger)
                            }
                            .padding()
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .background(AppColors.danger.opacity(0.1))
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                        }
                        
                        if let info = infoMessage {
                            HStack {
                                Image(systemName: "checkmark.circle.fill")
                                    .foregroundColor(AppColors.success)
                                Text(info)
                                    .font(.system(size: 13, weight: .medium))
                                    .foregroundColor(AppColors.success)
                            }
                            .padding()
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .background(AppColors.success.opacity(0.1))
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                        }
                        
                        // Email Input
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Email Address")
                                .font(.system(size: 13, weight: .semibold))
                                .foregroundColor(AppColors.textSecondary)
                            
                            HStack {
                                Image(systemName: "envelope")
                                    .foregroundColor(AppColors.textTertiary)
                                    .frame(width: 20)
                                TextField("", text: $email, prompt: Text("alex@example.com").foregroundColor(AppColors.textTertiary))
                                    .foregroundColor(AppColors.textPrimary)
                                    .tint(AppColors.primary)
                                    .autocapitalization(.none)
                                    .disableAutocorrection(true)
                                    .keyboardType(.emailAddress)
                            }
                            .padding()
                            .background(Color.white)
                            .clipShape(RoundedRectangle(cornerRadius: 16))
                            .overlay(RoundedRectangle(cornerRadius: 16).stroke(AppColors.border, lineWidth: 1))
                        }
                        
                        // Send OTP or OTP field
                        if !otpSent {
                            Button(action: sendOtp) {
                                HStack {
                                    if isLoading {
                                        ProgressView()
                                            .progressViewStyle(CircularProgressViewStyle(tint: email.isEmpty ? AppColors.textTertiary : .white))
                                    } else {
                                        Text("Send Verification Code")
                                            .font(.system(size: 16, weight: .semibold))
                                    }
                                }
                                .foregroundColor(email.isEmpty ? AppColors.textTertiary : .white)
                                .frame(maxWidth: .infinity)
                                .frame(height: 54)
                                .background(email.isEmpty ? AppColors.divider : AppColors.primary)
                                .clipShape(RoundedRectangle(cornerRadius: 16))
                            }
                            .disabled(email.isEmpty || isLoading)
                        } else {
                            // OTP input
                            VStack(alignment: .leading, spacing: 8) {
                                Text("6-Digit Code")
                                    .font(.system(size: 13, weight: .semibold))
                                    .foregroundColor(AppColors.textSecondary)
                                
                                HStack {
                                    Image(systemName: "key")
                                        .foregroundColor(AppColors.textTertiary)
                                        .frame(width: 20)
                                    TextField("", text: $otp, prompt: Text("123456").foregroundColor(AppColors.textTertiary))
                                        .foregroundColor(AppColors.textPrimary)
                                        .tint(AppColors.primary)
                                        .keyboardType(.numberPad)
                                }
                                .padding()
                                .background(Color.white)
                                .clipShape(RoundedRectangle(cornerRadius: 16))
                                .overlay(RoundedRectangle(cornerRadius: 16).stroke(AppColors.border, lineWidth: 1))
                            }
                            
                            Button(action: verifyOtp) {
                                HStack {
                                    if isLoading {
                                        ProgressView()
                                            .progressViewStyle(CircularProgressViewStyle(tint: otp.count < 4 ? AppColors.textTertiary : .white))
                                    } else {
                                        Text("Verify & Continue")
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
                            
                            Button(action: sendOtp) {
                                Text("Resend Code")
                                    .font(.system(size: 14, weight: .medium))
                                    .foregroundColor(AppColors.primary)
                                    .frame(maxWidth: .infinity)
                            }
                        }
                        
                        // Footer: Navigate to register
                        HStack {
                            Spacer()
                            Text("Don't have an account?")
                                .font(.system(size: 14))
                                .foregroundColor(AppColors.textSecondary)
                            Button(action: onNavigateToRegister) {
                                Text("Register")
                                    .font(.system(size: 14, weight: .bold))
                                    .foregroundColor(AppColors.primary)
                            }
                            Spacer()
                        }
                        .padding(.top, 16)
                    }
                    .padding(24)
                }
            }
            .ignoresSafeArea(edges: .top)
        }
    }
    
    private func sendOtp() {
        guard !email.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return }
        errorMessage = nil
        infoMessage = nil
        isLoading = true
        
        Task {
            do {
                let msg = try await authApi.sendOtp(email: email, mode: "login")
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
    
    private func verifyOtp() {
        guard !otp.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return }
        errorMessage = nil
        infoMessage = nil
        isLoading = true
        
        Task {
            do {
                let session = try await authApi.verifyLoginOtp(email: email, otp: otp)
                await MainActor.run {
                    self.isLoading = false
                    self.authManager.saveSession(session)
                    self.onLoginSuccess()
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
