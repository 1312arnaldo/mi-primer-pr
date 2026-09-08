package com.misaludyfuerza.core

import com.misaludyfuerza.core.agenda.CalendarioEscolar
import com.misaludyfuerza.core.agenda.ConfiguracionPlan
import com.misaludyfuerza.core.agenda.DecisionGimnasio
import com.misaludyfuerza.core.agenda.DetectorConflictos
import com.misaludyfuerza.core.agenda.GeneradorAgenda
import com.misaludyfuerza.core.agenda.ReglasGimnasio
import com.misaludyfuerza.core.agenda.TipoEvento
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AgendaTest {

    private val cfg = ConfiguracionPlan()

    // Semana de referencia: martes 8 de septiembre de 2026 en adelante.
    private val martes = LocalDate.of(2026, 9, 8)
    private val miercoles = LocalDate.of(2026, 9, 9)
    private val jueves = LocalDate.of(2026, 9, 10)
    private val viernes = LocalDate.of(2026, 9, 11)
    private val sabadoCustodia = LocalDate.of(2026, 9, 12)
    private val domingoCustodia = LocalDate.of(2026, 9, 13)
    private val lunesTrasCustodia = LocalDate.of(2026, 9, 14)
    private val lunesNormal = LocalDate.of(2026, 9, 21)

    private fun tipos(f: LocalDate) = GeneradorAgenda.generarDia(f, cfg).map { it.tipo }

    @Test
    fun `el gimnasio solo se programa lunes miercoles y viernes`() {
        assertTrue(TipoEvento.GIMNASIO in tipos(lunesNormal))
        assertTrue(TipoEvento.GIMNASIO in tipos(miercoles))
        assertTrue(TipoEvento.GIMNASIO in tipos(viernes))
        assertFalse(TipoEvento.GIMNASIO in tipos(martes))
        assertFalse(TipoEvento.GIMNASIO in tipos(jueves))
    }

    @Test
    fun `sin gimnasio durante el fin de semana con el nino`() {
        assertFalse(TipoEvento.GIMNASIO in tipos(sabadoCustodia))
        assertFalse(TipoEvento.GIMNASIO in tipos(domingoCustodia))
        val d = ReglasGimnasio.decidir(sabadoCustodia, cfg)
        assertTrue(d is DecisionGimnasio.NoProgramado)
    }

    @Test
    fun `ningun fin de semana lleva gimnasio obligatorio`() {
        val sabadoSinCustodia = LocalDate.of(2026, 9, 19)
        assertFalse(TipoEvento.GIMNASIO in tipos(sabadoSinCustodia))
        assertFalse(TipoEvento.GIMNASIO in tipos(sabadoSinCustodia.plusDays(1)))
    }

    @Test
    fun `el lunes despues del fin de semana con el nino si permite gimnasio`() {
        val d = ReglasGimnasio.decidir(lunesTrasCustodia, cfg)
        assertTrue(d is DecisionGimnasio.Programado, "El lunes tras la custodia debe permitir gimnasio")
        assertTrue(TipoEvento.GIMNASIO in tipos(lunesTrasCustodia))
    }

    @Test
    fun `el gimnasio nunca empieza antes de dejar al nino en la escuela`() {
        listOf(lunesNormal, miercoles, viernes, lunesTrasCustodia).forEach { f ->
            val eventos = GeneradorAgenda.generarDia(f, cfg)
            val entrega = eventos.first { it.tipo == TipoEvento.NINO_ESCUELA }
            val gimnasio = eventos.first { it.tipo == TipoEvento.GIMNASIO }
            assertTrue(
                !gimnasio.inicio.isBefore(entrega.fin!!),
                "El gimnasio de $f empieza antes de la entrega en la escuela",
            )
        }
    }

    @Test
    fun `un cierre escolar no programa gimnasio automaticamente`() {
        val conCierre = cfg.copy(escuela = CalendarioEscolar(cierres = setOf(miercoles)))
        val d = ReglasGimnasio.decidir(miercoles, conCierre)
        assertTrue(d is DecisionGimnasio.NoProgramado)
        assertTrue(d.motivo.contains("Cierre escolar"))
        val eventos = GeneradorAgenda.generarDia(miercoles, conCierre)
        assertFalse(eventos.any { it.tipo == TipoEvento.GIMNASIO })
        assertFalse(eventos.any { it.tipo == TipoEvento.NINO_ESCUELA })
    }

    @Test
    fun `no se duplica la entrega del nino cuando ya durmio en casa`() {
        // Miercoles y viernes: durmio en casa tras la recogida de martes/jueves.
        listOf(miercoles, viernes).forEach { f ->
            val t = tipos(f)
            assertTrue(TipoEvento.NINO_PREPARAR in t, "Falta Preparar al nino en $f")
            assertFalse(TipoEvento.NINO_RECIBIR in t, "No debe haber entrega que recibir en $f")
        }
        // Lunes tras el fin de semana de custodia: tambien durmio en casa.
        val tLunes = tipos(lunesTrasCustodia)
        assertTrue(TipoEvento.NINO_PREPARAR in tLunes)
        assertFalse(TipoEvento.NINO_RECIBIR in tLunes)
        // Martes y jueves: si hay entrega que recibir.
        listOf(martes, jueves).forEach { f ->
            assertTrue(TipoEvento.NINO_RECIBIR in tipos(f), "Falta Recibir al nino en $f")
            assertFalse(TipoEvento.NINO_PREPARAR in tipos(f))
        }
    }

    @Test
    fun `la recogida de martes y jueves se marca como estimacion sin confirmar`() {
        val e = GeneradorAgenda.generarDia(martes, cfg).first { it.tipo == TipoEvento.NINO_RECOGER }
        assertFalse(e.confirmado, "La hora de recogida es una estimacion, no una hora confirmada")
        assertEquals("7:15 p. m.", e.horaVisible)
    }

    @Test
    fun `el traslado gimnasio-casa se marca como estimacion sin confirmar`() {
        val e = GeneradorAgenda.generarDia(miercoles, cfg).first { it.tipo == TipoEvento.TRASLADO }
        assertFalse(e.confirmado)
        assertNotNull(e.nota)
        assertTrue(e.nota!!.contains("sin confirmar"))
    }

    @Test
    fun `mientras la autorizacion este pendiente el gimnasio se marca como propuesta`() {
        val e = GeneradorAgenda.generarDia(miercoles, cfg).first { it.tipo == TipoEvento.GIMNASIO }
        assertTrue(e.requiereValidacionMedica)
        assertTrue(e.titulo.contains("propuesta", ignoreCase = true))
    }

    @Test
    fun `el peso solo se programa lunes miercoles y viernes`() {
        assertTrue(TipoEvento.PESO in tipos(lunesNormal))
        assertTrue(TipoEvento.PESO in tipos(miercoles))
        assertTrue(TipoEvento.PESO in tipos(viernes))
        assertFalse(TipoEvento.PESO in tipos(martes))
        assertFalse(TipoEvento.PESO in tipos(jueves))
    }

    @Test
    fun `la cintura se mide los sabados y las tareas semanales caen en su dia`() {
        assertTrue(TipoEvento.CINTURA in tipos(sabadoCustodia))
        assertFalse(TipoEvento.CINTURA in tipos(martes))
        assertTrue(TipoEvento.RENOVAR_COMIDAS in tipos(miercoles))
        assertTrue(TipoEvento.PREP_COMIDAS in tipos(domingoCustodia))
        assertTrue(TipoEvento.REVISION_SEMANAL in tipos(domingoCustodia))
    }

    @Test
    fun `los identificadores son estables entre regeneraciones`() {
        val a = GeneradorAgenda.generarDia(miercoles, cfg).map { it.uid }
        val b = GeneradorAgenda.generarDia(miercoles, cfg).map { it.uid }
        assertEquals(a, b, "Regenerar la agenda debe producir los mismos identificadores")
        assertEquals(a.size, a.distinct().size, "Hay identificadores duplicados en el mismo dia")
    }

    @Test
    fun `no hay identificadores duplicados en todo el plan de referencia`() {
        val eventos = GeneradorAgenda.generarRango(
            cfg.fechaInicioReferencia, cfg.fechaFinReferencia, cfg,
        )
        val uids = eventos.map { it.uid }
        assertEquals(uids.size, uids.distinct().size, "El plan genera identificadores duplicados")
        assertTrue(eventos.size > 100, "El plan de referencia deberia generar bastantes eventos")
    }

    @Test
    fun `un dia laboral tipico no tiene conflictos de horario`() {
        listOf(martes, miercoles, jueves, viernes, lunesNormal, lunesTrasCustodia).forEach { f ->
            val conflictos = DetectorConflictos.detectar(GeneradorAgenda.generarDia(f, cfg))
            assertTrue(conflictos.isEmpty(), "Conflictos en $f: ${conflictos.map { it.descripcion }}")
        }
    }

    @Test
    fun `todas las horas visibles del plan usan formato de 12 horas`() {
        val patron = Regex("""^\d{1,2}:\d{2} [ap]\. m\.( - \d{1,2}:\d{2} [ap]\. m\.)?$""")
        GeneradorAgenda.generarRango(cfg.fechaInicioReferencia, cfg.fechaFinReferencia, cfg)
            .forEach { e ->
                assertTrue(patron.matches(e.horaVisible), "Hora no valida: '${e.horaVisible}' en ${e.titulo}")
            }
    }

    @Test
    fun `el objetivo de dormir avanza de forma gradual y no salta a las diez`() {
        val primerDia = GeneradorAgenda.generarDia(cfg.fechaActivacion, cfg)
            .first { it.tipo == TipoEvento.DORMIR }
        assertEquals("11:30 p. m.", primerDia.horaVisible)

        val seisSemanas = GeneradorAgenda.generarDia(cfg.fechaActivacion.plusWeeks(6), cfg)
            .first { it.tipo == TipoEvento.DORMIR }
        assertEquals("10:00 p. m.", seisSemanas.horaVisible)

        val muchoDespues = GeneradorAgenda.generarDia(cfg.fechaActivacion.plusWeeks(30), cfg)
            .first { it.tipo == TipoEvento.DORMIR }
        assertEquals("10:00 p. m.", muchoDespues.horaVisible, "No debe pasarse del objetivo")
    }

    @Test
    fun `si la app se activa mas tarde no se generan eventos vencidos`() {
        val activacionTardia = LocalDate.of(2026, 10, 20)
        val cfgTardio = cfg.copy(fechaActivacion = activacionTardia)
        val eventos = GeneradorAgenda.generarRango(
            activacionTardia, activacionTardia.plusDays(13), cfgTardio,
        )
        assertTrue(eventos.none { it.fecha.isBefore(activacionTardia) })
        // La paridad de custodia se mantiene desde el ancla original.
        assertTrue(cfgTardio.custodia.esSabadoDeCustodia(LocalDate.of(2026, 10, 24)))
    }

    @Test
    fun `el nino no aparece en el plan cuando no hay escuela ni custodia`() {
        val sabadoSinCustodia = LocalDate.of(2026, 9, 19)
        val t = tipos(sabadoSinCustodia)
        assertFalse(TipoEvento.NINO_RECIBIR in t)
        assertFalse(TipoEvento.NINO_ESCUELA in t)
        assertNull(t.firstOrNull { it == TipoEvento.NINO_CUSTODIA_INICIO })
    }
}
