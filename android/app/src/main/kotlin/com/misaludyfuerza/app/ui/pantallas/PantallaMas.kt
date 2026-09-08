package com.misaludyfuerza.app.ui.pantallas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.misaludyfuerza.app.salud.EstadoHealthConnect
import com.misaludyfuerza.app.salud.IntegracionGimnasio
import com.misaludyfuerza.app.ui.AppViewModel
import com.misaludyfuerza.app.ui.Aviso
import com.misaludyfuerza.app.ui.Colores
import com.misaludyfuerza.app.ui.Encabezado
import com.misaludyfuerza.app.ui.Etiqueta
import com.misaludyfuerza.app.ui.FilaDato
import com.misaludyfuerza.app.ui.TarjetaGrande
import com.misaludyfuerza.core.salud.EstadoAutorizacion
import com.misaludyfuerza.core.tiempo.Hora12
import java.time.LocalDate

@Composable
fun PantallaMas(
    vm: AppViewModel,
    abrirAjustesAlarmas: () -> Unit,
    abrirAjustesNotificaciones: () -> Unit,
) {
    val contexto = LocalContext.current
    val fecha by vm.fecha.collectAsStateWithLifecycle()
    val avisos by vm.estadoAvisos.collectAsStateWithLifecycle()
    val estadoSalud by vm.estadoSalud.collectAsStateWithLifecycle()
    val tiposSalud by vm.tiposSalud.collectAsStateWithLifecycle()
    val autorizacion by vm.autorizacion.collectAsStateWithLifecycle()
    val ajustes by vm.ajustes.collectAsStateWithLifecycle()
    val vistaPrevia by vm.vistaPrevia.collectAsStateWithLifecycle()

    val pedirPermisosSalud = rememberLauncherForActivityResult(vm.contratoPermisosSalud()) {
        vm.refrescarSalud()
    }

    var instruccion by remember { mutableStateOf("") }
    var profesional by remember { mutableStateOf("") }

    val desde = fecha.minusDays(6)

    LazyColumn(Modifier.fillMaxSize()) {
        item { Encabezado("Mas", "Salud, avisos, compartir y ajustes") }

        // ---------------------------------------------------------- autorizacion
        item {
            TarjetaGrande(
                titulo = "Autorizacion profesional",
                subtitulo = autorizacion.textoAviso,
                color = Colores.AmbarSuave,
            ) {
                Spacer(Modifier.height(8.dp))
                FilaDato("Estado", autorizacion.estado.etiqueta)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = profesional, onValueChange = { profesional = it },
                    label = { Text("Traumatologo o fisioterapeuta") }, singleLine = true,
                    modifier = Modifier.height(64.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        vm.guardarAutorizacion(
                            EstadoAutorizacion.AUTORIZADA_CON_LIMITES,
                            profesional.ifBlank { null },
                            LocalDate.now().toString(),
                        )
                    }) { Text("Autorizada con limites") }
                    OutlinedButton(onClick = {
                        vm.guardarAutorizacion(
                            EstadoAutorizacion.NO_AUTORIZADA,
                            profesional.ifBlank { null },
                            LocalDate.now().toString(),
                        )
                    }) { Text("No autorizada") }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = instruccion, onValueChange = { instruccion = it },
                    label = { Text("Instruccion o ejercicio") },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        if (instruccion.isNotBlank()) {
                            vm.anadirInstruccion(instruccion, "INSTRUCCION"); instruccion = ""
                        }
                    }) { Text("Guardar instruccion") }
                    TextButton(onClick = {
                        if (instruccion.isNotBlank()) {
                            vm.anadirInstruccion(instruccion, "EJERCICIO_PERMITIDO"); instruccion = ""
                        }
                    }) { Text("Permitido") }
                    TextButton(onClick = {
                        if (instruccion.isNotBlank()) {
                            vm.anadirInstruccion(instruccion, "EJERCICIO_PROHIBIDO"); instruccion = ""
                        }
                    }) { Text("Prohibido") }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Los ejercicios que cargan la rodilla siguen bloqueados aunque marques " +
                        "autorizada: eso no se puede desactivar desde aqui.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        // ---------------------------------------------------------- avisos
        item {
            TarjetaGrande(titulo = "Recordatorios", subtitulo = avisos.resumen) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { vm.probarNotificacion() }) { Text("Probar notificacion") }
                    OutlinedButton(onClick = { vm.reprogramarAvisos() }) { Text("Reprogramar") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!avisos.alarmasExactasConcedidas) {
                        TextButton(onClick = abrirAjustesAlarmas) { Text("Permitir alarmas exactas") }
                    }
                    if (!avisos.notificacionesConcedidas) {
                        TextButton(onClick = abrirAjustesNotificaciones) { Text("Ajustes de notificaciones") }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Para verlos en el Galaxy Watch: abre Galaxy Wearable, entra en " +
                        "Notificaciones y activa \"Mi Salud y Fuerza\". Comprueba con el boton " +
                        "de prueba antes de fiarte del reloj.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "El ahorro de bateria del fabricante puede retrasar avisos. Si te importa la " +
                        "puntualidad, excluye la app del ahorro de bateria en los ajustes del sistema.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        item {
            Aviso(
                "Tus avisos actuales de ChatGPT (6:00 a. m., 7:40 a. m. los lunes, miercoles y " +
                    "viernes, y 9:00 p. m.) siguen activos: esta app no puede modificarlos. " +
                    "Cuando compruebes que estas alertas funcionan, pide tu que se pausen alli.",
            )
        }

        // ---------------------------------------------------------- health connect
        item {
            TarjetaGrande(
                titulo = "Health Connect",
                subtitulo = when (estadoSalud) {
                    null -> "Comprobando..."
                    EstadoHealthConnect.NoDisponible ->
                        "Health Connect no esta disponible en este telefono. En Android 14 o " +
                            "superior viene integrado; en versiones anteriores se instala desde " +
                            "Play Store."
                    EstadoHealthConnect.RequiereActualizacion ->
                        "Health Connect necesita actualizarse antes de poder leer datos."
                    is EstadoHealthConnect.Disponible ->
                        "Disponible. Ruta: Galaxy Watch, Samsung Health, Health Connect y esta app."
                },
            ) {
                Spacer(Modifier.height(8.dp))
                if (estadoSalud is EstadoHealthConnect.Disponible) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { pedirPermisosSalud.launch(vm.permisosDeSalud()) }) {
                            Text("Conceder permisos")
                        }
                        OutlinedButton(onClick = { vm.sincronizarSalud() }) { Text("Sincronizar") }
                    }
                    Spacer(Modifier.height(8.dp))
                    tiposSalud.forEach { t ->
                        FilaDato(t.tipo.etiqueta, t.descripcion)
                        t.ultimaSincronizacionIso?.let {
                            Text(
                                "Ultima sincronizacion: $it",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        t.error?.let {
                            Text("Error: $it", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { vm.desconectarSalud() }) {
                        Text("Desconectar y borrar lo importado")
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Solo se lee lo que autorices y lo que la fuente comparta. No se prometen " +
                        "puntuacion de energia de Samsung, detalle de sueno ni HRV sin comprobar " +
                        "que existan. Sin dato no significa cero.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        // ---------------------------------------------------------- gimnasio
        item {
            TarjetaGrande(
                titulo = IntegracionGimnasio.NOMBRE,
                subtitulo = IntegracionGimnasio.ESTADO,
                color = Colores.AmbarSuave,
            ) {
                Spacer(Modifier.height(6.dp))
                Text(IntegracionGimnasio.EXPLICACION, style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = {
                    val intent = contexto.packageManager
                        .getLaunchIntentForPackage(IntegracionGimnasio.PAQUETE_APP)
                    if (intent != null) contexto.startActivity(intent)
                }) { Text("Abrir la app de Crunch") }
            }
        }

        // ---------------------------------------------------------- compartir
        item {
            TarjetaGrande(
                titulo = "Compartir revision semanal",
                subtitulo = "Del ${Hora12.fechaCorta(desde)} al ${Hora12.fechaCorta(fecha)}. " +
                    "Markdown para leer, JSON con esquema versionado y CSV de metricas.",
            ) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { vm.prepararRevision(desde, fecha) }) { Text("Vista previa") }
                    OutlinedButton(onClick = { vm.compartirRevision(desde, fecha) }) { Text("Compartir") }
                }
                OutlinedButton(onClick = { vm.compartirCalendario(fecha, fecha.plusDays(27)) }) {
                    Text("Exportar calendario (ICS)")
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Los detalles identificables del nino se omiten por defecto. " +
                        "Claude Code y ChatGPT no estan conectados entre si: compartes tu el archivo.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        // ---------------------------------------------------------- ajustes
        item {
            ajustes?.let { a ->
                TarjetaGrande(titulo = "Ajustes") {
                    Spacer(Modifier.height(8.dp))
                    FilaDato("Fecha de activacion", a.fechaActivacionIso)
                    FilaDato("Margen de aviso", "${a.margenAvisoMin} minutos antes")
                    FilaDato("Posponer", "${a.minutosPosponer} minutos")
                    FilaDato(
                        "Gimnasio a casa",
                        "${a.gimnasioACasaMin} min" + if (a.gimnasioACasaConfirmado) "" else " (sin confirmar)",
                    )
                    FilaDato(
                        "Recogida martes y jueves",
                        Hora12.hora(
                            java.time.LocalTime.ofSecondOfDay(
                                a.horaRecogidaMinutosDesdeMedianoche * 60L,
                            ),
                        ) + if (a.horaRecogidaConfirmada) "" else " (estimacion)",
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = a.gimnasioACasaConfirmado,
                            onCheckedChange = { vm.guardarAjustes(a.copy(gimnasioACasaConfirmado = it)) },
                        )
                        Text("Confirmar traslado gimnasio-casa", style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = a.horaRecogidaConfirmada,
                            onCheckedChange = { vm.guardarAjustes(a.copy(horaRecogidaConfirmada = it)) },
                        )
                        Text("Confirmar hora de recogida", style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = a.anonimizarNinoAlExportar,
                            onCheckedChange = { vm.guardarAjustes(a.copy(anonimizarNinoAlExportar = it)) },
                        )
                        Text("Omitir datos del nino al exportar", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        item {
            Aviso(
                "Todo se guarda solo en este telefono y funciona sin internet. No se envian " +
                    "datos de salud a ningun servidor ni a ninguna analitica.",
                Colores.VerdeSuave,
            )
        }

        item { Spacer(Modifier.height(24.dp)) }
    }

    vistaPrevia?.let { texto ->
        AlertDialog(
            onDismissRequest = { vm.limpiarVistaPrevia() },
            title = { Text("Vista previa antes de compartir") },
            text = {
                Text(
                    texto,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.limpiarVistaPrevia()
                    vm.compartirRevision(desde, fecha)
                }) { Text("Compartir") }
            },
            dismissButton = {
                TextButton(onClick = { vm.limpiarVistaPrevia() }) { Text("Cerrar") }
            },
        )
    }
}
