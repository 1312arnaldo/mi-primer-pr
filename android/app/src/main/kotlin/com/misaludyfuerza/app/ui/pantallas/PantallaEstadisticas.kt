package com.misaludyfuerza.app.ui.pantallas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.misaludyfuerza.app.ui.AppViewModel
import com.misaludyfuerza.app.ui.Aviso
import com.misaludyfuerza.app.ui.Colores
import com.misaludyfuerza.app.ui.Encabezado
import com.misaludyfuerza.app.ui.Etiqueta
import com.misaludyfuerza.app.ui.FilaDato
import com.misaludyfuerza.app.ui.TarjetaGrande
import com.misaludyfuerza.core.estadisticas.ReferenciaPerdida
import com.misaludyfuerza.core.estadisticas.Series
import com.misaludyfuerza.core.estadisticas.Veredicto
import com.misaludyfuerza.core.tiempo.Hora12

@Composable
fun PantallaEstadisticas(vm: AppViewModel) {
    val fecha by vm.fecha.collectAsStateWithLifecycle()
    val pesos by vm.pesos.collectAsStateWithLifecycle()
    val cinturas by vm.cinturas.collectAsStateWithLifecycle()

    var nuevoPeso by remember { mutableStateOf("") }
    var nuevaCintura by remember { mutableStateOf("") }

    val tPeso = vm.tendenciaPeso()
    val tCintura = vm.tendenciaCintura()
    val promediosPeso = Series.promediosSemanales(pesos)

    LazyColumn(Modifier.fillMaxSize()) {
        item { Encabezado("Progreso", "Al ${Hora12.fechaLarga(fecha)}") }

        item {
            TarjetaGrande(titulo = "Registrar hoy") {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = nuevoPeso, onValueChange = { nuevoPeso = it },
                        label = { Text("Peso (kg)") }, modifier = Modifier.weight(1f), singleLine = true,
                    )
                    Button(onClick = {
                        nuevoPeso.replace(',', '.').toDoubleOrNull()?.let { vm.guardarPeso(it) }
                        nuevoPeso = ""
                    }) { Text("Guardar") }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = nuevaCintura, onValueChange = { nuevaCintura = it },
                        label = { Text("Cintura (cm)") }, modifier = Modifier.weight(1f), singleLine = true,
                    )
                    Button(onClick = {
                        nuevaCintura.replace(',', '.').toDoubleOrNull()?.let { vm.guardarCintura(it) }
                        nuevaCintura = ""
                    }) { Text("Guardar") }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Pesate lunes, miercoles y viernes despues de ir al bano y antes de comer. " +
                        "La cintura, los sabados a las ${Hora12.hora(java.time.LocalTime.of(7, 15))}.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        item {
            TarjetaGrande(
                titulo = "Peso",
                subtitulo = tPeso.mensaje,
                color = if (tPeso.veredicto == Veredicto.BAJANDO) Colores.exitoSuave
                else Colores.tarjeta,
            ) {
                Spacer(Modifier.height(8.dp))
                Etiqueta(tPeso.cobertura.texto, Colores.infoSuave)
                Spacer(Modifier.height(8.dp))
                promediosPeso.takeLast(6).forEach {
                    FilaDato(
                        "Semana del ${Hora12.fechaCorta(java.time.LocalDate.parse(it.inicioIso))}",
                        "${"%.2f".format(it.promedio)} kg (${it.muestras} registros)",
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    ReferenciaPerdida.comentar(tPeso.cambioPorSemana),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        item {
            TarjetaGrande(
                titulo = "Cintura",
                subtitulo = tCintura.mensaje,
                color = if (tCintura.veredicto == Veredicto.BAJANDO) Colores.exitoSuave
                else Colores.tarjeta,
            ) {
                Spacer(Modifier.height(8.dp))
                Etiqueta(tCintura.cobertura.texto, Colores.infoSuave)
                Spacer(Modifier.height(8.dp))
                cinturas.takeLast(6).forEach {
                    FilaDato(Hora12.fechaCorta(java.time.LocalDate.parse(it.fechaIso)), "${it.valor} cm")
                }
            }
        }

        item {
            if (tPeso.veredicto == Veredicto.SIN_CAMBIO_CLARO &&
                tCintura.veredicto == Veredicto.BAJANDO
            ) {
                Aviso(
                    "La bascula no se mueve pero la cintura baja. Eso cuenta como progreso.",
                    Colores.exitoSuave,
                )
            }
        }

        item {
            Aviso(
                "Las graficas siempre muestran la cobertura de datos. Un solo dia no es una " +
                    "tendencia y la falta de dato no es un cero. Conviene revisar el plan con " +
                    "2 o 3 semanas de datos suficientes, y cualquier cambio de calorias o de " +
                    "entrenamiento lo aceptas tu.",
            )
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}
