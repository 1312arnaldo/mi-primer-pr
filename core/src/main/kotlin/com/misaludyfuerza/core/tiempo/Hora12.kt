package com.misaludyfuerza.core.tiempo

import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Month
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Formato de presentacion en espanol. Regla del proyecto: TODA hora visible al
 * usuario se muestra en formato de 12 horas con "a. m." / "p. m.".
 *
 * Internamente el dominio usa [LocalTime]/[LocalDateTime]/ISO-8601; esta clase es
 * la unica frontera de presentacion. Nunca formatee horas a mano en la UI.
 */
object Hora12 {

    /** Zona horaria del plan. Los cambios de horario de verano los resuelve la tzdb. */
    val ZONA: ZoneId = ZoneId.of("America/New_York")

    private const val ANTE_MERIDIEM = "a. m."
    private const val POST_MERIDIEM = "p. m."

    /** 7:45 -> "7:45 a. m."; 19:00 -> "7:00 p. m."; 00:30 -> "12:30 a. m."; 12:00 -> "12:00 p. m." */
    fun hora(t: LocalTime): String {
        val sufijo = if (t.hour < 12) ANTE_MERIDIEM else POST_MERIDIEM
        val h12 = when (val h = t.hour % 12) {
            0 -> 12
            else -> h
        }
        return "%d:%02d %s".format(h12, t.minute, sufijo)
    }

    fun hora(t: LocalDateTime): String = hora(t.toLocalTime())

    fun hora(t: ZonedDateTime): String = hora(t.withZoneSameInstant(ZONA).toLocalTime())

    /** "7:45 a. m. - 8:35 a. m." */
    fun rango(inicio: LocalTime, fin: LocalTime): String = "${hora(inicio)} - ${hora(fin)}"

    fun rango(inicio: LocalDateTime, fin: LocalDateTime): String =
        if (inicio.toLocalDate() == fin.toLocalDate()) rango(inicio.toLocalTime(), fin.toLocalTime())
        else "${fechaHora(inicio)} - ${fechaHora(fin)}"

    private val DIAS = mapOf(
        DayOfWeek.MONDAY to "lunes",
        DayOfWeek.TUESDAY to "martes",
        DayOfWeek.WEDNESDAY to "miercoles",
        DayOfWeek.THURSDAY to "jueves",
        DayOfWeek.FRIDAY to "viernes",
        DayOfWeek.SATURDAY to "sabado",
        DayOfWeek.SUNDAY to "domingo",
    )

    private val DIAS_ACENTUADOS = mapOf(
        DayOfWeek.MONDAY to "lunes",
        DayOfWeek.TUESDAY to "martes",
        DayOfWeek.WEDNESDAY to "miércoles",
        DayOfWeek.THURSDAY to "jueves",
        DayOfWeek.FRIDAY to "viernes",
        DayOfWeek.SATURDAY to "sábado",
        DayOfWeek.SUNDAY to "domingo",
    )

    private val MESES = mapOf(
        Month.JANUARY to "enero", Month.FEBRUARY to "febrero", Month.MARCH to "marzo",
        Month.APRIL to "abril", Month.MAY to "mayo", Month.JUNE to "junio",
        Month.JULY to "julio", Month.AUGUST to "agosto", Month.SEPTEMBER to "septiembre",
        Month.OCTOBER to "octubre", Month.NOVEMBER to "noviembre", Month.DECEMBER to "diciembre",
    )

    /** Nombre del dia sin acentos (para claves internas / nombres de archivo). */
    fun diaSemanaSimple(d: DayOfWeek): String = DIAS.getValue(d)

    /** Nombre del dia con acentos (para la interfaz). */
    fun diaSemana(d: DayOfWeek): String = DIAS_ACENTUADOS.getValue(d)

    fun mes(m: Month): String = MESES.getValue(m)

    /** "martes, 8 de septiembre de 2026" */
    fun fechaLarga(f: LocalDate): String =
        "${diaSemana(f.dayOfWeek)}, ${f.dayOfMonth} de ${mes(f.month)} de ${f.year}"

    /** "8 de septiembre" */
    fun fechaCorta(f: LocalDate): String = "${f.dayOfMonth} de ${mes(f.month)}"

    /** "martes, 8 de septiembre de 2026, 7:45 a. m." */
    fun fechaHora(t: LocalDateTime): String = "${fechaLarga(t.toLocalDate())}, ${hora(t)}"

    /** "50 minutos", "1 h 30 min" */
    fun duracion(d: Duration): String {
        val total = d.toMinutes()
        val h = total / 60
        val m = total % 60
        return when {
            h == 0L -> "$m minutos"
            m == 0L -> "$h h"
            else -> "$h h $m min"
        }
    }

    /** Instante absoluto para una fecha/hora local del plan, resolviendo DST. */
    fun enZona(fecha: LocalDate, hora: LocalTime): ZonedDateTime =
        ZonedDateTime.of(fecha, hora, ZONA)
}
