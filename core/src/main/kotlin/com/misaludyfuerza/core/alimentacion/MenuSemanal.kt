package com.misaludyfuerza.core.alimentacion

import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Objetivo nutricional de partida. Es una estimacion editable, no una necesidad
 * medida ni una promesa de perdida de peso.
 */
@Serializable
data class ObjetivoNutricional(
    val kcal: Int = 2000,
    val proteinaMinG: Int = 150,
    val proteinaMaxG: Int = 170,
    val esEstimacion: Boolean = true,
    val nota: String =
        "Punto de partida aproximado y editable. No proviene de una medicion de tu " +
            "metabolismo ni garantiza un ritmo de perdida concreto.",
)

@Serializable
data class PlanDiaComidas(val dia: DayOfWeek, val comidas: List<Comida>)

/** Menu precargado de 7 dias, tal como lo definio el usuario. */
object MenuBase {

    private fun p(id: String, g: Double, nota: String? = null) = Porcion(id, g, nota)

    private val DESAYUNO = Comida(
        TipoComida.DESAYUNO,
        listOf(
            p("huevo_revuelto", 100.0, "2 huevos revueltos"),
            p("claras", 100.0),
            p("pan_integral", 60.0, "2 tostadas integrales"),
            p("fruta", 150.0),
            p("aceite", 5.0, "hasta 5 g"),
        ),
    )

    private val MERIENDA_MANANA = Comida(
        TipoComida.MERIENDA_MANANA,
        listOf(p("yogur_griego", 200.0), p("fruta", 150.0)),
    )

    private val MERIENDA_TARDE = Comida(
        TipoComida.MERIENDA_TARDE,
        listOf(p("cottage", 150.0), p("nueces", 15.0)),
        nota = "Alternativa equivalente: yogur griego alto en proteina.",
    )

    private fun almuerzo(proteinaId: String, gramos: Double) = Comida(
        TipoComida.ALMUERZO,
        listOf(
            p(proteinaId, gramos),
            p("arroz", 150.0),
            p("frijoles", 100.0),
            p("ensalada", 150.0),
            p("aceite", 5.0),
        ),
    )

    private fun cena(vararg porciones: Porcion) = Comida(TipoComida.CENA, porciones.toList())

    /** Pesos ya cocinados; conservas escurridas. */
    val SEMANA: Map<DayOfWeek, PlanDiaComidas> = mapOf(
        DayOfWeek.MONDAY to listOf(
            almuerzo("pavo", 140.0),
            cena(p("salmon", 140.0), p("papa", 200.0), p("vegetales", 250.0), p("aceite", 5.0)),
        ),
        DayOfWeek.TUESDAY to listOf(
            almuerzo("cerdo_lomo", 150.0),
            cena(p("pescado_blanco", 160.0), p("boniato", 250.0), p("vegetales", 250.0), p("aceite", 10.0)),
        ),
        DayOfWeek.WEDNESDAY to listOf(
            almuerzo("res_magra", 140.0),
            cena(
                p("camarones", 160.0), p("arroz", 180.0), p("vegetales", 250.0),
                p("aguacate", 50.0), p("aceite", 5.0),
            ),
        ),
        DayOfWeek.THURSDAY to listOf(
            almuerzo("pavo", 140.0),
            cena(p("cerdo_lomo", 150.0), p("papa", 200.0), p("vegetales", 250.0), p("aceite", 5.0)),
        ),
        DayOfWeek.FRIDAY to listOf(
            almuerzo("pescado_blanco", 160.0),
            cena(p("res_magra", 140.0), p("boniato", 200.0), p("vegetales", 250.0), p("aceite", 5.0)),
        ),
        DayOfWeek.SATURDAY to listOf(
            almuerzo("pavo", 140.0),
            cena(p("salmon", 140.0), p("papa", 200.0), p("vegetales", 250.0), p("aceite", 5.0)),
        ),
        DayOfWeek.SUNDAY to listOf(
            almuerzo("cerdo_lomo", 150.0),
            cena(p("pescado_blanco", 160.0), p("boniato", 250.0), p("vegetales", 250.0), p("aceite", 10.0)),
        ),
    ).mapValues { (dia, comidas) ->
        PlanDiaComidas(dia, listOf(DESAYUNO, MERIENDA_MANANA) + comidas[0] + MERIENDA_TARDE + comidas[1])
    }

    fun dia(dia: DayOfWeek): PlanDiaComidas = SEMANA.getValue(dia)

    fun dia(fecha: LocalDate): PlanDiaComidas = dia(fecha.dayOfWeek)
}

@Serializable
enum class EstadoComida(val etiqueta: String) {
    PLANEADA("Planeada"),
    COMI("Comi"),
    CAMBIE("Cambie"),
    NO_COMI("No comi"),
}

/**
 * Lo que realmente paso con una comida.
 *
 * Regla dura: una comida planeada o notificada NUNCA pasa sola a [EstadoComida.COMI].
 * Solo el usuario confirma el consumo real, y [porcionesReales] solo tiene sentido
 * cuando el estado es COMI o CAMBIE.
 */
@Serializable
data class RegistroComida(
    val fechaIso: String,
    val tipo: TipoComida,
    val estado: EstadoComida = EstadoComida.PLANEADA,
    val porcionesReales: List<Porcion> = emptyList(),
    val nota: String? = null,
) {
    val cuentaComoConsumida: Boolean get() = estado == EstadoComida.COMI || estado == EstadoComida.CAMBIE
}

object Adherencia {
    /**
     * Adherencia = comidas confirmadas (Comi o Cambie) sobre comidas con respuesta.
     * Las comidas sin registrar no cuentan como cumplidas ni como incumplidas: se
     * reportan aparte como huecos de datos.
     */
    data class Resultado(
        val confirmadas: Int,
        val noComidas: Int,
        val sinRegistrar: Int,
        val planeadasTotales: Int,
    ) {
        val conRespuesta: Int get() = confirmadas + noComidas
        val porcentaje: Double? get() = if (conRespuesta == 0) null else confirmadas * 100.0 / conRespuesta
        val coberturaPorcentaje: Double
            get() = if (planeadasTotales == 0) 0.0 else conRespuesta * 100.0 / planeadasTotales
    }

    fun calcular(registros: List<RegistroComida>, planeadasTotales: Int): Resultado {
        val confirmadas = registros.count { it.cuentaComoConsumida }
        val noComidas = registros.count { it.estado == EstadoComida.NO_COMI }
        val sinRegistrar = (planeadasTotales - confirmadas - noComidas).coerceAtLeast(0)
        return Resultado(confirmadas, noComidas, sinRegistrar, planeadasTotales)
    }
}

@Serializable
data class LineaCompra(
    val alimentoId: String,
    val nombre: String,
    val gramosCocinados: Double,
    val gramosDeCompra: Double,
    val rendimiento: Double,
) {
    val notaRendimiento: String
        get() = "Rendimiento estimado y editable: ${"%.2f".format(rendimiento)} de peso cocinado " +
            "por cada gramo de compra."
}

object ListaDeCompra {
    /**
     * Agrega el menu de un rango de fechas y convierte peso cocinado a peso de
     * compra con rendimientos estimados y editables.
     */
    fun generar(desde: LocalDate, hasta: LocalDate, catalogo: Catalogo): List<LineaCompra> {
        val acumulado = mutableMapOf<String, Double>()
        var f = desde
        while (!f.isAfter(hasta)) {
            MenuBase.dia(f).comidas.forEach { c ->
                c.porciones.forEach { p ->
                    acumulado.merge(p.alimentoId, p.gramos, Double::plus)
                }
            }
            f = f.plusDays(1)
        }
        return acumulado.entries
            .map { (id, gramos) ->
                val a = catalogo.get(id)
                LineaCompra(
                    alimentoId = id,
                    nombre = a.nombre,
                    gramosCocinados = gramos,
                    gramosDeCompra = Math.round(a.gramosDeCompra(gramos)).toDouble(),
                    rendimiento = a.rendimientoCocidoSobreCrudo,
                )
            }
            .sortedBy { it.nombre }
    }
}

/** Guia de conservacion pedida por el usuario. */
object Conservacion {
    const val DIAS_EN_NEVERA = 4
    const val TEXTO =
        "Refrigera porciones para 3-4 dias y congela el resto. Etiqueta cada envase con la " +
            "fecha de preparacion."
}
