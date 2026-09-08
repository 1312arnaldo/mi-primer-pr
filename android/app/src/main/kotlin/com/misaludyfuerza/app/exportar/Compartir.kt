package com.misaludyfuerza.app.exportar

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.misaludyfuerza.app.datos.Repositorio
import com.misaludyfuerza.app.salud.GestorSalud
import com.misaludyfuerza.app.salud.TipoDatoSalud
import com.misaludyfuerza.core.agenda.GeneradorAgenda
import com.misaludyfuerza.core.agenda.ReglasGimnasio
import com.misaludyfuerza.core.agenda.DecisionGimnasio
import com.misaludyfuerza.core.alimentacion.Adherencia
import com.misaludyfuerza.core.alimentacion.MenuBase
import com.misaludyfuerza.core.alimentacion.ObjetivoNutricional
import com.misaludyfuerza.core.estadisticas.Series
import com.misaludyfuerza.core.estadisticas.Tendencia
import com.misaludyfuerza.core.estadisticas.Veredicto
import com.misaludyfuerza.core.exportacion.BloqueSalud
import com.misaludyfuerza.core.exportacion.ConstructorRevision
import com.misaludyfuerza.core.exportacion.ExportadorIcs
import com.misaludyfuerza.core.exportacion.ExportadorRevision
import com.misaludyfuerza.core.exportacion.Procedencia
import com.misaludyfuerza.core.exportacion.RevisionSemanal
import com.misaludyfuerza.core.estadisticas.Medicion
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Genera y comparte la revision semanal. Siempre se muestra una vista previa antes
 * de compartir: nada sale del telefono sin que el usuario lo vea.
 */
class Exportador(private val repo: Repositorio, private val gestorSalud: GestorSalud?) {

    suspend fun construirRevision(desde: LocalDate, hasta: LocalDate): RevisionSemanal {
        val cfg = repo.configuracion()
        val zona = ZoneId.of(repo.perfil().zonaHoraria)
        val ahora = LocalDateTime.now(zona).atZone(zona).toOffsetDateTime().toString()

        val registrosComida = repo.registrosComidaRango(desde, hasta)
        val dias = java.time.temporal.ChronoUnit.DAYS.between(desde, hasta).toInt() + 1
        val comidasPlaneadas = (0 until dias).sumOf { MenuBase.dia(desde.plusDays(it.toLong())).comidas.size }

        val sesiones = repo.sesionesRango(desde, hasta)
        val sesionesPlaneadas = (0 until dias).count { i ->
            ReglasGimnasio.decidir(desde.plusDays(i.toLong()), cfg) is DecisionGimnasio.Programado
        }

        val pesos = repo.medicionesRango(Repositorio.TIPO_PESO, desde, hasta)
        val cinturas = repo.medicionesRango(Repositorio.TIPO_CINTURA, desde, hasta)

        // Volumen por sesion como serie temporal, para ver si la fuerza sube.
        val volumen = sesiones.filter { it.completada }.map { s ->
            Medicion(
                s.fechaIso,
                s.ejercicios.sumOf { e -> e.series.sumOf { (it.pesoKg ?: 0.0) * it.repeticiones } },
                "Registro manual",
            )
        }

        val tPeso = Series.tendencia(pesos, desde, hasta)
        val tCintura = Series.tendencia(cinturas, desde, hasta)
        val tVolumen = Series.tendencia(volumen, desde, hasta, umbralCambioPorSemana = 50.0)

        val estadosSalud = repo.saludDao.leerEstado(TipoDatoSalud.PASOS.name)
        val disponibles = mutableListOf<String>()
        val noDisponibles = mutableListOf<String>()
        TipoDatoSalud.entries.forEach { t ->
            val e = repo.saludDao.leerEstado(t.name)
            if (e?.permisoConcedido == true && e.disponible) disponibles += t.etiqueta
            else noDisponibles += t.etiqueta
        }

        val objetivo = ObjetivoNutricional()
        return ConstructorRevision.construir(
            desde = desde,
            hasta = hasta,
            generadoEnIso = ahora,
            perfil = repo.perfil(),
            preferencias = repo.preferencias,
            autorizacion = repo.autorizacion(),
            versionPlan = repo.planDao.versionActual()?.numero ?: 1,
            adherencia = Adherencia.calcular(registrosComida, comidasPlaneadas),
            objetivoKcal = objetivo.kcal,
            objetivoProteina = "${objetivo.proteinaMinG}-${objetivo.proteinaMaxG} g",
            sesionesPlaneadas = sesionesPlaneadas,
            sesiones = sesiones,
            pesos = pesos,
            cinturas = cinturas,
            coberturaPeso = Series.cobertura(pesos, desde, hasta),
            coberturaCintura = Series.cobertura(cinturas, desde, hasta),
            tendenciaPeso = tPeso,
            tendenciaCintura = tCintura,
            tendenciaVolumen = tVolumen,
            lecturaGlobal = lectura(tPeso, tCintura, tVolumen),
            salud = BloqueSalud(
                conectado = disponibles.isNotEmpty(),
                tiposDisponibles = disponibles,
                tiposNoDisponibles = noDisponibles,
                procedencia = Procedencia(
                    fuente = "Health Connect",
                    ultimaSincronizacionIso = estadosSalud?.ultimaSincronizacionIso,
                    huecos = buildList {
                        if (noDisponibles.isNotEmpty()) {
                            add("Sin permiso o sin datos: ${noDisponibles.joinToString(", ")}")
                        }
                        if (estadosSalud?.ultimoErrorMensaje != null) {
                            add("Ultimo error: ${estadosSalud.ultimoErrorMensaje}")
                        }
                    },
                ),
            ),
            anonimizarNino = repo.ajustes().anonimizarNinoAlExportar,
            notas = listOf(
                "Integracion con ${com.misaludyfuerza.app.salud.IntegracionGimnasio.NOMBRE}: " +
                    com.misaludyfuerza.app.salud.IntegracionGimnasio.ESTADO + ".",
            ),
        )
    }

    private fun lectura(
        peso: Tendencia,
        cintura: Tendencia,
        volumen: Tendencia,
    ): String {
        val senales = buildList {
            if (cintura.veredicto == Veredicto.BAJANDO) add("la cintura baja")
            if (volumen.veredicto == Veredicto.SUBIENDO) add("el volumen de entrenamiento sube")
            if (peso.veredicto == Veredicto.BAJANDO) add("el peso promedio baja")
        }
        return when {
            senales.isEmpty() ->
                "Todavia no hay senales claras. Conviene revisar con 2-3 semanas de datos suficientes."
            peso.veredicto == Veredicto.SIN_CAMBIO_CLARO ->
                "Aunque la bascula no se mueve, ${senales.joinToString(" y ")}. Eso cuenta."
            else -> "Senales positivas: ${senales.joinToString(" y ")}."
        }
    }

    /** Vista previa en Markdown, para leerla antes de compartir nada. */
    fun previsualizar(revision: RevisionSemanal): String = ExportadorRevision.aMarkdown(revision)

    /** Escribe los tres formatos y devuelve los archivos generados. */
    fun escribirArchivos(context: Context, revision: RevisionSemanal): List<File> {
        val carpeta = File(context.cacheDir, "exportaciones").apply { mkdirs() }
        val sufijo = "${revision.desdeIso}_a_${revision.hastaIso}"
        return listOf(
            File(carpeta, "revision_$sufijo.md").apply { writeText(ExportadorRevision.aMarkdown(revision)) },
            File(carpeta, "revision_$sufijo.json").apply { writeText(ExportadorRevision.aJson(revision)) },
            File(carpeta, "metricas_$sufijo.csv").apply { writeText(ExportadorRevision.aCsv(revision)) },
        )
    }

    suspend fun escribirIcs(context: Context, desde: LocalDate, hasta: LocalDate): File {
        val cfg = repo.configuracion()
        val eventos = GeneradorAgenda.generarRango(desde, hasta, cfg)
        val carpeta = File(context.cacheDir, "exportaciones").apply { mkdirs() }
        return File(carpeta, "calendario_${desde}_a_$hasta.ics").apply {
            writeText(
                ExportadorIcs.exportar(
                    eventos,
                    anonimizarNino = repo.ajustes().anonimizarNinoAlExportar,
                ),
            )
        }
    }

    companion object {
        fun compartir(context: Context, archivos: List<File>, titulo: String) {
            val uris = ArrayList(
                archivos.map {
                    FileProvider.getUriForFile(context, "${context.packageName}.archivos", it)
                },
            )
            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "*/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, titulo))
        }
    }
}
