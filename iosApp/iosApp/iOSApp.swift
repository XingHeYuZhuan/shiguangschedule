import SwiftUI
import Network
import WidgetKit

private final class LocalNetworkPermissionRequester {
    private let queue = DispatchQueue(label: "com.xingheyuzhuan.shiguangschedule.local-network-permission", qos: .utility)
    private var browser: NWBrowser?

    func request() {
        queue.async { [weak self] in
            self?.start()
        }
    }

    private func start() {
        guard browser == nil else { return }

        let descriptor = NWBrowser.Descriptor.bonjour(type: "_webdav._tcp", domain: nil)
        let browser = NWBrowser(for: descriptor, using: .tcp)
        self.browser = browser

        browser.stateUpdateHandler = { [weak self] state in
            switch state {
            case .failed, .cancelled:
                self?.stop()
            default:
                break
            }
        }
        browser.start(queue: queue)

        queue.asyncAfter(deadline: .now() + 3) { [weak self] in
            self?.stop()
        }
    }

    private func stop() {
        browser?.cancel()
        browser = nil
    }
}

@main
struct iOSApp: App {
    private let localNetworkPermissionRequester = LocalNetworkPermissionRequester()
    private let widgetRefreshObserver = WidgetRefreshObserver()

    init() {
        localNetworkPermissionRequester.request()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}

private final class WidgetRefreshObserver {
    private var token: NSObjectProtocol?

    init() {
        token = NotificationCenter.default.addObserver(
            forName: Notification.Name("IosWidgetSnapshotUpdated"),
            object: nil,
            queue: .main
        ) { _ in
            WidgetCenter.shared.reloadAllTimelines()
        }
    }

    deinit {
        if let token {
            NotificationCenter.default.removeObserver(token)
        }
    }
}