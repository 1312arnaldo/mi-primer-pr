package com.misaludyfuerza.core

import com.misaludyfuerza.core.entrenamiento.RutinasBase
import com.misaludyfuerza.core.salud.AutorizacionProfesional
import com.misaludyfuerza.core.salud.EstadoAutorizacion
import com.misaludyfuerza.core.salud.FiltroRodilla
import com.misaludyfuerza.core.salud.ProtocoloSintomas
import com.misaludyfuerza.core.salud.ReporteSintoma
import com.misaludyfuerza.core.salud.VeredictoEjercicio
import com.misaludyfuerza.core.perfil.Ajustes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SaludTest {

    @Test
    fun `la autorizacion arranca pendiente de confirmar`() {
        val a = AutorizacionProfesional()
        assertEquals(EstadoAutorizacion.PENDIENTE_DE_CONFIRMAR, a.estado)
        assertFalse(a.puedeProgresarCarga)
        assertTrue(a.textoAviso.contains("propuesta para validar"))
        assertFalse(a.textoAviso.contains("rehabilitacion", ignoreCase = true) &&
            !a.textoAviso.contains("no es"), "No debe presentarse como rehabilitacion")
    }

    @Test
    fun `todo ejercicio con carga de pierna o impacto queda bloqueado`() {
        val prohibidos = listOf(
            "Sentadilla con barra", "Prensa de piernas", "Peso muerto rumano",
            "Zancadas con mancuernas", "Saltos al cajon", "Correr en cinta",
            "Extension de cuadriceps", "Curl femoral", "Elevacion de gemelos",
            "Hip thrust", "Step up al banco", "Eliptica", "Escaladora",
            "Sentadilla goblet", "Burpees", "Sprint",
        )
        prohibidos.forEach {
            assertEquals(
                VeredictoEjercicio.BLOQUEADO, FiltroRodilla.evaluar(it).veredicto,
                "Deberia bloquearse: $it",
            )
        }
    }

    @Test
    fun `los ejercicios de la propuesta no cargan la rodilla`() {
        assertEquals(
            emptyList(), RutinasBase.ejerciciosBloqueados(),
            "Ningun ejercicio del plan puede cargar la rodilla",
        )
    }

    @Test
    fun `el jalon sentado se marca para revision por la sujecion`() {
        val e = FiltroRodilla.evaluar("Jalon sentado")
        assertEquals(VeredictoEjercicio.REQUIERE_REVISION, e.veredicto)
        assertTrue(e.motivo.contains("sujecion") || e.motivo.contains("rodilla"))
    }

    @Test
    fun `los ejercicios de pie requieren revision`() {
        assertEquals(
            VeredictoEjercicio.REQUIERE_REVISION,
            FiltroRodilla.evaluar("Press militar de pie").veredicto,
        )
    }

    @Test
    fun `una prohibicion del profesional manda sobre el catalogo`() {
        val a = AutorizacionProfesional(
            estado = EstadoAutorizacion.AUTORIZADA_CON_LIMITES,
            ejerciciosProhibidos = setOf("Press de pecho con respaldo"),
        )
        assertEquals(
            VeredictoEjercicio.BLOQUEADO,
            FiltroRodilla.evaluar("Press de pecho con respaldo", a).veredicto,
        )
    }

    @Test
    fun `una autorizacion del profesional no desbloquea la carga de piernas`() {
        val a = AutorizacionProfesional(
            estado = EstadoAutorizacion.AUTORIZADA_CON_LIMITES,
            ejerciciosPermitidos = setOf("Prensa de piernas"),
        )
        assertEquals(
            VeredictoEjercicio.BLOQUEADO,
            FiltroRodilla.evaluar("Prensa de piernas", a).veredicto,
            "El filtro de piernas es duro: se revisa antes que la lista de permitidos",
        )
    }

    @Test
    fun `cualquier sintoma detiene el movimiento y congela la carga`() {
        listOf(
            ReporteSintoma("2026-09-14", dolor0a10 = 3),
            ReporteSintoma("2026-09-14", hinchazon = true),
            ReporteSintoma("2026-09-14", inestabilidad = true),
        ).forEach { r ->
            val resp = ProtocoloSintomas.responder(r)
            assertTrue(resp.detenerMovimiento)
            assertTrue(resp.bloquearAumentoDeCarga)
            assertTrue(resp.sugerirConsulta)
            assertTrue(resp.mensaje.contains("no aumentes carga"))
        }
    }

    @Test
    fun `sin sintomas no se dispara el protocolo`() {
        val resp = ProtocoloSintomas.responder(ReporteSintoma("2026-09-14"))
        assertFalse(resp.detenerMovimiento)
        assertFalse(resp.bloquearAumentoDeCarga)
    }

    @Test
    fun `no hay metas de pasos automaticas`() {
        assertFalse(Ajustes().metasDePasosAutomaticas)
    }
}
