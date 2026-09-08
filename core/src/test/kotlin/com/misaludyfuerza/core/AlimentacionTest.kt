package com.misaludyfuerza.core

import com.misaludyfuerza.core.alimentacion.Adherencia
import com.misaludyfuerza.core.alimentacion.CatalogoBase
import com.misaludyfuerza.core.alimentacion.EstadoComida
import com.misaludyfuerza.core.alimentacion.ListaDeCompra
import com.misaludyfuerza.core.alimentacion.MenuBase
import com.misaludyfuerza.core.alimentacion.ObjetivoNutricional
import com.misaludyfuerza.core.alimentacion.Porcion
import com.misaludyfuerza.core.alimentacion.RegistroComida
import com.misaludyfuerza.core.alimentacion.TipoComida
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AlimentacionTest {

    private val catalogo = CatalogoBase.catalogo()

    @Test
    fun `el catalogo no contiene alimentos prohibidos`() {
        val prohibidos = listOf("pollo", "huevo hervido", "avena")
        catalogo.todos.forEach { a ->
            prohibidos.forEach { p ->
                assertFalse(
                    a.nombre.lowercase().contains(p),
                    "El catalogo no puede incluir \"${a.nombre}\"",
                )
            }
        }
    }

    @Test
    fun `el huevo revuelto esta permitido y el hervido no`() {
        assertTrue(catalogo.esPermitido("Huevo revuelto"))
        assertTrue(catalogo.esPermitido("Tortilla de huevo"))
        assertFalse(catalogo.esPermitido("Huevo hervido"))
        assertFalse(catalogo.esPermitido("Pechuga de pollo"))
        assertFalse(catalogo.esPermitido("Avena en hojuelas"))
    }

    @Test
    fun `las sustituciones nunca proponen un alimento prohibido`() {
        MenuBase.SEMANA.values.flatMap { it.comidas }.flatMap { it.porciones }.forEach { p ->
            catalogo.sugerirSustituciones(p).forEach { s ->
                val nombre = catalogo.get(s.sustituto.alimentoId).nombre
                assertTrue(catalogo.esPermitido(nombre), "Sustitucion prohibida: $nombre")
            }
        }
    }

    @Test
    fun `las sustituciones mantienen proteina y energia comparables`() {
        val porcion = Porcion("pavo", 140.0)
        val sustituciones = catalogo.sugerirSustituciones(porcion)
        assertTrue(sustituciones.isNotEmpty(), "Deberia haber alternativas para el pavo")
        sustituciones.forEach { s ->
            val desvProt = Math.abs(s.proteinaSustituto - s.proteinaOriginal) / s.proteinaOriginal
            val desvKcal = Math.abs(s.kcalSustituto - s.kcalOriginal) / s.kcalOriginal
            assertTrue(desvProt <= 0.25, "Proteina demasiado distinta: $s")
            assertTrue(desvKcal <= 0.25, "Energia demasiado distinta: $s")
        }
    }

    @Test
    fun `el menu de cada dia se acerca al objetivo declarado`() {
        val objetivo = ObjetivoNutricional()
        assertTrue(objetivo.esEstimacion)
        DayOfWeek.entries.forEach { dia ->
            val totales = catalogo.totales(MenuBase.dia(dia).comidas)
            assertTrue(
                totales.kcal in 1800.0..2400.0,
                "$dia: ${totales.kcal} kcal fuera del rango razonable",
            )
            assertTrue(
                totales.proteinaG in 145.0..200.0,
                "$dia: ${totales.proteinaG} g de proteina fuera del rango razonable",
            )
        }
    }

    @Test
    fun `cada dia tiene las cinco comidas del plan`() {
        DayOfWeek.entries.forEach { dia ->
            val tipos = MenuBase.dia(dia).comidas.map { it.tipo }
            assertEquals(TipoComida.entries.toSet(), tipos.toSet(), "Faltan comidas en $dia")
            assertEquals(5, tipos.size)
        }
    }

    @Test
    fun `una comida planeada nunca cuenta sola como comida`() {
        val planeada = RegistroComida("2026-09-08", TipoComida.ALMUERZO)
        assertEquals(EstadoComida.PLANEADA, planeada.estado)
        assertFalse(planeada.cuentaComoConsumida, "Lo planeado no cuenta como comido")

        assertTrue(planeada.copy(estado = EstadoComida.COMI).cuentaComoConsumida)
        assertTrue(planeada.copy(estado = EstadoComida.CAMBIE).cuentaComoConsumida)
        assertFalse(planeada.copy(estado = EstadoComida.NO_COMI).cuentaComoConsumida)
    }

    @Test
    fun `la adherencia separa lo no registrado de lo incumplido`() {
        val registros = listOf(
            RegistroComida("2026-09-08", TipoComida.DESAYUNO, EstadoComida.COMI),
            RegistroComida("2026-09-08", TipoComida.ALMUERZO, EstadoComida.CAMBIE),
            RegistroComida("2026-09-08", TipoComida.CENA, EstadoComida.NO_COMI),
        )
        val r = Adherencia.calcular(registros, planeadasTotales = 5)
        assertEquals(2, r.confirmadas)
        assertEquals(1, r.noComidas)
        assertEquals(2, r.sinRegistrar)
        assertEquals(66.7, Math.round(r.porcentaje!! * 10) / 10.0)
        assertEquals(60.0, r.coberturaPorcentaje)
    }

    @Test
    fun `sin registros la adherencia no inventa un porcentaje`() {
        val r = Adherencia.calcular(emptyList(), planeadasTotales = 5)
        assertEquals(null, r.porcentaje)
        assertEquals(5, r.sinRegistrar)
    }

    @Test
    fun `la lista de compra convierte peso cocinado a peso de compra`() {
        val lista = ListaDeCompra.generar(
            LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 13), catalogo,
        )
        assertTrue(lista.isNotEmpty())

        val arroz = lista.first { it.alimentoId == "arroz" }
        assertTrue(
            arroz.gramosDeCompra < arroz.gramosCocinados,
            "El arroz crudo pesa menos que el cocido",
        )
        assertEquals(2.60, arroz.rendimiento)

        val pavo = lista.first { it.alimentoId == "pavo" }
        assertTrue(
            pavo.gramosDeCompra > pavo.gramosCocinados,
            "La carne cruda pesa mas que la cocinada",
        )
        assertTrue(pavo.notaRendimiento.contains("editable"))
    }

    @Test
    fun `todos los alimentos se declaran como estimacion con fuente`() {
        catalogo.todos.forEach {
            assertTrue(it.esEstimacion, "${it.nombre} deberia marcarse como estimacion")
            assertTrue(it.fuente.isNotBlank())
        }
    }
}
