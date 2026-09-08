package com.misaludyfuerza.app.notificaciones

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.misaludyfuerza.app.MainActivity
import com.misaludyfuerza.app.datos.Repositorio
import com.misaludyfuerza.core.agenda.EstadoEvento
import com.misaludyfuerza.core.tiempo.Hora12
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** Muestra el recordatorio con las acciones Hecho, Posponer y Omitir. */
class RecordatorioReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val uid = intent.getStringExtra(ProgramadorAlarmas.EXTRA_UID) ?: return
        val titulo = intent.getStringExtra(ProgramadorAlarmas.EXTRA_TITULO) ?: return
        val texto = intent.getStringExtra(ProgramadorAlarmas.EXTRA_TEXTO).orEmpty()
        val fecha = intent.getStringExtra(ProgramadorAlarmas.EXTRA_FECHA) ?: return
        val horaMin = intent.getIntExtra(ProgramadorAlarmas.EXTRA_HORA_MIN, 0)

        Canales.crear(context)

        fun accion(nombre: String, etiqueta: String, icono: Int): NotificationCompat.Action {
            val i = Intent(context, AccionRecordatorioReceiver::class.java).apply {
                action = nombre
                data = android.net.Uri.parse("misaludyfuerza://accion/$nombre/$uid")
                putExtra(ProgramadorAlarmas.EXTRA_UID, uid)
                putExtra(ProgramadorAlarmas.EXTRA_TITULO, titulo)
                putExtra(ProgramadorAlarmas.EXTRA_TEXTO, texto)
                putExtra(ProgramadorAlarmas.EXTRA_FECHA, fecha)
                putExtra(ProgramadorAlarmas.EXTRA_HORA_MIN, horaMin)
            }
            val pi = PendingIntent.getBroadcast(
                context, (nombre + uid).hashCode(), i,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            return NotificationCompat.Action.Builder(icono, etiqueta, pi).build()
        }

        val abrir = PendingIntent.getActivity(
            context, uid.hashCode(),
            Intent(context, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_ABRIR_FECHA, fecha)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notificacion = NotificationCompat.Builder(context, Canales.RECORDATORIOS)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(abrir)
            .setAutoCancel(true)
            .addAction(accion(AccionRecordatorioReceiver.HECHO, "Hecho", android.R.drawable.ic_menu_save))
            .addAction(accion(AccionRecordatorioReceiver.POSPONER, "Posponer", android.R.drawable.ic_menu_recent_history))
            .addAction(accion(AccionRecordatorioReceiver.OMITIR, "Omitir", android.R.drawable.ic_menu_close_clear_cancel))
            .build()

        NotificationManagerCompat.from(context).notify(uid.hashCode(), notificacion)
    }
}

/**
 * Procesa las acciones de la notificacion.
 *
 * "Hecho" en un recordatorio de comida marca el EVENTO como atendido, no la comida
 * como consumida: confirmar lo que se comio se hace en la pantalla de Comidas.
 */
class AccionRecordatorioReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val uid = intent.getStringExtra(ProgramadorAlarmas.EXTRA_UID) ?: return
        val fechaIso = intent.getStringExtra(ProgramadorAlarmas.EXTRA_FECHA) ?: return
        val horaMin = intent.getIntExtra(ProgramadorAlarmas.EXTRA_HORA_MIN, 0)
        val titulo = intent.getStringExtra(ProgramadorAlarmas.EXTRA_TITULO).orEmpty()
        val texto = intent.getStringExtra(ProgramadorAlarmas.EXTRA_TEXTO).orEmpty()
        val accion = intent.action ?: return

        NotificationManagerCompat.from(context).cancel(uid.hashCode())

        val pendiente = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = Repositorio.obtener(context)
                val zona = ZoneId.of(repo.perfil().zonaHoraria)
                val ahora = LocalDateTime.now(zona)
                val fecha = LocalDate.parse(fechaIso)
                val horaOriginal = LocalTime.ofSecondOfDay(horaMin * 60L)

                when (accion) {
                    HECHO -> repo.registrarEventoPorUid(
                        uid, fecha, horaOriginal, EstadoEvento.HECHO, ahoraIso = ahora.toString(),
                    )
                    OMITIR -> repo.registrarEventoPorUid(
                        uid, fecha, horaOriginal, EstadoEvento.OMITIDO, ahoraIso = ahora.toString(),
                    )
                    POSPONER -> {
                        val minutos = repo.ajustes().minutosPosponer.toLong()
                        val nueva = ahora.toLocalTime().plusMinutes(minutos)
                        // La fecha y la hora originales se conservan en el registro.
                        repo.registrarEventoPorUid(
                            uid, fecha, horaOriginal, EstadoEvento.POSPUESTO,
                            nuevaHora = nueva, ahoraIso = ahora.toString(),
                        )
                        ProgramadorAlarmas.posponer(
                            context, uid, titulo,
                            "$texto (pospuesto a ${Hora12.hora(nueva)})",
                            fecha, horaOriginal, nueva, zona,
                        )
                    }
                }
            } finally {
                pendiente.finish()
            }
        }
    }

    companion object {
        const val HECHO = "com.misaludyfuerza.app.HECHO"
        const val POSPONER = "com.misaludyfuerza.app.POSPONER"
        const val OMITIR = "com.misaludyfuerza.app.OMITIR"
    }
}

/**
 * Reprograma los avisos despues de reiniciar el telefono, actualizar la app,
 * cambiar la zona horaria o ajustar la hora del sistema. Sin esto, un reinicio
 * borraria todas las alarmas.
 */
class ReprogramarReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val accion = intent.action ?: return
        if (accion !in ACCIONES) return
        val pendiente = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Canales.crear(appContext)
                ProgramadorAlarmas.reprogramarTodo(appContext)
                ReprogramacionDiaria.programar(appContext)
            } finally {
                pendiente.finish()
            }
        }
    }

    private companion object {
        val ACCIONES = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
        )
    }
}
