package com.misaludyfuerza.core.perfil

import kotlinx.serialization.Serializable

/**
 * Datos personales que el usuario declaro. Todo es editable: nada aqui es una
 * medicion tomada por la app.
 */
@Serializable
data class Perfil(
    val nombre: String = "Arnaldo",
    val edad: Int = 33,
    val alturaM: Double = 1.67,
    val pesoInicialKg: Double = 91.0,
    /** Fecha de la que proviene [pesoInicialKg], en ISO-8601. */
    val fechaPesoInicial: String? = null,
    val objetivos: List<String> = listOf(
        "Reducir grasa abdominal",
        "Recuperar fuerza",
        "Ganar masa muscular visible",
    ),
    val zonaHoraria: String = "America/New_York",
    val tieneBascula: Boolean = true,
    val tieneCintaMetrica: Boolean = true,
) {
    /** Indice de masa corporal para el peso indicado. Orientativo, no diagnostico. */
    fun imc(pesoKg: Double): Double = pesoKg / (alturaM * alturaM)
}

/**
 * Preferencias alimentarias. Los alimentos de [prohibidos] son un filtro duro:
 * no pueden aparecer en el menu ni en una sustitucion automatica.
 */
@Serializable
data class PreferenciasAlimentarias(
    val prohibidos: Set<String> = setOf("pollo", "huevo hervido", "avena"),
    val permitidosExplicitos: Set<String> = setOf("huevo revuelto", "tortilla de huevo"),
    val puedeLlevarComidaAlTrabajo: Boolean = true,
    val tieneNevera: Boolean = true,
    val tieneMicroondas: Boolean = true,
    val presupuestoEsLimitante: Boolean = false,
)

/**
 * Tiempos de traslado en minutos. [gimnasioACasaConfirmado] recuerda que 15 min
 * es una estimacion provisional que el usuario todavia no ha verificado.
 */
@Serializable
data class Traslados(
    val casaATrabajoMin: Int = 10,
    val escuelaACasaMin: Int = 5,
    val escuelaAGimnasioMin: Int = 5,
    val gimnasioACasaMin: Int = 15,
    val gimnasioACasaConfirmado: Boolean = false,
)

/** Preferencias de la app que el usuario puede cambiar sin tocar el plan. */
@Serializable
data class Ajustes(
    val margenAvisoMin: Int = 10,
    val minutosPosponer: Int = 10,
    /** Nunca se fija una meta de pasos automatica: la rodilla no lo permite. */
    val metasDePasosAutomaticas: Boolean = false,
    /** Rango orientativo de liquidos, no una cuota obligatoria. */
    val aguaOrientativaLitros: ClosedFloatingPointRange<Double> = 2.0..2.5,
    /** Omitir datos identificables del nino en exportaciones. */
    val anonimizarNinoAlExportar: Boolean = true,
)
