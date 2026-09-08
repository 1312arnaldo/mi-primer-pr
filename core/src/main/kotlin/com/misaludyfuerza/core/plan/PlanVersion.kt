package com.misaludyfuerza.core.plan

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Un cambio concreto sobre una ruta conocida del plan. */
@Serializable
data class CambioPropuesto(
    val ruta: String,
    val valorAnterior: String? = null,
    val valorNuevo: String,
    val justificacion: String? = null,
)

/**
 * Propuesta estructurada de cambio, tipicamente generada por otro asistente
 * (ChatGPT/Codex) a partir de una exportacion. Nunca se aplica sola.
 */
@Serializable
data class PropuestaDeCambio(
    val esquema: String = ESQUEMA,
    val id: String,
    val origen: String,
    val fechaIso: String,
    val cambios: List<CambioPropuesto>,
    val comentario: String? = null,
) {
    companion object {
        const val ESQUEMA = "misaludyfuerza.propuesta.v1"
    }
}

@Serializable
data class VersionPlan(
    val numero: Int,
    val fechaIso: String,
    val descripcion: String,
    val cambios: List<CambioPropuesto> = emptyList(),
    val propuestaId: String? = null,
)

@Serializable
data class ResultadoValidacion(
    val valida: Boolean,
    val errores: List<String> = emptyList(),
    val avisos: List<String> = emptyList(),
    val propuesta: PropuestaDeCambio? = null,
) {
    /** Diferencias legibles para que el usuario decida antes de aceptar. */
    val diferencias: List<String>
        get() = propuesta?.cambios.orEmpty().map { c ->
            val antes = c.valorAnterior ?: "(valor actual)"
            "${c.ruta}: $antes -> ${c.valorNuevo}" +
                (c.justificacion?.let { "  ($it)" } ?: "")
        }
}

/**
 * Valida propuestas externas. Solo se aceptan rutas de una lista blanca: una
 * propuesta no puede tocar restricciones medicas, preferencias alimentarias
 * prohibidas ni datos del nino.
 */
object ValidadorPropuesta {

    /** Rutas que una propuesta externa puede modificar. */
    val RUTAS_PERMITIDAS: Set<String> = setOf(
        "nutricion.kcalObjetivo",
        "nutricion.proteinaMinG",
        "nutricion.proteinaMaxG",
        "nutricion.menu.almuerzo",
        "nutricion.menu.cena",
        "entrenamiento.series",
        "entrenamiento.repMin",
        "entrenamiento.repMax",
        "entrenamiento.descansoSeg",
        "agenda.horaGimnasio",
        "agenda.horaDormirObjetivo",
        "agenda.margenAvisoMin",
    )

    /** Rutas que jamas puede tocar una propuesta externa. */
    val RUTAS_BLOQUEADAS: Set<String> = setOf(
        "salud.autorizacion",
        "salud.restriccionRodilla",
        "preferencias.prohibidos",
        "nino",
        "custodia",
    )

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun validar(textoJson: String): ResultadoValidacion {
        val propuesta = try {
            json.decodeFromString<PropuestaDeCambio>(textoJson)
        } catch (e: Exception) {
            return ResultadoValidacion(false, listOf("No se pudo leer el JSON: ${e.message}"))
        }
        return validar(propuesta)
    }

    fun validar(propuesta: PropuestaDeCambio): ResultadoValidacion {
        val errores = mutableListOf<String>()
        val avisos = mutableListOf<String>()

        if (propuesta.esquema != PropuestaDeCambio.ESQUEMA) {
            errores += "Esquema desconocido: \"${propuesta.esquema}\". " +
                "Se espera \"${PropuestaDeCambio.ESQUEMA}\"."
        }
        if (propuesta.cambios.isEmpty()) {
            errores += "La propuesta no contiene cambios."
        }
        propuesta.cambios.forEach { c ->
            when {
                RUTAS_BLOQUEADAS.any { c.ruta == it || c.ruta.startsWith("$it.") } ->
                    errores += "La ruta \"${c.ruta}\" no se puede modificar desde una propuesta " +
                        "externa: afecta a restricciones de salud, preferencias prohibidas o al nino."
                c.ruta !in RUTAS_PERMITIDAS ->
                    errores += "Ruta desconocida: \"${c.ruta}\"."
                c.justificacion.isNullOrBlank() ->
                    avisos += "El cambio en \"${c.ruta}\" no trae justificacion."
            }
        }
        return ResultadoValidacion(
            valida = errores.isEmpty(),
            errores = errores,
            avisos = avisos,
            propuesta = propuesta,
        )
    }
}

/**
 * Historial de versiones del plan. Aceptar una propuesta crea una version nueva;
 * la anterior se conserva para poder revertir.
 */
class HistorialPlan(inicial: VersionPlan = VersionPlan(1, "", "Plan inicial precargado")) {

    private val versiones = mutableListOf(inicial)

    val actual: VersionPlan get() = versiones.last()
    val todas: List<VersionPlan> get() = versiones.toList()

    /**
     * Aplica una propuesta ya validada Y aceptada por el usuario. Lanza si se
     * intenta aplicar una propuesta invalida o sin aceptacion explicita.
     */
    fun aceptar(resultado: ResultadoValidacion, aceptadaPorUsuario: Boolean, fechaIso: String): VersionPlan {
        require(resultado.valida) { "No se puede aplicar una propuesta invalida." }
        require(aceptadaPorUsuario) { "Toda modificacion del plan requiere aceptacion explicita." }
        val p = resultado.propuesta!!
        val nueva = VersionPlan(
            numero = actual.numero + 1,
            fechaIso = fechaIso,
            descripcion = p.comentario ?: "Propuesta ${p.id} de ${p.origen}",
            cambios = p.cambios,
            propuestaId = p.id,
        )
        versiones += nueva
        return nueva
    }

    /** Revierte a una version anterior anadiendo una version nueva (no borra historial). */
    fun revertirA(numero: Int, fechaIso: String): VersionPlan {
        val objetivo = versiones.firstOrNull { it.numero == numero }
            ?: error("No existe la version $numero")
        val nueva = objetivo.copy(
            numero = actual.numero + 1,
            fechaIso = fechaIso,
            descripcion = "Reversion a la version ${objetivo.numero}",
        )
        versiones += nueva
        return nueva
    }
}
