package ca.zeezaglobal.gymsuitapp.data.remote

import org.json.JSONObject

/**
 * Turns server and network failures into one short, user-facing line.
 * Raw error bodies (Cloudflare JSON, stack traces, database messages) must
 * never reach the UI — screens show the message this returns, nothing else.
 * The full bodies are still written to Logcat by the callers for debugging.
 */
internal object ServerErrors {

    /**
     * Short message for an HTTP error response. [body] is the raw response text.
     */
    fun forStatus(responseCode: Int, body: String?): String {
        // 5xx (e.g. Cloudflare 530 when the tunnel is down): never trust the body.
        if (responseCode >= 500) return "Server is down"
        // The app fires its re-login flow on 401; Spring's own 401 body is technical.
        if (responseCode == 401) return "Session expired. Please log in again."
        // The backend's own ApiError.message is a proper short message when present.
        val serverMessage = body?.let { text ->
            runCatching { JSONObject(text).optString("message", "") }.getOrNull()
        }?.trim().orEmpty()
        if (serverMessage.isNotEmpty() && serverMessage.length <= 120) return serverMessage
        return when (responseCode) {
            403 -> "You don't have permission for this."
            404 -> "Not found."
            409 -> "This already exists."
            413 -> "That file is too large."
            422 -> "Please check the details and try again."
            429 -> "Too many requests. Please wait a moment and try again."
            else -> "Something went wrong. Please try again."
        }
    }

    /**
     * Short message when the request never reached the server
     * (phone offline, DNS failure, connection timeout).
     */
    fun forNetworkError(): String =
        "Couldn't reach the server. Check your connection and try again."
}
