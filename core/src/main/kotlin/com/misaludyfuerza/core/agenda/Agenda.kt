package com.misaludyfuerza.core.agenda

import com.misaludyfuerza.core.perfil.Traslados
import com.misaludyfuerza.core.salud.AutorizacionProfesional
import com.misaludyfuerza.core.salud.EstadoAutorizacion
import com.misaludyfuerza.core.tiempo.Hora12
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

enum class TipoEvento(val etiqueta: String) {
    DESPERTAR("Levantarme"),
    PESO("Pesarme"),
    CINTURA("Medir cintura"),
    NINO_RECIBIR("Recibir al nino"),
    NINO_PREPARAR("Preparar al nino"),
    NINO_ESCUELA("Llevar al nino a la escuela"),
    NINO_RECOGER("Recoger al nino"),
    NINO_CUSTODIA_INICIO("Inicio de fin de semana con el nino"),
    NINO_CUSTODIA_FIN("Fin de fin de semana con el nino"),
    DESAYUNO("Desayuno"),
    MERIENDA_MANANA("Merienda"),
    ALMUERZO("Almuerzo"),
    MERIENDA_TARDE("Merienda"),
    CENA("Cena"),
    GIMNASIO("Gimnasio"),
    TRASLADO("Traslado"),
    ASEO("Ducha, vestirme y afeitarme"),
    TRABAJO_SALIR("Salir al trabajo"),
    TRABAJO_LLEGAR("Llegar al trabajo"),
    TRABAJO_FIN("Salir del trabajo"),
    PREPARAR_COSAS_NINO("Preparar lonchera, ropa y cosas del nino"),
    DORMIR("Objetivo de acostarme"),
    PREP_COMIDAS("Preparar comidas de la semana"),
    REVISION_SEMANAL("Revisar la semana"),
    RENOVAR_COMIDAS("Renovar comidas"),
    ;

    val esComida: Boolean
        get() = this in setOf(DESAYUNO, MERIENDA_MANANA, ALMUERZO, MERIENDA_TARDE, CENA)
}

/**
 * Un evento del plan para un dia concreto.
 *
 * [uid] es estable y deterministico (tipo + fecha + orden): al regenerar la
 * agenda o sincronizar con un calendario externo el mismo evento conserva su
 * identidad, de modo que no se duplican entregas del nino ni entrenamientos.
 */
data class EventoPlan(
    val uid: String,
    val tipo: TipoEvento,
    val titulo: String,
    val fecha: LocalDate,
    val inicio: LocalTime,
    val fin: LocalTime? = null,
    /** false = estimacion editable que el usuario todavia no ha confirmado. */
    val confirmado: Boolean = true,
    /** true = horario de descanso, puede moverse sin romper el plan. */
    val flexible: Boolean = false,
    val nota: String? = null,
    val requiereValidacionMedica: Boolean = false,
) {
    val inicioLdt: LocalDateTime get() = LocalDateTime.of(fecha, inicio)
    val finLdt: LocalDateTime? get() = fin?.let { LocalDateTime.of(fecha, it) }

    /** Texto listo para la interfaz, siempre en 12 horas. */
    val horaVisible: String
        get() = if (fin != null) Hora12.rango(inicio, fin) else Hora12.hora(inicio)
}

enum class EstadoEvento { PENDIENTE, HECHO, POSPUESTO, OMITIDO }

/**
 * Registro de lo que realmente paso con un evento. La fecha y hora originales se
 * conservan siempre, aunque el evento se posponga.
 */
data class RegistroEvento(
    val uid: String,
    val estado: EstadoEvento = EstadoEvento.PENDIENTE,
    val fechaOriginal: LocalDate,
    val horaOriginal: LocalTime,
    val nuevaHora: LocalTime? = null,
    val nota: String? = null,
)

data class HorarioBase(
    val despertar: LocalTime = LocalTime.of(6, 0),
    val recibirNino: LocalTime = LocalTime.of(6, 20),
    val desayuno: LocalTime = LocalTime.of(6, 45),
    val salirEscuela: LocalTime = LocalTime.of(7, 30),
    val limiteEntregaEscuela: LocalTime = LocalTime.of(7, 40),
    val gimnasioInicio: LocalTime = LocalTime.of(7, 45),
    val gimnasioFin: LocalTime = LocalTime.of(8, 35),
    val aseoInicio: LocalTime = LocalTime.of(8, 50),
    val aseoFin: LocalTime = LocalTime.of(9, 20),
    val salirTrabajo: LocalTime = LocalTime.of(9, 25),
    val llegarTrabajo: LocalTime = LocalTime.of(9, 40),
    val meriendaManana: LocalTime = LocalTime.of(9, 45),
    val almuerzo: LocalTime = LocalTime.of(13, 0),
    val meriendaTarde: LocalTime = LocalTime.of(17, 0),
    val finTrabajo: LocalTime = LocalTime.of(19, 0),
    val cena: LocalTime = LocalTime.of(20, 0),
    val prepararCosasNino: LocalTime = LocalTime.of(21, 0),
    val cinturaSabado: LocalTime = LocalTime.of(7, 15),
    val prepComidasDomingo: LocalTime = LocalTime.of(18, 0),
    val renovarComidasMiercoles: LocalTime = LocalTime.of(20, 30),
    val diasDePeso: Set<DayOfWeek> = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
    val diasDeGimnasio: Set<DayOfWeek> = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
)

/**
 * Objetivo de sueno: llegar a las 10:00 p. m. avanzando de forma gradual desde el
 * horario habitual de 11:00 p. m. - 12:00 a. m., no de golpe.
 */
data class ObjetivoSueno(
    val horaPartida: LocalTime = LocalTime.of(23, 30),
    val horaObjetivo: LocalTime = LocalTime.of(22, 0),
    val minutosPorSemana: Int = 15,
) {
    fun horaPara(fechaActivacion: LocalDate, fecha: LocalDate): LocalTime {
        val semanas = java.time.temporal.ChronoUnit.WEEKS.between(fechaActivacion, fecha)
            .coerceAtLeast(0)
        val adelanto = (semanas * minutosPorSemana)
        val propuesta = horaPartida.minusMinutes(adelanto)
        return if (propuesta.isBefore(horaObjetivo)) horaObjetivo else propuesta
    }
}

/**
 * Ventana de referencia del plan. Si la app se instala despues del 8 de septiembre
 * de 2026, las fechas de referencia se conservan como documentacion pero los
 * eventos se generan desde [fechaActivacion] hacia adelante: nunca se disparan
 * avisos vencidos.
 */
data class ConfiguracionPlan(
    val fechaInicioReferencia: LocalDate = LocalDate.of(2026, 9, 8),
    val fechaFinReferencia: LocalDate = LocalDate.of(2026, 10, 5),
    val fechaActivacion: LocalDate = LocalDate.of(2026, 9, 8),
    val horario: HorarioBase = HorarioBase(),
    val traslados: Traslados = Traslados(),
    val custodia: Custodia = Custodia(),
    val custodiaEntreSemana: CustodiaEntreSemana = CustodiaEntreSemana(),
    val escuela: CalendarioEscolar = CalendarioEscolar(),
    val sueno: ObjetivoSueno = ObjetivoSueno(),
    val autorizacion: AutorizacionProfesional = AutorizacionProfesional(),
)

sealed interface DecisionGimnasio {
    val motivo: String

    data class Programado(val inicio: LocalTime, val fin: LocalTime, override val motivo: String) : DecisionGimnasio
    data class NoProgramado(override val motivo: String) : DecisionGimnasio
}

/**
 * Reglas de gimnasio. Nunca antes de dejar al nino en la escuela, nunca durante
 * la custodia de fin de semana, nunca obligatorio en fin de semana y nunca dentro
 * de un bloque de cuidado del nino.
 */
object ReglasGimnasio {

    fun decidir(fecha: LocalDate, cfg: ConfiguracionPlan): DecisionGimnasio {
        val h = cfg.horario

        if (fecha.dayOfWeek !in h.diasDeGimnasio) {
            return DecisionGimnasio.NoProgramado(
                "Fuera de los dias base de gimnasio (${h.diasDeGimnasio.joinToString { Hora12.diaSemana(it) }})."
            )
        }
        if (fecha.dayOfWeek in setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)) {
            return DecisionGimnasio.NoProgramado("Los fines de semana no llevan gimnasio obligatorio.")
        }
        if (cfg.custodia.tieneNinoEn(LocalDateTime.of(fecha, h.gimnasioInicio))) {
            return DecisionGimnasio.NoProgramado(
                "Fin de semana con el nino: no se programa gimnasio durante la custodia."
            )
        }
        // Martes/jueves por la noche y la manana siguiente: el bloque de cuidado manda.
        val bloqueAnterior = cfg.custodiaEntreSemana.bloqueDeCuidado(fecha.minusDays(1))
        if (bloqueAnterior != null && !cfg.escuela.hayEscuela(fecha)) {
            val motivoEscuela = cfg.escuela.motivoSinEscuela(fecha) ?: "Sin escuela"
            return DecisionGimnasio.NoProgramado(
                "$motivoEscuela y el nino esta contigo desde la recogida de ayer: " +
                    "no se programa gimnasio."
            )
        }
        if (!cfg.escuela.hayEscuela(fecha)) {
            val motivo = cfg.escuela.motivoSinEscuela(fecha) ?: "Sin escuela"
            return DecisionGimnasio.NoProgramado(
                "$motivo. El gimnasio va despues de dejar al nino; sin entrega confirmada no se " +
                    "programa solo. Puedes anadirlo a mano si ese dia si puedes ir."
            )
        }

        val motivo = if (cfg.custodia.durmioPorCustodia(fecha)) {
            "Lunes despues del fin de semana con el nino: el gimnasio sigue siendo posible " +
                "porque va despues de dejarlo en la escuela."
        } else {
            "Dia base de gimnasio, despues de dejar al nino en la escuela."
        }
        return DecisionGimnasio.Programado(h.gimnasioInicio, h.gimnasioFin, motivo)
    }
}

/** Genera los eventos de un dia aplicando todas las reglas de agenda. */
object GeneradorAgenda {

    fun generarDia(fecha: LocalDate, cfg: ConfiguracionPlan): List<EventoPlan> {
        val h = cfg.horario
        val eventos = mutableListOf<EventoPlan>()
        val esLaboral = fecha.dayOfWeek !in setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
        val hayEscuela = cfg.escuela.hayEscuela(fecha)
        val finDeSemana = !esLaboral

        fun add(
            tipo: TipoEvento,
            inicio: LocalTime,
            fin: LocalTime? = null,
            titulo: String = tipo.etiqueta,
            confirmado: Boolean = true,
            flexible: Boolean = false,
            nota: String? = null,
            requiereValidacionMedica: Boolean = false,
            sufijoUid: String = "",
        ) {
            eventos += EventoPlan(
                uid = uid(tipo, fecha, sufijoUid),
                tipo = tipo,
                titulo = titulo,
                fecha = fecha,
                inicio = inicio,
                fin = fin,
                confirmado = confirmado,
                flexible = flexible,
                nota = nota,
                requiereValidacionMedica = requiereValidacionMedica,
            )
        }

        // --- Manana ---
        add(
            TipoEvento.DESPERTAR, h.despertar,
            flexible = finDeSemana,
            nota = if (finDeSemana) "Fin de semana: puedes descansar mas, los compromisos con el nino se mantienen." else null,
        )

        if (fecha.dayOfWeek in h.diasDePeso) {
            add(
                TipoEvento.PESO, h.despertar.plusMinutes(5),
                nota = "Despues de ir al bano y antes de comer o beber.",
            )
        }
        if (fecha.dayOfWeek == DayOfWeek.SATURDAY) {
            add(TipoEvento.CINTURA, h.cinturaSabado, nota = "Con la cinta metrica, a la misma altura cada semana.")
        }

        // --- Nino: mananas escolares ---
        if (hayEscuela) {
            val durmioAqui = cfg.custodia.durmioPorCustodia(fecha) ||
                cfg.custodiaEntreSemana.durmioPorRecogida(fecha)
            if (durmioAqui) {
                add(
                    TipoEvento.NINO_PREPARAR, h.recibirNino,
                    nota = "Ya durmio contigo: no hay entrega que recibir, solo prepararlo.",
                )
            } else {
                add(TipoEvento.NINO_RECIBIR, h.recibirNino)
            }
            add(
                TipoEvento.NINO_ESCUELA, h.salirEscuela, h.limiteEntregaEscuela,
                titulo = "Salir a la escuela",
                nota = "Dejarlo antes de las ${Hora12.hora(h.limiteEntregaEscuela)}.",
            )
        }

        // --- Custodia de fin de semana ---
        if (fecha.dayOfWeek == DayOfWeek.SATURDAY && cfg.custodia.esSabadoDeCustodia(fecha)) {
            add(
                TipoEvento.NINO_CUSTODIA_INICIO, Custodia.INICIO_SABADO,
                nota = "Hasta el lunes a las ${Hora12.hora(Custodia.FIN_LUNES)}. Sin gimnasio en este periodo.",
            )
        }
        if (fecha.dayOfWeek == DayOfWeek.MONDAY && cfg.custodia.durmioPorCustodia(fecha)) {
            add(
                TipoEvento.NINO_CUSTODIA_FIN, Custodia.FIN_LUNES,
                nota = "Termina al dejarlo en la escuela.",
            )
        }

        // --- Desayuno ---
        add(TipoEvento.DESAYUNO, h.desayuno, flexible = finDeSemana)

        // --- Gimnasio y traslados ---
        when (val d = ReglasGimnasio.decidir(fecha, cfg)) {
            is DecisionGimnasio.Programado -> {
                val requiereValidacion = cfg.autorizacion.estado != EstadoAutorizacion.AUTORIZADA_CON_LIMITES
                add(
                    TipoEvento.GIMNASIO, d.inicio, d.fin,
                    titulo = if (requiereValidacion) "Gimnasio (propuesta por validar)" else "Gimnasio",
                    nota = d.motivo + " Limite de sesion: 50 minutos; sal a las " +
                        "${Hora12.hora(d.fin)} para no llegar tarde.",
                    requiereValidacionMedica = requiereValidacion,
                )
                if (esLaboral) {
                    add(
                        TipoEvento.TRASLADO, d.fin, d.fin.plusMinutes(cfg.traslados.gimnasioACasaMin.toLong()),
                        titulo = "Volver a casa",
                        confirmado = cfg.traslados.gimnasioACasaConfirmado,
                        nota = if (!cfg.traslados.gimnasioACasaConfirmado)
                            "Estimacion sin confirmar de ${cfg.traslados.gimnasioACasaMin} minutos. Ajustala cuando la midas."
                        else null,
                        sufijoUid = "gimnasio-casa",
                    )
                }
            }
            is DecisionGimnasio.NoProgramado -> Unit
        }

        // --- Rutina laboral ---
        if (esLaboral) {
            add(TipoEvento.ASEO, h.aseoInicio, h.aseoFin)
            add(TipoEvento.TRABAJO_SALIR, h.salirTrabajo, nota = "Casa-trabajo: ${cfg.traslados.casaATrabajoMin} minutos.")
            add(
                TipoEvento.TRABAJO_LLEGAR, h.llegarTrabajo,
                nota = "Debes estar en el trabajo a las ${Hora12.hora(h.llegarTrabajo)}.",
            )
            add(TipoEvento.MERIENDA_MANANA, h.meriendaManana)
            add(TipoEvento.ALMUERZO, h.almuerzo)
            add(TipoEvento.MERIENDA_TARDE, h.meriendaTarde)
            add(TipoEvento.TRABAJO_FIN, h.finTrabajo)
            if (cfg.custodiaEntreSemana.hayRecogida(fecha)) {
                add(
                    TipoEvento.NINO_RECOGER, cfg.custodiaEntreSemana.horaRecogidaEstimada,
                    confirmado = cfg.custodiaEntreSemana.horaRecogidaConfirmada,
                    nota = "Estimacion editable de \"7 y algo\". Se queda hasta la escuela de manana.",
                )
            }
        } else {
            add(TipoEvento.MERIENDA_MANANA, h.meriendaManana, flexible = true)
            add(TipoEvento.ALMUERZO, h.almuerzo, flexible = true)
            add(TipoEvento.MERIENDA_TARDE, h.meriendaTarde, flexible = true)
        }

        // --- Noche ---
        add(TipoEvento.CENA, h.cena, flexible = finDeSemana)

        if (fecha.dayOfWeek == DayOfWeek.WEDNESDAY) {
            add(TipoEvento.RENOVAR_COMIDAS, h.renovarComidasMiercoles)
        }
        if (fecha.dayOfWeek == DayOfWeek.SUNDAY) {
            add(TipoEvento.PREP_COMIDAS, h.prepComidasDomingo)
            add(
                TipoEvento.REVISION_SEMANAL, h.prepComidasDomingo.plusMinutes(30),
                nota = "Revisar la semana y preparar la exportacion para compartir.",
            )
        }

        // Preparar cosas del nino: solo si manana hay escuela y el nino esta o llega.
        val manana = fecha.plusDays(1)
        val ninoManana = cfg.escuela.hayEscuela(manana) &&
            (cfg.custodiaEntreSemana.durmioPorRecogida(manana) || cfg.custodia.durmioPorCustodia(manana) ||
                cfg.escuela.hayEscuela(manana))
        if (ninoManana) {
            add(TipoEvento.PREPARAR_COSAS_NINO, h.prepararCosasNino)
        }

        val horaDormir = cfg.sueno.horaPara(cfg.fechaActivacion, fecha)
        add(
            TipoEvento.DORMIR, horaDormir,
            flexible = true,
            nota = "Objetivo gradual hacia las ${Hora12.hora(cfg.sueno.horaObjetivo)}.",
        )

        return eventos.sortedWith(compareBy({ it.inicio }, { it.tipo.ordinal }))
    }

    fun generarRango(desde: LocalDate, hasta: LocalDate, cfg: ConfiguracionPlan): List<EventoPlan> =
        generateSequence(desde) { it.plusDays(1) }
            .takeWhile { !it.isAfter(hasta) }
            .flatMap { generarDia(it, cfg).asSequence() }
            .toList()

    /**
     * Identificador estable. Mismo tipo + misma fecha (+ sufijo) => mismo uid, de
     * forma que regenerar o resincronizar es idempotente.
     */
    fun uid(tipo: TipoEvento, fecha: LocalDate, sufijo: String = ""): String {
        val base = "${tipo.name.lowercase()}-$fecha"
        return if (sufijo.isBlank()) base else "$base-$sufijo"
    }
}

data class Conflicto(val a: EventoPlan, val b: EventoPlan, val descripcion: String)

object DetectorConflictos {
    /** Solapes reales entre eventos con duracion. Los eventos flexibles no generan conflicto. */
    fun detectar(eventos: List<EventoPlan>): List<Conflicto> {
        val conDuracion = eventos.filter { it.fin != null && !it.flexible }.sortedBy { it.inicio }
        val conflictos = mutableListOf<Conflicto>()
        for (i in conDuracion.indices) {
            for (j in i + 1 until conDuracion.size) {
                val a = conDuracion[i]
                val b = conDuracion[j]
                if (b.inicio < a.fin!! && a.inicio < b.fin!!) {
                    conflictos += Conflicto(
                        a, b,
                        "\"${a.titulo}\" (${a.horaVisible}) se solapa con \"${b.titulo}\" (${b.horaVisible}).",
                    )
                }
            }
        }
        return conflictos
    }
}
