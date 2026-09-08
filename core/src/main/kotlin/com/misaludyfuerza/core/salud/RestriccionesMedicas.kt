package com.misaludyfuerza.core.salud

import kotlinx.serialization.Serializable

/**
 * Estado de autorizacion profesional para entrenar. Arranca en
 * [PENDIENTE_DE_CONFIRMAR] y solo el usuario puede cambiarlo registrando lo que
 * le indico su traumatologo o fisioterapeuta. La app nunca se autoriza sola.
 */
@Serializable
enum class EstadoAutorizacion(val etiqueta: String) {
    PENDIENTE_DE_CONFIRMAR("Pendiente de confirmar"),
    AUTORIZADA_CON_LIMITES("Autorizada con limites"),
    NO_AUTORIZADA("No autorizada"),
}

@Serializable
data class AutorizacionProfesional(
    val estado: EstadoAutorizacion = EstadoAutorizacion.PENDIENTE_DE_CONFIRMAR,
    /** Quien lo indico, tal como lo escriba el usuario. Puede quedar vacio. */
    val profesional: String? = null,
    /** Fecha ISO-8601 de la indicacion. */
    val fecha: String? = null,
    val instrucciones: List<String> = emptyList(),
    /** Ejercicios que el profesional autorizo explicitamente. */
    val ejerciciosPermitidos: Set<String> = emptySet(),
    /** Ejercicios que el profesional prohibio explicitamente. */
    val ejerciciosProhibidos: Set<String> = emptySet(),
) {
    val puedeProgresarCarga: Boolean get() = estado == EstadoAutorizacion.AUTORIZADA_CON_LIMITES

    /**
     * Mientras no haya autorizacion, cualquier rutina se presenta como propuesta a
     * validar, nunca como un plan aprobado ni como rehabilitacion.
     */
    val textoAviso: String
        get() = when (estado) {
            EstadoAutorizacion.PENDIENTE_DE_CONFIRMAR ->
                "Autorizacion profesional pendiente de confirmar. Esta rutina es una " +
                    "propuesta para validar con tu traumatologo o fisioterapeuta; no es " +
                    "rehabilitacion ni autorizacion medica."
            EstadoAutorizacion.NO_AUTORIZADA ->
                "Marcaste que no tienes autorizacion para entrenar. La rutina queda solo " +
                    "como referencia; consulta antes de realizarla."
            EstadoAutorizacion.AUTORIZADA_CON_LIMITES ->
                "Autorizada con limites registrados. Respeta las instrucciones guardadas " +
                    "y detente si aparece dolor, hinchazon o inestabilidad."
        }
}

@Serializable
enum class VeredictoEjercicio { PERMITIDO, REQUIERE_REVISION, BLOQUEADO }

@Serializable
data class EvaluacionEjercicio(
    val veredicto: VeredictoEjercicio,
    val motivo: String,
)

/**
 * Filtro de seguridad para la rodilla. El usuario tiene ligamentos rotos y pidio
 * explicitamente no anadir carga a la rodilla; decir "puedo caminar y correr" no
 * es autorizacion medica, asi que la carga de piernas se bloquea siempre, y no
 * solo cuando falta la autorizacion.
 */
object FiltroRodilla {

    /** Patrones que se bloquean sin excepcion. */
    private val BLOQUEADOS = listOf(
        "sentadilla", "squat", "prensa de pierna", "prensa", "leg press",
        "peso muerto", "deadlift", "peso muerto rumano", "rdl",
        "zancada", "lunge", "desplante", "bulgara",
        "salto", "saltar", "jump", "pliometr", "burpee",
        "correr", "carrera", "trote", "running", "sprint", "cinta de correr",
        "extension de cuadriceps", "leg extension", "curl femoral", "leg curl",
        "gemelo", "pantorrilla", "calf", "hip thrust", "puente de gluteo",
        "step up", "subir escalon", "escaladora", "stair", "eliptica",
        "sled", "trineo", "sentadilla goblet", "kettlebell swing",
        "abductor", "aductor", "patada de gluteo",
    )

    /**
     * Patrones que no se bloquean pero exigen revision: son de tren superior y aun
     * asi pueden apoyar, trabar o cargar la rodilla segun la maquina.
     */
    private val REVISION = listOf(
        "de pie", "parado", "standing", "jalon", "pulldown", "polea alta",
        "remo de pie", "press militar de pie", "trasladar mancuerna",
        "mancuerna pesada", "rack", "banco inclinado sin respaldo",
    )

    fun evaluar(
        nombreEjercicio: String,
        autorizacion: AutorizacionProfesional = AutorizacionProfesional(),
    ): EvaluacionEjercicio {
        val n = normalizar(nombreEjercicio)

        autorizacion.ejerciciosProhibidos.firstOrNull { normalizar(it) == n }?.let {
            return EvaluacionEjercicio(
                VeredictoEjercicio.BLOQUEADO,
                "Tu profesional lo marco como prohibido.",
            )
        }

        BLOQUEADOS.firstOrNull { n.contains(normalizar(it)) }?.let { patron ->
            return EvaluacionEjercicio(
                VeredictoEjercicio.BLOQUEADO,
                "Anade carga o impacto a la rodilla (\"$patron\"). No se incluye en el plan.",
            )
        }

        if (autorizacion.ejerciciosPermitidos.any { normalizar(it) == n }) {
            return EvaluacionEjercicio(
                VeredictoEjercicio.PERMITIDO,
                "Autorizado explicitamente por tu profesional.",
            )
        }

        REVISION.firstOrNull { n.contains(normalizar(it)) }?.let { patron ->
            return EvaluacionEjercicio(
                VeredictoEjercicio.REQUIERE_REVISION,
                "La posicion o la sujecion (\"$patron\") puede cargar la rodilla. " +
                    "Revisalo antes de hacerlo; si no puedes colocarte sin apoyo firme, omitelo.",
            )
        }

        return EvaluacionEjercicio(
            VeredictoEjercicio.PERMITIDO,
            "Tren superior con apoyo, sin carga de piernas prevista.",
        )
    }

    private fun normalizar(s: String): String = s.lowercase()
        .replace('á', 'a').replace('é', 'e').replace('í', 'i')
        .replace('ó', 'o').replace('ú', 'u').replace('ü', 'u')
        .replace('ñ', 'n')
        .trim()
}

@Serializable
data class ReporteSintoma(
    val fechaIso: String,
    /** 0 = sin dolor, 10 = maximo. Escala declarada por el usuario. */
    val dolor0a10: Int = 0,
    val hinchazon: Boolean = false,
    val inestabilidad: Boolean = false,
    val nota: String? = null,
) {
    val haySintoma: Boolean get() = dolor0a10 > 0 || hinchazon || inestabilidad
}

@Serializable
data class RespuestaSintoma(
    val detenerMovimiento: Boolean,
    val bloquearAumentoDeCarga: Boolean,
    val sugerirConsulta: Boolean,
    val mensaje: String,
)

object ProtocoloSintomas {
    /**
     * Ante dolor, hinchazon o inestabilidad la respuesta nunca es progresar: se
     * detiene el movimiento, se congela la carga y se sugiere consultar.
     */
    fun responder(r: ReporteSintoma): RespuestaSintoma {
        if (!r.haySintoma) {
            return RespuestaSintoma(
                detenerMovimiento = false,
                bloquearAumentoDeCarga = false,
                sugerirConsulta = false,
                mensaje = "Sin sintomas registrados en esta sesion.",
            )
        }
        val partes = buildList {
            if (r.dolor0a10 > 0) add("dolor ${r.dolor0a10}/10")
            if (r.hinchazon) add("hinchazon")
            if (r.inestabilidad) add("inestabilidad")
        }
        return RespuestaSintoma(
            detenerMovimiento = true,
            bloquearAumentoDeCarga = true,
            sugerirConsulta = true,
            mensaje = "Registraste ${partes.joinToString(", ")}. Deten ese movimiento, no " +
                "aumentes carga y consulta con tu traumatologo o fisioterapeuta antes de " +
                "retomarlo. La app no propondra subir peso mientras el sintoma siga activo.",
        )
    }
}
