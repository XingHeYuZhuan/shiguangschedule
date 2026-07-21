import SwiftUI

struct ThemeSettingsView: View {
    @State private var settings = AppSettings.shared

    var body: some View {
        List {
            Section("主题模式") {
                ForEach(AppThemeMode.allCases, id: \.self) { mode in
                    Button {
                        settings.themeModeValue = mode
                    } label: {
                        HStack {
                            Text(mode.displayName)
                                .foregroundColor(.primary)
                            Spacer()
                            if settings.themeModeValue == mode {
                                Image(systemName: "checkmark")
                                    .foregroundColor(.accentColor)
                            }
                        }
                    }
                }
            }

            Section("动态颜色") {
                Toggle("使用动态颜色", isOn: $settings.useDynamicColor)

                if !settings.useDynamicColor {
                    ColorPicker("浅色主题主色",
                                selection: Binding(
                                    get: { colorFromHex(settings.customLightPrimaryHex) ?? .blue },
                                    set: { settings.customLightPrimaryHex = $0.hexString }
                                ))
                    ColorPicker("深色主题主色",
                                selection: Binding(
                                    get: { colorFromHex(settings.customDarkPrimaryHex) ?? .blue },
                                    set: { settings.customDarkPrimaryHex = $0.hexString }
                                ))
                }
            }

            Section("启动屏幕") {
                ForEach(StartScreen.allCases, id: \.self) { screen in
                    Button {
                        settings.startScreenValue = screen
                    } label: {
                        HStack {
                            Text(screen.displayName)
                                .foregroundColor(.primary)
                            Spacer()
                            if settings.startScreenValue == screen {
                                Image(systemName: "checkmark")
                                    .foregroundColor(.accentColor)
                            }
                        }
                    }
                }
            }
        }
        .navigationTitle("主题设置")
    }

    private func colorFromHex(_ hex: String) -> Color? {
        guard !hex.isEmpty else { return nil }
        let scanner = Scanner(string: hex)
        var value: UInt64 = 0
        guard scanner.scanHexInt64(&value) else { return nil }
        return Color(hex: Int64(value))
    }
}

extension Color {
    var hexString: String {
        let uiColor = UIColor(self)
        var r: CGFloat = 0
        var g: CGFloat = 0
        var b: CGFloat = 0
        var a: CGFloat = 0
        uiColor.getRed(&r, green: &g, blue: &b, alpha: &a)
        return String(format: "%02X%02X%02X", Int(r * 255), Int(g * 255), Int(b * 255))
    }
}
