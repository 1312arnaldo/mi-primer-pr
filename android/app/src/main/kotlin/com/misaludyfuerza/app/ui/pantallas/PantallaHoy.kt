package com.misaludyfuerza.app.ui.pantallas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.misaludyfuerza.app.datos.EventoConEstado
import com.misaludyfuerza.app.ui.Aviso
import com.misaludyfuerza.app.ui.Colores
import com.misaludyfuerza.app.ui.Encabezado
import com.misaludyfuerza.app.ui.Etiqueta
import com.misaludyfuerza.app.ui.AppViewModel
import com.misaludyfuerza.app.ui.TarjetaGrande
import com.misaludyfuerza.app.ui.TituloSeccion
import com.misaludyfuerza.core.agenda.EstadoEvento
import com.misaludyfuerza.core.agenda.TipoEvento
import com.misaludyfuerza.core.salud.EstadoAutorizacion
import com.misaludyfuerza.core.tiempo.Hora12
import java.time.LocalTime
import java.time.ZoneId

@Composable
fun PantallaHoy(vm: AppViewModel) {
    val fecha by vm.fecha.collectAsStateWithLifecycle()
    val eventos by vm.eventosDelDia.collectAsStateWithLifecycle()
    val autorizacion by vm.autorizacion.collectAsStateWithLifecycle()

    val ahora = LocalTime.now(ZoneId.of("America/New_York"))
    val proximo = eventos.firstOrNull {
        it.estado == EstadoEvento.PENDIENTE && it.horaEfectiva.isAfter(ahora)
    }
    val comida = eventos.firstOrNull { it.evento.tipo.esComida && it.horaEfectiva.isAfter(ahora) }
    val entreno = eventos.firstOrNull { it.evento.tipo == TipoEvento.GIMNASIO }
    val hechos = eventos.count { it.estado == EstadoEvento.HECHO }

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Encabezado("Hoy", Hora12.fechaLarga(fecha))
        }

        if (autorizacion.estado != EstadoAutorizacion.AUTORIZADA_CON_LIMITES) {
            item { Aviso(autorizacion.textoAviso) }
        }

        item {
            TarjetaGrande(
                titulo = proximo?.let { "Proximo: ${it.evento.titulo}" } ?: "Nada mas pendiente hoy",
                subtitulo = proximo?.let { e ->
                    buildString {
                        append(Hora12.hora(e.horaEfectiva))
                        if (e.nuevaHora != null) {
                            append(" (pospuesto desde ${Hora12.hora(e.evento.inicio)})")
                        }
                        e.evento.nota?.let { append("\n").append(it) }
                    }
                } ?: "Revisa el calendario para preparar manana.",
                color = Colores.infoSuave,
            )
        }

        item {
            TarjetaGrande(
                titulo = "Proxima comida",
                subtitulo = comida?.let { "${it.evento.titulo} - ${Hora12.hora(it.horaEfectiva)}" }
                    ?: "Sin comidas pendientes hoy.",
            ) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Confirmar lo que comiste se hace en la pantalla Comidas. Un aviso no cuenta " +
                        "como comida.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        item {
            TarjetaGrande(
                titulo = "Entrenamiento",
                subtitulo = entreno?.let {
                    "${Hora12.rango(it.evento.inicio, it.evento.fin ?: it.evento.inicio)} - " +
                        (it.evento.nota ?: "")
                } ?: "Hoy no hay gimnasio programado.",
                color = if (entreno != null) Colores.exitoSuave else Colores.tarjeta,
            )
        }

        item {
            TarjetaGrande(
                titulo = "Progreso del dia",
                subtitulo = "$hechos de ${eventos.size} actividades marcadas como hechas.",
            )
        }

        item {
            TituloSeccion("Agenda completa")
        }

        items(eventos, key = { it.evento.uid }) { e -> FilaEvento(e, vm) }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun FilaEvento(e: EventoConEstado, vm: AppViewModel) {
    val color = when {
        e.estado == EstadoEvento.HECHO -> Colores.exitoSuave
        e.estado == EstadoEvento.OMITIDO -> Colores.peligroSuave
        e.evento.requiereValidacionMedica -> Colores.avisoSuave
        else -> Colores.tarjeta
    }
    TarjetaGrande(titulo = e.evento.titulo, color = color) {
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Etiqueta(
                if (e.nuevaHora != null) {
                    "${Hora12.hora(e.horaEfectiva)} (original ${Hora12.hora(e.evento.inicio)})"
                } else {
                    e.evento.horaVisible
                },
                Colores.infoSuave,
            )
            if (!e.evento.confirmado) Etiqueta("Sin confirmar", Colores.avisoSuave)
            if (e.estado == EstadoEvento.HECHO) Etiqueta("Hecho", Colores.exitoSuave)
            if (e.estado == EstadoEvento.OMITIDO) Etiqueta("Omitido", Colores.peligroSuave)
        }
        e.evento.nota?.let {
            Spacer(Modifier.height(6.dp))
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { vm.marcarEvento(e.evento, EstadoEvento.HECHO) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(vertical = 12.dp),
            ) { Text("Hecho") }
            FilledTonalButton(
                onClick = { vm.posponerEvento(e.evento) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(vertical = 12.dp),
            ) { Text("Posponer") }
            FilledTonalButton(
                onClick = { vm.marcarEvento(e.evento, EstadoEvento.OMITIDO) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(vertical = 12.dp),
            ) { Text("Omitir") }
        }
    }
}
