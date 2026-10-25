import AppIntents
import WidgetKit
import SwiftUI

/// A challenge the widget can show. The picker lists every challenge the
/// main app last synced; leaving the parameter unset means "automatic"
/// (first active challenge).
struct ChallengeEntity: AppEntity {
    static var typeDisplayRepresentation = TypeDisplayRepresentation(name: "Challenge")
    static var defaultQuery = ChallengeEntityQuery()

    var id: String
    var name: String

    var displayRepresentation: DisplayRepresentation {
        DisplayRepresentation(title: "\(name)")
    }
}

struct ChallengeEntityQuery: EntityQuery {
    func entities(for identifiers: [String]) async throws -> [ChallengeEntity] {
        let all = WidgetSharedData.loadChallenges()
        return identifiers.compactMap { id in
            all.first { "\($0.id)" == id }.map { ChallengeEntity(id: "\($0.id)", name: $0.name) }
        }
    }

    func suggestedEntities() async throws -> [ChallengeEntity] {
        WidgetSharedData.loadChallenges().map { ChallengeEntity(id: "\($0.id)", name: $0.name) }
    }

    func defaultResult() async -> ChallengeEntity? {
        nil
    }
}

struct ChallengeSelectionIntent: WidgetConfigurationIntent {
    static var title: LocalizedStringResource = "Challenge"
    static var description = IntentDescription("Choose which challenge this widget shows. Leave it unset for the first active challenge.")

    @Parameter(title: "Challenge")
    var challenge: ChallengeEntity?
}

struct ChallengesEntry: TimelineEntry {
    let date: Date
    let challenge: WidgetChallenge?
    let hasSnapshot: Bool
}

struct ChallengesProvider: AppIntentTimelineProvider {
    func placeholder(in context: Context) -> ChallengesEntry {
        ChallengesEntry(
            date: Date(),
            challenge: WidgetChallenge(
                id: 1, name: "October Steps", status: "ACTIVE", metric: "STEPS",
                participantCount: 4, myRank: 2,
                members: [
                    WidgetMember(name: "Athul", value: 48200),
                    WidgetMember(name: "Ananthu", value: 41500),
                    WidgetMember(name: "Vijin", value: 38900),
                ]
            ),
            hasSnapshot: true
        )
    }

    func snapshot(for configuration: ChallengeSelectionIntent, in context: Context) async -> ChallengesEntry {
        resolveEntry(for: configuration)
    }

    func timeline(for configuration: ChallengeSelectionIntent, in context: Context) async -> Timeline<ChallengesEntry> {
        let entry = resolveEntry(for: configuration)
        let next = Calendar.current.date(byAdding: .minute, value: 30, to: Date())!
        return Timeline(entries: [entry], policy: .after(next))
    }

    private func resolveEntry(for configuration: ChallengeSelectionIntent) -> ChallengesEntry {
        let all = WidgetSharedData.loadChallenges()
        guard !all.isEmpty else {
            return ChallengesEntry(date: Date(), challenge: nil, hasSnapshot: false)
        }
        if let selectedId = configuration.challenge?.id,
           let selected = all.first(where: { "\($0.id)" == selectedId }) {
            return ChallengesEntry(date: Date(), challenge: selected, hasSnapshot: true)
        }
        // Automatic: first active challenge, else the first synced one.
        let automatic = all.first { $0.status == "ACTIVE" } ?? all.first!
        return ChallengesEntry(date: Date(), challenge: automatic, hasSnapshot: true)
    }
}

struct ChallengesWidgetView: View {
    @Environment(\.widgetFamily) private var family
    let entry: ChallengesEntry

    var body: some View {
        WidgetCard {
            VStack(alignment: .leading, spacing: 4) {
                WidgetTitle(text: "Challenges")
                if let challenge = entry.challenge {
                    WidgetBigValue(text: challenge.name)
                    let rankText = challenge.myRank.map { " · You're #\($0)" } ?? ""
                    WidgetSubtitle(text: "\(challenge.participantCount) members · \(WidgetSharedData.metricDisplayName(challenge.metric))\(rankText)")
                    if family != .systemSmall {
                        ForEach(Array(challenge.members.prefix(3).enumerated()), id: \.offset) { index, member in
                            HStack {
                                Text("#\(index + 1) \(member.name)")
                                    .font(.system(size: 13))
                                    .foregroundStyle(.white)
                                    .lineLimit(1)
                                Spacer()
                                Text(WidgetSharedData.formatMetricValue(metric: challenge.metric, value: member.value))
                                    .font(.system(size: 13))
                                    .foregroundStyle(Color(red: 0.58, green: 0.77, blue: 0.99))
                            }
                            .padding(.top, index == 0 ? 6 : 2)
                        }
                    }
                } else {
                    WidgetBigValue(text: "—")
                    WidgetSubtitle(text: entry.hasSnapshot ? "No active challenge" : "Open GymSuit to sync")
                }
            }
        }
    }
}

struct ChallengesWidget: Widget {
    var body: some WidgetConfiguration {
        AppIntentConfiguration(
            kind: "GymsuitChallengesWidget",
            intent: ChallengeSelectionIntent.self,
            provider: ChallengesProvider()
        ) { entry in
            ChallengesWidgetView(entry: entry)
        }
        .configurationDisplayName("GymSuit Challenges")
        .description("A challenge's members and their progress. Long-press the widget to pick a different challenge.")
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}
