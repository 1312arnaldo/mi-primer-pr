package com.misaludyfuerza.core.alimentacion

import com.misaludyfuerza.core.perfil.PreferenciasAlimentarias
import kotlinx.serialization.Serializable

@Serializable
enum class Categoria { PROTEINA, CARBOHIDRATO, VEGETAL, FRUTA, LACTEO, GRASA, FRUTO_SECO, CEREAL }

/**
 * Alimento del catalogo. Todos los valores son ESTIMACIONES de referencia por
 * 100 g de peso ya cocinado (conservas escurridas), no mediciones del producto
 * concreto que el usuario compre.
 *
 * [rendimientoCocidoSobreCrudo] convierte peso cocinado en peso de compra:
 * crudo = cocinado / rendimiento. Es editable porque depende del corte y de la
 * coccion.
 */
@Serializable
data class Alimento(
    val id: String,
    val nombre: String,
    val categoria: Categoria,
    val kcalPor100g: Double,
    val proteinaPor100g: Double,
    val rendimientoCocidoSobreCrudo: Double = 1.0,
    val fuente: String = FUENTE_GENERICA,
    val esEstimacion: Boolean = true,
) {
    fun kcalPara(gramos: Double): Double = kcalPor100g * gramos / 100.0
    fun proteinaPara(gramos: Double): Double = proteinaPor100g * gramos / 100.0

    /** Gramos de compra (crudo) necesarios para obtener [gramosCocinados]. */
    fun gramosDeCompra(gramosCocinados: Double): Double =
        gramosCocinados / rendimientoCocidoSobreCrudo

    companion object {
        const val FUENTE_GENERICA =
            "Estimacion generica por 100 g cocinados. Verifica la etiqueta del producto; " +
                "la app no mide tu comida."
    }
}

/** Una porcion concreta dentro de una comida. */
@Serializable
data class Porcion(val alimentoId: String, val gramos: Double, val nota: String? = null)

@Serializable
data class Comida(
    val tipo: TipoComida,
    val porciones: List<Porcion>,
    val nota: String? = null,
)

@Serializable
enum class TipoComida(val etiqueta: String) {
    DESAYUNO("Desayuno"),
    MERIENDA_MANANA("Merienda de la manana"),
    ALMUERZO("Almuerzo"),
    MERIENDA_TARDE("Merienda de la tarde"),
    CENA("Cena"),
}

@Serializable
data class TotalesNutricionales(val kcal: Double, val proteinaG: Double) {
    operator fun plus(o: TotalesNutricionales) =
        TotalesNutricionales(kcal + o.kcal, proteinaG + o.proteinaG)

    companion object {
        val CERO = TotalesNutricionales(0.0, 0.0)
    }
}

/**
 * Catalogo de alimentos. Aplica el filtro duro de preferencias: lo prohibido no
 * se puede consultar ni proponer como sustitucion.
 */
class Catalogo(
    alimentos: List<Alimento>,
    private val preferencias: PreferenciasAlimentarias = PreferenciasAlimentarias(),
) {
    private val porId = alimentos.associateBy { it.id }

    val todos: List<Alimento> get() = porId.values.toList()

    /** Alimentos que se pueden ofrecer al usuario (excluye los prohibidos). */
    val permitidos: List<Alimento> get() = todos.filter { esPermitido(it) }

    fun get(id: String): Alimento = porId[id]
        ?: error("Alimento desconocido: $id")

    fun esPermitido(a: Alimento): Boolean = esPermitido(a.nombre)

    /**
     * Filtro duro de preferencias. Compara por texto normalizado para que
     * "Huevo hervido" quede fuera y "Huevo revuelto" siga permitido.
     */
    fun esPermitido(nombre: String): Boolean {
        val n = normalizar(nombre)
        if (preferencias.permitidosExplicitos.any { normalizar(it) == n }) return true
        return preferencias.prohibidos.none { p ->
            val np = normalizar(p)
            n == np || n.contains(np)
        }
    }

    fun totales(comida: Comida): TotalesNutricionales =
        comida.porciones.fold(TotalesNutricionales.CERO) { acc, p ->
            val a = get(p.alimentoId)
            acc + TotalesNutricionales(a.kcalPara(p.gramos), a.proteinaPara(p.gramos))
        }

    fun totales(comidas: List<Comida>): TotalesNutricionales =
        comidas.fold(TotalesNutricionales.CERO) { acc, c -> acc + totales(c) }

    /**
     * Sustituciones con proteina y energia comparables. Nunca devuelve alimentos
     * prohibidos, ni siquiera si el usuario los busca a proposito.
     */
    fun sugerirSustituciones(
        porcion: Porcion,
        tolerancia: Double = 0.25,
        maximo: Int = 5,
    ): List<Sustitucion> {
        val original = get(porcion.alimentoId)
        val kcalObjetivo = original.kcalPara(porcion.gramos)
        val proteinaObjetivo = original.proteinaPara(porcion.gramos)

        return permitidos
            .filter { it.id != original.id && it.categoria == original.categoria }
            .mapNotNull { candidato ->
                // Ajusta la porcion para igualar la proteina cuando el alimento la aporta;
                // si no aporta proteina (grasas, fruta), iguala la energia.
                val gramos = when {
                    proteinaObjetivo > 1.0 && candidato.proteinaPor100g > 0.5 ->
                        proteinaObjetivo * 100.0 / candidato.proteinaPor100g
                    candidato.kcalPor100g > 0 -> kcalObjetivo * 100.0 / candidato.kcalPor100g
                    else -> return@mapNotNull null
                }.let { redondear5(it) }

                val kcal = candidato.kcalPara(gramos)
                val prot = candidato.proteinaPara(gramos)
                val desvKcal = if (kcalObjetivo > 0) Math.abs(kcal - kcalObjetivo) / kcalObjetivo else 0.0
                val desvProt = if (proteinaObjetivo > 0) Math.abs(prot - proteinaObjetivo) / proteinaObjetivo else 0.0
                if (desvKcal > tolerancia || desvProt > tolerancia) return@mapNotNull null

                Sustitucion(
                    original = porcion,
                    sustituto = Porcion(candidato.id, gramos),
                    kcalOriginal = kcalObjetivo,
                    kcalSustituto = kcal,
                    proteinaOriginal = proteinaObjetivo,
                    proteinaSustituto = prot,
                )
            }
            .sortedBy { it.desviacion }
            .take(maximo)
    }

    private fun redondear5(g: Double): Double = Math.round(g / 5.0) * 5.0

    private fun normalizar(s: String) = s.lowercase()
        .replace('á', 'a').replace('é', 'e').replace('í', 'i')
        .replace('ó', 'o').replace('ú', 'u').replace('ñ', 'n')
        .trim()
}

@Serializable
data class Sustitucion(
    val original: Porcion,
    val sustituto: Porcion,
    val kcalOriginal: Double,
    val kcalSustituto: Double,
    val proteinaOriginal: Double,
    val proteinaSustituto: Double,
) {
    val desviacion: Double
        get() {
            val k = if (kcalOriginal > 0) Math.abs(kcalSustituto - kcalOriginal) / kcalOriginal else 0.0
            val p = if (proteinaOriginal > 0) Math.abs(proteinaSustituto - proteinaOriginal) / proteinaOriginal else 0.0
            return k + p
        }
}

/** Catalogo precargado. Ningun alimento prohibido entra en esta lista. */
object CatalogoBase {

    val ALIMENTOS: List<Alimento> = listOf(
        // Proteinas (peso cocinado)
        Alimento("pavo", "Pavo fresco", Categoria.PROTEINA, 135.0, 29.0, 0.75),
        Alimento("cerdo_lomo", "Lomo de cerdo", Categoria.PROTEINA, 175.0, 28.0, 0.75),
        Alimento("res_magra", "Res magra", Categoria.PROTEINA, 190.0, 28.0, 0.73),
        Alimento("pescado_blanco", "Pescado blanco", Categoria.PROTEINA, 110.0, 23.0, 0.80),
        Alimento("salmon", "Salmon", Categoria.PROTEINA, 210.0, 25.0, 0.80),
        Alimento("camarones", "Camarones", Categoria.PROTEINA, 100.0, 21.0, 0.85),
        Alimento("atun_lata", "Atun en lata escurrido", Categoria.PROTEINA, 116.0, 26.0, 1.0),
        Alimento("huevo_revuelto", "Huevo revuelto", Categoria.PROTEINA, 155.0, 13.0, 1.0),
        Alimento("claras", "Claras de huevo", Categoria.PROTEINA, 52.0, 11.0, 1.0),
        // Carbohidratos (peso cocinado)
        Alimento("arroz", "Arroz cocido", Categoria.CARBOHIDRATO, 130.0, 2.7, 2.60),
        Alimento("papa", "Papa cocida", Categoria.CARBOHIDRATO, 87.0, 2.0, 0.90),
        Alimento("boniato", "Boniato cocido", Categoria.CARBOHIDRATO, 90.0, 2.0, 0.90),
        Alimento("frijoles", "Frijoles cocidos escurridos", Categoria.CARBOHIDRATO, 120.0, 8.0, 2.30),
        Alimento("pasta", "Pasta integral cocida", Categoria.CARBOHIDRATO, 145.0, 6.0, 2.40),
        Alimento("quinoa", "Quinoa cocida", Categoria.CARBOHIDRATO, 120.0, 4.4, 2.80),
        Alimento("pan_integral", "Pan integral tostado", Categoria.CEREAL, 250.0, 10.0, 1.0),
        // Vegetales y frutas
        Alimento("vegetales", "Vegetales cocidos", Categoria.VEGETAL, 40.0, 2.5, 0.90),
        Alimento("ensalada", "Ensalada mixta", Categoria.VEGETAL, 20.0, 1.5, 1.0),
        Alimento("fruta", "Fruta (pieza mediana)", Categoria.FRUTA, 60.0, 0.7, 1.0),
        // Lacteos
        Alimento("yogur_griego", "Yogur griego alto en proteina", Categoria.LACTEO, 60.0, 10.0, 1.0),
        Alimento("cottage", "Cottage / requeson alto en proteina", Categoria.LACTEO, 90.0, 12.0, 1.0),
        Alimento("skyr", "Skyr natural", Categoria.LACTEO, 63.0, 11.0, 1.0),
        // Grasas
        Alimento("aceite", "Aceite de oliva", Categoria.GRASA, 884.0, 0.0, 1.0),
        Alimento("aguacate", "Aguacate", Categoria.GRASA, 160.0, 2.0, 1.0),
        Alimento("nueces", "Nueces", Categoria.FRUTO_SECO, 650.0, 15.0, 1.0),
        Alimento("almendras", "Almendras", Categoria.FRUTO_SECO, 600.0, 21.0, 1.0),
    )

    fun catalogo(preferencias: PreferenciasAlimentarias = PreferenciasAlimentarias()) =
        Catalogo(ALIMENTOS, preferencias)
}
