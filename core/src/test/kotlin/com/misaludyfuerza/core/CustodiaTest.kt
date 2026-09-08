package com.misaludyfuerza.core

import com.misaludyfuerza.core.agenda.Custodia
import com.misaludyfuerza.core.agenda.CustodiaEntreSemana
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CustodiaTest {

    private val custodia = Custodia()

    @Test
    fun `el ancla confirmada es un sabado`() {
        assertEquals(DayOfWeek.SATURDAY, Custodia.ANCLA_CONFIRMADA.dayOfWeek)
        assertEquals(LocalDate.of(2026, 9, 12), Custodia.ANCLA_CONFIRMADA)
    }

    @Test
    fun `la paridad alterna cada dos semanas desde el ancla`() {
        assertTrue(custodia.esSabadoDeCustodia(LocalDate.of(2026, 9, 12)))
        assertFalse(custodia.esSabadoDeCustodia(LocalDate.of(2026, 9, 19)))
        assertTrue(custodia.esSabadoDeCustodia(LocalDate.of(2026, 9, 26)))
        assertFalse(custodia.esSabadoDeCustodia(LocalDate.of(2026, 10, 3)))
        assertTrue(custodia.esSabadoDeCustodia(LocalDate.of(2026, 10, 10)))
    }

    @Test
    fun `la paridad tambien funciona hacia atras del ancla`() {
        assertTrue(custodia.esSabadoDeCustodia(LocalDate.of(2026, 8, 29)))
        assertFalse(custodia.esSabadoDeCustodia(LocalDate.of(2026, 9, 5)))
    }

    @Test
    fun `el periodo va del sabado 8 de la manana al lunes 7 y 40`() {
        val sabado = LocalDate.of(2026, 9, 12)
        assertFalse(custodia.tieneNinoEn(LocalDateTime.of(sabado, LocalTime.of(7, 59))))
        assertTrue(custodia.tieneNinoEn(LocalDateTime.of(sabado, LocalTime.of(8, 0))))
        assertTrue(custodia.tieneNinoEn(LocalDateTime.of(sabado.plusDays(1), LocalTime.of(23, 0))))
        assertTrue(custodia.tieneNinoEn(LocalDateTime.of(sabado.plusDays(2), LocalTime.of(7, 39))))
        assertFalse(custodia.tieneNinoEn(LocalDateTime.of(sabado.plusDays(2), LocalTime.of(7, 40))))
    }

    @Test
    fun `el lunes por la manana pertenece al fin de semana anterior`() {
        val lunes = LocalDate.of(2026, 9, 14)
        assertEquals(LocalDate.of(2026, 9, 12), custodia.sabadoDelFinDeSemana(lunes))
        assertTrue(custodia.durmioPorCustodia(lunes))
        assertFalse(custodia.durmioPorCustodia(LocalDate.of(2026, 9, 21)))
    }

    @Test
    fun `una excepcion puede cancelar o forzar un fin de semana sin romper la paridad`() {
        val conExcepcion = Custodia(excepciones = mapOf(LocalDate.of(2026, 9, 12) to false))
        assertFalse(conExcepcion.esSabadoDeCustodia(LocalDate.of(2026, 9, 12)))
        // La paridad general se mantiene para los demas sabados.
        assertTrue(conExcepcion.esSabadoDeCustodia(LocalDate.of(2026, 9, 26)))
    }

    @Test
    fun `martes y jueves generan noche con el nino y la hora es una estimacion`() {
        val entreSemana = CustodiaEntreSemana()
        assertFalse(entreSemana.horaRecogidaConfirmada)
        assertEquals(LocalTime.of(19, 15), entreSemana.horaRecogidaEstimada)

        assertTrue(entreSemana.hayRecogida(LocalDate.of(2026, 9, 8)))  // martes
        assertTrue(entreSemana.hayRecogida(LocalDate.of(2026, 9, 10))) // jueves
        assertFalse(entreSemana.hayRecogida(LocalDate.of(2026, 9, 9))) // miercoles

        // Miercoles y viernes por la manana el nino ya durmio en casa.
        assertTrue(entreSemana.durmioPorRecogida(LocalDate.of(2026, 9, 9)))
        assertTrue(entreSemana.durmioPorRecogida(LocalDate.of(2026, 9, 11)))
        assertFalse(entreSemana.durmioPorRecogida(LocalDate.of(2026, 9, 8)))
    }

    @Test
    fun `proximos sabados de custodia`() {
        val proximos = custodia.proximosSabados(LocalDate.of(2026, 9, 8), 3)
        assertEquals(
            listOf(LocalDate.of(2026, 9, 12), LocalDate.of(2026, 9, 26), LocalDate.of(2026, 10, 10)),
            proximos,
        )
    }
}
