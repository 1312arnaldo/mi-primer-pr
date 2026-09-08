package com.misaludyfuerza.app

import android.app.Application
import com.misaludyfuerza.app.datos.AjustesEntidad
import com.misaludyfuerza.app.datos.AutorizacionEntidad
import com.misaludyfuerza.app.datos.PerfilEntidad
import com.misaludyfuerza.app.datos.Repositorio
import com.misaludyfuerza.app.datos.VersionPlanEntidad
import com.misaludyfuerza.app.notificaciones.Canales
import com.misaludyfuerza.app.notificaciones.ProgramadorAlarmas
import com.misaludyfuerza.app.notificaciones.ReprogramacionDiaria
import com.misaludyfuerza.core.salud.EstadoAutorizacion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate

class MiSaludApp : Application() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        Canales.crear(this)
        scope.launch {
            sembrarSiHaceFalta()
            ProgramadorAlarmas.reprogramarTodo(this@MiSaludApp)
            ReprogramacionDiaria.programar(this@MiSaludApp)
        }
    }

    /**
     * Datos de partida reales del usuario. NO se precargan lecturas del reloj ni
     * mediciones inventadas: el historial arranca vacio.
     */
    private suspend fun sembrarSiHaceFalta() {
        val db = com.misaludyfuerza.app.datos.BaseDatos.obtener(this)
        if (db.perfilDao().leer() == null) {
            db.perfilDao().guardar(PerfilEntidad())
        }
        if (db.ajustesDao().leer() == null) {
            db.ajustesDao().guardar(
                AjustesEntidad(fechaActivacionIso = LocalDate.now().toString()),
            )
        }
        if (db.autorizacionDao().leer() == null) {
            db.autorizacionDao().guardar(
                AutorizacionEntidad(estado = EstadoAutorizacion.PENDIENTE_DE_CONFIRMAR.name),
            )
        }
        if (db.planDao().versionActual() == null) {
            db.planDao().guardarVersion(
                VersionPlanEntidad(
                    numero = 1,
                    fechaIso = LocalDate.now().toString(),
                    descripcion = "Plan inicial precargado",
                ),
            )
        }
        // Toca el repositorio para dejarlo listo antes de programar alarmas.
        Repositorio.obtener(this).configuracion()
    }
}
