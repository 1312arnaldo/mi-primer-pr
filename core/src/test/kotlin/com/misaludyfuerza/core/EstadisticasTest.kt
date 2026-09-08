package com.misaludyfuerza.core

import com.misaludyfuerza.core.estadisticas.Medicion
import com.misaludyfuerza.core.estadisticas.ReferenciaPerdida
import com.misaludyfuerza.core.estadisticas.ReglasEnergia
import com.misaludyfuerza.core.estadisticas.Series
import com.misaludyfuerza.core.estadisticas.Veredicto
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EstadisticasTest {

    private val desde = LocalDate.of(2026, 9, 8)

    @Test
    fun `un solo dia nunca es una tendencia`() {
        val t = Series.tendencia(
            listOf(Medicion("2026-09-08", 91.0)), desde, desde.plusDays(13),
        )
        assertEquals(Veredicto.DATOS_INSUFICIENTES, t.veredicto)
        assertTrue(t.mensaje.contains("no hay datos suficientes", ignoreCase = true))
    }

    @Test
    fun `la cobertura se reporta siempre`() {
        val mediciones = listOf(
            Medicion("2026-09-08", 91.0),
            Medicion("2026-09-10", 90.8),
            Medicion("2026-09-12", 90.6),
        )
        val c = Series.cobertura(mediciones, desde, desde.plusDays(13))
        assertEquals(3, c.diasConDatos)
        assertEquals(14, c.diasDelPeriodo)
        assertTrue(c.texto.contains("3 de 14"))
    }

    @Test
    fun `con datos suficientes detecta bajada`() {
        val mediciones = (0..20 step 2).map {
            Medicion(desde.plusDays(it.toLong()).toString(), 91.0 - it * 0.05)
        }
        val t = Series.tendencia(mediciones, desde, desde.plusDays(20))
        assertEquals(Veredicto.BAJANDO, t.veredicto)
        assertTrue(t.cambioPorSemana!! < 0)
    }

    @Test
    fun `un cambio minimo se reporta como sin cambio claro`() {
        val mediciones = (0..20 step 2).map {
            Medicion(desde.plusDays(it.toLong()).toString(), 91.0 + (it % 4) * 0.01)
        }
        val t = Series.tendencia(mediciones, desde, desde.plusDays(20))
        assertEquals(Veredicto.SIN_CAMBIO_CLARO, t.veredicto)
    }

    @Test
    fun `los promedios semanales agrupan por semana`() {
        val mediciones = listOf(
            Medicion("2026-09-07", 91.0), Medicion("2026-09-09", 90.0),
            Medicion("2026-09-14", 89.0), Medicion("2026-09-16", 89.0),
        )
        val promedios = Series.promediosSemanales(mediciones)
        assertEquals(2, promedios.size)
        assertEquals(90.5, promedios[0].promedio)
        assertEquals(89.0, promedios[1].promedio)
        assertEquals(2, promedios[0].muestras)
    }

    @Test
    fun `la referencia de perdida es orientativa y no promete nada`() {
        assertTrue(ReferenciaPerdida.comentar(-0.5).contains("referencia orientativa"))
        assertTrue(ReferenciaPerdida.comentar(-1.5).contains("por encima"))
        assertTrue(ReferenciaPerdida.comentar(null).contains("Sin datos suficientes"))
        assertTrue(ReferenciaPerdida.comentar(0.2).contains("una sola semana no basta"))
    }

    @Test
    fun `sumar calorias activas y totales esta prohibido por diseno`() {
        val e = assertFailsWith<IllegalStateException> {
            ReglasEnergia.sumarActivasYTotales(400.0, 2500.0)
        }
        assertTrue(e.message!!.contains("se solapan"))
    }

    @Test
    fun `la ingesta no se ajusta sola por el reloj`() {
        assertFalse(ReglasEnergia.AJUSTE_AUTOMATICO_DE_INGESTA_HABILITADO)
    }
}
