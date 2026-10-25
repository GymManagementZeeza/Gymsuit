import WidgetKit
import SwiftUI

/// Dark widget card shared by all four GymSuit widgets.
struct WidgetCard<Content: View>: View {
    let content: Content

    init(@ViewBuilder content: () -> Content) {
        self.content = content()
    }

    var body: some View {
        content
            .padding(14)
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
            .containerBackground(Color(red: 0.07, green: 0.10, blue: 0.15), for: .widget)
    }
}

struct WidgetTitle: View {
    let text: String

    var body: some View {
        Text(text)
            .font(.caption)
            .foregroundStyle(Color(red: 0.65, green: 0.68, blue: 0.72))
    }
}

struct WidgetBigValue: View {
    let text: String

    var body: some View {
        Text(text)
            .font(.system(size: 24, weight: .bold))
            .foregroundStyle(.white)
            .lineLimit(1)
            .minimumScaleFactor(0.7)
    }
}

struct WidgetSubtitle: View {
    let text: String

    var body: some View {
        Text(text)
            .font(.caption)
            .foregroundStyle(Color(red: 0.82, green: 0.84, blue: 0.86))
            .lineLimit(1)
    }
}
