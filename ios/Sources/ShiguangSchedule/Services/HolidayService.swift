import Foundation

actor HolidayService {
    static let shared = HolidayService()
    private let baseURL = "https://timor.tech/api/holiday/year"

    private var cache: [Int: [String: Bool]] = [:]

    func fetchHolidays(year: Int) async throws -> [String: Bool] {
        if let cached = cache[year] {
            return cached
        }

        guard let url = URL(string: "\(baseURL)/\(year)") else {
            throw URLError(.badURL)
        }

        var request = URLRequest(url: url)
        request.timeoutInterval = 15
        request.setValue("ShiguangSchedule-iOS/1.0", forHTTPHeaderField: "User-Agent")

        let (data, _) = try await URLSession.shared.data(for: request)
        let decoder = JSONDecoder()
        let response = try decoder.decode(HolidayResponse.self, from: data)

        let holidays = response.holiday.mapValues { info in
            info.holiday
        }
        cache[year] = holidays
        return holidays
    }

    func isHoliday(date: Date, holidays: [String: Bool]) -> Bool {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        let dateStr = formatter.string(from: date)
        return holidays[dateStr] == true
    }

    func isHoliday(dateStr: String, holidays: [String: Bool]) -> Bool {
        holidays[dateStr] == true
    }
}

struct HolidayResponse: Codable {
    let holiday: [String: HolidayInfo]
    let code: Int?
}

struct HolidayInfo: Codable {
    let holiday: Bool
    let name: String?
    let date: String?
    let rest: Int?
    let wage: Int?
    let after: String?
    let target: String?
}
