import SwiftUI

/// Bottom sheet shared by the food and workout pages: describe what you did/ate in
/// plain words and the backend AI logs it. `submit` returns nil on success or an
/// error message to show under the text box.
struct AiLogSheet: View {
    let title: String
    let prompt: String
    let placeholder: String
    let fieldLabel: String
    let buttonTitle: String
    let submit: (String) async -> String?
    let onDone: () -> Void

    @State private var text = ""
    @State private var loading = false
    @State private var error: String?
    @FocusState private var focused: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(title)
                .font(.app(size: 22, weight: .bold))
                .foregroundColor(AppColors.textPrimary)
            Text(prompt)
                .font(.app(size: 14))
                .foregroundColor(AppColors.textSecondary)

            ZStack(alignment: .topLeading) {
                TextEditor(text: $text)
                    .focused($focused)
                    .disabled(loading)
                    .scrollContentBackground(.hidden)
                    .padding(8)
                    .frame(minHeight: 130, maxHeight: 220)
                    .background(AppColors.primaryLight)
                    .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                    .overlay(
                        RoundedRectangle(cornerRadius: 14, style: .continuous)
                            .stroke(error == nil ? Color.clear : Color.red, lineWidth: 1)
                    )
                    .accessibilityLabel(fieldLabel)
                if text.isEmpty {
                    Text(placeholder)
                        .font(.app(size: 15))
                        .foregroundColor(AppColors.textSecondary.opacity(0.7))
                        .padding(.horizontal, 14)
                        .padding(.vertical, 16)
                        .allowsHitTesting(false)
                }
            }

            if let error {
                Text(error)
                    .font(.app(size: 13))
                    .foregroundColor(.red)
            }

            Button {
                focused = false
                loading = true
                error = nil
                Task {
                    let message = await submit(text.trimmingCharacters(in: .whitespacesAndNewlines))
                    loading = false
                    if let message { error = message } else { onDone() }
                }
            } label: {
                HStack(spacing: 8) {
                    if loading {
                        ProgressView().tint(.white)
                        Text("Logging…")
                    } else {
                        Image(systemName: "sparkles")
                        Text(buttonTitle)
                    }
                }
                .font(.app(size: 16, weight: .semibold))
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .frame(height: 50)
                .background(AppColors.primary.opacity(text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || loading ? 0.4 : 1))
                .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
            }
            .disabled(text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || loading)
        }
        .padding(.horizontal, 24)
        .padding(.top, 24)
        .padding(.bottom, 16)
        .presentationDetents([.medium, .large])
        .presentationDragIndicator(.visible)
        .interactiveDismissDisabled(loading)
    }
}

/// Floating "Log with AI" pill used on the food and workout pages.
struct AiLogButton: View {
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 8) {
                Image(systemName: "sparkles")
                Text("Log with AI")
            }
            .font(.app(size: 15, weight: .semibold))
            .foregroundColor(.white)
            .padding(.horizontal, 18)
            .frame(height: 52)
            .background(AppColors.primary)
            .clipShape(Capsule())
            .shadow(color: Color.black.opacity(0.18), radius: 8, y: 4)
        }
        .accessibilityLabel("Log with AI")
    }
}
