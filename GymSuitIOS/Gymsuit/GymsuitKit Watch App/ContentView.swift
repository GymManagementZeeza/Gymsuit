//
//  ContentView.swift
//  GymsuitKit Watch App
//

import SwiftUI

struct ContentView: View {
    @StateObject private var metrics = WatchMetricsManager.shared

    private var stepProgress: Double {
        guard metrics.stepGoal > 0 else { return 0.0 }
        return min(1.0, Double(metrics.steps) / Double(metrics.stepGoal))
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 12) {
                // Header
                HStack {
                    HStack(spacing: 4) {
                        Image(systemName: "figure.walk")
                            .font(.system(size: 13, weight: .bold))
                            .foregroundColor(.blue)
                        Text("GymSuit")
                            .font(.system(size: 14, weight: .bold, design: .rounded))
                            .foregroundColor(.white)
                    }

                    Spacer()

                    Button(action: {
                        metrics.requestSync()
                    }) {
                        Image(systemName: "arrow.clockwise")
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundColor(.secondary)
                            .rotationEffect(.degrees(metrics.isSyncing ? 360 : 0))
                            .animation(
                                metrics.isSyncing
                                    ? Animation.linear(duration: 1).repeatForever(autoreverses: false)
                                    : .default,
                                value: metrics.isSyncing
                            )
                    }
                    .buttonStyle(PlainButtonStyle())
                }
                .padding(.horizontal, 4)

                // Central Steps Ring Card
                ZStack {
                    RoundedRectangle(cornerRadius: 18, style: .continuous)
                        .fill(Color(white: 0.12))

                    VStack(spacing: 10) {
                        ZStack {
                            // Background Track
                            Circle()
                                .stroke(Color(white: 0.22), lineWidth: 9)
                                .frame(width: 100, height: 100)

                            // Foreground Gradient Progress
                            Circle()
                                .trim(from: 0.0, to: CGFloat(stepProgress))
                                .stroke(
                                    AngularGradient(
                                        gradient: Gradient(colors: [
                                            Color(red: 0.2, green: 0.5, blue: 1.0),
                                            Color(red: 0.0, green: 0.85, blue: 1.0)
                                        ]),
                                        center: .center
                                    ),
                                    style: StrokeStyle(lineWidth: 9, lineCap: .round)
                                )
                                .rotationEffect(.degrees(-90))
                                .frame(width: 100, height: 100)
                                .animation(.easeOut(duration: 0.7), value: stepProgress)

                            // Center Label
                            VStack(spacing: 1) {
                                Text("\(metrics.steps.formatted())")
                                    .font(.system(size: 18, weight: .bold, design: .rounded))
                                    .foregroundColor(.white)
                                    .minimumScaleFactor(0.7)
                                    .lineLimit(1)

                                Text("steps")
                                    .font(.system(size: 10, weight: .medium))
                                    .foregroundColor(.secondary)
                            }
                        }
                        .padding(.top, 8)

                        // Goal indicator pill
                        HStack(spacing: 4) {
                            Text("\(Int(stepProgress * 100))%")
                                .font(.system(size: 11, weight: .bold))
                                .foregroundColor(stepProgress >= 1.0 ? .green : .cyan)
                            Text("of \(metrics.stepGoal.formatted()) goal")
                                .font(.system(size: 10, weight: .medium))
                                .foregroundColor(.secondary)
                        }
                        .padding(.bottom, 6)
                    }
                    .padding(8)
                }

                // Metric Cards
                VStack(spacing: 8) {
                    // Calories Card
                    watchMetricRow(
                        title: "Calories",
                        value: "\(Int(metrics.calories))",
                        unit: "kcal",
                        icon: "flame.fill",
                        tint: Color.orange
                    )

                    // Heart Rate Card
                    watchMetricRow(
                        title: "Heart Rate",
                        value: metrics.heartRate > 0 ? "\(metrics.heartRate)" : "--",
                        unit: "bpm",
                        icon: "heart.fill",
                        tint: Color.red
                    )

                    // Sleep Card
                    watchMetricRow(
                        title: "Sleep",
                        value: metrics.sleepHours > 0 ? String(format: "%.1f", metrics.sleepHours) : "--",
                        unit: "hours",
                        icon: "bed.double.fill",
                        tint: Color.purple
                    )
                }

                // Timestamp
                if let updated = metrics.lastUpdated {
                    Text("Updated \(timeAgo(from: updated))")
                        .font(.system(size: 9, weight: .regular))
                        .foregroundColor(Color(white: 0.5))
                        .padding(.top, 4)
                }
            }
            .padding(.horizontal, 6)
        }
        .onAppear {
            metrics.requestSync()
        }
    }

    private func watchMetricRow(title: String, value: String, unit: String, icon: String, tint: Color) -> some View {
        HStack(spacing: 10) {
            ZStack {
                Circle()
                    .fill(tint.opacity(0.2))
                    .frame(width: 30, height: 30)

                Image(systemName: icon)
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundColor(tint)
            }

            VStack(alignment: .leading, spacing: 1) {
                Text(title)
                    .font(.system(size: 11, weight: .medium))
                    .foregroundColor(.secondary)

                HStack(alignment: .firstTextBaseline, spacing: 3) {
                    Text(value)
                        .font(.system(size: 15, weight: .bold, design: .rounded))
                        .foregroundColor(.white)

                    Text(unit)
                        .font(.system(size: 10, weight: .medium))
                        .foregroundColor(.secondary)
                }
            }

            Spacer()
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 8)
        .background(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .fill(Color(white: 0.12))
        )
    }

    private func timeAgo(from date: Date) -> String {
        let seconds = Int(Date().timeIntervalSince(date))
        if seconds < 60 {
            return "just now"
        } else if seconds < 3600 {
            return "\(seconds / 60)m ago"
        } else {
            return "\(seconds / 3600)h ago"
        }
    }
}

#Preview {
    ContentView()
}
