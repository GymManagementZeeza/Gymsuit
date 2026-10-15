package ca.zeezaglobal.gymsuitapp.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import ca.zeezaglobal.gymsuitapp.R
import ca.zeezaglobal.gymsuitapp.data.local.AuthManager

/**
 * One stable avatar per member.
 *
 * Previously the dashboard picked `gazeAvatars.random()` on every cold start,
 * so a member's face changed on each app open — and the detail screens
 * hardcoded `avatar_gaze_1`, so they never matched the dashboard either.
 *
 * The avatar is now derived deterministically from the member's stable id
 * (falling back to email, then a guest default): same member -> same face on
 * every launch, on every screen, on every device. No storage needed.
 */
object MemberAvatar {

    private val gazeAvatars = listOf(
        R.drawable.avatar_gaze_1,
        R.drawable.avatar_gaze_2,
        R.drawable.avatar_gaze_3,
        R.drawable.avatar_gaze_4,
        R.drawable.avatar_gaze_5,
        R.drawable.avatar_gaze_6,
        R.drawable.avatar_gaze_7,
        R.drawable.avatar_gaze_8
    )

    @DrawableRes
    fun forMember(memberId: Long?, email: String?): Int {
        val key = memberId?.toString()
            ?: email?.takeIf { it.isNotBlank() }
            ?: "guest"
        // String.hashCode() is specified and stable across runs/devices.
        val index = (key.hashCode() and Int.MAX_VALUE) % gazeAvatars.size
        return gazeAvatars[index]
    }
}

/** Stable avatar for the currently logged-in member (guest default when logged out). */
@Composable
fun rememberMemberAvatar(): Int {
    val context = LocalContext.current
    return remember {
        val session = AuthManager(context).getSession()
        MemberAvatar.forMember(session?.memberId, session?.email)
    }
}
