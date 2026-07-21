import SwiftUI

struct BackupRestoreView: View {
    @State private var vm = SettingsViewModel()
    @State private var showFilePicker = false
    @State private var showShareSheet = false
    @State private var backupData: Data? = nil
    @State private var showAlert = false
    @State private var alertTitle = ""
    @State private var alertMessage = ""

    var body: some View {
        List {
            Section("本地备份") {
                Button {
                    if let data = vm.exportBackup() {
                        backupData = data
                        showShareSheet = true
                    } else {
                        alertMessage = "导出失败"
                        showAlert = true
                    }
                } label: {
                    Label("导出备份", systemImage: "square.and.arrow.up")
                }

                Button {
                    showFilePicker = true
                } label: {
                    Label("导入备份", systemImage: "square.and.arrow.down")
                }
            }

            Section("WebDAV 云备份") {
                NavigationLink("WebDAV 配置") {
                    WebDAVConfigView()
                }

                Button {
                    Task {
                        await uploadBackup()
                    }
                } label: {
                    Label("上传到 WebDAV", systemImage: "icloud.and.arrow.up")
                }

                Button {
                    Task {
                        await downloadBackup()
                    }
                } label: {
                    Label("从 WebDAV 恢复", systemImage: "icloud.and.arrow.down")
                }
            }

            Section("说明") {
                Text("备份文件包含当前课表的所有课程、节次和配置信息。")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
        .navigationTitle("备份与恢复")
        .onAppear { vm.load() }
        .alert(alertTitle, isPresented: $showAlert) {
            Button("确定", role: .cancel) {}
        } message: {
            Text(alertMessage)
        }
        .fileImporter(
            isPresented: $showFilePicker,
            allowedContentTypes: [.json],
            allowsMultipleSelection: false
        ) { result in
            switch result {
            case .success(let urls):
                if let url = urls.first {
                    do {
                        let data = try Data(contentsOf: url)
                        if vm.importBackup(data: data) {
                            alertTitle = "成功"
                            alertMessage = "备份导入成功"
                        } else {
                            alertTitle = "失败"
                            alertMessage = "备份文件格式不正确"
                        }
                    } catch {
                        alertTitle = "错误"
                        alertMessage = error.localizedDescription
                    }
                    showAlert = true
                }
            case .failure(let error):
                alertTitle = "错误"
                alertMessage = error.localizedDescription
                showAlert = true
            }
        }
        .sheet(isPresented: $showShareSheet) {
            if let data = backupData {
                ShareSheet(activityItems: [data])
            }
        }
    }

    private func uploadBackup() async {
        guard let data = vm.exportBackup() else {
            alertMessage = "导出数据失败"
            showAlert = true
            return
        }
        do {
            let fileName = vm.backupFileName
            try await WebDAVService.shared.ensureDirectoryExists()
            try await WebDAVService.shared.upload(data: data, fileName: fileName)
            alertTitle = "成功"
            alertMessage = "备份已上传到 WebDAV"
        } catch {
            alertTitle = "上传失败"
            alertMessage = error.localizedDescription
        }
        showAlert = true
    }

    private func downloadBackup() async {
        do {
            let files = try await WebDAVService.shared.listFiles()
            let backupFiles = files.filter { $0.hasPrefix("backup_") }.sorted(by: >)
            guard let latest = backupFiles.first else {
                alertTitle = "提示"
                alertMessage = "没有找到备份文件"
                showAlert = true
                return
            }
            let data = try await WebDAVService.shared.download(fileName: latest)
            if vm.importBackup(data: data) {
                alertTitle = "成功"
                alertMessage = "已从 \(latest) 恢复数据"
            } else {
                alertTitle = "失败"
                alertMessage = "备份文件格式不正确"
            }
        } catch {
            alertTitle = "下载失败"
            alertMessage = error.localizedDescription
        }
        showAlert = true
    }
}

struct WebDAVConfigView: View {
    @AppStorage("webdav_baseUrl") private var baseUrl = ""
    @AppStorage("webdav_username") private var username = ""
    @AppStorage("webdav_password") private var password = ""
    @AppStorage("webdav_rootPath") private var rootPath = "ShiguangSchedule"
    @State private var showTestResult = false
    @State private var testSuccess = false

    var body: some View {
        Form {
            Section("服务器配置") {
                TextField("服务器地址", text: $baseUrl)
                    .autocapitalization(.none)
                    .disableAutocorrection(true)
                TextField("用户名", text: $username)
                    .autocapitalization(.none)
                    .disableAutocorrection(true)
                SecureField("密码", text: $password)
                TextField("根路径", text: $rootPath)
                    .autocapitalization(.none)
            }

            Section {
                Button("测试连接") {
                    testConnection()
                }
            }

            if showTestResult {
                Section {
                    HStack {
                        Image(systemName: testSuccess ? "checkmark.circle.fill" : "xmark.circle.fill")
                            .foregroundColor(testSuccess ? .green : .red)
                        Text(testSuccess ? "连接成功" : "连接失败")
                    }
                }
            }

            Section("说明") {
                Text("支持任何 WebDAV 服务，如坚果云、NextCloud 等。请填写完整的服务器地址。")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
        .navigationTitle("WebDAV 配置")
    }

    private func testConnection() {
        let config = WebDAVConfig(
            baseUrl: baseUrl,
            username: username,
            password: password,
            rootPath: rootPath
        )
        Task {
            await WebDAVService.shared.configure(config)
            do {
                try await WebDAVService.shared.ensureDirectoryExists()
                testSuccess = true
            } catch {
                testSuccess = false
            }
            showTestResult = true
        }
    }
}

struct ShareSheet: UIViewControllerRepresentable {
    let activityItems: [Any]

    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: activityItems, applicationActivities: nil)
    }

    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}
