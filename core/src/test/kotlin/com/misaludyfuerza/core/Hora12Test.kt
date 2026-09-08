package com.misaludyfuerza.core

import com.misaludyfuerza.core.tiempo.Hora12
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Hora12Test {

    @Test
    fun `formatea las horas del plan en 12 horas`() {
        assertEquals("7:45 a. m.", Hora12.hora(LocalTime.of(7, 45)))
        assertEquals("7:00 p. m.", Hora12.hora(LocalTime.of(19, 0)))
        assertEquals("6:20 a. m.", Hora12.hora(LocalTime.of(6, 20)))
        assertEquals("1:00 p. m.", Hora12.hora(LocalTime.of(13, 0)))
        assertEquals("8:35 a. m.", Hora12.hora(LocalTime.of(8, 35)))
    }

    @Test
    fun `medianoche y mediodia se muestran como 12`() {
        assertEquals("12:00 a. m.", Hora12.hora(LocalTime.MIDNIGHT))
        assertEquals("12:30 a. m.", Hora12.hora(LocalTime.of(0, 30)))
        assertEquals("12:00 p. m.", Hora12.hora(LocalTime.NOON))
        assertEquals("12:45 p. m.", Hora12.hora(LocalTime.of(12, 45)))
        assertEquals("11:59 p. m.", Hora12.hora(LocalTime.of(23, 59)))
    }

    @Test
    fun `ninguna hora visible usa formato de 24 horas`() {
        val patron = Regex("""^\d{1,2}:\d{2} [ap]\. m\.$""")
        for (h in 0..23) for (m in listOf(0, 5, 30, 59)) {
            val texto = Hora12.hora(LocalTime.of(h, m))
            assertTrue(patron.matches(texto), "Formato invalido: $texto")
            val horaMostrada = texto.substringBefore(':').toInt()
            assertTrue(horaMostrada in 1..12, "Hora fuera de 1-12: $texto")
        }
    }

    @Test
    fun `rangos y fechas en espanol`() {
        assertEquals("7:45 a. m. - 8:35 a. m.", Hora12.rango(LocalTime.of(7, 45), LocalTime.of(8, 35)))
        assertEquals("martes, 8 de septiembre de 2026", Hora12.fechaLarga(LocalDate.of(2026, 9, 8)))
        assertEquals("sábado, 12 de septiembre de 2026", Hora12.fechaLarga(LocalDate.of(2026, 9, 12)))
    }

    @Test
    fun `duraciones legibles`() {
        assertEquals("50 minutos", Hora12.duracion(Duration.ofMinutes(50)))
        assertEquals("1 h 30 min", Hora12.duracion(Duration.ofMinutes(90)))
        assertEquals("2 h", Hora12.duracion(Duration.ofHours(2)))
    }

    @Test
    fun `la zona resuelve el horario de verano`() {
        // 8 de septiembre de 2026: horario de verano (EDT, -04:00).
        val verano = Hora12.enZona(LocalDate.of(2026, 9, 8), LocalTime.of(7, 45))
        assertEquals("-04:00", verano.offset.id)
        // 8 de diciembre de 2026: horario estandar (EST, -05:00).
        val invierno = Hora12.enZona(LocalDate.of(2026, 12, 8), LocalTime.of(7, 45))
        assertEquals("-05:00", invierno.offset.id)
    }
}
