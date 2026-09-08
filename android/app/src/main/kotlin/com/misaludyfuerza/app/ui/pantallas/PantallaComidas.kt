package com.misaludyfuerza.app.ui.pantallas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.misaludyfuerza.core.alimentacion.CatalogoBase
import com.misaludyfuerza.core.alimentacion.EstadoComida
import com.misaludyfuerza.core.alimentacion.MenuBase
import com.misaludyfuerza.core.alimentacion.ObjetivoNutricional
import com.misaludyfuerza.core.tiempo.Hora12

@Composable
fun PantallaComidas(vm: AppViewModel) {
    val fecha by vm.fecha.collectAsStateWithLifecycle()
    val registros by vm.comidasDelDia.collectAsStateWithLifecycle()
    val catalogo = remember { CatalogoBase.catalogo() }
    val objetivo = remember { ObjetivoNutricional() }
    var mostrarCompra by remember { mutableStateOf(false) }

    val plan = MenuBase.dia(fecha)
    val totales = catalogo.totales(plan.comidas)

    LazyColumn(Modifier.fillMaxSize()) {
        item { Encabezado("Comidas", Hora12.fechaLarga(fecha)) }

        item {
            TarjetaGrande(titulo = "Objetivo del dia", color = Colores.infoSuave) {
                Spacer(Modifier.height(8.dp))
                FilaDato("Objetivo", "${objetivo.kcal} kcal - ${objetivo.proteinaMinG}-${objetivo.proteinaMaxG} g proteina")
                FilaDato("Menu de hoy", "${totales.kcal.toInt()} kcal - ${totales.proteinaG.toInt()} g proteina")
                Spacer(Modifier.height(6.dp))
                Text(objetivo.nota, style = MaterialTheme.typography.bodyMedium)
            }
        }

        item {
            Aviso(
                "Estas cifras son estimaciones del menu planeado, no de lo que comiste. " +
                    "Marca \"Comi\", \"Cambie\" o \"No comi\" en cada comida: nada se da por hecho.",
            )
        }

        items(plan.comidas.size) { i ->
            val comida = plan.comidas[i]
            val registro = registros.firstOrNull { it.tipo == comida.tipo }
            val estado = registro?.estado ?: EstadoComida.PLANEADA
            val t = catalogo.totales(comida)

            TarjetaGrande(
                titulo = comida.tipo.etiqueta,
                subtitulo = comida.porciones.joinToString("\n") {
                    "- ${catalogo.get(it.alimentoId).nombre}: ${it.gramos.toInt()} g" +
                        (it.nota?.let { n -> " ($n)" } ?: "")
                },
                color = when (estado) {
                    EstadoComida.COMI, EstadoComida.CAMBIE -> Colores.exitoSuave
                    EstadoComida.NO_COMI -> Colores.peligroSuave
                    else -> Colores.tarjeta
                },
            ) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Etiqueta("${t.kcal.toInt()} kcal", Colores.infoSuave)
                    Etiqueta("${t.proteinaG.toInt()} g proteina", Colores.infoSuave)
                    Etiqueta(estado.etiqueta, Colores.avisoSuave)
                }
                comida.nota?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { vm.registrarComida(comida.tipo, EstadoComida.COMI) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(vertical = 12.dp),
                    ) { Text("Comi") }
                    FilledTonalButton(
                        onClick = { vm.registrarComida(comida.tipo, EstadoComida.CAMBIE) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(vertical = 12.dp),
                    ) { Text("Cambie") }
                    FilledTonalButton(
                        onClick = { vm.registrarComida(comida.tipo, EstadoComida.NO_COMI) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(vertical = 12.dp),
                    ) { Text("No comi") }
                }

                val sustituciones = comida.porciones.firstOrNull()
                    ?.let { catalogo.sugerirSustituciones(it, maximo = 3) }
                    .orEmpty()
                if (sustituciones.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text("Cambios equivalentes:", style = MaterialTheme.typography.labelLarge)
                    sustituciones.forEach { s ->
                        Text(
                            "- ${catalogo.get(s.sustituto.alimentoId).nombre}: " +
                                "${s.sustituto.gramos.toInt()} g " +
                                "(${s.kcalSustituto.toInt()} kcal, ${s.proteinaSustituto.toInt()} g proteina)",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }

        item {
            TarjetaGrande(titulo = "Lista de compra de 7 dias") {
                TextButton(onClick = { mostrarCompra = !mostrarCompra }) {
                    Text(if (mostrarCompra) "Ocultar" else "Ver lista")
                }
                if (mostrarCompra) {
                    vm.listaDeCompra().forEach { l ->
                        Text(
                            "- ${l.nombre}: ${l.gramosDeCompra.toInt()} g de compra " +
                                "(${l.gramosCocinados.toInt()} g ya cocinados)",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Los rendimientos de coccion son estimaciones editables. " +
                            com.misaludyfuerza.core.alimentacion.Conservacion.TEXTO,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        item {
            Aviso(
                "Nunca se proponen pollo, huevo hervido ni avena, tampoco como sustitucion. " +
                    "El agua es orientativa (2 a 2.5 litros de bebidas), sin cuota obligatoria.",
                Colores.exitoSuave,
            )
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}
