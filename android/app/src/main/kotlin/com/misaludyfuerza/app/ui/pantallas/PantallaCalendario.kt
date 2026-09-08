package com.misaludyfuerza.app.ui.pantallas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.misaludyfuerza.app.ui.AppViewModel
import com.misaludyfuerza.app.ui.Aviso
import com.misaludyfuerza.app.ui.Colores
import com.misaludyfuerza.app.ui.Encabezado
import com.misaludyfuerza.app.ui.Etiqueta
import com.misaludyfuerza.app.ui.TarjetaGrande
import com.misaludyfuerza.core.agenda.DetectorConflictos
import com.misaludyfuerza.core.agenda.GeneradorAgenda
import com.misaludyfuerza.core.tiempo.Hora12

@Composable
fun PantallaCalendario(vm: AppViewModel) {
    val fecha by vm.fecha.collectAsStateWithLifecycle()
    val cfg by vm.configuracion.collectAsStateWithLifecycle()

    val dias = (0..13).map { fecha.plusDays(it.toLong()) }
    val proximosSabados = cfg.custodia.proximosSabados(fecha, 3)

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Encabezado("Calendario", "Proximas dos semanas desde ${Hora12.fechaCorta(fecha)}")
            Row(
                Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(onClick = { vm.irA(fecha.minusWeeks(1)) }) { Text("Semana anterior") }
                OutlinedButton(onClick = { vm.irA(fecha.plusWeeks(1)) }) { Text("Semana siguiente") }
            }
        }

        item {
            TarjetaGrande(
                titulo = "Fines de semana con el nino",
                subtitulo = proximosSabados.joinToString("\n") {
                    "${Hora12.fechaLarga(it)} - ${Hora12.hora(com.misaludyfuerza.core.agenda.Custodia.INICIO_SABADO)} " +
                        "hasta el lunes a las ${Hora12.hora(com.misaludyfuerza.core.agenda.Custodia.FIN_LUNES)}"
                },
                color = Colores.AzulSuave,
            )
        }

        item {
            Aviso(
                "La recogida de martes y jueves esta guardada como estimacion " +
                    "(${Hora12.hora(cfg.custodiaEntreSemana.horaRecogidaEstimada)}). " +
                    "Editala en Mas > Ajustes cuando sepas la hora real.",
            )
        }

        items(dias.size) { i ->
            val d = dias[i]
            val eventos = GeneradorAgenda.generarDia(d, cfg)
            val conflictos = DetectorConflictos.detectar(eventos)
            TarjetaGrande(
                titulo = Hora12.fechaLarga(d),
                subtitulo = eventos.joinToString("\n") { "${it.horaVisible}  ${it.titulo}" },
                color = if (cfg.custodia.esFinDeSemanaDeCustodia(d) &&
                    d.dayOfWeek in setOf(java.time.DayOfWeek.SATURDAY, java.time.DayOfWeek.SUNDAY)
                ) {
                    Colores.AzulSuave
                } else {
                    MaterialTheme.colorScheme.surface
                },
            ) {
                if (conflictos.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    conflictos.forEach { Etiqueta(it.descripcion, Colores.AmbarSuave) }
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}
