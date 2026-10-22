import SwiftUI
import Combine

enum WorkoutSetupStep {
    case goal
    case height
    case weight
    case targetWeight
    case birthYear
    case workoutFrequency
    case projection
}

struct WorkoutsView: View {
    var onBackToHome: (() -> Void)? = nil

    // First-time Goal & Body Setup state
    @AppStorage("workout_goal_setup_completed") private var isGoalSetupCompleted: Bool = false
    @AppStorage("workout_selected_goal") private var selectedGoalRaw: String = WorkoutGoal.loseWeight.rawValue
    @AppStorage("workout_user_height_cm") private var userHeightCm: Double = 172.72 // 5 ft 8 in
    @AppStorage("workout_user_weight_kg") private var userWeightKg: Double = 74.526 // ~164.3 lbs
    @AppStorage("workout_user_target_weight_kg") private var userTargetWeightKg: Double = 71.0
    @AppStorage("workout_user_birth_year") private var userBirthYear: Int = 2003
    @AppStorage("workout_user_days_per_week") private var userWorkoutDaysPerWeek: Int = 2
    @State private var setupStep: WorkoutSetupStep = .goal
    @State private var selectedDate: Date = Date()

    private var currentGoal: WorkoutGoal {
        WorkoutGoal(rawValue: selectedGoalRaw) ?? .loseWeight
    }

    var body: some View {
        Group {
            if !isGoalSetupCompleted {
                switch setupStep {
                case .goal:
                    WorkoutGoalSelectionView(
                        selectedGoal: Binding(
                            get: { currentGoal },
                            set: { selectedGoalRaw = $0.rawValue }
                        ),
                        onBack: {
                            onBackToHome?()
                        },
                        onNext: {
                            withAnimation(.spring(response: 0.35, dampingFraction: 0.82)) {
                                setupStep = .height
                            }
                        }
                    )
                    .transition(.asymmetric(
                        insertion: .move(edge: .trailing).combined(with: .opacity),
                        removal: .move(edge: .leading).combined(with: .opacity)
                    ))

                case .height:
                    WorkoutBodyHeightView(
                        heightCm: $userHeightCm,
                        onBack: {
                            withAnimation(.spring(response: 0.35, dampingFraction: 0.82)) {
                                setupStep = .goal
                            }
                        },
                        onNext: {
                            withAnimation(.spring(response: 0.35, dampingFraction: 0.82)) {
                                setupStep = .weight
                            }
                        }
                    )
                    .transition(.asymmetric(
                        insertion: .move(edge: .trailing).combined(with: .opacity),
                        removal: .move(edge: .leading).combined(with: .opacity)
                    ))

                case .weight:
                    WorkoutBodyWeightView(
                        weightKg: $userWeightKg,
                        heightCm: userHeightCm,
                        onBack: {
                            withAnimation(.spring(response: 0.35, dampingFraction: 0.82)) {
                                setupStep = .height
                            }
                        },
                        onNext: {
                            withAnimation(.spring(response: 0.35, dampingFraction: 0.82)) {
                                setupStep = .targetWeight
                            }
                        }
                    )
                    .transition(.asymmetric(
                        insertion: .move(edge: .trailing).combined(with: .opacity),
                        removal: .move(edge: .leading).combined(with: .opacity)
                    ))

                case .targetWeight:
                    WorkoutTargetWeightView(
                        goal: currentGoal,
                        targetWeightKg: $userTargetWeightKg,
                        currentWeightKg: userWeightKg,
                        onBack: {
                            withAnimation(.spring(response: 0.35, dampingFraction: 0.82)) {
                                setupStep = .weight
                            }
                        },
                        onNext: {
                            withAnimation(.spring(response: 0.35, dampingFraction: 0.82)) {
                                setupStep = .birthYear
                            }
                        }
                    )
                    .transition(.asymmetric(
                        insertion: .move(edge: .trailing).combined(with: .opacity),
                        removal: .move(edge: .leading).combined(with: .opacity)
                    ))

                case .birthYear:
                    WorkoutBirthYearView(
                        birthYear: $userBirthYear,
                        onBack: {
                            withAnimation(.spring(response: 0.35, dampingFraction: 0.82)) {
                                setupStep = .targetWeight
                            }
                        },
                        onNext: {
                            withAnimation(.spring(response: 0.35, dampingFraction: 0.82)) {
                                setupStep = .workoutFrequency
                            }
                        }
                    )
                    .transition(.asymmetric(
                        insertion: .move(edge: .trailing).combined(with: .opacity),
                        removal: .move(edge: .leading).combined(with: .opacity)
                    ))

                case .workoutFrequency:
                    WorkoutFrequencyView(
                        workoutDaysPerWeek: $userWorkoutDaysPerWeek,
                        onBack: {
                            withAnimation(.spring(response: 0.35, dampingFraction: 0.82)) {
                                setupStep = .birthYear
                            }
                        },
                        onNext: {
                            withAnimation(.spring(response: 0.35, dampingFraction: 0.82)) {
                                setupStep = .projection
                            }
                        }
                    )
                    .transition(.asymmetric(
                        insertion: .move(edge: .trailing).combined(with: .opacity),
                        removal: .move(edge: .leading).combined(with: .opacity)
                    ))

                case .projection:
                    WorkoutProjectionGraphView(
                        goal: currentGoal,
                        currentWeightKg: userWeightKg,
                        targetWeightKg: userTargetWeightKg,
                        onBack: {
                            withAnimation(.spring(response: 0.35, dampingFraction: 0.82)) {
                                setupStep = .workoutFrequency
                            }
                        },
                        onFinish: {
                            withAnimation(.spring(response: 0.35, dampingFraction: 0.82)) {
                                isGoalSetupCompleted = true
                            }
                        },
                        onFinishWithTargetDate: { calculatedTargetDate in
                            syncOnboardingToBackend(targetDate: calculatedTargetDate)
                        }
                    )
                    .transition(.asymmetric(
                        insertion: .move(edge: .trailing).combined(with: .opacity),
                        removal: .move(edge: .leading).combined(with: .opacity)
                    ))
                }
            } else {
                // Post-onboarding main workout page: blank page with only the back button
                VStack(alignment: .leading, spacing: 0) {
                    HStack {
                        if let onBackToHome {
                            Button(action: onBackToHome) {
                                Image(systemName: "chevron.left")
                                    .font(.system(size: 16, weight: .bold))
                                    .foregroundColor(AppColors.textPrimary)
                                    .frame(width: 44, height: 44)
                                    .background(Color.white)
                                    .clipShape(Circle())
                                    .shadow(color: Color.black.opacity(0.06), radius: 4, y: 2)
                            }
                        }

                        Spacer()
                    }
                    .padding(.horizontal, 20)
                    .padding(.top, 10)
                    .padding(.bottom, 8)

                    ScrollView(showsIndicators: false) {
                        VStack(spacing: 20) {
                            // First Card: Week Calendar with Checkmark & Day Streak / Personal Best
                            WorkoutStreakCardView(selectedDate: $selectedDate)

                            // Second Card: Selected Day's Workout Count, Kcal burned, and Minutes
                            WorkoutDaySummaryCardView(selectedDate: selectedDate)

                            // Third Section: Body Focus Pills and GIF Grid with Shimmer Loading
                            WorkoutBodyFocusSectionView()
                                .padding(.top, 4)

                            // Fourth Card: Progress Projection Graph & Record Weight Option
                            WorkoutProgressProjectionCardView()
                                .padding(.top, 4)
                        }
                        .padding(.horizontal, 20)
                        .padding(.top, 8)
                        .padding(.bottom, 24)
                    }
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background(Color(hex: 0xFFFAFBFD).ignoresSafeArea())
            }
        }
        .background(Color(hex: 0xFFFAFBFD))
        .onChange(of: isGoalSetupCompleted) { _, completed in
            if !completed {
                setupStep = .goal
            }
        }
        .onAppear {
            if !isGoalSetupCompleted {
                setupStep = .goal
            }
        }
        .task {
            await WorkoutStore.shared.fetchLatestExercisesFromBackend()
        }
    }

    private func syncOnboardingToBackend(targetDate: Date) {
        Task {
            _ = try? await WorkoutOnboardingApi.shared.syncOnboarding(
                goal: selectedGoalRaw,
                heightCm: userHeightCm,
                weightKg: userWeightKg,
                targetWeightKg: userTargetWeightKg,
                birthYear: userBirthYear,
                workoutDaysPerWeek: userWorkoutDaysPerWeek,
                targetDate: targetDate
            )
        }
    }
}
