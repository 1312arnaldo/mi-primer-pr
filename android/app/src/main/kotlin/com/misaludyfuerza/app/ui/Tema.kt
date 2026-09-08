package com.misaludyfuerza.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.misaludyfuerza.app.R

/** Preferencia de tema del usuario. */
enum class ModoTema { AUTOMATICO, CLARO, OSCURO }

// --------------------------------------------------------------------- tipografia

private val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

/**
 * Escala tipografica con interlineado y espaciado cuidados. El cuerpo es grande a
 * proposito: la pantalla se lee de un vistazo, entre ejercicio y ejercicio.
 */
private val Tipografia = Typography(
    displaySmall = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.Bold,
        fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = (-0.5).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.Bold,
        fontSize = 27.sp, lineHeight = 33.sp, letterSpacing = (-0.4).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.2).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = (-0.2).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp, lineHeight = 23.sp, letterSpacing = 0.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.Medium,
        fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.1.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.Normal,
        fontSize = 14.5.sp, lineHeight = 21.sp, letterSpacing = 0.1.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.Normal,
        fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 0.2.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.SemiBold,
        fontSize = 14.5.sp, lineHeight = 19.sp, letterSpacing = 0.2.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.Medium,
        fontSize = 12.5.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp,
    ),
)

// --------------------------------------------------------------------- formas

private val Formas = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

// --------------------------------------------------------------------- paletas

private val EsquemaClaro = lightColorScheme(
    primary = Color(0xFF1D4ED8),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCE7FF),
    onPrimaryContainer = Color(0xFF0B2A6B),
    secondary = Color(0xFF127A45),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFC9F3DA),
    onSecondaryContainer = Color(0xFF06301A),
    tertiary = Color(0xFF9A5B00),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFE7C2),
    onTertiaryContainer = Color(0xFF432500),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFBDEDC),
    onErrorContainer = Color(0xFF410E0B),
    background = Color(0xFFF6F7FA),
    onBackground = Color(0xFF12161F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF12161F),
    surfaceVariant = Color(0xFFE8ECF4),
    onSurfaceVariant = Color(0xFF48505E),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFBFCFE),
    surfaceContainer = Color(0xFFF1F3F8),
    surfaceContainerHigh = Color(0xFFEAEEF5),
    surfaceContainerHighest = Color(0xFFE3E8F1),
    outline = Color(0xFFB9C0CD),
    outlineVariant = Color(0xFFDCE1EA),
    inverseSurface = Color(0xFF1B2029),
    inverseOnSurface = Color(0xFFF1F3F8),
    scrim = Color(0xFF000000),
)

/**
 * Modo oscuro pensado, no un simple "poner todo negro": superficies escalonadas,
 * texto de alto contraste y acentos claros que se leen bien de noche.
 */
private val EsquemaOscuro = darkColorScheme(
    primary = Color(0xFF9DBCFF),
    onPrimary = Color(0xFF06255F),
    primaryContainer = Color(0xFF1B3A7A),
    onPrimaryContainer = Color(0xFFD9E5FF),
    secondary = Color(0xFF6FE0A4),
    onSecondary = Color(0xFF00351C),
    secondaryContainer = Color(0xFF12492C),
    onSecondaryContainer = Color(0xFFC6F6DA),
    tertiary = Color(0xFFFFC06B),
    onTertiary = Color(0xFF442A00),
    tertiaryContainer = Color(0xFF5E3D00),
    onTertiaryContainer = Color(0xFFFFE3B8),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF5F1410),
    errorContainer = Color(0xFF7C2F28),
    onErrorContainer = Color(0xFFFFDAD5),
    background = Color(0xFF0E1218),
    onBackground = Color(0xFFE6EAF3),
    surface = Color(0xFF141922),
    onSurface = Color(0xFFE6EAF3),
    surfaceVariant = Color(0xFF262E3A),
    onSurfaceVariant = Color(0xFFB2BBCA),
    surfaceContainerLowest = Color(0xFF0B0F14),
    surfaceContainerLow = Color(0xFF131820),
    surfaceContainer = Color(0xFF1A202A),
    surfaceContainerHigh = Color(0xFF222933),
    surfaceContainerHighest = Color(0xFF2B323E),
    outline = Color(0xFF3E4756),
    outlineVariant = Color(0xFF2A323E),
    inverseSurface = Color(0xFFE6EAF3),
    inverseOnSurface = Color(0xFF1B2029),
    scrim = Color(0xFF000000),
)

/**
 * Colores con significado, no con nombre de color. Se resuelven contra el tema
 * activo, asi que la misma tarjeta se ve bien de dia y de noche.
 */
object Colores {
    /** Completado. */
    val exito: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.secondary
    val exitoSuave: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.secondaryContainer
    val sobreExito: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSecondaryContainer

    /** Informacion y horarios. */
    val info: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.primary
    val infoSuave: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.primaryContainer
    val sobreInfo: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onPrimaryContainer

    /** Pendiente de confirmar, estimaciones, permisos que faltan. */
    val aviso: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.tertiary
    val avisoSuave: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.tertiaryContainer
    val sobreAviso: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onTertiaryContainer

    /** Omitido, sintomas, ejercicios bloqueados. */
    val peligro: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.error
    val peligroSuave: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.errorContainer
    val sobrePeligro: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onErrorContainer

    /** Superficie neutra de tarjeta. */
    val tarjeta: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.surfaceContainerLow
    val texto: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSurface
    val textoSuave: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
fun TemaMiSalud(
    modo: ModoTema = ModoTema.AUTOMATICO,
    contenido: @Composable () -> Unit,
) {
    val oscuro = when (modo) {
        ModoTema.AUTOMATICO -> isSystemInDarkTheme()
        ModoTema.CLARO -> false
        ModoTema.OSCURO -> true
    }
    MaterialTheme(
        colorScheme = if (oscuro) EsquemaOscuro else EsquemaClaro,
        typography = Tipografia,
        shapes = Formas,
        content = contenido,
    )
}
