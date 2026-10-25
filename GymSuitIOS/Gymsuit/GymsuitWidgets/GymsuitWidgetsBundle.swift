import WidgetKit
import SwiftUI

@main
struct GymsuitWidgetsBundle: WidgetBundle {
    var body: some Widget {
        StepsWidget()
        HeartRateWidget()
        WorkoutsWidget()
        ChallengesWidget()
    }
}
