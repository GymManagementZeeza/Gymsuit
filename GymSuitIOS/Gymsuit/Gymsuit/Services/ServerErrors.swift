import Foundation

/// Turns server and network failures into one short, user-facing line.
/// Raw error bodies (Cloudflare JSON, stack traces, database messages) must
/// never reach the UI — screens show the message this returns, nothing else.
enum ServerErrors {

    /// Short message for an HTTP error response. `body` is the raw response data.
    static func message(forStatusCode statusCode: Int, body: Data?) -> String {
        // 5xx (e.g. Cloudflare 530 when the tunnel is down): never trust the body.
        if statusCode >= 500 { return "Server is down" }
        // The app fires its re-login flow on 401; Spring's own 401 body is technical.
        if statusCode == 401 { return "Session expired. Please log in again." }
        // The backend's own ApiError.message is a proper short message when present.
        if let body = body,
           let json = try? JSONSerialization.jsonObject(with: body) as? [String: Any],
           let serverMessage = (json["message"] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines),
           !serverMessage.isEmpty, serverMessage.count <= 120 {
            return serverMessage
        }
        switch statusCode {
        case 403: return "You don't have permission for this."
        case 404: return "Not found."
        case 409: return "This already exists."
        case 413: return "That file is too large."
        case 422: return "Please check the details and try again."
        case 429: return "Too many requests. Please wait a moment and try again."
        default: return "Something went wrong. Please try again."
        }
    }

    /// Short message when the request never reached the server
    /// (phone offline, DNS failure, connection timeout).
    static func networkErrorMessage() -> String {
        "Couldn't reach the server. Check your connection and try again."
    }
}
