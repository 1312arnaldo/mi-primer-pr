package com.misaludyfuerza.core

import com.misaludyfuerza.core.agenda.ConfiguracionPlan
import com.misaludyfuerza.core.agenda.GeneradorAgenda
import com.misaludyfuerza.core.agenda.TipoEvento
import com.misaludyfuerza.core.alimentacion.Adherencia
import com.misaludyfuerza.core.alimentacion.EstadoComida
import com.misaludyfuerza.core.alimentacion.RegistroComida
import com.misaludyfuerza.core.alimentacion.TipoComida
import com.misaludyfuerza.core.estadisticas.Cobertura
import com.misaludyfuerza.core.estadisticas.Medicion
import com.misaludyfuerza.core.estadisticas.Series
import com.misaludyfuerza.core.exportacion.BloqueSalud
import com.misaludyfuerza.core.exportacion.ConstructorRevision
import com.misaludyfuerza.core.exportacion.ExportadorIcs
import com.misaludyfuerza.core.exportacion.ExportadorRevision
import com.misaludyfuerza.core.exportacion.MetadatosExport
import com.misaludyfuerza.core.exportacion.Procedencia
import com.misaludyfuerza.core.perfil.Perfil
import com.misaludyfuerza.core.perfil.PreferenciasAlimentarias
import com.misaludyfuerza.core.salud.AutorizacionProfesional
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExportacionTest {

    private val cfg = ConfiguracionPlan()
    private val semana = GeneradorAgenda.generarRango(
        LocalDate.of(2026, 9, 8), LocalDate.of(2026, 9, 14), cfg,
    )

    @Test
    fun `el ics declara la zona horaria y sus reglas de horario de verano`() {
        val ics = ExportadorIcs.exportar(semana)
        assertTrue(ics.contains("BEGIN:VTIMEZONE"))
        assertTrue(ics.contains("TZID:America/New_York"))
        assertTrue(ics.contains("BYDAY=2SU"), "Falta la regla de inicio del horario de verano")
        assertTrue(ics.contains("BYDAY=1SU"), "Falta la regla de fin del horario de verano")
        assertTrue(ics.contains("X-WR-CALNAME:Mi Salud y Fuerza"), "Debe ser un calendario separado")
    }

    @Test
    fun `el ics usa horas locales con TZID y no horas flotantes`() {
        val ics = ExportadorIcs.exportar(semana)
        // Solo los VEVENT: el bloque VTIMEZONE usa DTSTART sin zona por especificacion.
        val eventos = ics.split("BEGIN:VEVENT").drop(1)
        assertTrue(eventos.isNotEmpty())
        eventos.forEach { bloque ->
            Regex("DTSTART[^\r\n]*").findAll(bloque).forEach {
                assertTrue(
                    it.value.contains("TZID=America/New_York"),
                    "DTSTART sin zona: ${it.value}",
                )
            }
        }
        val gimnasio = semana.first { it.tipo == TipoEvento.GIMNASIO }
        assertTrue(ics.contains("DTSTART;TZID=America/New_York:${gimnasio.fecha.toString().replace("-", "")}T074500"))
    }

    @Test
    fun `reexportar produce los mismos identificadores y no duplica eventos`() {
        val a = ExportadorIcs.exportar(semana, generadoEn = java.time.LocalDateTime.of(2026, 9, 8, 6, 0))
        val b = ExportadorIcs.exportar(
            GeneradorAgenda.generarRango(LocalDate.of(2026, 9, 8), LocalDate.of(2026, 9, 14), cfg),
            generadoEn = java.time.LocalDateTime.of(2026, 9, 8, 6, 0),
        )
        assertEquals(a, b, "La exportacion debe ser idempotente")

        val uids = Regex("UID:([^\r\n]+)").findAll(a).map { it.groupValues[1] }.toList()
        assertEquals(uids.size, uids.distinct().size, "Hay UID duplicados en el ICS")
    }

    @Test
    fun `la anonimizacion oculta los eventos del nino`() {
        val ics = ExportadorIcs.exportar(semana, anonimizarNino = true)
        assertFalse(ics.contains("SUMMARY:Recibir al nino"))
        assertFalse(ics.contains("SUMMARY:Salir a la escuela"))
        assertTrue(ics.contains("SUMMARY:Compromiso familiar"))
        // Sin anonimizar si aparecen.
        assertTrue(ExportadorIcs.exportar(semana, anonimizarNino = false).contains("Recibir al nino"))
    }

    private fun revision() = ConstructorRevision.construir(
        desde = LocalDate.of(2026, 9, 8),
        hasta = LocalDate.of(2026, 9, 14),
        generadoEnIso = "2026-09-14T18:00:00-04:00",
        perfil = Perfil(),
        preferencias = PreferenciasAlimentarias(),
        autorizacion = AutorizacionProfesional(),
        versionPlan = 1,
        adherencia = Adherencia.calcular(
            listOf(
                RegistroComida("2026-09-08", TipoComida.DESAYUNO, EstadoComida.COMI),
                RegistroComida("2026-09-08", TipoComida.CENA, EstadoComida.NO_COMI),
            ),
            planeadasTotales = 35,
        ),
        objetivoKcal = 2000,
        objetivoProteina = "150-170 g",
        sesionesPlaneadas = 3,
        sesiones = emptyList(),
        pesos = listOf(Medicion("2026-09-08", 91.0), Medicion("2026-09-11", 90.7)),
        cinturas = listOf(Medicion("2026-09-12", 102.0)),
        coberturaPeso = Cobertura(2, 7),
        coberturaCintura = Cobertura(1, 7),
        tendenciaPeso = Series.tendencia(emptyList(), LocalDate.of(2026, 9, 8), LocalDate.of(2026, 9, 14)),
        tendenciaCintura = Series.tendencia(emptyList(), LocalDate.of(2026, 9, 8), LocalDate.of(2026, 9, 14)),
        tendenciaVolumen = Series.tendencia(emptyList(), LocalDate.of(2026, 9, 8), LocalDate.of(2026, 9, 14)),
        lecturaGlobal = "Todavia no hay senales claras.",
        salud = BloqueSalud(
            conectado = false,
            tiposNoDisponibles = listOf("pasos", "sueno", "frecuencia cardiaca"),
            procedencia = Procedencia("Health Connect", null, listOf("Sin permisos concedidos")),
        ),
    )

    @Test
    fun `el json lleva esquema versionado zona unidades y procedencia`() {
        val json = ExportadorRevision.aJson(revision())
        assertTrue(json.contains("\"esquema\": \"${MetadatosExport.ESQUEMA}\""))
        assertTrue(json.contains("America/New_York"))
        assertTrue(json.contains("\"unidades\""))
        assertTrue(json.contains("\"procedencia\""))
        assertTrue(json.contains("\"huecos\""))
        assertTrue(json.contains("\"versionPlan\": 1"))
        assertTrue(json.contains("\"ninoAnonimizado\": true"))
    }

    @Test
    fun `el markdown muestra huecos de datos y no confunde ausencia con cero`() {
        val md = ExportadorRevision.aMarkdown(revision())
        assertTrue(md.contains("Sin registrar (hueco de datos)"))
        assertTrue(md.contains("Pendiente de confirmar"))
        assertTrue(md.contains("datos insuficientes"))
        assertTrue(md.contains("12 horas"))
        assertTrue(md.contains("pollo"), "Deben constar los alimentos excluidos")
    }

    @Test
    fun `el markdown recuerda que lo planeado no cuenta como comido`() {
        val md = ExportadorRevision.aMarkdown(revision())
        assertTrue(md.contains("no cuentan como consumidas"))
        assertTrue(md.contains("no prueba que se hicieran los ejercicios"))
    }

    @Test
    fun `el csv incluye cobertura y origen de cada metrica`() {
        val csv = ExportadorRevision.aCsv(revision())
        val lineas = csv.trim().lines()
        assertEquals(
            "metrica,valor,unidad,desde,hasta,cobertura_dias_con_dato,cobertura_dias_periodo,origen",
            lineas.first(),
        )
        assertTrue(lineas.size > 5)
        assertTrue(lineas.any { it.startsWith("peso_promedio,90.85,kg") })
        assertTrue(lineas.any { it.startsWith("comidas_sin_registrar,") })
    }
}
