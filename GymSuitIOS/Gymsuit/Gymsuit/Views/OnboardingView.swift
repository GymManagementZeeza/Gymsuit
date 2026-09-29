import SwiftUI

struct OnboardingCarouselCard: Identifiable {
    let id = UUID()
    let imageName: String
    let tag: String
    let headline: String
    let description: String
}

struct OnboardingView: View {
    var onSkip: () -> Void
    var onContinue: () -> Void
    
    @State private var currentIndex = 0
    
    private let items: [OnboardingCarouselCard] = [
        OnboardingCarouselCard(
            imageName: "fitness_gym",
            tag: "Strength • Training",
            headline: "Elevate Your Gym Journey",
            description: "Customized progressive overload workout plans built to sculpt and strengthen your body."
        ),
        OnboardingCarouselCard(
            imageName: "fitness_workout",
            tag: "Cardio • Endurance",
            headline: "Push Beyond Your Limits",
            description: "High-energy training programs and real-time biometric stats to boost stamina and burn calories."
        ),
        OnboardingCarouselCard(
            imageName: "fitness_yoga",
            tag: "Mobility • Balance",
            headline: "Restore & Recover Smarter",
            description: "Guided recovery sessions, flexibility routines, and mindful wellness tracking for longevity."
        ),
        OnboardingCarouselCard(
            imageName: "fitness_cardio",
            tag: "HIIT • Power",
            headline: "High Intensity Conditioning",
            description: "Ignite your metabolism and maximize your athletic output with high-intensity circuit training."
        ),
        OnboardingCarouselCard(
            imageName: "fitness_stretching",
            tag: "Flexibility • Mind",
            headline: "Stay Centered & Flexible",
            description: "Targeted stretching routines to prevent injuries, release tension, and prime your body."
        )
    ]
    
    var body: some View {
        ZStack {
            AppColors.surface.ignoresSafeArea()
            
            VStack(spacing: 20) {
                // Top Header: Skip Button
                HStack {
                    Spacer()
                    Button(action: onSkip) {
                        Text("Skip")
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundColor(AppColors.textSecondary)
                            .padding(.horizontal, 16)
                            .padding(.vertical, 8)
                            .background(Color.white.opacity(0.8))
                            .clipShape(Capsule())
                    }
                }
                .padding(.horizontal, 24)
                .padding(.top, 12)
                
                // Carousel Cards
                TabView(selection: $currentIndex) {
                    ForEach(0..<items.count, id: \.self) { idx in
                        let item = items[idx]
                        VStack(spacing: 0) {
                            ZStack(alignment: .bottomLeading) {
                                Image(item.imageName)
                                    .resizable()
                                    .scaledToFill()
                                    .frame(maxWidth: .infinity)
                                    .frame(height: 380)
                                    .clipped()
                                
                                LinearGradient(
                                    colors: [Color.clear, Color.black.opacity(0.75)],
                                    startPoint: .center,
                                    endPoint: .bottom
                                )
                                
                                Text(item.tag)
                                    .font(.system(size: 12, weight: .bold))
                                    .textCase(.uppercase)
                                    .padding(.horizontal, 14)
                                    .padding(.vertical, 6)
                                    .background(AppColors.primary)
                                    .foregroundColor(.white)
                                    .clipShape(Capsule())
                                    .padding(20)
                            }
                            .clipShape(RoundedRectangle(cornerRadius: 32, style: .continuous))
                            .padding(.horizontal, 20)
                            
                            VStack(alignment: .leading, spacing: 10) {
                                Text(item.headline)
                                    .font(.system(size: 24, weight: .bold))
                                    .foregroundColor(AppColors.textPrimary)
                                    .multilineTextAlignment(.leading)
                                
                                Text(item.description)
                                    .font(.system(size: 15))
                                    .foregroundColor(AppColors.textSecondary)
                                    .lineSpacing(4)
                                    .multilineTextAlignment(.leading)
                            }
                            .padding(.horizontal, 24)
                            .padding(.top, 24)
                            
                            Spacer()
                        }
                        .tag(idx)
                    }
                }
                .tabViewStyle(PageTabViewStyle(indexDisplayMode: .never))
                
                // Bottom Section: Dots + Next Button
                HStack(alignment: .center) {
                    // Page indicator
                    HStack(spacing: 6) {
                        ForEach(0..<items.count, id: \.self) { idx in
                            Capsule()
                                .fill(idx == currentIndex ? AppColors.primary : AppColors.divider)
                                .frame(width: idx == currentIndex ? 24 : 8, height: 8)
                                .animation(.spring(), value: currentIndex)
                        }
                    }
                    
                    Spacer()
                    
                    Button(action: {
                        if currentIndex < items.count - 1 {
                            withAnimation { currentIndex += 1 }
                        } else {
                            onContinue()
                        }
                    }) {
                        HStack(spacing: 8) {
                            Text(currentIndex == items.count - 1 ? "Get Started" : "Continue")
                                .font(.system(size: 16, weight: .semibold))
                            Image(systemName: "arrow.right")
                                .font(.system(size: 14, weight: .bold))
                        }
                        .foregroundColor(.white)
                        .padding(.horizontal, 24)
                        .padding(.vertical, 14)
                        .background(AppColors.primary)
                        .clipShape(Capsule())
                    }
                }
                .padding(.horizontal, 24)
                .padding(.bottom, 24)
            }
        }
    }
}
