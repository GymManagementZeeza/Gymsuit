package ca.zeezaglobal.gymsuitapp.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import org.json.JSONObject

data class UserSession(
    val token: String,
    val refreshToken: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val role: String,
    val memberId: Long?,
    val gymId: Long?,
    val gymName: String?
)

class AuthManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "gymsuit_auth_prefs"
        private const val KEY_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_EMAIL = "user_email"
        private const val KEY_FIRST_NAME = "first_name"
        private const val KEY_LAST_NAME = "last_name"
        private const val KEY_ROLE = "user_role"
        private const val KEY_MEMBER_ID = "member_id"
        private const val KEY_GYM_ID = "gym_id"
        private const val KEY_GYM_NAME = "gym_name"

        val unauthorizedEvent = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    }

    fun saveSession(session: UserSession) {
        prefs.edit()
            .putString(KEY_TOKEN, session.token)
            .putString(KEY_REFRESH_TOKEN, session.refreshToken)
            .putString(KEY_EMAIL, session.email)
            .putString(KEY_FIRST_NAME, session.firstName)
            .putString(KEY_LAST_NAME, session.lastName)
            .putString(KEY_ROLE, session.role)
            .apply {
                if (session.memberId != null) putLong(KEY_MEMBER_ID, session.memberId) else remove(KEY_MEMBER_ID)
                if (session.gymId != null) putLong(KEY_GYM_ID, session.gymId) else remove(KEY_GYM_ID)
                if (session.gymName != null) putString(KEY_GYM_NAME, session.gymName) else remove(KEY_GYM_NAME)
            }
            .apply()
    }

    fun getAccessToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

    fun isLoggedIn(): Boolean = !getAccessToken().isNullOrBlank()

    /**
     * True when the stored access token is expired or will expire within
     * [graceSeconds]. Reads only the JWT "exp" claim (no signature verification —
     * this is a client-side freshness hint; the server remains authoritative).
     * Returns false for tokens we cannot parse, so an unfamiliar token format
     * never triggers a logout on its own — the 401 path still handles those.
     */
    fun isAccessTokenExpired(graceSeconds: Long = 300): Boolean {
        val token = getAccessToken() ?: return true
        return try {
            val parts = token.split(".")
            if (parts.size != 3) return false
            val payload = String(
                Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP),
                Charsets.UTF_8
            )
            val exp = JSONObject(payload).optLong("exp", -1L)
            if (exp <= 0L) return false
            (System.currentTimeMillis() / 1000) + graceSeconds >= exp
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Silently renews the session with the stored refresh token. Rotation-safe:
     * always reads the latest stored token, and [saveSession] persists the
     * rotated token the server returns. Returns true when the session is usable
     * afterwards. Never clears storage — the caller decides what a failed
     * refresh means (e.g. offline vs. genuinely revoked).
     */
    suspend fun refreshSession(refresh: suspend (String) -> Result<UserSession>): Boolean {
        val refreshToken = getRefreshToken()
        if (refreshToken.isNullOrBlank()) return false
        val session = try {
            refresh(refreshToken).getOrNull()
        } catch (e: Exception) {
            null
        } ?: return false
        if (session.token.isBlank()) return false
        saveSession(session)
        return true
    }

    fun getSession(): UserSession? {
        val token = getAccessToken() ?: return null
        val refreshToken = getRefreshToken() ?: ""
        return UserSession(
            token = token,
            refreshToken = refreshToken,
            email = prefs.getString(KEY_EMAIL, "") ?: "",
            firstName = prefs.getString(KEY_FIRST_NAME, "") ?: "",
            lastName = prefs.getString(KEY_LAST_NAME, "") ?: "",
            role = prefs.getString(KEY_ROLE, "MEMBER") ?: "MEMBER",
            memberId = if (prefs.contains(KEY_MEMBER_ID)) prefs.getLong(KEY_MEMBER_ID, -1L) else null,
            gymId = if (prefs.contains(KEY_GYM_ID)) prefs.getLong(KEY_GYM_ID, -1L) else null,
            gymName = prefs.getString(KEY_GYM_NAME, null)
        )
    }

    fun updateName(firstName: String, lastName: String) {
        prefs.edit()
            .putString(KEY_FIRST_NAME, firstName.trim())
            .putString(KEY_LAST_NAME, lastName.trim())
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
