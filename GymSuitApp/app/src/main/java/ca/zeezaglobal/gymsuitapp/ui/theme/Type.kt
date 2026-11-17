package ca.zeezaglobal.gymsuitapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import ca.zeezaglobal.gymsuitapp.R

val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val PoppinsFontName = GoogleFont("Poppins")

val PoppinsFontFamily = FontFamily(
    Font(googleFont = PoppinsFontName, fontProvider = provider, weight = FontWeight.Light),
    Font(googleFont = PoppinsFontName, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = PoppinsFontName, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = PoppinsFontName, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = PoppinsFontName, fontProvider = provider, weight = FontWeight.Bold),
    Font(googleFont = PoppinsFontName, fontProvider = provider, weight = FontWeight.ExtraBold)
)

// App-wide typography: every Material text style uses Poppins, so the whole
// app renders in Poppins. The font is fetched on demand from Google Fonts
// (already wired up above) and cached on device after the first load.
val Typography = androidx.compose.material3.Typography().let { base ->
    androidx.compose.material3.Typography(
        displayLarge = base.displayLarge.copy(fontFamily = PoppinsFontFamily),
        displayMedium = base.displayMedium.copy(fontFamily = PoppinsFontFamily),
        displaySmall = base.displaySmall.copy(fontFamily = PoppinsFontFamily),
        headlineLarge = base.headlineLarge.copy(fontFamily = PoppinsFontFamily),
        headlineMedium = base.headlineMedium.copy(fontFamily = PoppinsFontFamily),
        headlineSmall = base.headlineSmall.copy(fontFamily = PoppinsFontFamily),
        titleLarge = base.titleLarge.copy(fontFamily = PoppinsFontFamily),
        titleMedium = base.titleMedium.copy(fontFamily = PoppinsFontFamily),
        titleSmall = base.titleSmall.copy(fontFamily = PoppinsFontFamily),
        bodyLarge = base.bodyLarge.copy(fontFamily = PoppinsFontFamily),
        bodyMedium = base.bodyMedium.copy(fontFamily = PoppinsFontFamily),
        bodySmall = base.bodySmall.copy(fontFamily = PoppinsFontFamily),
        labelLarge = base.labelLarge.copy(fontFamily = PoppinsFontFamily),
        labelMedium = base.labelMedium.copy(fontFamily = PoppinsFontFamily),
        labelSmall = base.labelSmall.copy(fontFamily = PoppinsFontFamily)
    )
}