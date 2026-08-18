package com.utags.androidpc.CensusHub.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.utags.androidpc.CensusHub.R

// ── Figtree font family (bundled TTFs) ──────────────────────────────────────
val Figtree = FontFamily(
    Font(R.font.figtree_regular, FontWeight.Normal),
    Font(R.font.figtree_medium,  FontWeight.Medium),
    Font(R.font.figtree_bold,    FontWeight.Bold),
)

// ── UTAGS brand palette ──────────────────────────────────────────────────────
private val UtaGreen       = Color(0xFF00BB47)  // primary brand green
private val UtaGreenDark   = Color(0xFF2B532D)  // dark forest green
private val UtaLime        = Color(0xFFB1F946)  // lime accent
private val UtaCream       = Color(0xFFF2F3EA)  // off-white background
private val UtaGreenMid    = Color(0xFF3A7A3E)  // mid green for containers
private val UtaGreenDeep   = Color(0xFF0E1F0F)  // near-black green for dark bg

private val LightColors = lightColorScheme(
    primary             = UtaGreen,
    onPrimary           = Color.White,
    primaryContainer    = Color(0xFFD6F5E3),
    onPrimaryContainer  = UtaGreenDark,
    secondary           = UtaGreenDark,
    onSecondary         = Color.White,
    secondaryContainer  = Color(0xFFCCE8CF),
    onSecondaryContainer = UtaGreenDark,
    tertiary            = Color(0xFF5A8F2A),
    onTertiary          = Color.White,
    background          = UtaCream,
    surface             = Color.White,
    surfaceVariant      = Color(0xFFE8EDE0),
    onBackground        = Color(0xFF1A1A1A),
    onSurface           = Color(0xFF1A1A1A),
    onSurfaceVariant    = Color(0xFF3A3A3A),
    error               = Color(0xFFD32F2F),
    onError             = Color.White,
    outline             = Color(0xFFADBAA0),
)

private val DarkColors = darkColorScheme(
    primary             = UtaLime,
    onPrimary           = UtaGreenDeep,
    primaryContainer    = UtaGreenDark,
    onPrimaryContainer  = UtaLime,
    secondary           = UtaGreen,
    onSecondary         = Color.White,
    secondaryContainer  = Color(0xFF1A3B1C),
    onSecondaryContainer = UtaLime,
    tertiary            = Color(0xFF8FD44F),
    onTertiary          = UtaGreenDeep,
    background          = UtaGreenDeep,
    surface             = Color(0xFF142016),
    surfaceVariant      = Color(0xFF1E2F1F),
    onBackground        = Color(0xFFE8F5E9),
    onSurface           = Color(0xFFE8F5E9),
    onSurfaceVariant    = Color(0xFFB8CFB9),
    error               = Color(0xFFFF6B6B),
    onError             = Color(0xFF690005),
    outline             = Color(0xFF4A6B4D),
)

// ── Typography (Figtree throughout) ─────────────────────────────────────────
private val UtaTypography = Typography(
    displayLarge  = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Bold,   fontSize = 57.sp, lineHeight = 64.sp),
    displayMedium = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Bold,   fontSize = 45.sp, lineHeight = 52.sp),
    displaySmall  = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Bold,   fontSize = 36.sp, lineHeight = 44.sp),
    headlineLarge = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Bold,   fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium= TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Bold,   fontSize = 28.sp, lineHeight = 36.sp),
    headlineSmall = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Bold,   fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge    = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Bold,   fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium   = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp),
    titleSmall    = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    bodyLarge     = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium    = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall     = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge    = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    labelMedium   = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
    labelSmall    = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
)

@Composable
fun CensusHubTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography  = UtaTypography,
        content     = content
    )
}
