package com.misaludyfuerza.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.misaludyfuerza.app.ui.pantallas.PantallaCalendario
import com.misaludyfuerza.app.ui.pantallas.PantallaComidas
import com.misaludyfuerza.app.ui.pantallas.PantallaEntrenamiento
import com.misaludyfuerza.app.ui.pantallas.PantallaEstadisticas
import com.misaludyfuerza.app.ui.pantallas.PantallaHoy
import com.misaludyfuerza.app.ui.pantallas.PantallaMas
import java.time.LocalDate

private data class Seccion(val ruta: String, val etiqueta: String, val icono: ImageVector)

private val SECCIONES = listOf(
    Seccion("hoy", "Hoy", Icons.Filled.Today),
    Seccion("calendario", "Calendario", Icons.Filled.CalendarMonth),
    Seccion("comidas", "Comidas", Icons.Filled.Restaurant),
    Seccion("entreno", "Entreno", Icons.Filled.FitnessCenter),
    Seccion("progreso", "Progreso", Icons.Filled.Insights),
    Seccion("mas", "Mas", Icons.Filled.Tune),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AplicacionUi(
    fechaInicialIso: String?,
    abrirAjustesAlarmas: () -> Unit,
    abrirAjustesNotificaciones: () -> Unit,
) {
    val vm: AppViewModel = viewModel()
    val tema by vm.tema.collectAsStateWithLifecycle()

    TemaMiSalud(modo = tema) {
        val nav = rememberNavController()
        val snackbar = remember { SnackbarHostState() }
        val mensaje by vm.mensaje.collectAsStateWithLifecycle()

        LaunchedEffect(fechaInicialIso) {
            fechaInicialIso?.let { runCatching { vm.irA(LocalDate.parse(it)) } }
        }
        LaunchedEffect(mensaje) {
            mensaje?.let {
                snackbar.showSnackbar(it)
                vm.limpiarMensaje()
            }
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                val entrada by nav.currentBackStackEntryAsState()
                val actual = entrada?.destination
                NavigationBar {
                    SECCIONES.forEach { s ->
                        NavigationBarItem(
                            selected = actual?.hierarchy?.any { it.route == s.ruta } == true,
                            onClick = {
                                nav.navigate(s.ruta) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(s.icono, contentDescription = s.etiqueta) },
                            label = { Text(s.etiqueta) },
                        )
                    }
                }
            },
        ) { padding ->
            NavHost(nav, startDestination = "hoy", modifier = Modifier.padding(padding)) {
                composable("hoy") { PantallaHoy(vm) }
                composable("calendario") { PantallaCalendario(vm) }
                composable("comidas") { PantallaComidas(vm) }
                composable("entreno") { PantallaEntrenamiento(vm) }
                composable("progreso") { PantallaEstadisticas(vm) }
                composable("mas") {
                    PantallaMas(vm, abrirAjustesAlarmas, abrirAjustesNotificaciones)
                }
            }
        }
    }
}
