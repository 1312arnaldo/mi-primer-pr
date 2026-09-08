package com.misaludyfuerza.core.agenda

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

/**
 * Custodia de fin de semana alterna: desde el sabado a las 8:00 a. m. hasta el
 * lunes a las 7:40 a. m.
 *
 * El ancla confirmada es el sabado 12 de septiembre de 2026 y la paridad se
 * mantiene salvo que el usuario registre una excepcion explicita. La paridad no
 * se recalcula sola aunque la app se instale mas tarde.
 */
data class Custodia(
    val anclaSabado: LocalDate = ANCLA_CONFIRMADA,
    /**
     * Excepciones por sabado: true fuerza custodia, false la cancela.
     * Permite ajustar un fin de semana concreto sin romper la paridad general.
     */
    val excepciones: Map<LocalDate, Boolean> = emptyMap(),
) {
    init {
        require(anclaSabado.dayOfWeek == DayOfWeek.SATURDAY) {
            "El ancla de custodia debe ser un sabado"
        }
    }

    /** Sabado en el que empieza el fin de semana que contiene [fecha]. */
    fun sabadoDelFinDeSemana(fecha: LocalDate): LocalDate = when (fecha.dayOfWeek) {
        DayOfWeek.SATURDAY -> fecha
        DayOfWeek.SUNDAY -> fecha.minusDays(1)
        // El lunes hasta las 7:40 a. m. todavia pertenece al fin de semana anterior.
        DayOfWeek.MONDAY -> fecha.minusDays(2)
        else -> fecha.with(DayOfWeek.SATURDAY)
    }

    fun esSabadoDeCustodia(sabado: LocalDate): Boolean {
        excepciones[sabado]?.let { return it }
        val dias = ChronoUnit.DAYS.between(anclaSabado, sabado)
        return Math.floorMod(dias, 14L) == 0L
    }

    /** True si ese fin de semana (sabado 8:00 a. m. -> lunes 7:40 a. m.) es de custodia. */
    fun esFinDeSemanaDeCustodia(fecha: LocalDate): Boolean =
        esSabadoDeCustodia(sabadoDelFinDeSemana(fecha))

    /** True si en ese instante exacto el nino esta con el usuario por custodia de fin de semana. */
    fun tieneNinoEn(momento: LocalDateTime): Boolean {
        val sabado = sabadoDelFinDeSemana(momento.toLocalDate())
        if (!esSabadoDeCustodia(sabado)) return false
        val inicio = LocalDateTime.of(sabado, INICIO_SABADO)
        val fin = LocalDateTime.of(sabado.plusDays(2), FIN_LUNES)
        return !momento.isBefore(inicio) && momento.isBefore(fin)
    }

    /** El nino durmio en casa la noche anterior a [fecha] por custodia de fin de semana. */
    fun durmioPorCustodia(fecha: LocalDate): Boolean = when (fecha.dayOfWeek) {
        DayOfWeek.SUNDAY, DayOfWeek.MONDAY -> esFinDeSemanaDeCustodia(fecha)
        else -> false
    }

    /** Proximos sabados de custodia a partir de [desde], inclusive. */
    fun proximosSabados(desde: LocalDate, cuantos: Int): List<LocalDate> {
        val primerSabado = if (desde.dayOfWeek == DayOfWeek.SATURDAY) desde
        else desde.with(java.time.temporal.TemporalAdjusters.next(DayOfWeek.SATURDAY))
        return generateSequence(primerSabado) { it.plusWeeks(1) }
            .filter { esSabadoDeCustodia(it) }
            .take(cuantos)
            .toList()
    }

    companion object {
        val ANCLA_CONFIRMADA: LocalDate = LocalDate.of(2026, 9, 12)
        val INICIO_SABADO: LocalTime = LocalTime.of(8, 0)
        val FIN_LUNES: LocalTime = LocalTime.of(7, 40)
    }
}

/**
 * Noches entre semana con el nino: martes y jueves lo recoge despues de las
 * 7:00 p. m. y se queda hasta la escuela de la manana siguiente.
 *
 * La hora 7:15 p. m. es una estimacion editable ("7 y algo"), nunca una hora
 * confirmada.
 */
data class CustodiaEntreSemana(
    val diasDeRecogida: Set<DayOfWeek> = setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY),
    val horaRecogidaEstimada: LocalTime = LocalTime.of(19, 15),
    val horaRecogidaConfirmada: Boolean = false,
) {
    fun hayRecogida(fecha: LocalDate): Boolean = fecha.dayOfWeek in diasDeRecogida

    /** El nino durmio en casa la noche anterior a [fecha] por recogida entre semana. */
    fun durmioPorRecogida(fecha: LocalDate): Boolean =
        fecha.minusDays(1).dayOfWeek in diasDeRecogida

    /** Bloque de cuidado que empieza al recoger y termina al dejarlo en la escuela. */
    fun bloqueDeCuidado(fecha: LocalDate): Pair<LocalDateTime, LocalDateTime>? {
        if (!hayRecogida(fecha)) return null
        return LocalDateTime.of(fecha, horaRecogidaEstimada) to
            LocalDateTime.of(fecha.plusDays(1), LocalTime.of(7, 40))
    }
}

/** Cierres y festivos escolares. Configurable: la app no adivina el calendario. */
data class CalendarioEscolar(
    val cierres: Set<LocalDate> = emptySet(),
    val festivos: Set<LocalDate> = emptySet(),
) {
    fun hayEscuela(fecha: LocalDate): Boolean =
        fecha.dayOfWeek !in setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY) &&
            fecha !in cierres && fecha !in festivos

    fun motivoSinEscuela(fecha: LocalDate): String? = when {
        fecha in cierres -> "Cierre escolar registrado"
        fecha in festivos -> "Festivo escolar registrado"
        fecha.dayOfWeek in setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY) -> "Fin de semana"
        else -> null
    }
}
