package com.misaludyfuerza.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Tarjeta grande y legible: la unidad basica de toda la interfaz. */
@Composable
fun TarjetaGrande(
    titulo: String,
    subtitulo: String? = null,
    color: Color = MaterialTheme.colorScheme.surface,
    modifier: Modifier = Modifier,
    contenido: @Composable () -> Unit = {},
) {
    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium)
            if (subtitulo != null) {
                Spacer(Modifier.height(4.dp))
                Text(subtitulo, style = MaterialTheme.typography.bodyMedium)
            }
            contenido()
        }
    }
}

/** Etiqueta corta de estado. */
@Composable
fun Etiqueta(texto: String, fondo: Color, textoColor: Color = Colores.Grafito) {
    Text(
        text = texto,
        style = MaterialTheme.typography.labelLarge,
        color = textoColor,
        modifier = Modifier
            .background(fondo, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

/**
 * Aviso destacado. Se usa para cosas que el usuario DEBE leer: autorizacion
 * pendiente, estimaciones sin confirmar, permisos que faltan.
 */
@Composable
fun Aviso(texto: String, fondo: Color = Colores.AmbarSuave, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = fondo),
    ) {
        Text(
            texto,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
fun Encabezado(texto: String, detalle: String? = null) {
    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 4.dp)) {
        Text(texto, style = MaterialTheme.typography.headlineMedium)
        if (detalle != null) {
            Text(
                detalle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
fun FilaDato(etiqueta: String, valor: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(etiqueta, style = MaterialTheme.typography.bodyMedium)
        Text(valor, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
    }
}
