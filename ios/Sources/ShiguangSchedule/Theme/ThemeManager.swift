import SwiftUI

struct ThemeManager {
    static func resolvedColorScheme() -> ColorScheme? {
        switch AppSettings.shared.themeModeValue {
        case .light: return .light
        case .dark: return .dark
        case .followSystem: return nil
        }
    }

    static func accentColor(for scheme: ColorScheme) -> Color {
        let settings = AppSettings.shared
        if !settings.useDynamicColor {
            let hex = scheme == .dark ? settings.customDarkPrimaryHex : settings.customLightPrimaryHex
            if !hex.isEmpty {
                let scanner = Scanner(string: hex)
                var value: UInt64 = 0
                if scanner.scanHexInt64(&value) {
                    return Color(hex: Int64(value))
                }
            }
        }
        return .accentColor
    }
}

struct ThemeModifier: ViewModifier {
    @ObservedObject private var settings = AppSettings.shared

    func body(content: Content) -> some View {
        content
            .preferredColorScheme(ThemeManager.resolvedColorScheme())
    }
}

extension View {
    func applyTheme() -> some View {
        modifier(ThemeModifier())
    }
}
