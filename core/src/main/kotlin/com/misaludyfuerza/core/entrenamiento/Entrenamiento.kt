package com.misaludyfuerza.core.entrenamiento

import com.misaludyfuerza.core.salud.AutorizacionProfesional
import com.misaludyfuerza.core.salud.FiltroRodilla
import com.misaludyfuerza.core.salud.ReporteSintoma
import com.misaludyfuerza.core.salud.VeredictoEjercicio
import kotlinx.serialization.Serializable
import java.time.DayOfWeek

@Serializable
enum class GrupoMuscular { PECHO, ESPALDA, HOMBRO_LATERAL, HOMBRO_POSTERIOR, BICEPS, TRICEPS }

/**
 * Ejercicio de la propuesta. Solo maquinas con respaldo o pecho apoyado, sin
 * empuje de piernas y sin trasladar mancuernas pesadas.
 */
@Serializable
data class Ejercicio(
    val id: String,
    val nombre: String,
    val maquina: String,
    val grupo: GrupoMuscular,
    val repMin: Int,
    val repMax: Int,
    val seriesIniciales: Int = 2,
    val notas: String? = null,
) {
    val rangoTexto: String get() = "$repMin-$repMax repeticiones"
}

@Serializable
data class Rutina(
    val id: String,
    val nombre: String,
    val dia: DayOfWeek,
    val ejercicios: List<Ejercicio>,
)

@Serializable
data class ParametrosSesion(
    val descansoMinSeg: Int = 90,
    val descansoMaxSeg: Int = 120,
    /** Repeticiones que deberian sobrar al terminar la serie. No entrenar al fallo. */
    val repeticionesEnReserva: Int = 3,
    val limiteSesionMin: Int = 50,
    val incrementoMinimoKg: Double = 2.5,
) {
    val descansoTexto: String get() = "$descansoMinSeg-$descansoMaxSeg segundos"
}

/** Rutinas propuestas. Ninguna incluye carga de piernas. */
object RutinasBase {

    val A = Rutina(
        id = "A", nombre = "Rutina A", dia = DayOfWeek.MONDAY,
        ejercicios = listOf(
            Ejercicio("a1", "Press de pecho con respaldo", "Press de pecho sentado", GrupoMuscular.PECHO, 10, 12),
            Ejercicio("a2", "Remo con pecho apoyado", "Remo sentado con apoyo de pecho", GrupoMuscular.ESPALDA, 10, 12),
            Ejercicio("a3", "Elevacion lateral en maquina", "Lateral sentado", GrupoMuscular.HOMBRO_LATERAL, 12, 15),
            Ejercicio("a4", "Biceps en maquina", "Predicador / biceps sentado", GrupoMuscular.BICEPS, 10, 12),
            Ejercicio("a5", "Triceps en maquina sentado", "Extension de triceps sentado", GrupoMuscular.TRICEPS, 10, 12),
        ),
    )

    val B = Rutina(
        id = "B", nombre = "Rutina B", dia = DayOfWeek.WEDNESDAY,
        ejercicios = listOf(
            Ejercicio(
                "b1", "Jalon sentado", "Jalon al pecho sentado", GrupoMuscular.ESPALDA, 10, 12,
                notas = "Solo si la almohadilla de sujecion no presiona ni traba la rodilla. " +
                    "Si no puedes ajustarla, omitelo y usa remo con pecho apoyado.",
            ),
            Ejercicio("b2", "Press de pecho con respaldo", "Press de pecho sentado", GrupoMuscular.PECHO, 10, 12),
            Ejercicio("b3", "Deltoide posterior en maquina", "Peck deck invertido", GrupoMuscular.HOMBRO_POSTERIOR, 12, 15),
            Ejercicio("b4", "Biceps en maquina", "Predicador / biceps sentado", GrupoMuscular.BICEPS, 10, 12),
            Ejercicio("b5", "Triceps en maquina sentado", "Extension de triceps sentado", GrupoMuscular.TRICEPS, 10, 12),
        ),
    )

    val C = Rutina(
        id = "C", nombre = "Rutina C", dia = DayOfWeek.FRIDAY,
        ejercicios = listOf(
            Ejercicio("c1", "Remo con pecho apoyado", "Remo sentado con apoyo de pecho", GrupoMuscular.ESPALDA, 10, 12),
            Ejercicio("c2", "Press de pecho con respaldo", "Press de pecho sentado", GrupoMuscular.PECHO, 10, 12),
            Ejercicio("c3", "Elevacion lateral en maquina", "Lateral sentado", GrupoMuscular.HOMBRO_LATERAL, 12, 15),
            Ejercicio("c4", "Deltoide posterior en maquina", "Peck deck invertido", GrupoMuscular.HOMBRO_POSTERIOR, 12, 15),
            Ejercicio("c5", "Biceps en maquina", "Predicador / biceps sentado", GrupoMuscular.BICEPS, 10, 12),
        ),
    )

    val TODAS = listOf(A, B, C)

    fun porDia(dia: DayOfWeek): Rutina? = TODAS.firstOrNull { it.dia == dia }

    /** Advertencias generales que acompanan a cualquier sesion. */
    val ADVERTENCIAS = listOf(
        "Maquinas con respaldo o pecho apoyado; sin empuje de piernas.",
        "No cargues mancuernas pesadas para trasladarlas: pide ayuda para los ajustes.",
        "Si una posicion o sujecion no es comoda para la rodilla, omite el ejercicio.",
        "No entrenes al fallo: termina cada serie con unas 3 repeticiones de margen.",
        "Limite de sesion de 50 minutos.",
    )

    /**
     * Verificacion de seguridad de la propuesta completa. Debe devolver lista vacia:
     * ningun ejercicio del plan puede cargar la rodilla.
     */
    fun ejerciciosBloqueados(autorizacion: AutorizacionProfesional = AutorizacionProfesional()): List<String> =
        TODAS.flatMap { it.ejercicios }
            .filter { FiltroRodilla.evaluar(it.nombre, autorizacion).veredicto == VeredictoEjercicio.BLOQUEADO }
            .map { it.nombre }
}

@Serializable
data class SerieRegistrada(
    val numero: Int,
    val pesoKg: Double?,
    val repeticiones: Int,
    /** Repeticiones que sintio que le sobraban. null = no lo registro. */
    val repeticionesEnReserva: Int? = null,
    val tecnicaBuena: Boolean = true,
    val nota: String? = null,
)

@Serializable
data class EjercicioRegistrado(val ejercicioId: String, val series: List<SerieRegistrada>)

/**
 * Sesion realmente realizada. [completada] la marca el usuario: entrar al gimnasio
 * o que el reloj detecte actividad NO prueba que hizo los ejercicios.
 */
@Serializable
data class SesionEntrenamiento(
    val fechaIso: String,
    val rutinaId: String,
    val ejercicios: List<EjercicioRegistrado> = emptyList(),
    val completada: Boolean = false,
    val duracionMin: Int? = null,
    val sintoma: ReporteSintoma? = null,
    val origen: OrigenRegistro = OrigenRegistro.MANUAL,
)

@Serializable
enum class OrigenRegistro(val etiqueta: String) {
    MANUAL("Registro manual"),
    IMPORTADO("Importado de un archivo"),
    HEALTH_CONNECT("Health Connect"),
}

@Serializable
enum class TipoPropuesta { MANTENER, ANADIR_SERIE, SUBIR_PESO, CONGELAR_POR_SINTOMA, ESPERAR_AUTORIZACION }

/**
 * Propuesta de progresion. Siempre requiere confirmacion explicita del usuario:
 * la app no cambia el plan sola.
 */
@Serializable
data class PropuestaProgresion(
    val ejercicioId: String,
    val tipo: TipoPropuesta,
    val mensaje: String,
    val nuevoNumeroDeSeries: Int? = null,
    val incrementoKg: Double? = null,
    val requiereConfirmacion: Boolean = true,
)

/**
 * Progresion basada en sesiones EFECTIVAMENTE REALIZADAS, no en fechas
 * transcurridas.
 */
object MotorProgresion {

    fun evaluar(
        rutina: Rutina,
        ejercicio: Ejercicio,
        historial: List<SesionEntrenamiento>,
        autorizacion: AutorizacionProfesional = AutorizacionProfesional(),
        parametros: ParametrosSesion = ParametrosSesion(),
    ): PropuestaProgresion {
        val sesiones = historial
            .filter { it.rutinaId == rutina.id && it.completada }
            .sortedBy { it.fechaIso }

        // 1. Cualquier sintoma activo congela la progresion.
        val ultimaConSintoma = sesiones.lastOrNull()?.sintoma
        if (ultimaConSintoma != null && ultimaConSintoma.haySintoma) {
            return PropuestaProgresion(
                ejercicio.id, TipoPropuesta.CONGELAR_POR_SINTOMA,
                "Registraste sintomas en la ultima sesion. No se propone subir carga ni anadir " +
                    "series. Deten ese movimiento y consulta antes de retomarlo.",
            )
        }

        val realizadas = sesiones.count { s -> s.ejercicios.any { it.ejercicioId == ejercicio.id } }

        // 2. Las dos primeras sesiones efectivas son de 2 series, sin cambios.
        if (realizadas < 2) {
            return PropuestaProgresion(
                ejercicio.id, TipoPropuesta.MANTENER,
                "Llevas $realizadas de 2 sesiones iniciales con este ejercicio. Manten " +
                    "${ejercicio.seriesIniciales} series de ${ejercicio.rangoTexto} y descansa " +
                    "${parametros.descansoTexto}.",
                nuevoNumeroDeSeries = ejercicio.seriesIniciales,
            )
        }

        val ultimasDos = sesiones.takeLast(2)
            .mapNotNull { s -> s.ejercicios.firstOrNull { it.ejercicioId == ejercicio.id } }

        // 3. Subida de peso: dos sesiones cumpliendo el tope del rango con buena tecnica y margen.
        val cumpleDosSesiones = ultimasDos.size == 2 && ultimasDos.all { reg ->
            reg.series.isNotEmpty() && reg.series.all { serie ->
                serie.repeticiones >= ejercicio.repMax &&
                    serie.tecnicaBuena &&
                    (serie.repeticionesEnReserva ?: 0) >= 2
            }
        }
        if (cumpleDosSesiones) {
            if (!autorizacion.puedeProgresarCarga) {
                return PropuestaProgresion(
                    ejercicio.id, TipoPropuesta.ESPERAR_AUTORIZACION,
                    "Cumpliste el rango con margen en dos sesiones. La subida de carga queda en " +
                        "espera hasta que registres la autorizacion de tu traumatologo o " +
                        "fisioterapeuta (estado actual: ${autorizacion.estado.etiqueta}).",
                )
            }
            return PropuestaProgresion(
                ejercicio.id, TipoPropuesta.SUBIR_PESO,
                "Dos sesiones seguidas en ${ejercicio.repMax} repeticiones con buena tecnica y " +
                    "margen. Propuesta: subir el menor incremento disponible en la maquina " +
                    "(aprox. ${parametros.incrementoMinimoKg} kg) y volver al minimo del rango. " +
                    "Confirma tu si quieres aplicarlo.",
                incrementoKg = parametros.incrementoMinimoKg,
            )
        }

        // 4. Tercera serie: solo en los dos primeros ejercicios, con tolerancia y autorizacion.
        val esDeLosDosPrimeros = rutina.ejercicios.indexOfFirst { it.id == ejercicio.id } in 0..1
        val seriesActuales = ultimasDos.lastOrNull()?.series?.size ?: ejercicio.seriesIniciales
        val buenaTolerancia = sesiones.takeLast(2).none { it.sintoma?.haySintoma == true }
        if (esDeLosDosPrimeros && seriesActuales < 3 && realizadas >= 2 && buenaTolerancia) {
            if (!autorizacion.puedeProgresarCarga) {
                return PropuestaProgresion(
                    ejercicio.id, TipoPropuesta.ESPERAR_AUTORIZACION,
                    "Toleraste bien las sesiones iniciales. La tercera serie queda en espera " +
                        "hasta que registres la autorizacion profesional " +
                        "(estado actual: ${autorizacion.estado.etiqueta}).",
                )
            }
            return PropuestaProgresion(
                ejercicio.id, TipoPropuesta.ANADIR_SERIE,
                "Propuesta: anadir una tercera serie en este ejercicio, manteniendo " +
                    "${ejercicio.rangoTexto} y ${parametros.repeticionesEnReserva} repeticiones de " +
                    "margen. Confirma tu si quieres aplicarlo.",
                nuevoNumeroDeSeries = 3,
            )
        }

        return PropuestaProgresion(
            ejercicio.id, TipoPropuesta.MANTENER,
            "Manten $seriesActuales series de ${ejercicio.rangoTexto}. Todavia no se cumplen las " +
                "condiciones para proponer un cambio.",
            nuevoNumeroDeSeries = seriesActuales,
        )
    }
}
