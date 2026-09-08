package com.misaludyfuerza.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Verde para lo completado, colores con contraste alto y tipografia grande. */
object Colores {
    val VerdeHecho = Color(0xFF1B7F3B)
    val VerdeSuave = Color(0xFFD7F0DF)
    val Azul = Color(0xFF14548C)
    val AzulSuave = Color(0xFFD9E8F5)
    val Ambar = Color(0xFF8A5A00)
    val AmbarSuave = Color(0xFFFFF0D1)
    val Rojo = Color(0xFF9B1C1C)
    val RojoSuave = Color(0xFFFBDDDD)
    val Grafito = Color(0xFF1C1C1E)
    val Papel = Color(0xFFF7F7F5)
}

private val ClaroEsquema = lightColorScheme(
    primary = Colores.Azul,
    onPrimary = Color.White,
    primaryContainer = Colores.AzulSuave,
    onPrimaryContainer = Colores.Grafito,
    secondary = Colores.VerdeHecho,
    secondaryContainer = Colores.VerdeSuave,
    onSecondaryContainer = Colores.Grafito,
    error = Colores.Rojo,
    errorContainer = Colores.RojoSuave,
    background = Colores.Papel,
    surface = Color.White,
    onSurface = Colores.Grafito,
)

private val OscuroEsquema = darkColorScheme(
    primary = Color(0xFF8FC3EE),
    secondary = Color(0xFF7FD79A),
    error = Color(0xFFEF9A9A),
)

/** Tipografia con cuerpo grande: la pantalla se lee de un vistazo. */
private val Tipografia = Typography(
    headlineMedium = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 17.sp),
    bodyMedium = TextStyle(fontSize = 15.sp),
    labelLarge = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun TemaMiSalud(oscuro: Boolean = isSystemInDarkTheme(), contenido: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (oscuro) OscuroEsquema else ClaroEsquema,
        typography = Tipografia,
        content = contenido,
    )
}
