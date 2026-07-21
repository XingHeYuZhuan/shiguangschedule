import SwiftUI

struct StyleSettingsView: View {
    @State private var style = ScheduleGridStyle.default
    @State private var previewColors = ScheduleGridStyle.defaultColors

    var body: some View {
        List {
            Section("课程块样式") {
                VStack {
                    Text("圆角: \(Int(style.courseBlockCornerRadius ?? 6))")
                        .font(.caption)
                    Slider(value: Binding(
                        get: { style.courseBlockCornerRadius ?? 6 },
                        set: { style.courseBlockCornerRadius = $0 }
                    ), in: 0...20, step: 1)
                }

                VStack {
                    Text("透明度: \(Int((style.courseBlockAlpha ?? 0.85) * 100))%")
                        .font(.caption)
                    Slider(value: Binding(
                        get: { style.courseBlockAlpha ?? 0.85 },
                        set: { style.courseBlockAlpha = $0 }
                    ), in: 0.3...1.0, step: 0.05)
                }

                Toggle("隐藏位置", isOn: Binding(
                    get: { style.hideLocation ?? false },
                    set: { style.hideLocation = $0 }
                ))

                Toggle("隐藏教师", isOn: Binding(
                    get: { style.hideTeacher ?? false },
                    set: { style.hideTeacher = $0 }
                ))
            }

            Section("网格样式") {
                Toggle("隐藏网格线", isOn: Binding(
                    get: { style.hideGridLines ?? false },
                    set: { style.hideGridLines = $0 }
                ))

                Toggle("隐藏节次时间", isOn: Binding(
                    get: { style.hideSectionTime ?? false },
                    set: { style.hideSectionTime = $0 }
                ))

                Toggle("日期下隐藏日期", isOn: Binding(
                    get: { style.hideDateUnderDay ?? false },
                    set: { style.hideDateUnderDay = $0 }
                ))
            }

            Section("颜色方案") {
                ForEach(previewColors.indices, id: \.self) { index in
                    HStack {
                        Circle()
                            .fill(Color(hex: previewColors[index].lightColor))
                            .frame(width: 24, height: 24)
                        Circle()
                            .fill(Color(hex: previewColors[index].darkColor))
                            .frame(width: 24, height: 24)
                        Text("颜色 \(index + 1)")
                            .font(.caption)
                        Spacer()
                    }
                }
            }

            Section("文字对齐") {
                Toggle("水平居中", isOn: Binding(
                    get: { style.textAlignCenterHorizontal ?? false },
                    set: { style.textAlignCenterHorizontal = $0 }
                ))
                Toggle("垂直居中", isOn: Binding(
                    get: { style.textAlignCenterVertical ?? false },
                    set: { style.textAlignCenterVertical = $0 }
                ))
            }
        }
        .navigationTitle("样式设置")
    }
}
