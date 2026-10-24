import SwiftUI

struct HeartRateDetailView: View {
    let initialDate: Date
    var onBack: () -> Void
    
    @State private var selectedDate: Date
    @State private var hrSummary: HeartRateSummaryData?
    @State private var hrvData: DetailedHrvData?
    @State private var isLoading: Bool = true
    @State private var scrubbedPoint: HeartRatePoint? = nil
    
    private let healthKit = HealthKitManager.shared
    
    init(date: Date, onBack: @escaping () -> Void) {
        self.initialDate = date
        self.onBack = onBack
        self._selectedDate = State(initialValue: date)
    }
    
    private var isSelectedDateToday: Bool {
        Calendar.current.isDateInToday(selectedDate)
    }
    
    private var recentDays: [Date] {
        let calendar = Calendar.current
        let today = calendar.startOfDay(for: Date())
        return (0..<7).reversed().compactMap { offset in
            calendar.date(byAdding: .day, value: -offset, to: today)
        }
    }
    
    var body: some View {
        ZStack {
            Color(hex: 0xFFF3F2F8).ignoresSafeArea()
            
            VStack(spacing: 0) {
                // Top App Bar
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
                    
                    Text("Heart Rate & HRV")
                        .font(.system(size: 18, weight: .bold))
                        .foregroundColor(AppColors.textPrimary)
                    
                    Spacer()
                    
                    if !isSelectedDateToday {
                        Button(action: {
                            changeToDate(Date())
                        }) {
                            Text("Today")
                                .font(.system(size: 13, weight: .bold))
                                .foregroundColor(Color(hex: 0xFFDC2626))
                                .padding(.horizontal, 12)
                                .padding(.vertical, 6)
                                .background(Color(hex: 0xFFFEE2E2))
                                .clipShape(Capsule())
                        }
                    } else {
                        Image("avatar_gaze_1")
                            .resizable()
                            .scaledToFill()
                            .frame(width: 44, height: 44)
                            .clipShape(Circle())
                    }
                }
                .padding(.horizontal, 20)
                .padding(.top, 10)
                .padding(.bottom, 10)
                
                // Day Selector Section
                daySelectorSection
                    .padding(.horizontal, 20)
                    .padding(.bottom, 12)
                
                ScrollView(showsIndicators: false) {
                    VStack(spacing: 18) {
                        if isLoading {
                            VStack(spacing: 14) {
                                ProgressView()
                                    .scaleEffect(1.2)
                                Text("Loading heart rate & HRV data…")
                                    .font(.system(size: 14, weight: .medium))
                                    .foregroundColor(AppColors.textSecondary)
                            }
                            .frame(maxWidth: .infinity, minHeight: 280)
                            .background(Color.white)
                            .clipShape(RoundedRectangle(cornerRadius: 24))
                            .shadow(color: Color.black.opacity(0.03), radius: 6, y: 3)
                        } else if let summary = hrSummary, !summary.points.isEmpty {
                            // 1. Heart Rate Line Graph Card (Per Day)
                            heartRateGraphCard(summary)
                            
                            // 2. HRV Bead / Scatter Graph Card
                            if let hrv = hrvData {
                                hrvScatterCard(hrv)
                                
                                // 3. Key Metrics Pair Cards
                                HStack(spacing: 14) {
                                    MetricCard(title: "Stress Level", value: hrv.stressLevel, subtitle: "Autonomic state", icon: "waveform.path.ecg", color: Color(hex: 0xFF10B981))
                                    MetricCard(title: "Ave. Variability", value: "\(hrv.aveVariabilityMs) ms", subtitle: "Resting average", icon: "heart.fill", color: Color(hex: 0xFF6366F1))
                                }
                            }
                            
                            // 4. Pulse Summary Range Card
                            pulseSummaryCard(summary)
                        } else {
                            VStack(spacing: 12) {
                                Image(systemName: "heart.slash")
                                    .font(.system(size: 40))
                                    .foregroundColor(Color(hex: 0xFF94A3B8))
                                Text("No heart rate data for this day")
                                    .font(.system(size: 16, weight: .bold))
                                    .foregroundColor(AppColors.textPrimary)
                                Text("Switch to another day using the toggle above to inspect recorded metrics.")
                                    .font(.system(size: 13))
                                    .foregroundColor(AppColors.textSecondary)
                                    .multilineTextAlignment(.center)
                                    .padding(.horizontal, 24)
                            }
                            .frame(maxWidth: .infinity, minHeight: 220)
                            .background(Color.white)
                            .clipShape(RoundedRectangle(cornerRadius: 24))
                            .shadow(color: Color.black.opacity(0.03), radius: 6, y: 3)
                        }
                    }
                    .padding(.horizontal, 20)
                    .padding(.bottom, 30)
                }
            }
        }
        .task(id: selectedDate) {
            await loadData(for: selectedDate)
        }
    }
    
    // MARK: - Day Selector Section
    
    private var daySelectorSection: some View {
        VStack(spacing: 10) {
            // Day Toggle Controls Row
            HStack {
                Button(action: {
                    changeDay(by: -1)
                }) {
                    Image(systemName: "chevron.left")
                        .font(.system(size: 14, weight: .bold))
                        .foregroundColor(AppColors.textPrimary)
                        .frame(width: 36, height: 36)
                        .background(Color.white)
                        .clipShape(Circle())
                        .shadow(color: Color.black.opacity(0.03), radius: 3, y: 1)
                }
                
                Spacer()
                
                HStack(spacing: 6) {
                    Image(systemName: "calendar")
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundColor(Color(hex: 0xFFDC2626))
                    
                    Text(formattedSelectedDateTitle)
                        .font(.system(size: 15, weight: .bold))
                        .foregroundColor(AppColors.textPrimary)
                }
                
                Spacer()
                
                Button(action: {
                    changeDay(by: 1)
                }) {
                    Image(systemName: "chevron.right")
                        .font(.system(size: 14, weight: .bold))
                        .foregroundColor(isSelectedDateToday ? Color(hex: 0xFFCBD5E1) : AppColors.textPrimary)
                        .frame(width: 36, height: 36)
                        .background(Color.white)
                        .clipShape(Circle())
                        .shadow(color: Color.black.opacity(0.03), radius: 3, y: 1)
                }
                .disabled(isSelectedDateToday)
            }
            
            // 7-Day Quick Selection Strip
            HStack(spacing: 6) {
                ForEach(recentDays, id: \.self) { day in
                    let isSelected = Calendar.current.isDate(day, inSameDayAs: selectedDate)
                    let dayLetter = day.formatted(.dateTime.weekday(.narrow))
                    let dayNum = day.formatted(.dateTime.day())
                    
                    Button(action: {
                        changeToDate(day)
                    }) {
                        VStack(spacing: 3) {
                            Text(dayLetter)
                                .font(.system(size: 10, weight: isSelected ? .bold : .medium))
                                .foregroundColor(isSelected ? .white.opacity(0.9) : AppColors.textSecondary)
                            
                            Text(dayNum)
                                .font(.system(size: 13, weight: isSelected ? .bold : .semibold))
                                .foregroundColor(isSelected ? .white : AppColors.textPrimary)
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 8)
                        .background(
                            isSelected ?
                            LinearGradient(
                                colors: [Color(hex: 0xFFEF4444), Color(hex: 0xFFDC2626)],
                                startPoint: .top,
                                endPoint: .bottom
                            ) :
                            LinearGradient(
                                colors: [Color.white, Color.white],
                                startPoint: .top,
                                endPoint: .bottom
                            )
                        )
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                        .shadow(color: isSelected ? Color(hex: 0xFFDC2626).opacity(0.25) : Color.black.opacity(0.02), radius: 4, y: 2)
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }
    
    // MARK: - Heart Rate Line Graph Card
    
    private func heartRateGraphCard(_ summary: HeartRateSummaryData) -> some View {
        let points = summary.points
        let minBpm = summary.minBpm
        let maxBpm = summary.maxBpm
        let avgBpm = points.isEmpty ? 0 : Int(points.map(\.bpm).reduce(0, +) / points.count)
        
        return VStack(alignment: .leading, spacing: 16) {
            // Card Title & Range
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    HStack(spacing: 6) {
                        Text("Heart Rate")
                            .font(.system(size: 17, weight: .bold))
                            .foregroundColor(AppColors.textPrimary)
                        
                        Circle()
                            .fill(Color(hex: 0xFFEF4444))
                            .frame(width: 7, height: 7)
                    }
                    
                    Text(summary.timeRangeFormatted.isEmpty ? "Daily intraday pulse readings" : summary.timeRangeFormatted)
                        .font(.system(size: 12))
                        .foregroundColor(AppColors.textSecondary)
                }
                
                Spacer()
                
                HStack(spacing: 5) {
                    Image(systemName: "heart.fill")
                        .font(.system(size: 13))
                        .foregroundColor(Color(hex: 0xFFEF4444))
                    Text("\(summary.latestBpm) BPM")
                        .font(.system(size: 20, weight: .bold, design: .rounded))
                        .foregroundColor(Color(hex: 0xFFDC2626))
                }
                .padding(.horizontal, 10)
                .padding(.vertical, 6)
                .background(Color(hex: 0xFFFEF2F2))
                .clipShape(Capsule())
            }
            
            // Highlight Badges Row (Lowest, Highest, Average)
            HStack(spacing: 8) {
                // Highest Badge
                HStack(spacing: 6) {
                    Image(systemName: "arrow.up.circle.fill")
                        .font(.system(size: 14, weight: .bold))
                        .foregroundColor(Color(hex: 0xFFDC2626))
                    VStack(alignment: .leading, spacing: 1) {
                        Text("Highest")
                            .font(.system(size: 10, weight: .semibold))
                            .foregroundColor(AppColors.textSecondary)
                        Text("\(maxBpm) BPM")
                            .font(.system(size: 13, weight: .bold))
                            .foregroundColor(Color(hex: 0xFFDC2626))
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, 10)
                .padding(.vertical, 8)
                .background(Color(hex: 0xFFFEF2F2))
                .clipShape(RoundedRectangle(cornerRadius: 12))
                
                // Lowest Badge
                HStack(spacing: 6) {
                    Image(systemName: "arrow.down.circle.fill")
                        .font(.system(size: 14, weight: .bold))
                        .foregroundColor(Color(hex: 0xFF2563EB))
                    VStack(alignment: .leading, spacing: 1) {
                        Text("Lowest")
                            .font(.system(size: 10, weight: .semibold))
                            .foregroundColor(AppColors.textSecondary)
                        Text("\(minBpm) BPM")
                            .font(.system(size: 13, weight: .bold))
                            .foregroundColor(Color(hex: 0xFF2563EB))
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, 10)
                .padding(.vertical, 8)
                .background(Color(hex: 0xFFEFF6FF))
                .clipShape(RoundedRectangle(cornerRadius: 12))
                
                // Average Badge
                HStack(spacing: 6) {
                    Image(systemName: "waveform.path.ecg")
                        .font(.system(size: 14, weight: .bold))
                        .foregroundColor(Color(hex: 0xFF9333EA))
                    VStack(alignment: .leading, spacing: 1) {
                        Text("Average")
                            .font(.system(size: 10, weight: .semibold))
                            .foregroundColor(AppColors.textSecondary)
                        Text("\(avgBpm) BPM")
                            .font(.system(size: 13, weight: .bold))
                            .foregroundColor(Color(hex: 0xFF9333EA))
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, 10)
                .padding(.vertical, 8)
                .background(Color(hex: 0xFFFAF5FF))
                .clipShape(RoundedRectangle(cornerRadius: 12))
            }
            
            // The Intraday Line Graph Canvas
            HeartRateLineChartCanvas(
                points: points,
                minBpm: minBpm,
                maxBpm: maxBpm,
                selectedDate: selectedDate,
                scrubbedPoint: $scrubbedPoint
            )
            .frame(height: 220)
            
            // Bottom Helper Text
            HStack {
                Text("Tap or drag along curve to inspect pulse at any hour")
                    .font(.system(size: 11, weight: .medium))
                    .foregroundColor(AppColors.textTertiary)
                Spacer()
            }
        }
        .padding(20)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 24))
        .shadow(color: Color.black.opacity(0.03), radius: 6, y: 3)
    }
    
    // MARK: - HRV Scatter Graph Card
    
    private func hrvScatterCard(_ hrv: DetailedHrvData) -> some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Heart Rate Variability")
                        .font(.system(size: 17, weight: .bold))
                        .foregroundColor(AppColors.textPrimary)
                    Text("Daily autonomic nervous system balance")
                        .font(.system(size: 13))
                        .foregroundColor(AppColors.textSecondary)
                }
                Spacer()
                Text("\(hrv.latestHrvMs) ms")
                    .font(.system(size: 28, weight: .bold))
                    .foregroundColor(Color(hex: 0xFFDC2626))
            }
            
            // Vertical Bead Scatter Columns
            HStack(alignment: .bottom, spacing: 12) {
                ForEach(hrv.buckets) { bucket in
                    VStack(spacing: 8) {
                        ZStack(alignment: .bottom) {
                            Capsule()
                                .fill(bucket.isHighlighted ? Color(hex: 0xFFFEE2E2) : Color(hex: 0xFFF1F5F9))
                                .frame(width: 32, height: 140)
                            
                            VStack(spacing: 6) {
                                ForEach(0..<bucket.samples.count, id: \.self) { _ in
                                    Circle()
                                        .fill(bucket.isHighlighted ? Color(hex: 0xFFEF4444) : Color(hex: 0xFF94A3B8))
                                        .frame(width: 8, height: 8)
                                }
                            }
                            .padding(.bottom, 16)
                        }
                        
                        Text(bucket.hourLabel)
                            .font(.system(size: 11, weight: bucket.isHighlighted ? .bold : .medium))
                            .foregroundColor(bucket.isHighlighted ? Color(hex: 0xFFDC2626) : AppColors.textTertiary)
                    }
                    .frame(maxWidth: .infinity)
                }
            }
            .padding(.vertical, 10)
        }
        .padding(20)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 24))
        .shadow(color: Color.black.opacity(0.03), radius: 6, y: 3)
    }
    
    // MARK: - Pulse Summary Card
    
    private func pulseSummaryCard(_ summary: HeartRateSummaryData) -> some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("Heart Rate Summary")
                .font(.system(size: 17, weight: .bold))
                .foregroundColor(AppColors.textPrimary)
            
            HStack(spacing: 20) {
                PulseItem(label: isSelectedDateToday ? "Current" : "Latest", bpm: "\(summary.latestBpm) BPM", color: Color(hex: 0xFFEF4444))
                PulseItem(label: "Min Resting", bpm: "\(summary.minBpm) BPM", color: Color(hex: 0xFF3B82F6))
                PulseItem(label: "Max Peak", bpm: "\(summary.maxBpm) BPM", color: Color(hex: 0xFFF59E0B))
            }
        }
        .padding(20)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 24))
        .shadow(color: Color.black.opacity(0.03), radius: 6, y: 3)
    }
    
    // MARK: - Actions & Helpers
    
    private var formattedSelectedDateTitle: String {
        let calendar = Calendar.current
        if calendar.isDateInToday(selectedDate) {
            return "Today, \(selectedDate.formatted(.dateTime.month(.abbreviated).day()))"
        } else if calendar.isDateInYesterday(selectedDate) {
            return "Yesterday, \(selectedDate.formatted(.dateTime.month(.abbreviated).day()))"
        } else {
            return selectedDate.formatted(.dateTime.weekday(.abbreviated).month(.abbreviated).day())
        }
    }
    
    private func changeDay(by offset: Int) {
        if let newDate = Calendar.current.date(byAdding: .day, value: offset, to: selectedDate) {
            if offset > 0 && newDate > Date() { return }
            changeToDate(newDate)
        }
    }
    
    private func changeToDate(_ newDate: Date) {
        withAnimation(.easeInOut(duration: 0.2)) {
            selectedDate = newDate
            scrubbedPoint = nil
        }
    }
    
    private func loadData(for date: Date) async {
        self.isLoading = true
        async let hrTask = healthKit.fetchHeartRateSummary(for: date)
        async let hrvTask = healthKit.fetchDetailedHrv(for: date)
        
        let (hr, hrv) = await (hrTask, hrvTask)
        
        await MainActor.run {
            self.hrSummary = hr
            self.hrvData = hrv
            self.isLoading = false
        }
    }
}

// MARK: - Heart Rate Line Chart Canvas

private struct HeartRateLineChartCanvas: View {
    let points: [HeartRatePoint]
    let minBpm: Int
    let maxBpm: Int
    let selectedDate: Date
    @Binding var scrubbedPoint: HeartRatePoint?
    
    @State private var dragLocation: CGPoint? = nil
    
    private static let timeFormatter: DateFormatter = {
        let f = DateFormatter()
        f.timeStyle = .short
        return f
    }()
    
    var body: some View {
        GeometryReader { geo in
            let w = geo.size.width
            let h = geo.size.height
            let padLeft: CGFloat = 32
            let padRight: CGFloat = 16
            let padTop: CGFloat = 36
            let padBottom: CGFloat = 28
            let plotW = w - padLeft - padRight
            let plotH = h - padTop - padBottom
            
            let sortedPoints = points.sorted(by: { $0.time < $1.time })
            let yMin = max(35.0, Double(minBpm) - 10.0)
            let yMax = min(220.0, Double(maxBpm) + 12.0)
            let yRange = max(20.0, yMax - yMin)
            
            // X-axis mapping
            let dayStart = Calendar.current.startOfDay(for: selectedDate)
            let firstTime = sortedPoints.first?.time.timeIntervalSince(dayStart) ?? 0
            let lastTime = sortedPoints.last?.time.timeIntervalSince(dayStart) ?? 86400
            let timeSpan = lastTime - firstTime
            let use24HourScale = timeSpan >= 10800 // 3 hours or more
            
            let coords: [CGPoint] = sortedPoints.indices.map { idx in
                let pt = sortedPoints[idx]
                let xFrac: CGFloat
                if use24HourScale {
                    let sec = pt.time.timeIntervalSince(dayStart)
                    xFrac = min(1.0, max(0.0, CGFloat(sec / 86400.0)))
                } else {
                    xFrac = sortedPoints.count == 1 ? 0.5 : CGFloat(idx) / CGFloat(sortedPoints.count - 1)
                }
                let x = padLeft + xFrac * plotW
                let yNorm = CGFloat((Double(pt.bpm) - yMin) / yRange)
                let y = padTop + plotH * (1.0 - yNorm)
                return CGPoint(x: x, y: y)
            }
            
            // Identify minimum and maximum point indices
            let minIndex = sortedPoints.indices.min(by: { sortedPoints[$0].bpm < sortedPoints[$1].bpm }) ?? 0
            let maxIndex = sortedPoints.indices.max(by: { sortedPoints[$0].bpm < sortedPoints[$1].bpm }) ?? 0
            
            let minCoord = coords.indices.contains(minIndex) ? coords[minIndex] : CGPoint(x: padLeft + plotW * 0.3, y: padTop + plotH * 0.8)
            let maxCoord = coords.indices.contains(maxIndex) ? coords[maxIndex] : CGPoint(x: padLeft + plotW * 0.7, y: padTop + plotH * 0.2)
            
            // Reference gridline values
            let gridYValues: [Double] = [
                yMax - yRange * 0.1,
                (yMin + yMax) / 2.0,
                yMin + yRange * 0.1
            ]
            
            ZStack {
                // Background Horizontal Gridlines & Y-Axis Labels
                ForEach(gridYValues, id: \.self) { val in
                    let norm = CGFloat((val - yMin) / yRange)
                    let y = padTop + plotH * (1.0 - norm)
                    
                    Path { p in
                        p.move(to: CGPoint(x: padLeft, y: y))
                        p.addLine(to: CGPoint(x: w - padRight, y: y))
                    }
                    .stroke(Color(hex: 0xFFE2E8F0), style: StrokeStyle(lineWidth: 1, dash: [4, 4]))
                    
                    Text("\(Int(val))")
                        .font(.system(size: 10, weight: .medium))
                        .foregroundColor(Color(hex: 0xFF94A3B8))
                        .frame(width: 28, alignment: .trailing)
                        .position(x: 14, y: y)
                }
                
                // Bottom X-Axis Time Labels
                HStack {
                    Text("12 AM")
                    Spacer()
                    Text("6 AM")
                    Spacer()
                    Text("12 PM")
                    Spacer()
                    Text("6 PM")
                    Spacer()
                    Text("12 AM")
                }
                .font(.system(size: 10, weight: .medium))
                .foregroundColor(Color(hex: 0xFF94A3B8))
                .padding(.leading, padLeft)
                .padding(.trailing, padRight)
                .position(x: w / 2, y: h - 10)
                
                if coords.count >= 2 {
                    // Soft Gradient Fill Under Curve
                    Path { p in
                        guard let first = coords.first, let last = coords.last else { return }
                        let bottomY = padTop + plotH
                        p.move(to: CGPoint(x: first.x, y: bottomY))
                        p.addLine(to: first)
                        for i in 1..<coords.count {
                            let prev = coords[i - 1]
                            let curr = coords[i]
                            let midX = (prev.x + curr.x) / 2
                            p.addCurve(to: curr, control1: CGPoint(x: midX, y: prev.y), control2: CGPoint(x: midX, y: curr.y))
                        }
                        p.addLine(to: CGPoint(x: last.x, y: bottomY))
                        p.closeSubpath()
                    }
                    .fill(
                        LinearGradient(
                            colors: [
                                Color(hex: 0xFFEF4444).opacity(0.24),
                                Color(hex: 0xFFEF4444).opacity(0.01)
                            ],
                            startPoint: .top,
                            endPoint: .bottom
                        )
                    )
                    
                    // The Smooth Heart Rate Curve Stroke
                    Path { p in
                        guard let first = coords.first else { return }
                        p.move(to: first)
                        for i in 1..<coords.count {
                            let prev = coords[i - 1]
                            let curr = coords[i]
                            let midX = (prev.x + curr.x) / 2
                            p.addCurve(to: curr, control1: CGPoint(x: midX, y: prev.y), control2: CGPoint(x: midX, y: curr.y))
                        }
                    }
                    .stroke(
                        LinearGradient(
                            colors: [
                                Color(hex: 0xFFF87171),
                                Color(hex: 0xFFEF4444),
                                Color(hex: 0xFFDC2626)
                            ],
                            startPoint: .leading,
                            endPoint: .trailing
                        ),
                        style: StrokeStyle(lineWidth: 3, lineCap: .round, lineJoin: .round)
                    )
                } else if let single = coords.first {
                    // Single point display
                    Circle()
                        .fill(Color(hex: 0xFFDC2626))
                        .frame(width: 10, height: 10)
                        .position(single)
                }
                
                // MARK: - HIGHEST MARKED POINT
                if !sortedPoints.isEmpty {
                    // Outer pulse ring & solid dot
                    Circle()
                        .stroke(Color(hex: 0xFFDC2626).opacity(0.3), lineWidth: 5)
                        .frame(width: 20, height: 20)
                        .position(maxCoord)
                    
                    Circle()
                        .fill(Color(hex: 0xFFDC2626))
                        .frame(width: 8, height: 8)
                        .overlay(Circle().stroke(Color.white, lineWidth: 2))
                        .position(maxCoord)
                    
                    // Callout Badge for Highest
                    let badgeW: CGFloat = 94
                    let clampedMaxX = min(w - padRight - badgeW / 2, max(padLeft + badgeW / 2, maxCoord.x))
                    let clampedMaxY = max(padTop - 12, maxCoord.y - 20)
                    
                    HStack(spacing: 3) {
                        Image(systemName: "arrow.up")
                            .font(.system(size: 8, weight: .bold))
                        Text("Highest \(maxBpm)")
                            .font(.system(size: 10, weight: .bold))
                    }
                    .foregroundColor(.white)
                    .padding(.horizontal, 7)
                    .padding(.vertical, 3.5)
                    .background(Color(hex: 0xFFDC2626))
                    .clipShape(Capsule())
                    .shadow(color: Color.black.opacity(0.12), radius: 3, y: 1.5)
                    .position(x: clampedMaxX, y: clampedMaxY)
                }
                
                // MARK: - LOWEST MARKED POINT
                if !sortedPoints.isEmpty {
                    // Outer pulse ring & solid dot
                    Circle()
                        .stroke(Color(hex: 0xFF2563EB).opacity(0.3), lineWidth: 5)
                        .frame(width: 20, height: 20)
                        .position(minCoord)
                    
                    Circle()
                        .fill(Color(hex: 0xFF2563EB))
                        .frame(width: 8, height: 8)
                        .overlay(Circle().stroke(Color.white, lineWidth: 2))
                        .position(minCoord)
                    
                    // Callout Badge for Lowest
                    let badgeW: CGFloat = 90
                    let clampedMinX = min(w - padRight - badgeW / 2, max(padLeft + badgeW / 2, minCoord.x))
                    let clampedMinY = min(padTop + plotH + 8, minCoord.y + 20)
                    
                    HStack(spacing: 3) {
                        Image(systemName: "arrow.down")
                            .font(.system(size: 8, weight: .bold))
                        Text("Lowest \(minBpm)")
                            .font(.system(size: 10, weight: .bold))
                    }
                    .foregroundColor(.white)
                    .padding(.horizontal, 7)
                    .padding(.vertical, 3.5)
                    .background(Color(hex: 0xFF2563EB))
                    .clipShape(Capsule())
                    .shadow(color: Color.black.opacity(0.12), radius: 3, y: 1.5)
                    .position(x: clampedMinX, y: clampedMinY)
                }
                
                // MARK: - INTERACTIVE SCRUBBER
                if let scrub = scrubbedPoint, let dragPos = dragLocation {
                    // Vertical guideline
                    Path { p in
                        p.move(to: CGPoint(x: dragPos.x, y: padTop))
                        p.addLine(to: CGPoint(x: dragPos.x, y: padTop + plotH))
                    }
                    .stroke(Color(hex: 0xFF64748B), style: StrokeStyle(lineWidth: 1.5, dash: [3, 3]))
                    
                    // Indicator dot at exact curve height
                    Circle()
                        .fill(Color(hex: 0xFF0F172A))
                        .frame(width: 10, height: 10)
                        .overlay(Circle().stroke(Color.white, lineWidth: 2))
                        .position(dragPos)
                    
                    // Scrubber popup tooltip
                    VStack(spacing: 2) {
                        Text("\(scrub.bpm) BPM")
                            .font(.system(size: 12, weight: .bold))
                            .foregroundColor(.white)
                        Text(Self.timeFormatter.string(from: scrub.time))
                            .font(.system(size: 9, weight: .medium))
                            .foregroundColor(.white.opacity(0.85))
                    }
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(Color(hex: 0xFF0F172A))
                    .clipShape(RoundedRectangle(cornerRadius: 8))
                    .shadow(color: Color.black.opacity(0.2), radius: 4, y: 2)
                    .position(
                        x: min(w - padRight - 45, max(padLeft + 45, dragPos.x)),
                        y: max(padTop + 14, dragPos.y - 30)
                    )
                }
            }
            .contentShape(Rectangle())
            .gesture(
                DragGesture(minimumDistance: 0)
                    .onChanged { value in
                        guard !coords.isEmpty else { return }
                        let touchX = value.location.x
                        // Find closest point
                        if let closestIndex = coords.indices.min(by: { abs(coords[$0].x - touchX) < abs(coords[$1].x - touchX) }) {
                            self.scrubbedPoint = sortedPoints[closestIndex]
                            self.dragLocation = coords[closestIndex]
                        }
                    }
                    .onEnded { _ in
                        self.scrubbedPoint = nil
                        self.dragLocation = nil
                    }
            )
        }
    }
}

// MARK: - MetricCard & PulseItem Supporting Views

private struct MetricCard: View {
    let title: String
    let value: String
    let subtitle: String
    let icon: String
    let color: Color
    
    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Image(systemName: icon)
                    .foregroundColor(color)
                Spacer()
            }
            Text(title)
                .font(.system(size: 13, weight: .medium))
                .foregroundColor(AppColors.textSecondary)
            Text(value)
                .font(.system(size: 22, weight: .bold))
                .foregroundColor(AppColors.textPrimary)
            Text(subtitle)
                .font(.system(size: 11))
                .foregroundColor(AppColors.textTertiary)
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 20))
        .shadow(color: Color.black.opacity(0.03), radius: 6, y: 3)
    }
}

private struct PulseItem: View {
    let label: String
    let bpm: String
    let color: Color
    
    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label)
                .font(.system(size: 12))
                .foregroundColor(AppColors.textSecondary)
            Text(bpm)
                .font(.system(size: 18, weight: .bold))
                .foregroundColor(color)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}
