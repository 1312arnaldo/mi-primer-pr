package com.misaludyfuerza.core.exportacion

import com.misaludyfuerza.core.alimentacion.Adherencia
import com.misaludyfuerza.core.entrenamiento.SesionEntrenamiento
import com.misaludyfuerza.core.estadisticas.Cobertura
import com.misaludyfuerza.core.estadisticas.Medicion
import com.misaludyfuerza.core.estadisticas.Tendencia
import com.misaludyfuerza.core.estadisticas.Veredicto
import com.misaludyfuerza.core.perfil.Perfil
import com.misaludyfuerza.core.perfil.PreferenciasAlimentarias
import com.misaludyfuerza.core.salud.AutorizacionProfesional
import com.misaludyfuerza.core.tiempo.Hora12
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate

@Serializable
data class MetadatosExport(
    val esquema: String = ESQUEMA,
    val generadoEnIso: String,
    val zonaHoraria: String = "America/New_York",
    val formatoHoraVisible: String = "12 horas con a. m. / p. m.",
    val unidades: Map<String, String> = mapOf(
        "peso" to "kg",
        "cintura" to "cm",
        "energia" to "kcal",
        "proteina" to "g",
        "carga" to "kg",
        "sueno" to "horas",
    ),
    val ninoAnonimizado: Boolean = true,
    val advertencia: String =
        "Datos declarados y registrados por el usuario. Las cifras nutricionales son " +
            "estimaciones, no mediciones. Nada aqui constituye indicacion medica.",
) {
    companion object {
        const val ESQUEMA = "misaludyfuerza.revision.v1"
    }
}

/** De donde vienen los datos de un bloque y que falta. */
@Serializable
data class Procedencia(
    val fuente: String,
    val ultimaSincronizacionIso: String? = null,
    val huecos: List<String> = emptyList(),
)

@Serializable
data class BloqueNutricion(
    val kcalObjetivo: Int,
    val proteinaObjetivoG: String,
    val comidasPlaneadas: Int,
    val comidasConfirmadas: Int,
    val comidasNoComidas: Int,
    val comidasSinRegistrar: Int,
    val adherenciaPorcentaje: Double?,
    val procedencia: Procedencia,
    val nota: String =
        "El objetivo es una estimacion editable. Las comidas planeadas o notificadas no " +
            "cuentan como consumidas: solo cuenta lo que el usuario confirmo.",
)

@Serializable
data class BloqueEntrenamiento(
    val sesionesPlaneadas: Int,
    val sesionesCompletadas: Int,
    val seriesTotales: Int,
    val volumenKg: Double,
    val sintomasRegistrados: Int,
    val autorizacion: String,
    val procedencia: Procedencia,
    val nota: String =
        "Sesion completada = confirmada por el usuario. Asistir al gimnasio o que el reloj " +
            "detecte actividad no prueba que se hicieran los ejercicios.",
)

@Serializable
data class BloqueMediciones(
    val pesos: List<Medicion>,
    val cinturas: List<Medicion>,
    val coberturaPeso: Cobertura,
    val coberturaCintura: Cobertura,
    val procedencia: Procedencia,
)

@Serializable
data class BloqueSalud(
    val conectado: Boolean,
    val tiposDisponibles: List<String> = emptyList(),
    val tiposNoDisponibles: List<String> = emptyList(),
    val procedencia: Procedencia,
    val nota: String =
        "La ausencia de dato no es un cero. Las calorias activas y las totales no se suman " +
            "y la ingesta no se ajusta automaticamente por lo que estime el reloj.",
)

@Serializable
data class BloqueTendencias(
    val peso: Tendencia,
    val cintura: Tendencia,
    val volumen: Tendencia,
    val lecturaGlobal: String,
)

@Serializable
data class RevisionSemanal(
    val metadatos: MetadatosExport,
    val desdeIso: String,
    val hastaIso: String,
    val versionPlan: Int,
    val perfil: Perfil,
    val preferencias: PreferenciasAlimentarias,
    val autorizacion: AutorizacionProfesional,
    val restriccionesClave: List<String>,
    val nutricion: BloqueNutricion,
    val entrenamiento: BloqueEntrenamiento,
    val mediciones: BloqueMediciones,
    val salud: BloqueSalud,
    val tendencias: BloqueTendencias,
    val notas: List<String> = emptyList(),
)

object ExportadorRevision {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    fun aJson(r: RevisionSemanal): String = json.encodeToString(r)

    /** Markdown para leer y pegar en una conversacion. */
    fun aMarkdown(r: RevisionSemanal): String = buildString {
        val desde = LocalDate.parse(r.desdeIso)
        val hasta = LocalDate.parse(r.hastaIso)
        appendLine("# Revision semanal - ${r.perfil.nombre}")
        appendLine()
        appendLine("Periodo: ${Hora12.fechaLarga(desde)} a ${Hora12.fechaLarga(hasta)}")
        appendLine()
        appendLine("Zona horaria: ${r.metadatos.zonaHoraria}. Horas en ${r.metadatos.formatoHoraVisible}.")
        appendLine("Version del plan: ${r.versionPlan}. Esquema: `${r.metadatos.esquema}`.")
        appendLine()
        appendLine("> ${r.metadatos.advertencia}")
        appendLine()

        appendLine("## Perfil y limites")
        appendLine()
        appendLine("- Altura: ${r.perfil.alturaM} m. Peso inicial declarado: ${r.perfil.pesoInicialKg} kg.")
        appendLine("- Autorizacion profesional: **${r.autorizacion.estado.etiqueta}**.")
        r.restriccionesClave.forEach { appendLine("- $it") }
        appendLine("- Alimentos excluidos siempre: ${r.preferencias.prohibidos.joinToString(", ")}.")
        appendLine()

        appendLine("## Alimentacion")
        appendLine()
        val n = r.nutricion
        appendLine("| Dato | Valor |")
        appendLine("|---|---|")
        appendLine("| Objetivo | ${n.kcalObjetivo} kcal, ${n.proteinaObjetivoG} de proteina (estimacion editable) |")
        appendLine("| Comidas planeadas | ${n.comidasPlaneadas} |")
        appendLine("| Confirmadas (comi / cambie) | ${n.comidasConfirmadas} |")
        appendLine("| No comidas | ${n.comidasNoComidas} |")
        appendLine("| Sin registrar (hueco de datos) | ${n.comidasSinRegistrar} |")
        appendLine("| Adherencia sobre lo registrado | ${porcentaje(n.adherenciaPorcentaje)} |")
        appendLine()
        appendLine("${n.nota}")
        appendLine()

        appendLine("## Entrenamiento")
        appendLine()
        val e = r.entrenamiento
        appendLine("- Sesiones planeadas: ${e.sesionesPlaneadas}. Completadas y confirmadas: ${e.sesionesCompletadas}.")
        appendLine("- Series registradas: ${e.seriesTotales}. Volumen: ${"%.0f".format(e.volumenKg)} kg (peso x repeticiones).")
        appendLine("- Sintomas registrados (dolor, hinchazon o inestabilidad): ${e.sintomasRegistrados}.")
        appendLine("- Autorizacion: ${e.autorizacion}.")
        appendLine()
        appendLine("${e.nota}")
        appendLine()

        appendLine("## Mediciones")
        appendLine()
        appendLine("- Peso: ${r.mediciones.pesos.size} registros. Cobertura: ${r.mediciones.coberturaPeso.texto}.")
        appendLine("- Cintura: ${r.mediciones.cinturas.size} registros. Cobertura: ${r.mediciones.coberturaCintura.texto}.")
        appendLine()

        appendLine("## Tendencias")
        appendLine()
        appendLine("- Peso: ${veredicto(r.tendencias.peso)}")
        appendLine("- Cintura: ${veredicto(r.tendencias.cintura)}")
        appendLine("- Volumen de entrenamiento: ${veredicto(r.tendencias.volumen)}")
        appendLine()
        appendLine("**Lectura:** ${r.tendencias.lecturaGlobal}")
        appendLine()

        appendLine("## Datos de salud conectados")
        appendLine()
        val s = r.salud
        appendLine("- Estado: ${if (s.conectado) "conectado" else "sin conexion activa"}.")
        if (s.tiposDisponibles.isNotEmpty()) appendLine("- Disponibles: ${s.tiposDisponibles.joinToString(", ")}.")
        if (s.tiposNoDisponibles.isNotEmpty()) appendLine("- No disponibles o sin permiso: ${s.tiposNoDisponibles.joinToString(", ")}.")
        s.procedencia.ultimaSincronizacionIso?.let { appendLine("- Ultima sincronizacion: $it.") }
        if (s.procedencia.huecos.isNotEmpty()) appendLine("- Huecos: ${s.procedencia.huecos.joinToString("; ")}.")
        appendLine()
        appendLine("${s.nota}")
        appendLine()

        if (r.notas.isNotEmpty()) {
            appendLine("## Notas")
            appendLine()
            r.notas.forEach { appendLine("- $it") }
            appendLine()
        }
        appendLine("---")
        appendLine()
        appendLine("Generado el ${r.metadatos.generadoEnIso} por Mi Salud y Fuerza. " +
            "Detalles identificables del nino omitidos: ${if (r.metadatos.ninoAnonimizado) "si" else "no"}.")
    }

    /** CSV de metricas, una fila por metrica y periodo. */
    fun aCsv(r: RevisionSemanal): String = buildString {
        appendLine("metrica,valor,unidad,desde,hasta,cobertura_dias_con_dato,cobertura_dias_periodo,origen")
        fun fila(metrica: String, valor: String, unidad: String, cob: Cobertura?, origen: String) {
            appendLine(
                listOf(
                    metrica, valor, unidad, r.desdeIso, r.hastaIso,
                    cob?.diasConDatos?.toString() ?: "",
                    cob?.diasDelPeriodo?.toString() ?: "",
                    origen,
                ).joinToString(",") { csv(it) },
            )
        }
        fila("peso_promedio", promedio(r.mediciones.pesos), "kg", r.mediciones.coberturaPeso, r.mediciones.procedencia.fuente)
        fila("cintura_promedio", promedio(r.mediciones.cinturas), "cm", r.mediciones.coberturaCintura, r.mediciones.procedencia.fuente)
        fila("sesiones_completadas", r.entrenamiento.sesionesCompletadas.toString(), "sesiones", null, r.entrenamiento.procedencia.fuente)
        fila("sesiones_planeadas", r.entrenamiento.sesionesPlaneadas.toString(), "sesiones", null, "Plan")
        fila("volumen_entrenamiento", "%.0f".format(r.entrenamiento.volumenKg), "kg", null, r.entrenamiento.procedencia.fuente)
        fila("comidas_confirmadas", r.nutricion.comidasConfirmadas.toString(), "comidas", null, r.nutricion.procedencia.fuente)
        fila("comidas_sin_registrar", r.nutricion.comidasSinRegistrar.toString(), "comidas", null, r.nutricion.procedencia.fuente)
        fila("adherencia_alimentaria", r.nutricion.adherenciaPorcentaje?.let { "%.1f".format(it) } ?: "", "%", null, r.nutricion.procedencia.fuente)
    }

    private fun promedio(m: List<Medicion>): String =
        if (m.isEmpty()) "" else "%.2f".format(m.sumOf { it.valor } / m.size)

    private fun porcentaje(p: Double?): String =
        p?.let { "${"%.0f".format(it)} %" } ?: "sin datos suficientes"

    private fun veredicto(t: Tendencia): String = when (t.veredicto) {
        Veredicto.DATOS_INSUFICIENTES -> "datos insuficientes. ${t.mensaje}"
        Veredicto.SIN_CAMBIO_CLARO -> "sin cambio claro. ${t.mensaje}"
        Veredicto.BAJANDO -> "bajando. ${t.mensaje}"
        Veredicto.SUBIENDO -> "subiendo. ${t.mensaje}"
    }

    private fun csv(s: String): String =
        if (s.contains(',') || s.contains('"')) "\"${s.replace("\"", "\"\"")}\"" else s
}

/** Construye la revision a partir de los datos crudos del periodo. */
object ConstructorRevision {

    fun construir(
        desde: LocalDate,
        hasta: LocalDate,
        generadoEnIso: String,
        perfil: Perfil,
        preferencias: PreferenciasAlimentarias,
        autorizacion: AutorizacionProfesional,
        versionPlan: Int,
        adherencia: Adherencia.Resultado,
        objetivoKcal: Int,
        objetivoProteina: String,
        sesionesPlaneadas: Int,
        sesiones: List<SesionEntrenamiento>,
        pesos: List<Medicion>,
        cinturas: List<Medicion>,
        coberturaPeso: Cobertura,
        coberturaCintura: Cobertura,
        tendenciaPeso: Tendencia,
        tendenciaCintura: Tendencia,
        tendenciaVolumen: Tendencia,
        lecturaGlobal: String,
        salud: BloqueSalud,
        anonimizarNino: Boolean = true,
        notas: List<String> = emptyList(),
    ): RevisionSemanal {
        val completadas = sesiones.filter { it.completada }
        val series = completadas.sumOf { s -> s.ejercicios.sumOf { it.series.size } }
        val volumen = completadas.sumOf { s ->
            s.ejercicios.sumOf { e -> e.series.sumOf { (it.pesoKg ?: 0.0) * it.repeticiones } }
        }
        return RevisionSemanal(
            metadatos = MetadatosExport(generadoEnIso = generadoEnIso, ninoAnonimizado = anonimizarNino),
            desdeIso = desde.toString(),
            hastaIso = hasta.toString(),
            versionPlan = versionPlan,
            perfil = perfil,
            preferencias = preferencias,
            autorizacion = autorizacion,
            restriccionesClave = listOf(
                "Ligamentos de rodilla rotos: sin carga de piernas, sin impacto y sin metas de pasos automaticas.",
                "Las rutinas son propuestas para validar mientras la autorizacion siga pendiente.",
                "Ante dolor, hinchazon o inestabilidad: detener el movimiento, no subir carga y consultar.",
            ),
            nutricion = BloqueNutricion(
                kcalObjetivo = objetivoKcal,
                proteinaObjetivoG = objetivoProteina,
                comidasPlaneadas = adherencia.planeadasTotales,
                comidasConfirmadas = adherencia.confirmadas,
                comidasNoComidas = adherencia.noComidas,
                comidasSinRegistrar = adherencia.sinRegistrar,
                adherenciaPorcentaje = adherencia.porcentaje,
                procedencia = Procedencia("Registro manual del usuario"),
            ),
            entrenamiento = BloqueEntrenamiento(
                sesionesPlaneadas = sesionesPlaneadas,
                sesionesCompletadas = completadas.size,
                seriesTotales = series,
                volumenKg = volumen,
                sintomasRegistrados = sesiones.count { it.sintoma?.haySintoma == true },
                autorizacion = autorizacion.estado.etiqueta,
                procedencia = Procedencia("Registro manual del usuario"),
            ),
            mediciones = BloqueMediciones(
                pesos = pesos, cinturas = cinturas,
                coberturaPeso = coberturaPeso, coberturaCintura = coberturaCintura,
                procedencia = Procedencia("Bascula y cinta metrica, registro manual"),
            ),
            salud = salud,
            tendencias = BloqueTendencias(tendenciaPeso, tendenciaCintura, tendenciaVolumen, lecturaGlobal),
            notas = notas,
        )
    }
}
