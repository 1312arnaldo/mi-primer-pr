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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.misaludyfuerza.app.ui.TarjetaGrande
import com.misaludyfuerza.core.entrenamiento.ParametrosSesion
import com.misaludyfuerza.core.entrenamiento.RutinasBase
import com.misaludyfuerza.core.entrenamiento.TipoPropuesta
import com.misaludyfuerza.core.salud.FiltroRodilla
import com.misaludyfuerza.core.salud.ReporteSintoma
import com.misaludyfuerza.core.salud.VeredictoEjercicio
import com.misaludyfuerza.core.tiempo.Hora12

@Composable
fun PantallaEntrenamiento(vm: AppViewModel) {
    val fecha by vm.fecha.collectAsStateWithLifecycle()
    val autorizacion by vm.autorizacion.collectAsStateWithLifecycle()
    val propuestas by vm.propuestas.collectAsStateWithLifecycle()
    val respuestaSintoma by vm.respuestaSintoma.collectAsStateWithLifecycle()
    val parametros = remember { ParametrosSesion() }

    val rutina = RutinasBase.porDia(fecha.dayOfWeek)

    LaunchedEffect(rutina?.id) { rutina?.let { vm.calcularPropuestas(it.id) } }

    var dolor by remember { mutableStateOf("0") }
    var hinchazon by remember { mutableStateOf(false) }
    var inestabilidad by remember { mutableStateOf(false) }

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Encabezado(
                "Entrenamiento",
                rutina?.let { "${it.nombre} - ${Hora12.fechaLarga(fecha)}" }
                    ?: "Hoy no toca gimnasio",
            )
        }

        item { Aviso(autorizacion.textoAviso) }

        respuestaSintoma?.let {
            item { Aviso(it.mensaje, Colores.RojoSuave) }
        }

        if (rutina == null) {
            item {
                TarjetaGrande(
                    titulo = "Sin sesion hoy",
                    subtitulo = "La base es lunes, miercoles y viernes por la manana, despues de " +
                        "dejar al nino en la escuela. Ningun fin de semana lleva gimnasio obligatorio.",
                )
            }
        } else {
            item {
                TarjetaGrande(titulo = "Como entrenar hoy", color = Colores.AzulSuave) {
                    Spacer(Modifier.height(6.dp))
                    RutinasBase.ADVERTENCIAS.forEach {
                        Text("- $it", style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Descanso ${parametros.descansoTexto}. Sal a las " +
                            "${Hora12.hora(java.time.LocalTime.of(8, 35))} para no llegar tarde.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            items(rutina.ejercicios.size) { i ->
                val ej = rutina.ejercicios[i]
                val evaluacion = FiltroRodilla.evaluar(ej.nombre, autorizacion)
                val propuesta = propuestas.firstOrNull { it.ejercicioId == ej.id }
                var peso by remember(ej.id) { mutableStateOf("") }
                var reps by remember(ej.id) { mutableStateOf("") }
                var rir by remember(ej.id) { mutableStateOf("3") }
                var tecnica by remember(ej.id) { mutableStateOf(true) }

                TarjetaGrande(
                    titulo = ej.nombre,
                    subtitulo = "${ej.maquina} - ${ej.seriesIniciales} series de ${ej.rangoTexto}",
                    color = when (evaluacion.veredicto) {
                        VeredictoEjercicio.BLOQUEADO -> Colores.RojoSuave
                        VeredictoEjercicio.REQUIERE_REVISION -> Colores.AmbarSuave
                        else -> MaterialTheme.colorScheme.surface
                    },
                ) {
                    Spacer(Modifier.height(6.dp))
                    Text(evaluacion.motivo, style = MaterialTheme.typography.bodyMedium)
                    ej.notas?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }

                    propuesta?.let { p ->
                        Spacer(Modifier.height(8.dp))
                        Etiqueta(
                            when (p.tipo) {
                                TipoPropuesta.MANTENER -> "Mantener"
                                TipoPropuesta.ANADIR_SERIE -> "Propuesta: tercera serie"
                                TipoPropuesta.SUBIR_PESO -> "Propuesta: subir peso"
                                TipoPropuesta.CONGELAR_POR_SINTOMA -> "Congelado por sintomas"
                                TipoPropuesta.ESPERAR_AUTORIZACION -> "En espera de autorizacion"
                            },
                            Colores.AzulSuave,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(p.mensaje, style = MaterialTheme.typography.bodyMedium)
                    }

                    if (evaluacion.veredicto != VeredictoEjercicio.BLOQUEADO) {
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = peso, onValueChange = { peso = it },
                                label = { Text("Peso (kg)") }, modifier = Modifier.weight(1f),
                                singleLine = true,
                            )
                            OutlinedTextField(
                                value = reps, onValueChange = { reps = it },
                                label = { Text("Reps") }, modifier = Modifier.weight(1f),
                                singleLine = true,
                            )
                            OutlinedTextField(
                                value = rir, onValueChange = { rir = it },
                                label = { Text("Sobran") }, modifier = Modifier.weight(1f),
                                singleLine = true,
                            )
                        }
                        Row(
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Switch(checked = tecnica, onCheckedChange = { tecnica = it })
                            Text("Buena tecnica", style = MaterialTheme.typography.bodyMedium)
                            TextButton(
                                onClick = {
                                    val r = reps.toIntOrNull() ?: return@TextButton
                                    vm.guardarSerie(
                                        rutina.id, ej.id, 1, peso.toDoubleOrNull(), r,
                                        rir.toIntOrNull(), tecnica, null,
                                    )
                                    peso = ""; reps = ""
                                },
                            ) { Text("Guardar serie") }
                        }
                    }
                }
            }

            item {
                TarjetaGrande(
                    titulo = "Cerrar la sesion",
                    subtitulo = "Solo tu marcas la sesion como hecha. Ir al gimnasio no la completa.",
                ) {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = dolor, onValueChange = { dolor = it },
                            label = { Text("Dolor 0-10") }, modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                    }
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Switch(checked = hinchazon, onCheckedChange = { hinchazon = it })
                        Text("Hinchazon", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                        Switch(checked = inestabilidad, onCheckedChange = { inestabilidad = it })
                        Text("Inestabilidad", style = MaterialTheme.typography.bodyMedium)
                    }
                    Button(onClick = {
                        vm.completarSesion(
                            rutina.id, parametros.limiteSesionMin,
                            ReporteSintoma(
                                fechaIso = fecha.toString(),
                                dolor0a10 = dolor.toIntOrNull() ?: 0,
                                hinchazon = hinchazon,
                                inestabilidad = inestabilidad,
                            ),
                        )
                    }) { Text("Marcar sesion como hecha") }
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}
