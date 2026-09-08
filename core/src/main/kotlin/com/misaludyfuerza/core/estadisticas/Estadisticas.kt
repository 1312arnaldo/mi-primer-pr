package com.misaludyfuerza.core.estadisticas

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import java.util.Locale

@Serializable
data class Medicion(val fechaIso: String, val valor: Double, val origen: String = "Manual") {
    val fecha: LocalDate get() = LocalDate.parse(fechaIso)
}

/**
 * Cobertura de datos de un periodo. Se muestra SIEMPRE junto a cualquier grafica:
 * un solo dia nunca es una tendencia, y la ausencia de dato no es un cero.
 */
@Serializable
data class Cobertura(val diasConDatos: Int, val diasDelPeriodo: Int) {
    val porcentaje: Double get() = if (diasDelPeriodo == 0) 0.0 else diasConDatos * 100.0 / diasDelPeriodo
    val texto: String get() = "$diasConDatos de $diasDelPeriodo dias con dato (${"%.0f".format(porcentaje)} %)"
}

@Serializable
data class PromedioSemanal(
    val anio: Int,
    val semana: Int,
    val inicioIso: String,
    val promedio: Double,
    val muestras: Int,
)

@Serializable
enum class Veredicto { DATOS_INSUFICIENTES, SIN_CAMBIO_CLARO, BAJANDO, SUBIENDO }

@Serializable
data class Tendencia(
    val veredicto: Veredicto,
    val cambioPorSemana: Double?,
    val semanas: Int,
    val cobertura: Cobertura,
    val mensaje: String,
)

object Series {

    private val SEMANA = WeekFields.of(Locale.forLanguageTag("es-US"))

    /** Promedios por semana. Solo se agregan semanas con al menos una medicion. */
    fun promediosSemanales(mediciones: List<Medicion>): List<PromedioSemanal> =
        mediciones
            .groupBy { m ->
                val f = m.fecha
                f.get(SEMANA.weekBasedYear()) to f.get(SEMANA.weekOfWeekBasedYear())
            }
            .map { (clave, ms) ->
                val fechas = ms.map { it.fecha }.sorted()
                PromedioSemanal(
                    anio = clave.first,
                    semana = clave.second,
                    inicioIso = fechas.first().toString(),
                    promedio = ms.sumOf { it.valor } / ms.size,
                    muestras = ms.size,
                )
            }
            .sortedWith(compareBy({ it.anio }, { it.semana }))

    fun cobertura(mediciones: List<Medicion>, desde: LocalDate, hasta: LocalDate): Cobertura {
        val dias = ChronoUnit.DAYS.between(desde, hasta).toInt() + 1
        val conDato = mediciones.map { it.fecha }
            .filter { !it.isBefore(desde) && !it.isAfter(hasta) }
            .distinct().size
        return Cobertura(conDato, dias)
    }

    /**
     * Tendencia entre promedios semanales. Exige [minimoSemanas] semanas con datos
     * y una cobertura minima; por debajo devuelve DATOS_INSUFICIENTES en lugar de
     * inventar una direccion.
     */
    fun tendencia(
        mediciones: List<Medicion>,
        desde: LocalDate,
        hasta: LocalDate,
        minimoSemanas: Int = 2,
        minimoCoberturaPorcentaje: Double = 40.0,
        umbralCambioPorSemana: Double = 0.1,
    ): Tendencia {
        val cobertura = cobertura(mediciones, desde, hasta)
        val semanas = promediosSemanales(
            mediciones.filter { !it.fecha.isBefore(desde) && !it.fecha.isAfter(hasta) },
        )
        if (semanas.size < minimoSemanas || cobertura.porcentaje < minimoCoberturaPorcentaje) {
            return Tendencia(
                Veredicto.DATOS_INSUFICIENTES, null, semanas.size, cobertura,
                "Aun no hay datos suficientes para hablar de tendencia. " +
                    "${cobertura.texto}; se necesitan al menos $minimoSemanas semanas con registros.",
            )
        }
        val primera = semanas.first()
        val ultima = semanas.last()
        val intervalos = (semanas.size - 1).coerceAtLeast(1)
        val cambio = (ultima.promedio - primera.promedio) / intervalos
        val veredicto = when {
            cambio <= -umbralCambioPorSemana -> Veredicto.BAJANDO
            cambio >= umbralCambioPorSemana -> Veredicto.SUBIENDO
            else -> Veredicto.SIN_CAMBIO_CLARO
        }
        return Tendencia(
            veredicto, cambio, semanas.size, cobertura,
            "Cambio medio de ${"%+.2f".format(cambio)} por semana sobre ${semanas.size} semanas. " +
                cobertura.texto + ".",
        )
    }
}

/** Referencia orientativa de ritmo de perdida. No es una promesa. */
object ReferenciaPerdida {
    const val MIN_KG_SEMANA = 0.3
    const val MAX_KG_SEMANA = 0.7

    fun comentar(cambioKgPorSemana: Double?): String = when {
        cambioKgPorSemana == null -> "Sin datos suficientes para comentar el ritmo."
        cambioKgPorSemana > 0 -> "El promedio semanal subio. Revisa antes de cambiar nada: " +
            "una sola semana no basta para decidir."
        -cambioKgPorSemana in MIN_KG_SEMANA..MAX_KG_SEMANA ->
            "Ritmo dentro de la referencia orientativa de $MIN_KG_SEMANA-$MAX_KG_SEMANA kg por semana."
        -cambioKgPorSemana > MAX_KG_SEMANA ->
            "Ritmo por encima de la referencia orientativa. Conviene revisarlo con calma."
        else -> "Ritmo por debajo de la referencia orientativa. Con 2-3 semanas de datos podras decidir."
    }
}

/**
 * Resumen combinado. La bascula no es el unico criterio: si la cintura baja o la
 * fuerza sube, se muestra aunque el peso no se mueva.
 */
@Serializable
data class ResumenProgreso(
    val peso: Tendencia,
    val cintura: Tendencia,
    val volumenEntrenamiento: Tendencia,
    val sesionesCompletadas: Int,
    val sesionesPlaneadas: Int,
    val adherenciaAlimentariaPorcentaje: Double?,
    val coberturaSueno: Cobertura,
) {
    val lecturaGlobal: String
        get() {
            val senales = buildList {
                if (cintura.veredicto == Veredicto.BAJANDO) add("la cintura baja")
                if (volumenEntrenamiento.veredicto == Veredicto.SUBIENDO) add("el volumen de entrenamiento sube")
                if (peso.veredicto == Veredicto.BAJANDO) add("el peso promedio baja")
            }
            return when {
                senales.isEmpty() -> "Todavia no hay senales claras. Sigue registrando; " +
                    "conviene revisar con 2-3 semanas de datos."
                peso.veredicto == Veredicto.SIN_CAMBIO_CLARO && senales.isNotEmpty() ->
                    "Aunque la bascula no se mueve, ${senales.joinToString(" y ")}. Eso cuenta."
                else -> "Senales positivas: ${senales.joinToString(" y ")}."
            }
        }
}

/**
 * Reglas duras sobre datos del reloj. Existen como codigo para que ningun calculo
 * las salte por descuido.
 */
object ReglasEnergia {
    /**
     * Las calorias activas y las totales miden cosas distintas y se solapan: sumarlas
     * duplica el gasto. Esta funcion existe para dejar constancia y fallar en pruebas
     * si alguien lo intenta.
     */
    fun sumarActivasYTotales(activas: Double, totales: Double): Nothing =
        error(
            "No se pueden sumar calorias activas ($activas) y totales ($totales): se solapan. " +
                "Muestralas por separado indicando su origen.",
        )

    /** La ingesta no se ajusta sola por lo que estime el reloj. */
    const val AJUSTE_AUTOMATICO_DE_INGESTA_HABILITADO = false
}
