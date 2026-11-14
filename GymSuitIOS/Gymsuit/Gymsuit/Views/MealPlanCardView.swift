import SwiftUI

/// Top card on the Food tab: the meal plan the member's nutritionist sent.
/// Renders nothing until a plan has been sent. Tap the header to collapse or expand it.
struct MealPlanCardView: View {
    let plan: MealPlan
    @State private var expanded = true

    private struct Meal: Identifiable {
        let id: String
        let symbol: String
        let text: String
    }

    private var meals: [Meal] {
        [
            ("Breakfast", "cup.and.saucer.fill", plan.breakfast),
            ("Lunch", "fork.knife", plan.lunch),
            ("Evening snack", "carrot.fill", plan.eveningSnack),
            ("Dinner", "takeoutbag.and.cup.and.straw.fill", plan.dinner)
        ].compactMap { label, symbol, text in
            guard let text, !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return nil }
            return Meal(id: label, symbol: symbol, text: text)
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Button {
                withAnimation(.spring(response: 0.4, dampingFraction: 0.85)) { expanded.toggle() }
            } label: {
                HStack(spacing: 10) {
                    Image(systemName: "menucard.fill")
                        .font(.system(size: 20, weight: .semibold))
                        .foregroundColor(AppColors.success)
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Your meal plan · \(dayLabel)")
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(AppColors.textPrimary)
                        Text("From \(plan.nutritionistName)")
                            .font(.system(size: 12))
                            .foregroundColor(AppColors.textSecondary)
                    }
                    Spacer()
                    Image(systemName: "chevron.down")
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundColor(AppColors.textSecondary)
                        .rotationEffect(.degrees(expanded ? 180 : 0))
                }
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(expanded ? "Collapse meal plan" : "Expand meal plan")

            if expanded {
                VStack(alignment: .leading, spacing: 10) {
                    ForEach(meals) { meal in
                        HStack(alignment: .top, spacing: 12) {
                            Image(systemName: meal.symbol)
                                .font(.system(size: 14, weight: .semibold))
                                .foregroundColor(AppColors.success)
                                .frame(width: 32, height: 32)
                                .background(AppColors.success.opacity(0.12))
                                .clipShape(RoundedRectangle(cornerRadius: 10))
                            VStack(alignment: .leading, spacing: 2) {
                                Text(meal.id.uppercased())
                                    .font(.system(size: 10, weight: .bold))
                                    .tracking(0.8)
                                    .foregroundColor(AppColors.textSecondary)
                                Text(meal.text)
                                    .font(.system(size: 14))
                                    .foregroundColor(AppColors.textPrimary)
                                    .fixedSize(horizontal: false, vertical: true)
                            }
                        }
                    }
                    if let note = plan.note, !note.isEmpty {
                        Text(note)
                            .font(.system(size: 13))
                            .foregroundColor(AppColors.textSecondary)
                            .padding(.leading, 10)
                            .overlay(alignment: .leading) {
                                Rectangle().fill(AppColors.success).frame(width: 2)
                            }
                    }
                }
                .transition(.opacity.combined(with: .move(edge: .top)))
            }
        }
        .padding(16)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 20))
        .shadow(color: Color.black.opacity(0.06), radius: 4, y: 2)
    }

    private var dayLabel: String {
        let parser = DateFormatter()
        parser.calendar = Calendar(identifier: .gregorian)
        parser.locale = Locale(identifier: "en_US_POSIX")
        parser.dateFormat = "yyyy-MM-dd"
        guard let date = parser.date(from: plan.date) else { return plan.date }
        if Calendar.current.isDateInToday(date) { return "Today" }
        if Calendar.current.isDateInYesterday(date) { return "Yesterday" }
        return date.formatted(.dateTime.day().month(.abbreviated))
    }
}
