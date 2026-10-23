import SwiftUI

/// Graph / progress cards indicating daily intake values for Protein, Carbs, and Fat.
/// Matches the reference UI with custom vector icons, diagonal striped track,
/// filled colored bar, donut thumb indicator, and customizable targets.
public struct DailyIntakeGraphView: View {
    public let protein: Double
    public let carbs: Double
    public let fat: Double

    @AppStorage("dailyTargetProteinG") private var targetProtein: Double = 160
    @AppStorage("dailyTargetCarbsG") private var targetCarbs: Double = 280
    @AppStorage("dailyTargetFatG") private var targetFat: Double = 75

    @State private var editingNutrient: NutrientType?
    @State private var editTargetText: String = ""

    private enum NutrientType: String, Identifiable {
        case protein = "Daily Protein Intake"
        case carbs = "Carbohydrates"
        case fat = "Fat Progress"

        var id: String { rawValue }

        var defaultTarget: Double {
            switch self {
            case .protein: return 160
            case .carbs: return 280
            case .fat: return 75
            }
        }
    }

    public init(protein: Double, carbs: Double, fat: Double) {
        self.protein = protein
        self.carbs = carbs
        self.fat = fat
    }

    public var body: some View {
        VStack(spacing: 12) {
            // 1. Protein card
            DailyIntakeCard(
                icon: AnyView(BicepIconView()),
                current: protein,
                target: targetProtein,
                title: "Daily Protein Intake",
                barColor: Color(hex: 0xFF94E03C), // Fresh lime green
                onEdit: {
                    editTargetText = "\(Int(targetProtein))"
                    editingNutrient = .protein
                },
                onReset: {
                    targetProtein = 160
                }
            )

            // 2. Carbohydrates card
            DailyIntakeCard(
                icon: AnyView(AppleIconView()),
                current: carbs,
                target: targetCarbs,
                title: "Carbohydrates",
                barColor: Color(hex: 0xFF15C695), // Emerald / teal mint
                onEdit: {
                    editTargetText = "\(Int(targetCarbs))"
                    editingNutrient = .carbs
                },
                onReset: {
                    targetCarbs = 280
                }
            )

            // 3. Fat progress card
            DailyIntakeCard(
                icon: AnyView(FatClusterIconView()),
                current: fat,
                target: targetFat,
                title: "Fat Progress",
                barColor: Color(hex: 0xFFF8990E), // Vibrant amber orange
                onEdit: {
                    editTargetText = "\(Int(targetFat))"
                    editingNutrient = .fat
                },
                onReset: {
                    targetFat = 75
                }
            )
        }
        .alert(
            editingNutrient != nil ? "Set Target for \(editingNutrient!.rawValue)" : "Set Target",
            isPresented: Binding(
                get: { editingNutrient != nil },
                set: { if !$0 { editingNutrient = nil } }
            )
        ) {
            TextField("Grams (e.g. 160)", text: $editTargetText)
                .keyboardType(.numberPad)
            Button("Save") {
                if let val = Double(editTargetText), val > 0, let nutrient = editingNutrient {
                    switch nutrient {
                    case .protein: targetProtein = val
                    case .carbs: targetCarbs = val
                    case .fat: targetFat = val
                    }
                }
                editingNutrient = nil
            }
            Button("Cancel", role: .cancel) {
                editingNutrient = nil
            }
        } message: {
            Text("Enter your daily target in grams.")
        }
    }
}

// MARK: - Individual Card

private struct DailyIntakeCard: View {
    let icon: AnyView
    let current: Double
    let target: Double
    let title: String
    let barColor: Color
    let onEdit: () -> Void
    let onReset: () -> Void

    var body: some View {
        VStack(spacing: 14) {
            HStack(spacing: 14) {
                // Circular icon container
                ZStack {
                    Circle()
                        .fill(Color(hex: 0xFFF3F4F6))
                        .frame(width: 44, height: 44)

                    icon
                        .frame(width: 24, height: 24)
                }

                // Text values
                VStack(alignment: .leading, spacing: 3) {
                    Text("\(Int(current))/\(Int(target))g")
                        .font(.system(size: 20, weight: .bold))
                        .foregroundColor(Color(hex: 0xFF111827))

                    Text(title)
                        .font(.system(size: 13, weight: .regular))
                        .foregroundColor(Color(hex: 0xFF9CA3AF))
                }

                Spacer()

                // Three vertical dots menu
                Menu {
                    Button(action: onEdit) {
                        Label("Set Target", systemImage: "pencil")
                    }
                    Button(action: onReset) {
                        Label("Reset to Default", systemImage: "arrow.counterclockwise")
                    }
                } label: {
                    Image(systemName: "ellipsis")
                        .rotationEffect(.degrees(90))
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundColor(Color(hex: 0xFF9CA3AF))
                        .frame(width: 32, height: 32)
                        .contentShape(Rectangle())
                }
            }

            // Custom Progress Bar with diagonal hatch pattern and donut indicator
            IntakeProgressBar(current: current, target: target, barColor: barColor)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 16)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
        .shadow(color: Color.black.opacity(0.03), radius: 6, x: 0, y: 2)
    }
}

// MARK: - Progress Bar with Stripes & Donut Cap

private struct IntakeProgressBar: View {
    let current: Double
    let target: Double
    let barColor: Color

    var body: some View {
        GeometryReader { geo in
            let totalWidth = geo.size.width
            let barHeight: CGFloat = 20
            let ratio = target > 0 ? current / target : 0
            let clampedRatio = max(0.0, min(ratio, 1.0))

            // Height-based calculations
            let ringDiameter: CGFloat = 13
            let minFillWidth: CGFloat = barHeight
            let fillWidth: CGFloat = clampedRatio > 0 ? max(minFillWidth, totalWidth * CGFloat(clampedRatio)) : 0

            ZStack(alignment: .leading) {
                // 1. Soft grey track background
                Capsule()
                    .fill(Color(hex: 0xFFF3F4F6))

                // 2. Diagonal stripes pattern across the track
                DiagonalStripes()
                    .clipShape(Capsule())

                // 3. Colored progress fill
                if fillWidth > 0 {
                    ZStack(alignment: .trailing) {
                        Capsule()
                            .fill(barColor)
                            .frame(width: fillWidth, height: barHeight)

                        // 4. Circular white donut indicator thumb
                        Circle()
                            .strokeBorder(Color.white, lineWidth: 3.2)
                            .frame(width: ringDiameter, height: ringDiameter)
                            .padding(.trailing, 4)
                    }
                    .animation(.spring(response: 0.45, dampingFraction: 0.8), value: current)
                }
            }
        }
        .frame(height: 20)
    }
}

// MARK: - Diagonal Stripes

private struct DiagonalStripes: View {
    var color: Color = Color.black.opacity(0.045)
    var spacing: CGFloat = 7
    var lineWidth: CGFloat = 1.5

    var body: some View {
        Canvas { context, size in
            let w = size.width
            let h = size.height
            var x: CGFloat = -h
            while x < w + h {
                var p = Path()
                p.move(to: CGPoint(x: x, y: h))
                p.addLine(to: CGPoint(x: x + h, y: 0))
                context.stroke(p, with: .color(color), style: StrokeStyle(lineWidth: lineWidth, lineCap: .butt))
                x += spacing
            }
        }
    }
}

// MARK: - Vector Icons

/// Flexed bicep icon matching the reference outline
public struct BicepIconView: View {
    public init() {}

    public var body: some View {
        Canvas { context, size in
            let w = size.width
            let h = size.height
            let s = min(w, h) / 24.0
            let ox = (w - 24 * s) / 2
            let oy = (h - 24 * s) / 2

            var path = Path()
            // Fist starting point
            path.move(to: CGPoint(x: ox + 11.2 * s, y: oy + 5.0 * s))
            // Fist knuckles curve
            path.addQuadCurve(
                to: CGPoint(x: ox + 8.5 * s, y: oy + 6.5 * s),
                control: CGPoint(x: ox + 9.5 * s, y: oy + 4.8 * s)
            )
            // Outer forearm to elbow
            path.addCurve(
                to: CGPoint(x: ox + 7.2 * s, y: oy + 13.8 * s),
                control1: CGPoint(x: ox + 7.6 * s, y: oy + 8.5 * s),
                control2: CGPoint(x: ox + 6.8 * s, y: oy + 11.5 * s)
            )
            // Elbow curve
            path.addQuadCurve(
                to: CGPoint(x: ox + 10.5 * s, y: oy + 16.5 * s),
                control: CGPoint(x: ox + 7.8 * s, y: oy + 16.5 * s)
            )
            // Tricep / underarm line
            path.addCurve(
                to: CGPoint(x: ox + 17.2 * s, y: oy + 14.2 * s),
                control1: CGPoint(x: ox + 13.0 * s, y: oy + 16.5 * s),
                control2: CGPoint(x: ox + 15.2 * s, y: oy + 15.8 * s)
            )
            // Shoulder / armpit fold
            path.addQuadCurve(
                to: CGPoint(x: ox + 15.8 * s, y: oy + 11.8 * s),
                control: CGPoint(x: ox + 17.2 * s, y: oy + 12.8 * s)
            )
            // Bicep bulge peak
            path.addCurve(
                to: CGPoint(x: ox + 12.0 * s, y: oy + 8.8 * s),
                control1: CGPoint(x: ox + 15.2 * s, y: oy + 10.0 * s),
                control2: CGPoint(x: ox + 13.8 * s, y: oy + 8.8 * s)
            )
            // Inner crease between bicep and forearm
            path.addQuadCurve(
                to: CGPoint(x: ox + 10.8 * s, y: oy + 10.5 * s),
                control: CGPoint(x: ox + 11.2 * s, y: oy + 9.6 * s)
            )
            // Up to wrist and complete loop to fist
            path.addCurve(
                to: CGPoint(x: ox + 11.2 * s, y: oy + 5.0 * s),
                control1: CGPoint(x: ox + 10.2 * s, y: oy + 8.5 * s),
                control2: CGPoint(x: ox + 12.5 * s, y: oy + 6.0 * s)
            )

            context.stroke(
                path,
                with: .color(Color(hex: 0xFF1F2937)),
                style: StrokeStyle(lineWidth: 1.8 * s, lineCap: .round, lineJoin: .round)
            )
        }
    }
}

/// Apple with stem and leaf icon matching the reference outline
public struct AppleIconView: View {
    public init() {}

    public var body: some View {
        Canvas { context, size in
            let w = size.width
            let h = size.height
            let s = min(w, h) / 24.0
            let ox = (w - 24 * s) / 2
            let oy = (h - 24 * s) / 2

            // Apple body
            var bodyPath = Path()
            bodyPath.move(to: CGPoint(x: ox + 12.0 * s, y: oy + 8.2 * s))
            bodyPath.addCurve(
                to: CGPoint(x: ox + 5.5 * s, y: oy + 13.5 * s),
                control1: CGPoint(x: ox + 8.2 * s, y: oy + 6.2 * s),
                control2: CGPoint(x: ox + 5.5 * s, y: oy + 9.5 * s)
            )
            bodyPath.addCurve(
                to: CGPoint(x: ox + 12.0 * s, y: oy + 19.5 * s),
                control1: CGPoint(x: ox + 5.5 * s, y: oy + 18.0 * s),
                control2: CGPoint(x: ox + 9.0 * s, y: oy + 19.8 * s)
            )
            bodyPath.addCurve(
                to: CGPoint(x: ox + 18.5 * s, y: oy + 13.5 * s),
                control1: CGPoint(x: ox + 15.0 * s, y: oy + 19.8 * s),
                control2: CGPoint(x: ox + 18.5 * s, y: oy + 18.0 * s)
            )
            bodyPath.addCurve(
                to: CGPoint(x: ox + 12.0 * s, y: oy + 8.2 * s),
                control1: CGPoint(x: ox + 18.5 * s, y: oy + 9.5 * s),
                control2: CGPoint(x: ox + 15.8 * s, y: oy + 6.2 * s)
            )

            // Stem
            var stemPath = Path()
            stemPath.move(to: CGPoint(x: ox + 12.0 * s, y: oy + 8.0 * s))
            stemPath.addQuadCurve(
                to: CGPoint(x: ox + 13.5 * s, y: oy + 4.2 * s),
                control: CGPoint(x: ox + 11.8 * s, y: oy + 5.6 * s)
            )

            // Leaf
            var leafPath = Path()
            leafPath.move(to: CGPoint(x: ox + 13.5 * s, y: oy + 6.0 * s))
            leafPath.addQuadCurve(
                to: CGPoint(x: ox + 16.8 * s, y: oy + 4.6 * s),
                control: CGPoint(x: ox + 15.2 * s, y: oy + 4.5 * s)
            )
            leafPath.addQuadCurve(
                to: CGPoint(x: ox + 13.5 * s, y: oy + 6.0 * s),
                control: CGPoint(x: ox + 15.2 * s, y: oy + 6.8 * s)
            )

            let strokeStyle = StrokeStyle(lineWidth: 1.8 * s, lineCap: .round, lineJoin: .round)
            context.stroke(bodyPath, with: .color(Color(hex: 0xFF1F2937)), style: strokeStyle)
            context.stroke(stemPath, with: .color(Color(hex: 0xFF1F2937)), style: strokeStyle)
            context.stroke(leafPath, with: .color(Color(hex: 0xFF1F2937)), style: strokeStyle)
        }
    }
}

/// Blackberry / cluster icon matching the fat progress icon
public struct FatClusterIconView: View {
    public init() {}

    public var body: some View {
        Canvas { context, size in
            let w = size.width
            let h = size.height
            let s = min(w, h) / 24.0
            let ox = (w - 24 * s) / 2
            let oy = (h - 24 * s) / 2

            let globules: [(CGFloat, CGFloat)] = [
                (12.0, 7.8),
                (9.2, 10.2),
                (14.8, 10.2),
                (8.2, 13.6),
                (12.0, 13.2),
                (15.8, 13.6),
                (10.2, 16.6),
                (13.8, 16.6)
            ]
            let r: CGFloat = 2.3 * s
            let strokeStyle = StrokeStyle(lineWidth: 1.5 * s, lineCap: .round, lineJoin: .round)

            for (gx, gy) in globules {
                let center = CGPoint(x: ox + gx * s, y: oy + gy * s)
                var circlePath = Path()
                circlePath.addEllipse(in: CGRect(x: center.x - r, y: center.y - r, width: r * 2, height: r * 2))
                context.fill(circlePath, with: .color(.white))
                context.stroke(circlePath, with: .color(Color(hex: 0xFF1F2937)), style: strokeStyle)

                // Highlight dot
                var dotPath = Path()
                let dotR = 0.5 * s
                dotPath.addEllipse(in: CGRect(x: center.x - dotR + 0.4 * s, y: center.y - dotR - 0.4 * s, width: dotR * 2, height: dotR * 2))
                context.fill(dotPath, with: .color(Color(hex: 0xFF1F2937)))
            }
        }
    }
}
