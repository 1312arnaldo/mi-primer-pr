package com.misaludyfuerza.core

import com.misaludyfuerza.core.plan.CambioPropuesto
import com.misaludyfuerza.core.plan.HistorialPlan
import com.misaludyfuerza.core.plan.PropuestaDeCambio
import com.misaludyfuerza.core.plan.ValidadorPropuesta
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlanVersionTest {

    private fun propuesta(vararg cambios: CambioPropuesto) = PropuestaDeCambio(
        id = "p1", origen = "ChatGPT", fechaIso = "2026-09-28", cambios = cambios.toList(),
    )

    @Test
    fun `una propuesta valida se acepta con diferencias legibles`() {
        val r = ValidadorPropuesta.validar(
            propuesta(
                CambioPropuesto("nutricion.kcalObjetivo", "2000", "1900", "Tres semanas sin cambio de peso"),
            ),
        )
        assertTrue(r.valida)
        assertEquals(1, r.diferencias.size)
        assertTrue(r.diferencias[0].contains("2000 -> 1900"))
    }

    @Test
    fun `una propuesta no puede tocar las restricciones de salud`() {
        val r = ValidadorPropuesta.validar(
            propuesta(CambioPropuesto("salud.autorizacion", null, "AUTORIZADA_CON_LIMITES")),
        )
        assertFalse(r.valida)
        assertTrue(r.errores.any { it.contains("no se puede modificar") })
    }

    @Test
    fun `una propuesta no puede tocar los alimentos prohibidos ni al nino`() {
        listOf("preferencias.prohibidos", "nino.nombre", "custodia.ancla").forEach { ruta ->
            val r = ValidadorPropuesta.validar(propuesta(CambioPropuesto(ruta, null, "x")))
            assertFalse(r.valida, "La ruta $ruta deberia rechazarse")
        }
    }

    @Test
    fun `una ruta desconocida se rechaza`() {
        val r = ValidadorPropuesta.validar(propuesta(CambioPropuesto("cualquier.cosa", null, "1")))
        assertFalse(r.valida)
        assertTrue(r.errores.any { it.contains("Ruta desconocida") })
    }

    @Test
    fun `un esquema distinto se rechaza`() {
        val r = ValidadorPropuesta.validar(
            propuesta(CambioPropuesto("nutricion.kcalObjetivo", "2000", "1900")).copy(esquema = "otro.v9"),
        )
        assertFalse(r.valida)
        assertTrue(r.errores.any { it.contains("Esquema desconocido") })
    }

    @Test
    fun `un json mal formado no rompe la app`() {
        val r = ValidadorPropuesta.validar("{ esto no es json ")
        assertFalse(r.valida)
        assertTrue(r.errores.first().contains("No se pudo leer el JSON"))
    }

    @Test
    fun `se lee una propuesta desde json`() {
        val json = """
            {
              "esquema": "misaludyfuerza.propuesta.v1",
              "id": "p-42",
              "origen": "Codex",
              "fechaIso": "2026-09-28",
              "cambios": [
                { "ruta": "entrenamiento.descansoSeg", "valorAnterior": "120", "valorNuevo": "90",
                  "justificacion": "Para caber en 50 minutos" }
              ]
            }
        """.trimIndent()
        val r = ValidadorPropuesta.validar(json)
        assertTrue(r.valida, r.errores.toString())
        assertEquals("p-42", r.propuesta!!.id)
    }

    @Test
    fun `un cambio sin justificacion pasa pero deja aviso`() {
        val r = ValidadorPropuesta.validar(propuesta(CambioPropuesto("nutricion.kcalObjetivo", "2000", "1900")))
        assertTrue(r.valida)
        assertTrue(r.avisos.any { it.contains("no trae justificacion") })
    }

    @Test
    fun `nada se aplica sin aceptacion explicita del usuario`() {
        val historial = HistorialPlan()
        val r = ValidadorPropuesta.validar(
            propuesta(CambioPropuesto("nutricion.kcalObjetivo", "2000", "1900", "motivo")),
        )
        assertFailsWith<IllegalArgumentException> {
            historial.aceptar(r, aceptadaPorUsuario = false, fechaIso = "2026-09-28")
        }
        assertEquals(1, historial.actual.numero, "El plan no debe cambiar sin aceptacion")
    }

    @Test
    fun `aceptar crea version nueva y se puede revertir conservando historial`() {
        val historial = HistorialPlan()
        val r = ValidadorPropuesta.validar(
            propuesta(CambioPropuesto("nutricion.kcalObjetivo", "2000", "1900", "motivo")),
        )
        val v2 = historial.aceptar(r, aceptadaPorUsuario = true, fechaIso = "2026-09-28")
        assertEquals(2, v2.numero)
        assertEquals(1, v2.cambios.size)

        val v3 = historial.revertirA(1, "2026-09-30")
        assertEquals(3, v3.numero)
        assertTrue(v3.descripcion.contains("Reversion a la version 1"))
        assertEquals(3, historial.todas.size, "El historial anterior se conserva")
    }
}
