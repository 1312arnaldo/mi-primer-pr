package com.misaludyfuerza.app.notificaciones

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.misaludyfuerza.app.datos.Repositorio
import com.misaludyfuerza.core.agenda.EstadoEvento
import com.misaludyfuerza.core.agenda.EventoPlan
import com.misaludyfuerza.core.tiempo.Hora12
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Programa los recordatorios con AlarmManager.
 *
 * Notas honestas sobre limites reales de Android:
 * - Solo se programan los proximos [DIAS_POR_ADELANTADO] dias. WorkManager vuelve
 *   a llenar la ventana una vez al dia; WorkManager NO es un mecanismo de alarma
 *   exacta y por eso no se usa para disparar el aviso en si.
 * - Si el usuario no concede el permiso de alarmas exactas, se usan alarmas
 *   inexactas y la app lo indica en pantalla en lugar de prometer puntualidad.
 * - El ahorro de bateria del fabricante puede retrasar cualquier aviso. Eso no se
 *   puede garantizar desde la app; se explica en Ajustes.
 */
object ProgramadorAlarmas {

    const val DIAS_POR_ADELANTADO = 3

    const val EXTRA_UID = "uid"
    const val EXTRA_TITULO = "titulo"
    const val EXTRA_TEXTO = "texto"
    const val EXTRA_FECHA = "fecha"
    const val EXTRA_HORA_MIN = "horaMin"
    const val EXTRA_TIPO = "tipo"

    suspend fun reprogramarTodo(context: Context) {
        val repo = Repositorio.obtener(context)
        val cfg = repo.configuracion()
        val ajustes = repo.ajustes()
        val zona = ZoneId.of(repo.perfil().zonaHoraria)
        val ahora = LocalDateTime.now(zona)
        val hoy = ahora.toLocalDate()

        repo.guardarAjustes(
            ajustes.copy(alarmasExactasDisponibles = Avisos.puedeProgramarAlarmasExactas(context)),
        )

        for (i in 0 until DIAS_POR_ADELANTADO) {
            val dia = hoy.plusDays(i.toLong())
            // Nunca se generan avisos anteriores a la fecha de activacion del plan.
            if (dia.isBefore(cfg.fechaActivacion)) continue

            val registros = repo.eventosDe(dia)
            val estados = repo.registrosDeEventos(dia)
            registros.forEach { evento ->
                val registro = estados[evento.uid]
                val estado = runCatching { EstadoEvento.valueOf(registro?.estado ?: "") }
                    .getOrDefault(EstadoEvento.PENDIENTE)
                if (estado == EstadoEvento.HECHO || estado == EstadoEvento.OMITIDO) {
                    cancelar(context, evento.uid)
                    return@forEach
                }
                val hora = registro?.nuevaHoraMin?.let { LocalTime.ofSecondOfDay(it * 60L) }
                    ?: evento.inicio
                val cuando = LocalDateTime.of(dia, hora).minusMinutes(ajustes.margenAvisoMin.toLong())
                // No se disparan avisos vencidos.
                if (cuando.isBefore(ahora)) return@forEach
                programar(context, evento, dia, hora, cuando.atZone(zona).toInstant().toEpochMilli())
            }
        }
    }

    fun programar(
        context: Context,
        evento: EventoPlan,
        fecha: LocalDate,
        hora: LocalTime,
        cuandoMillis: Long,
    ) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val pi = pendingIntent(context, evento, fecha, hora)
        val exactas = Avisos.puedeProgramarAlarmasExactas(context)
        if (exactas) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cuandoMillis, pi)
        } else {
            // Sin permiso de exactitud: ventana de 10 minutos, y la UI lo advierte.
            am.setWindow(AlarmManager.RTC_WAKEUP, cuandoMillis, 10 * 60_000L, pi)
        }
    }

    fun cancelar(context: Context, uid: String) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val intent = Intent(context, RecordatorioReceiver::class.java).apply {
            action = ACCION_RECORDATORIO
            data = android.net.Uri.parse("misaludyfuerza://evento/$uid")
        }
        val pi = PendingIntent.getBroadcast(
            context, uid.hashCode(), intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
        if (pi != null) {
            am.cancel(pi)
            pi.cancel()
        }
    }

    /** Reprograma un unico evento pospuesto. */
    fun posponer(
        context: Context,
        uid: String,
        titulo: String,
        texto: String,
        fecha: LocalDate,
        horaOriginal: LocalTime,
        nuevaHora: LocalTime,
        zona: ZoneId,
    ) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val intent = Intent(context, RecordatorioReceiver::class.java).apply {
            action = ACCION_RECORDATORIO
            data = android.net.Uri.parse("misaludyfuerza://evento/$uid")
            putExtra(EXTRA_UID, uid)
            putExtra(EXTRA_TITULO, titulo)
            putExtra(EXTRA_TEXTO, texto)
            putExtra(EXTRA_FECHA, fecha.toString())
            putExtra(EXTRA_HORA_MIN, horaOriginal.toSecondOfDay() / 60)
        }
        val pi = PendingIntent.getBroadcast(
            context, uid.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val cuando = LocalDateTime.of(fecha, nuevaHora).atZone(zona).toInstant().toEpochMilli()
        if (Avisos.puedeProgramarAlarmasExactas(context)) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cuando, pi)
        } else {
            am.setWindow(AlarmManager.RTC_WAKEUP, cuando, 10 * 60_000L, pi)
        }
    }

    private fun pendingIntent(
        context: Context,
        evento: EventoPlan,
        fecha: LocalDate,
        hora: LocalTime,
    ): PendingIntent {
        val texto = buildString {
            append(Hora12.hora(hora))
            evento.nota?.let { append(" - ").append(it) }
        }
        val intent = Intent(context, RecordatorioReceiver::class.java).apply {
            action = ACCION_RECORDATORIO
            // El uid en el data hace unico el PendingIntent sin colisiones de requestCode.
            data = android.net.Uri.parse("misaludyfuerza://evento/${evento.uid}")
            putExtra(EXTRA_UID, evento.uid)
            putExtra(EXTRA_TITULO, evento.titulo)
            putExtra(EXTRA_TEXTO, texto)
            putExtra(EXTRA_FECHA, fecha.toString())
            putExtra(EXTRA_HORA_MIN, hora.toSecondOfDay() / 60)
            putExtra(EXTRA_TIPO, evento.tipo.name)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, evento.uid.hashCode(), intent, flags)
    }

    const val ACCION_RECORDATORIO = "com.misaludyfuerza.app.RECORDATORIO"
}
