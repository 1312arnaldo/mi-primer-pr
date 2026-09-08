package com.misaludyfuerza.app.notificaciones

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.misaludyfuerza.app.MainActivity
import com.misaludyfuerza.app.R

object Canales {
    const val RECORDATORIOS = "recordatorios"
    const val PRUEBA = "prueba"

    fun crear(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(
                RECORDATORIOS,
                context.getString(R.string.canal_recordatorios_nombre),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.canal_recordatorios_desc)
                enableVibration(true)
                // Visible en el reloj a traves de Galaxy Wearable: sin datos sensibles en la pantalla de bloqueo.
                lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(
                PRUEBA,
                context.getString(R.string.canal_prueba_nombre),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = context.getString(R.string.canal_prueba_desc) },
        )
    }
}

/**
 * Estado real de los permisos y capacidades de aviso. La pantalla de Ajustes lo
 * muestra tal cual: si algo no esta concedido, la app lo dice en vez de simular
 * que funciona.
 */
data class EstadoAvisos(
    val notificacionesConcedidas: Boolean,
    val canalActivo: Boolean,
    val alarmasExactasConcedidas: Boolean,
    val necesitaPermisoNotificaciones: Boolean,
) {
    val resumen: String
        get() = buildList {
            add(if (notificacionesConcedidas) "Notificaciones permitidas." else "Notificaciones bloqueadas: la app no podra avisarte.")
            if (!canalActivo) add("El canal de recordatorios esta desactivado en los ajustes del sistema.")
            add(
                if (alarmasExactasConcedidas) {
                    "Alarmas exactas permitidas: los avisos llegan a la hora exacta."
                } else {
                    "Sin permiso de alarmas exactas: los avisos pueden llegar con unos minutos de retraso " +
                        "segun el ahorro de bateria. Puedes concederlo en Ajustes del sistema."
                },
            )
        }.joinToString(" ")
}

object Avisos {

    fun estado(context: Context): EstadoAvisos {
        val nmc = NotificationManagerCompat.from(context)
        val permiso = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        val canal = nmc.getNotificationChannel(Canales.RECORDATORIOS)
        return EstadoAvisos(
            notificacionesConcedidas = permiso && nmc.areNotificationsEnabled(),
            canalActivo = canal == null || canal.importance != NotificationManager.IMPORTANCE_NONE,
            alarmasExactasConcedidas = puedeProgramarAlarmasExactas(context),
            necesitaPermisoNotificaciones = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !permiso,
        )
    }

    fun puedeProgramarAlarmasExactas(context: Context): Boolean {
        val am = context.getSystemService(AlarmManager::class.java) ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) am.canScheduleExactAlarms() else true
    }

    /** Notificacion de prueba, para comprobar permisos y la entrega al Galaxy Watch. */
    fun enviarPrueba(context: Context) {
        Canales.crear(context)
        val abrir = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(context, Canales.PRUEBA)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Prueba de notificacion")
            .setContentText("Si ves esto en el telefono y en el reloj, los avisos funcionan.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Si ves esto en el telefono pero NO en el Galaxy Watch, abre Galaxy Wearable, " +
                        "entra en Notificaciones y activa \"Mi Salud y Fuerza\".",
                ),
            )
            .setContentIntent(abrir)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(ID_PRUEBA, n)
    }

    const val ID_PRUEBA = 999_001
}
