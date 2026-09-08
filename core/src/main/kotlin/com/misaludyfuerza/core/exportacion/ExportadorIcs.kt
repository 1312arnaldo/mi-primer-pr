package com.misaludyfuerza.core.exportacion

import com.misaludyfuerza.core.agenda.EventoPlan
import com.misaludyfuerza.core.agenda.TipoEvento
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Exporta la agenda a ICS. Cada evento conserva su UID estable, de modo que
 * reimportar o resincronizar actualiza el evento existente en vez de duplicarlo.
 *
 * Las horas van con TZID=America/New_York y el archivo incluye el bloque
 * VTIMEZONE con las reglas de horario de verano de Estados Unidos, para que
 * cualquier calendario las interprete igual.
 */
object ExportadorIcs {

    private const val ZONA = "America/New_York"
    private const val DOMINIO_UID = "misaludyfuerza.app"
    private val LOCAL = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
    private val UTC = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")

    private val EVENTOS_DEL_NINO = setOf(
        TipoEvento.NINO_RECIBIR, TipoEvento.NINO_PREPARAR, TipoEvento.NINO_ESCUELA,
        TipoEvento.NINO_RECOGER, TipoEvento.NINO_CUSTODIA_INICIO, TipoEvento.NINO_CUSTODIA_FIN,
        TipoEvento.PREPARAR_COSAS_NINO,
    )

    fun exportar(
        eventos: List<EventoPlan>,
        nombreCalendario: String = "Mi Salud y Fuerza",
        generadoEn: LocalDateTime = LocalDateTime.now(),
        anonimizarNino: Boolean = false,
    ): String {
        val sb = StringBuilder()
        sb.linea("BEGIN:VCALENDAR")
        sb.linea("VERSION:2.0")
        sb.linea("PRODID:-//Mi Salud y Fuerza//Agenda//ES")
        sb.linea("CALSCALE:GREGORIAN")
        sb.linea("METHOD:PUBLISH")
        sb.linea("X-WR-CALNAME:${escapar(nombreCalendario)}")
        sb.linea("X-WR-TIMEZONE:$ZONA")
        sb.append(bloqueVtimezone())

        val marca = generadoEn.atZone(java.time.ZoneId.systemDefault())
            .withZoneSameInstant(ZoneOffset.UTC).format(UTC)

        eventos.forEach { e ->
            val titulo = if (anonimizarNino && e.tipo in EVENTOS_DEL_NINO) "Compromiso familiar"
            else e.titulo
            sb.linea("BEGIN:VEVENT")
            sb.linea("UID:${e.uid}@$DOMINIO_UID")
            sb.linea("DTSTAMP:$marca")
            sb.linea("DTSTART;TZID=$ZONA:${e.inicioLdt.format(LOCAL)}")
            val fin = e.finLdt ?: e.inicioLdt.plusMinutes(15)
            sb.linea("DTEND;TZID=$ZONA:${fin.format(LOCAL)}")
            sb.linea("SUMMARY:${escapar(titulo)}")
            val descripcion = buildList {
                e.nota?.let { add(it) }
                if (!e.confirmado) add("Estimacion sin confirmar; editable.")
                if (e.requiereValidacionMedica) {
                    add("Propuesta pendiente de validar con tu traumatologo o fisioterapeuta.")
                }
            }.joinToString(" ")
            if (descripcion.isNotBlank()) sb.linea("DESCRIPTION:${escapar(descripcion)}")
            sb.linea("CATEGORIES:${e.tipo.name}")
            sb.linea("END:VEVENT")
        }
        sb.linea("END:VCALENDAR")
        return sb.toString()
    }

    /** Reglas de horario de verano de Estados Unidos vigentes desde 2007. */
    private fun bloqueVtimezone(): String = buildString {
        linea("BEGIN:VTIMEZONE")
        linea("TZID:$ZONA")
        linea("BEGIN:DAYLIGHT")
        linea("TZOFFSETFROM:-0500")
        linea("TZOFFSETTO:-0400")
        linea("TZNAME:EDT")
        linea("DTSTART:20070311T020000")
        linea("RRULE:FREQ=YEARLY;BYMONTH=3;BYDAY=2SU")
        linea("END:DAYLIGHT")
        linea("BEGIN:STANDARD")
        linea("TZOFFSETFROM:-0400")
        linea("TZOFFSETTO:-0500")
        linea("TZNAME:EST")
        linea("DTSTART:20071104T020000")
        linea("RRULE:FREQ=YEARLY;BYMONTH=11;BYDAY=1SU")
        linea("END:STANDARD")
        linea("END:VTIMEZONE")
    }

    private fun StringBuilder.linea(s: String) {
        append(s).append("\r\n")
    }

    private fun escapar(s: String): String = s
        .replace("\\", "\\\\")
        .replace(";", "\\;")
        .replace(",", "\\,")
        .replace("\n", "\\n")
}
