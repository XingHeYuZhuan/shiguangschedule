// swift-tools-version:5.9
import PackageDescription

let package = Package(
    name: "ShiguangSchedule",
    platforms: [
        .iOS(.v17)
    ],
    products: [
        .library(
            name: "ShiguangSchedule",
            targets: ["ShiguangSchedule"]
        ),
    ],
    targets: [
        .target(
            name: "ShiguangSchedule",
            resources: [
                .process("Resources")
            ]
        ),
        .testTarget(
            name: "ShiguangScheduleTests",
            dependencies: ["ShiguangSchedule"]
        ),
    ]
)
