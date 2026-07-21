import Foundation

struct WebDAVConfig: Codable {
    var baseUrl: String
    var username: String
    var password: String
    var rootPath: String = "ShiguangSchedule"

    var cleanRootPath: String {
        let trimmed = rootPath.trimmingCharacters(in: CharacterSet(charactersIn: "/ "))
        return trimmed.isEmpty ? "" : "\(trimmed)/"
    }
}

actor WebDAVService {
    static let shared = WebDAVService()
    private var config: WebDAVConfig?

    func configure(_ config: WebDAVConfig) {
        self.config = config
    }

    private func request(_ path: String, method: String, data: Data? = nil) async throws -> Data {
        guard let config = config else { throw WebDAVError.notConfigured }
        guard let baseURL = URL(string: config.baseUrl.hasSuffix("/") ? config.baseUrl : "\(config.baseUrl)/") else {
            throw WebDAVError.invalidURL
        }

        let fullURL = baseURL.appendingPathComponent(path)
        var request = URLRequest(url: fullURL)
        request.httpMethod = method
        request.timeoutInterval = 30

        let authStr = "\(config.username):\(config.password)"
        if let authData = authStr.data(using: .utf8) {
            request.setValue("Basic \(authData.base64EncodedString())", forHTTPHeaderField: "Authorization")
        }

        if let data = data {
            request.httpBody = data
            request.setValue("application/octet-stream", forHTTPHeaderField: "Content-Type")
        }

        let (responseData, response) = try await URLSession.shared.data(for: request)
        guard let httpResponse = response as? HTTPURLResponse else {
            throw WebDAVError.invalidResponse
        }
        if (200...299).contains(httpResponse.statusCode) {
            return responseData
        } else if httpResponse.statusCode == 404 {
            throw WebDAVError.notFound
        } else if httpResponse.statusCode == 401 || httpResponse.statusCode == 403 {
            throw WebDAVError.unauthorized
        } else {
            throw WebDAVError.serverError(httpResponse.statusCode)
        }
    }

    func ensureDirectoryExists() async throws {
        guard let config = config else { throw WebDAVError.notConfigured }
        let pathComponents = config.cleanRootPath.split(separator: "/").map(String.init)
        var currentPath = ""
        for component in pathComponents {
            currentPath = currentPath.isEmpty ? component : "\(currentPath)/\(component)"
            try? await makeCollection(path: currentPath)
        }
    }

    private func makeCollection(path: String) async throws {
        guard let config = config else { throw WebDAVError.notConfigured }
        guard let baseURL = URL(string: config.baseUrl.hasSuffix("/") ? config.baseUrl : "\(config.baseUrl)/") else {
            throw WebDAVError.invalidURL
        }

        let fullURL = baseURL.appendingPathComponent(path)
        var request = URLRequest(url: fullURL)
        request.httpMethod = "MKCOL"
        request.timeoutInterval = 15

        let authStr = "\(config.username):\(config.password)"
        if let authData = authStr.data(using: .utf8) {
            request.setValue("Basic \(authData.base64EncodedString())", forHTTPHeaderField: "Authorization")
        }

        let (_, response) = try await URLSession.shared.data(for: request)
        guard let httpResponse = response as? HTTPURLResponse else {
            throw WebDAVError.invalidResponse
        }
        if httpResponse.statusCode == 405 { return }
        if !(200...299).contains(httpResponse.statusCode) {
            throw WebDAVError.serverError(httpResponse.statusCode)
        }
    }

    func upload(data: Data, fileName: String) async throws {
        guard let config = config else { throw WebDAVError.notConfigured }
        let path = "\(config.cleanRootPath)ShiguangSchedule/\(fileName)"
        _ = try await request(path, method: "PUT", data: data)
    }

    func download(fileName: String) async throws -> Data {
        guard let config = config else { throw WebDAVError.notConfigured }
        let path = "\(config.cleanRootPath)ShiguangSchedule/\(fileName)"
        return try await request(path, method: "GET")
    }

    func listFiles() async throws -> [String] {
        guard let config = config else { throw WebDAVError.notConfigured }
        let path = "\(config.cleanRootPath)ShiguangSchedule/"
        let data = try await request(path, method: "PROPFIND")
        let xml = String(data: data, encoding: .utf8) ?? ""
        return parseFileNames(from: xml)
    }

    private func parseFileNames(from xml: String) -> [String] {
        var files: [String] = []
        let pattern = "<D:href>([^<]+)</D:href>"
        guard let regex = try? NSRegularExpression(pattern: pattern) else { return [] }
        let matches = regex.matches(in: xml, range: NSRange(xml.startIndex..., in: xml))
        for match in matches.dropFirst() {
            if let range = Range(match.range(at: 1), in: xml) {
                let href = String(xml[range])
                let fileName = href.split(separator: "/").last.map(String.init) ?? href
                if !fileName.isEmpty {
                    files.append(fileName)
                }
            }
        }
        return files
    }

    func generateBackupFileName() -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyyMMdd_HHmmss"
        return "backup_\(formatter.string(from: Date())).json"
    }
}

enum WebDAVError: LocalizedError {
    case notConfigured
    case invalidURL
    case invalidResponse
    case notFound
    case unauthorized
    case serverError(Int)

    var errorDescription: String? {
        switch self {
        case .notConfigured: return "WebDAV 未配置"
        case .invalidURL: return "无效的 URL"
        case .invalidResponse: return "无效的服务器响应"
        case .notFound: return "文件未找到"
        case .unauthorized: return "认证失败，请检查用户名和密码"
        case .serverError(let code): return "服务器错误: \(code)"
        }
    }
}
