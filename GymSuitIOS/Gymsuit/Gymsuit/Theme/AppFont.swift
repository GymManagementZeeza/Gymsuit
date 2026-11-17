import SwiftUI
import UIKit
import CoreText

/// App-wide font: Poppins for all text (mirrors Android's Type.kt).
///
/// The Poppins .ttf files ship in Resources/Fonts and are registered at
/// launch, so no Info.plist `UIAppFonts` entry is needed. Views use
/// `.font(.app(size:weight:))`, a drop-in for `.font(.system(size:weight:))`
/// that keeps the same fixed sizes.
enum AppFont {
    private static let family = "Poppins"

    /// Registers the bundled Poppins faces and points the navigation bar
    /// titles at them. Call once at launch.
    static func register() {
        let urls = (Bundle.main.urls(forResourcesWithExtension: "ttf", subdirectory: nil) ?? [])
            + (Bundle.main.urls(forResourcesWithExtension: "ttf", subdirectory: "Fonts") ?? [])
        for url in urls where url.lastPathComponent.hasPrefix(family) {
            CTFontManagerRegisterFontsForURL(url as CFURL, .process, nil)
        }

        let appearance = UINavigationBar.appearance()
        if let title = UIFont(name: postScriptName(for: .semibold), size: 17) {
            appearance.titleTextAttributes = [.font: title]
        }
        if let large = UIFont(name: postScriptName(for: .bold), size: 34) {
            appearance.largeTitleTextAttributes = [.font: large]
        }
    }

    static func postScriptName(for weight: Font.Weight) -> String {
        switch weight {
        case .ultraLight, .thin, .light: return "\(family)-Light"
        case .medium: return "\(family)-Medium"
        case .semibold: return "\(family)-SemiBold"
        case .bold: return "\(family)-Bold"
        case .heavy, .black: return "\(family)-ExtraBold"
        default: return "\(family)-Regular"
        }
    }
}

extension Font {
    /// Poppins at a fixed size. Same parameters as `Font.system(size:weight:design:)`;
    /// `.monospaced` keeps the system monospaced face (Poppins has none).
    static func app(size: CGFloat, weight: Font.Weight = .regular, design: Font.Design? = nil) -> Font {
        if design == .monospaced {
            return .system(size: size, weight: weight, design: .monospaced)
        }
        return .custom(AppFont.postScriptName(for: weight), fixedSize: size)
    }
}
