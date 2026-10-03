import SwiftUI
import UIKit

// MARK: - Date helpers

private let challengeApiDateFormatter: DateFormatter = {
    let f = DateFormatter()
    f.dateFormat = "yyyy-MM-dd"
    f.locale = Locale(identifier: "en_US_POSIX")
    return f
}()

private let challengeDisplayDateFormatter: DateFormatter = {
    let f = DateFormatter()
    f.dateFormat = "MMM d"
    return f
}()

private func challengeDateRangeText(startDate: String?, endDate: String?) -> String {
    guard
        let startRaw = startDate,
        let endRaw = endDate,
        let start = challengeApiDateFormatter.date(from: startRaw),
        let end = challengeApiDateFormatter.date(from: endRaw)
    else {
        return "Dates not set"
    }
    let startText = challengeDisplayDateFormatter.string(from: start)
    let endText = challengeDisplayDateFormatter.string(from: end)
    let yearText = Calendar.current.component(.year, from: end)
    return "\(startText) – \(endText), \(yearText)"
}

// MARK: - HealthKit progress

private struct ChallengeProgress {
    var steps: Int64 = 0
    var workouts: Int = 0
    var calories: Double = 0
    var distanceKm: Double = 0
}

/// Sums real HealthKit totals for every day in [startDate, endDate] (inclusive).
/// Returns nil when the dates can't be parsed. Never fabricates values —
/// days with no recorded data contribute zero.
private func computeChallengeProgress(startDate: String, endDate: String) async -> ChallengeProgress? {
    guard
        let start = challengeApiDateFormatter.date(from: startDate),
        let end = challengeApiDateFormatter.date(from: endDate)
    else { return nil }

    let calendar = Calendar.current
    var day = calendar.startOfDay(for: start)
    let lastDay = calendar.startOfDay(for: end)
    guard day <= lastDay else { return ChallengeProgress() }

    var progress = ChallengeProgress()
    var dayCount = 0
    while day <= lastDay && dayCount < 366 {
        progress.steps += await HealthKitManager.shared.fetchSteps(for: day)
        let workouts = await HealthKitManager.shared.fetchWorkouts(for: day)
        progress.workouts += workouts.count
        let breakdown = await HealthKitManager.shared.fetchCaloriesBreakdown(for: day)
        progress.calories += breakdown.totalKcal
        progress.distanceKm += (await HealthKitManager.shared.fetchDistanceMeters(for: day) ?? 0) / 1000.0

        guard let next = calendar.date(byAdding: .day, value: 1, to: day) else { break }
        day = next
        dayCount += 1
    }
    return progress
}

// MARK: - List view model

@MainActor
final class ChallengesViewModel: ObservableObject {
    @Published var challenges: [ChallengeSummary] = []
    @Published var isLoading = false
    @Published var errorMessage: String? = nil
    @Published var joinCode = ""
    @Published var isJoining = false
    @Published var joinError: String? = nil

    func load() async {
        isLoading = true
        errorMessage = nil
        do {
            challenges = try await ChallengeApi.shared.fetchChallenges()
        } catch {
            errorMessage = error.localizedDescription
        }
        isLoading = false
    }

    /// Returns the joined challenge detail on success, nil on failure.
    func join() async -> ChallengeDetail? {
        let code = joinCode.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !code.isEmpty else {
            joinError = "Enter an invite code."
            return nil
        }
        isJoining = true
        joinError = nil
        defer { isJoining = false }
        do {
            let detail = try await ChallengeApi.shared.join(code: code)
            joinCode = ""
            await load()
            return detail
        } catch {
            joinError = error.localizedDescription
            return nil
        }
    }
}

// MARK: - Challenges list

struct ChallengesView: View {
    @StateObject private var viewModel = ChallengesViewModel()
    @State private var showingCreate = false
    @State private var navigationPath: [Int64] = []

    var body: some View {
        NavigationStack(path: $navigationPath) {
            ScrollView {
                VStack(spacing: 14) {
                    joinCard

                    if viewModel.isLoading && viewModel.challenges.isEmpty {
                        ProgressView("Loading challenges…")
                            .padding(.top, 40)
                    } else if let error = viewModel.errorMessage, viewModel.challenges.isEmpty {
                        emptyState(
                            icon: "exclamationmark.triangle",
                            title: "Couldn't load challenges",
                            subtitle: error
                        )
                    } else if viewModel.challenges.isEmpty {
                        emptyState(
                            icon: "person.3.fill",
                            title: "No challenges yet",
                            subtitle: "Create one and invite friends, or join with an invite code above."
                        )
                    } else {
                        ForEach(viewModel.challenges) { challenge in
                            NavigationLink(value: challenge.challengeId ?? -1) {
                                ChallengeCard(challenge: challenge)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
                .padding(.horizontal, 16)
                .padding(.top, 8)
                .padding(.bottom, 24)
            }
            .navigationTitle("Challenges")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button {
                        showingCreate = true
                    } label: {
                        Image(systemName: "plus.circle.fill")
                            .font(.system(size: 22))
                            .foregroundColor(AppColors.primary)
                    }
                }
            }
            .sheet(isPresented: $showingCreate) {
                CreateChallengeSheet { detail in
                    if let id = detail.challengeId {
                        navigationPath.append(id)
                    }
                    Task { await viewModel.load() }
                }
            }
            .navigationDestination(for: Int64.self) { challengeId in
                ChallengeDetailView(challengeId: challengeId) {
                    navigationPath.removeAll()
                    Task { await viewModel.load() }
                }
            }
            .task {
                await viewModel.load()
            }
            .refreshable {
                await viewModel.load()
            }
        }
    }

    // MARK: Join card

    private var joinCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Join with invite code")
                .font(.system(size: 15, weight: .semibold))
                .foregroundColor(AppColors.textPrimary)
            HStack(spacing: 10) {
                TextField("Enter code", text: $viewModel.joinCode)
                    .textInputAutocapitalization(.characters)
                    .autocorrectionDisabled()
                    .padding(12)
                    .background(AppColors.surface)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                Button {
                    Task {
                        if let detail = await viewModel.join(), let id = detail.challengeId {
                            navigationPath.append(id)
                        }
                    }
                } label: {
                    if viewModel.isJoining {
                        ProgressView()
                            .frame(width: 64, height: 20)
                    } else {
                        Text("Join")
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundColor(.white)
                            .frame(width: 64, height: 20)
                    }
                }
                .padding(12)
                .background(AppColors.primary)
                .clipShape(RoundedRectangle(cornerRadius: 12))
                .disabled(viewModel.isJoining)
            }
            if let joinError = viewModel.joinError {
                Text(joinError)
                    .font(.system(size: 13))
                    .foregroundColor(AppColors.danger)
            }
        }
        .padding(16)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 20))
        .shadow(color: Color.black.opacity(0.06), radius: 8, y: 3)
    }

    private func emptyState(icon: String, title: String, subtitle: String) -> some View {
        VStack(spacing: 12) {
            Image(systemName: icon)
                .font(.system(size: 44))
                .foregroundColor(AppColors.primary.opacity(0.4))
            Text(title)
                .font(.system(size: 17, weight: .bold))
                .foregroundColor(AppColors.textPrimary)
            Text(subtitle)
                .font(.system(size: 14))
                .foregroundColor(AppColors.textSecondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 24)
        }
        .padding(.top, 60)
    }
}

// MARK: - Challenge card

private struct ChallengeCard: View {
    let challenge: ChallengeSummary

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 12) {
                Image(systemName: challenge.metric.iconName)
                    .font(.system(size: 18))
                    .foregroundColor(AppColors.primary)
                    .frame(width: 40, height: 40)
                    .background(AppColors.primaryLight)
                    .clipShape(Circle())

                VStack(alignment: .leading, spacing: 2) {
                    Text(challenge.name ?? "Challenge")
                        .font(.system(size: 16, weight: .bold))
                        .foregroundColor(AppColors.textPrimary)
                        .lineLimit(1)
                    Text(challenge.metric.displayName)
                        .font(.system(size: 13))
                        .foregroundColor(AppColors.textSecondary)
                }

                Spacer()

                StatusPill(status: challenge.challengeStatus)
            }

            Text(challengeDateRangeText(startDate: challenge.startDate, endDate: challenge.endDate))
                .font(.system(size: 13))
                .foregroundColor(AppColors.textSecondary)

            HStack(spacing: 16) {
                Label("\(challenge.participantCount ?? 0) joined", systemImage: "person.2.fill")
                    .font(.system(size: 13))
                    .foregroundColor(AppColors.textSecondary)
                if let rank = challenge.myRank, rank > 0 {
                    Label("Rank #\(rank)", systemImage: "trophy.fill")
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundColor(AppColors.warning)
                }
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundColor(AppColors.textTertiary)
            }
        }
        .padding(16)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 20))
        .shadow(color: Color.black.opacity(0.06), radius: 8, y: 3)
    }
}

private struct StatusPill: View {
    let status: ChallengeStatus

    var body: some View {
        Text(status.displayName)
            .font(.system(size: 11, weight: .bold))
            .foregroundColor(pillColor)
            .padding(.horizontal, 10)
            .padding(.vertical, 5)
            .background(pillColor.opacity(0.12))
            .clipShape(Capsule())
    }

    private var pillColor: Color {
        switch status {
        case .active: return AppColors.success
        case .upcoming: return AppColors.primary
        case .ended: return AppColors.textTertiary
        }
    }
}

// MARK: - Create sheet

private struct CreateChallengeSheet: View {
    var onCreated: (ChallengeDetail) -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var name = ""
    @State private var description = ""
    @State private var metricType: ChallengeMetricType = .steps
    @State private var startDate = Date()
    @State private var endDate = Calendar.current.date(byAdding: .day, value: 7, to: Date()) ?? Date()
    @State private var errorMessage: String? = nil
    @State private var isCreating = false

    var body: some View {
        NavigationStack {
            Form {
                Section("Challenge") {
                    TextField("Name (e.g. October Step Battle)", text: $name)
                    TextField("Description (optional)", text: $description, axis: .vertical)
                        .lineLimit(3)
                }

                Section("Metric") {
                    Picker("Metric", selection: $metricType) {
                        ForEach(ChallengeMetricType.allCases, id: \.self) { metric in
                            Text(metric.displayName).tag(metric)
                        }
                    }
                    .pickerStyle(.segmented)
                    Text("Everyone is ranked by total \(metricType.unitLabel) in the date range.")
                        .font(.system(size: 13))
                        .foregroundColor(AppColors.textSecondary)
                }

                Section("Dates") {
                    DatePicker("Starts", selection: $startDate, displayedComponents: .date)
                    DatePicker("Ends", selection: $endDate, displayedComponents: .date)
                }

                if let errorMessage = errorMessage {
                    Section {
                        Text(errorMessage)
                            .foregroundColor(AppColors.danger)
                            .font(.system(size: 14))
                    }
                }

                Section {
                    Button {
                        Task { await create() }
                    } label: {
                        HStack {
                            Spacer()
                            if isCreating {
                                ProgressView()
                            } else {
                                Text("Create Challenge")
                                    .fontWeight(.semibold)
                            }
                            Spacer()
                        }
                    }
                    .disabled(isCreating)
                }
            }
            .navigationTitle("New Challenge")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") { dismiss() }
                }
            }
        }
    }

    private func create() async {
        let trimmedName = name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmedName.isEmpty else {
            errorMessage = "Give your challenge a name."
            return
        }
        let calendar = Calendar.current
        let startDay = calendar.startOfDay(for: startDate)
        let endDay = calendar.startOfDay(for: endDate)
        guard endDay >= startDay else {
            errorMessage = "End date must be on or after the start date."
            return
        }

        errorMessage = nil
        isCreating = true
        defer { isCreating = false }

        do {
            let detail = try await ChallengeApi.shared.createChallenge(
                name: trimmedName,
                description: description,
                metricType: metricType,
                startDate: challengeApiDateFormatter.string(from: startDay),
                endDate: challengeApiDateFormatter.string(from: endDay)
            )
            onCreated(detail)
            dismiss()
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}

// MARK: - Detail view model

@MainActor
final class ChallengeDetailViewModel: ObservableObject {
    enum SyncState: Equatable {
        case idle
        case syncing
        case synced(Date)
        case failed(String)
        case needsHealthAccess
    }

    @Published var detail: ChallengeDetail? = nil
    @Published var isLoading = false
    @Published var errorMessage: String? = nil
    @Published var syncState: SyncState = .idle
    @Published var inviteEmail = ""
    @Published var inviteMessage: String? = nil
    @Published var isInviting = false
    @Published var showingLeaveConfirm = false
    @Published var isLeaving = false
    @Published var codeCopied = false

    private let challengeId: Int64

    init(challengeId: Int64) {
        self.challengeId = challengeId
    }

    func load() async {
        isLoading = true
        errorMessage = nil
        do {
            detail = try await ChallengeApi.shared.fetchDetail(id: challengeId)
        } catch {
            errorMessage = error.localizedDescription
        }
        isLoading = false
    }

    /// Sums real HealthKit totals over the challenge window and posts them.
    /// When `requestAccessIfNeeded` is false (automatic sync on appear), a
    /// missing HealthKit authorization is reported instead of prompting.
    func syncProgress(requestAccessIfNeeded: Bool) async {
        guard
            let detail = detail,
            let startRaw = detail.startDate,
            let endRaw = detail.endDate
        else { return }

        if !HealthKitManager.shared.isAuthorized {
            if requestAccessIfNeeded {
                let granted = await HealthKitManager.shared.requestAuthorization()
                guard granted else {
                    syncState = .needsHealthAccess
                    return
                }
            } else {
                syncState = .needsHealthAccess
                return
            }
        }

        syncState = .syncing
        guard let progress = await computeChallengeProgress(startDate: startRaw, endDate: endRaw) else {
            syncState = .failed("Couldn't read the challenge dates.")
            return
        }

        do {
            _ = try await ChallengeApi.shared.postProgress(
                challengeId: challengeId,
                steps: progress.steps,
                workouts: progress.workouts,
                calories: progress.calories,
                distanceKm: progress.distanceKm
            )
            syncState = .synced(Date())
            await load()
        } catch {
            syncState = .failed(error.localizedDescription)
        }
    }

    func sendInvite() async {
        let email = inviteEmail.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        guard email.contains("@"), email.contains(".") else {
            inviteMessage = "Enter a valid email address."
            return
        }
        isInviting = true
        inviteMessage = nil
        defer { isInviting = false }
        do {
            let message = try await ChallengeApi.shared.invite(email: email, challengeId: challengeId)
            inviteMessage = message
            inviteEmail = ""
        } catch {
            inviteMessage = error.localizedDescription
        }
    }

    func leave() async -> Bool {
        isLeaving = true
        defer { isLeaving = false }
        do {
            _ = try await ChallengeApi.shared.leaveChallenge(id: challengeId)
            return true
        } catch {
            errorMessage = error.localizedDescription
            return false
        }
    }

    func copyInviteCode() {
        guard let code = detail?.inviteCode, !code.isEmpty else { return }
        UIPasteboard.general.string = code
        codeCopied = true
        Task {
            try? await Task.sleep(nanoseconds: 1_500_000_000)
            codeCopied = false
        }
    }
}

// MARK: - Detail view

private struct ChallengeDetailView: View {
    let challengeId: Int64
    var onLeft: () -> Void

    @StateObject private var viewModel: ChallengeDetailViewModel
    @Environment(\.dismiss) private var dismiss

    init(challengeId: Int64, onLeft: @escaping () -> Void) {
        self.challengeId = challengeId
        self.onLeft = onLeft
        _viewModel = StateObject(wrappedValue: ChallengeDetailViewModel(challengeId: challengeId))
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 14) {
                if viewModel.isLoading && viewModel.detail == nil {
                    ProgressView("Loading challenge…")
                        .padding(.top, 40)
                } else if let detail = viewModel.detail {
                    headerCard(detail: detail)
                    syncCard
                    inviteCard(detail: detail)
                    leaderboardCard(detail: detail)
                    leaveButton
                } else if let error = viewModel.errorMessage {
                    Text(error)
                        .foregroundColor(AppColors.danger)
                        .padding(.top, 40)
                }
            }
            .padding(.horizontal, 16)
            .padding(.top, 8)
            .padding(.bottom, 24)
        }
        .navigationTitle(viewModel.detail?.name ?? "Challenge")
        .navigationBarTitleDisplayMode(.inline)
        .task {
            await viewModel.load()
            await viewModel.syncProgress(requestAccessIfNeeded: false)
        }
        .refreshable {
            await viewModel.load()
            await viewModel.syncProgress(requestAccessIfNeeded: false)
        }
        .alert("Leave challenge?", isPresented: $viewModel.showingLeaveConfirm) {
            Button("Cancel", role: .cancel) {}
            Button("Leave", role: .destructive) {
                Task {
                    if await viewModel.leave() {
                        onLeft()
                        dismiss()
                    }
                }
            }
        } message: {
            Text("Your progress will be removed from the leaderboard.")
        }
    }

    // MARK: Header

    private func headerCard(detail: ChallengeDetail) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Image(systemName: detail.metric.iconName)
                    .font(.system(size: 18))
                    .foregroundColor(AppColors.primary)
                    .frame(width: 40, height: 40)
                    .background(AppColors.primaryLight)
                    .clipShape(Circle())
                VStack(alignment: .leading, spacing: 2) {
                    Text("Most \(detail.metric.unitLabel)")
                        .font(.system(size: 13))
                        .foregroundColor(AppColors.textSecondary)
                    Text(challengeDateRangeText(startDate: detail.startDate, endDate: detail.endDate))
                        .font(.system(size: 14, weight: .medium))
                        .foregroundColor(AppColors.textPrimary)
                }
                Spacer()
                StatusPill(status: detail.challengeStatus)
            }
            if let description = detail.description, !description.isEmpty {
                Text(description)
                    .font(.system(size: 14))
                    .foregroundColor(AppColors.textSecondary)
            }
            HStack(spacing: 16) {
                Label("\(detail.participantCount ?? 0) participants", systemImage: "person.2.fill")
                    .font(.system(size: 13))
                    .foregroundColor(AppColors.textSecondary)
                if let rank = detail.myRank, rank > 0 {
                    Label("You rank #\(rank)", systemImage: "trophy.fill")
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundColor(AppColors.warning)
                }
            }
        }
        .padding(16)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 20))
        .shadow(color: Color.black.opacity(0.06), radius: 8, y: 3)
    }

    // MARK: Sync

    private var syncCard: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text("My progress")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundColor(AppColors.textPrimary)
                Spacer()
                Button {
                    Task { await viewModel.syncProgress(requestAccessIfNeeded: true) }
                } label: {
                    HStack(spacing: 6) {
                        if case .syncing = viewModel.syncState {
                            ProgressView()
                                .scaleEffect(0.8)
                        } else {
                            Image(systemName: "arrow.triangle.2.circlepath")
                        }
                        Text("Sync now")
                            .font(.system(size: 13, weight: .semibold))
                    }
                    .foregroundColor(AppColors.primary)
                }
                .disabled({
                    if case .syncing = viewModel.syncState { return true }
                    return false
                }())
            }
            syncStatusText
                .font(.system(size: 13))
                .foregroundColor(AppColors.textSecondary)
        }
        .padding(16)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 20))
        .shadow(color: Color.black.opacity(0.06), radius: 8, y: 3)
    }

    private var syncStatusText: Text {
        switch viewModel.syncState {
        case .idle:
            return Text("Your Health totals for the challenge dates sync automatically.")
        case .syncing:
            return Text("Syncing your Health data…")
        case .synced(let date):
            let formatter = DateFormatter()
            formatter.dateStyle = .none
            formatter.timeStyle = .short
            return Text("Last synced at \(formatter.string(from: date)).")
        case .failed(let message):
            return Text("Sync failed: \(message)")
        case .needsHealthAccess:
            return Text("Allow Health access to sync your progress from this device.")
        }
    }

    // MARK: Invite

    private func inviteCard(detail: ChallengeDetail) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Invite friends")
                .font(.system(size: 15, weight: .semibold))
                .foregroundColor(AppColors.textPrimary)

            if let code = detail.inviteCode, !code.isEmpty {
                Button {
                    viewModel.copyInviteCode()
                } label: {
                    HStack {
                        Text(code)
                            .font(.system(size: 22, weight: .bold, design: .monospaced))
                            .foregroundColor(AppColors.textPrimary)
                        Spacer()
                        Image(systemName: viewModel.codeCopied ? "checkmark.circle.fill" : "doc.on.doc")
                            .foregroundColor(viewModel.codeCopied ? AppColors.success : AppColors.primary)
                        Text(viewModel.codeCopied ? "Copied" : "Tap to copy")
                            .font(.system(size: 13))
                            .foregroundColor(AppColors.textSecondary)
                    }
                    .padding(12)
                    .background(AppColors.surface)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                }
                .buttonStyle(.plain)
            }

            HStack(spacing: 10) {
                TextField("Friend's email", text: $viewModel.inviteEmail)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .keyboardType(.emailAddress)
                    .padding(12)
                    .background(AppColors.surface)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                Button {
                    Task { await viewModel.sendInvite() }
                } label: {
                    if viewModel.isInviting {
                        ProgressView()
                            .frame(width: 52, height: 20)
                    } else {
                        Text("Send")
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundColor(.white)
                            .frame(width: 52, height: 20)
                    }
                }
                .padding(12)
                .background(AppColors.primary)
                .clipShape(RoundedRectangle(cornerRadius: 12))
                .disabled(viewModel.isInviting)
            }
            if let inviteMessage = viewModel.inviteMessage {
                Text(inviteMessage)
                    .font(.system(size: 13))
                    .foregroundColor(AppColors.textSecondary)
            }
        }
        .padding(16)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 20))
        .shadow(color: Color.black.opacity(0.06), radius: 8, y: 3)
    }

    // MARK: Leaderboard

    private func leaderboardCard(detail: ChallengeDetail) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Leaderboard")
                .font(.system(size: 15, weight: .semibold))
                .foregroundColor(AppColors.textPrimary)

            let entries = (detail.leaderboard ?? []).sorted {
                ($0.rank ?? Int.max) < ($1.rank ?? Int.max)
            }
            if entries.isEmpty {
                Text("No participants yet — invite friends to get the competition going.")
                    .font(.system(size: 14))
                    .foregroundColor(AppColors.textSecondary)
                    .padding(.vertical, 8)
            } else {
                ForEach(Array(entries.enumerated()), id: \.offset) { index, entry in
                    leaderboardRow(entry: entry, index: index, metric: detail.metric)
                    if index < entries.count - 1 {
                        Divider()
                    }
                }
            }
        }
        .padding(16)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 20))
        .shadow(color: Color.black.opacity(0.06), radius: 8, y: 3)
    }

    private func leaderboardRow(entry: LeaderboardEntry, index: Int, metric: ChallengeMetricType) -> some View {
        let rank = entry.rank ?? (index + 1)
        return HStack(spacing: 12) {
            Text("\(rank)")
                .font(.system(size: 15, weight: .bold))
                .foregroundColor(rankColor(rank))
                .frame(width: 28, height: 28)
                .background(rankColor(rank).opacity(0.12))
                .clipShape(Circle())

            Text(entry.displayName ?? "Member")
                .font(.system(size: 15, weight: rank == 1 ? .semibold : .regular))
                .foregroundColor(AppColors.textPrimary)
                .lineLimit(1)

            Spacer()

            Text("\(metric.formatValue(entry.value ?? 0)) \(metric.unitLabel)")
                .font(.system(size: 14, weight: .semibold))
                .foregroundColor(AppColors.textPrimary)
        }
        .padding(.vertical, 4)
    }

    private func rankColor(_ rank: Int) -> Color {
        switch rank {
        case 1: return AppColors.warning
        case 2: return AppColors.textSecondary
        case 3: return Color(hex: 0xFFB45309)
        default: return AppColors.textTertiary
        }
    }

    // MARK: Leave

    private var leaveButton: some View {
        Button {
            viewModel.showingLeaveConfirm = true
        } label: {
            HStack {
                Spacer()
                if viewModel.isLeaving {
                    ProgressView()
                } else {
                    Text("Leave challenge")
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundColor(AppColors.danger)
                }
                Spacer()
            }
            .padding(14)
            .background(Color.white)
            .clipShape(RoundedRectangle(cornerRadius: 20))
            .shadow(color: Color.black.opacity(0.06), radius: 8, y: 3)
        }
        .disabled(viewModel.isLeaving)
    }
}
