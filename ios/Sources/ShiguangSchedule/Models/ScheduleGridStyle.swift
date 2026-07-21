import Foundation
import SwiftUI

struct DualColor: Codable, Equatable {
    var lightColor: Int64
    var darkColor: Int64

    var light: Color { Color(hex: lightColor) }
    var dark: Color { Color(hex: darkColor) }

    func color(for colorScheme: ColorScheme) -> Color {
        colorScheme == .dark ? dark : light
    }
}

enum BorderType: String, Codable, CaseIterable {
    case none
    case solid
    case dashed
}

enum ScheduleMode: String, Codable, CaseIterable {
    case sectionMode
    case time24hMode
}

struct ScheduleGridStyle: Codable, Equatable {
    var timeColumnWidth: CGFloat?
    var dayHeaderHeight: CGFloat?
    var sectionHeight: CGFloat?
    var courseBlockCornerRadius: CGFloat?
    var courseBlockOuterPadding: CGFloat?
    var courseBlockInnerPadding: CGFloat?
    var courseBlockAlpha: Double?
    var courseColorMaps: [DualColor] = Self.defaultColors
    var hideSectionTime: Bool?
    var hideDateUnderDay: Bool?
    var showStartTime: Bool?
    var hideGridLines: Bool?
    var pageTextColor: Int64?
    var courseBlockFontScale: Double?
    var hideLocation: Bool?
    var hideTeacher: Bool?
    var removeLocationAt: Bool?
    var textAlignCenterHorizontal: Bool?
    var textAlignCenterVertical: Bool?
    var borderType: BorderType?
    var courseTextColor: Int64?
    var scheduleMode: ScheduleMode?
    var backgroundImagePath: String?

    static let defaultColors: [DualColor] = [
        DualColor(lightColor: 0xFF4A90D9, darkColor: 0xFF5BA3E6),
        DualColor(lightColor: 0xFF50B86C, darkColor: 0xFF66D482),
        DualColor(lightColor: 0xFFE8913A, darkColor: 0xFFF5A550),
        DualColor(lightColor: 0xFFE06060, darkColor: 0xFFEE7A7A),
        DualColor(lightColor: 0xFF9B59B6, darkColor: 0xFFB07CC6),
        DualColor(lightColor: 0xFF1ABC9C, darkColor: 0xFF3DD6B4),
        DualColor(lightColor: 0xFFE67E22, darkColor: 0xFFF0994A),
        DualColor(lightColor: 0xFF2ECC71, darkColor: 0xFF52D98B),
        DualColor(lightColor: 0xFFE74C3C, darkColor: 0xFFF06A5C),
        DualColor(lightColor: 0xFF3498DB, darkColor: 0xFF5BADE2),
        DualColor(lightColor: 0xFFF1C40F, darkColor: 0xFFF5D643),
        DualColor(lightColor: 0xFF9E9E9E, darkColor: 0xFFBDBDBD),
    ]

    static let `default` = ScheduleGridStyle()
}

extension Color {
    init(hex: Int64) {
        let r = Double((hex >> 16) & 0xFF) / 255.0
        let g = Double((hex >> 8) & 0xFF) / 255.0
        let b = Double(hex & 0xFF) / 255.0
        let a = Double((hex >> 24) & 0xFF) / 255.0
        self.init(red: r, green: g, blue: b, opacity: a)
    }

    var hex: Int64 {
        guard let components = UIColor(self).cgColor.components, components.count >= 3 else { return 0 }
        let r = Int(components[0] * 255.0)
        let g = Int(components[1] * 255.0)
        let b = Int(components[2] * 255.0)
        let a = Int((components.count >= 4 ? components[3] : 1.0) * 255.0)
        return Int64((a << 24) | (r << 16) | (g << 8) | b)
    }
}
