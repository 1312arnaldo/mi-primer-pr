package com.misaludyfuerza.core

import com.misaludyfuerza.core.entrenamiento.EjercicioRegistrado
import com.misaludyfuerza.core.entrenamiento.MotorProgresion
import com.misaludyfuerza.core.entrenamiento.ParametrosSesion
import com.misaludyfuerza.core.entrenamiento.RutinasBase
import com.misaludyfuerza.core.entrenamiento.SerieRegistrada
import com.misaludyfuerza.core.entrenamiento.SesionEntrenamiento
import com.misaludyfuerza.core.entrenamiento.TipoPropuesta
import com.misaludyfuerza.core.salud.AutorizacionProfesional
import com.misaludyfuerza.core.salud.EstadoAutorizacion
import com.misaludyfuerza.core.salud.ReporteSintoma
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProgresionTest {

    private val rutina = RutinasBase.A
    private val ejercicio = rutina.ejercicios[0]
    private val autorizada = AutorizacionProfesional(estado = EstadoAutorizacion.AUTORIZADA_CON_LIMITES)

    private fun sesion(
        fecha: String,
        reps: Int,
        series: Int = 2,
        rir: Int? = 3,
        tecnica: Boolean = true,
        sintoma: ReporteSintoma? = null,
    ) = SesionEntrenamiento(
        fechaIso = fecha,
        rutinaId = rutina.id,
        completada = true,
        sintoma = sintoma,
        ejercicios = listOf(
            EjercicioRegistrado(
                ejercicio.id,
                (1..series).map { SerieRegistrada(it, 20.0, reps, rir, tecnica) },
            ),
        ),
    )

    @Test
    fun `las dos primeras sesiones efectivas se mantienen en dos series`() {
        val p0 = MotorProgresion.evaluar(rutina, ejercicio, emptyList())
        assertEquals(TipoPropuesta.MANTENER, p0.tipo)
        assertEquals(2, p0.nuevoNumeroDeSeries)

        val p1 = MotorProgresion.evaluar(rutina, ejercicio, listOf(sesion("2026-09-14", 12)))
        assertEquals(TipoPropuesta.MANTENER, p1.tipo)
        assertTrue(p1.mensaje.contains("1 de 2 sesiones"))
    }

    @Test
    fun `la progresion cuenta sesiones realizadas y no fechas transcurridas`() {
        // Un mes de calendario pero ninguna sesion completada: sigue sin progresar.
        val noCompletadas = listOf(
            sesion("2026-09-14", 12).copy(completada = false),
            sesion("2026-09-16", 12).copy(completada = false),
            sesion("2026-10-14", 12).copy(completada = false),
        )
        val p = MotorProgresion.evaluar(rutina, ejercicio, noCompletadas, autorizada)
        assertEquals(TipoPropuesta.MANTENER, p.tipo)
        assertTrue(p.mensaje.contains("0 de 2 sesiones"))
    }

    @Test
    fun `sin autorizacion la subida de peso queda en espera`() {
        val historial = listOf(sesion("2026-09-14", 12), sesion("2026-09-16", 12))
        val p = MotorProgresion.evaluar(rutina, ejercicio, historial)
        assertEquals(TipoPropuesta.ESPERAR_AUTORIZACION, p.tipo)
        assertTrue(p.mensaje.contains("Pendiente de confirmar"))
        assertTrue(p.requiereConfirmacion)
    }

    @Test
    fun `con autorizacion se propone el incremento minimo y siempre pide confirmacion`() {
        val historial = listOf(sesion("2026-09-14", 12), sesion("2026-09-16", 12))
        val p = MotorProgresion.evaluar(rutina, ejercicio, historial, autorizada)
        assertEquals(TipoPropuesta.SUBIR_PESO, p.tipo)
        assertEquals(ParametrosSesion().incrementoMinimoKg, p.incrementoKg)
        assertTrue(p.requiereConfirmacion, "Ningun cambio se aplica sin confirmacion")
        assertTrue(p.mensaje.contains("Confirma tu"))
    }

    @Test
    fun `no se sube peso si no se llega al tope del rango`() {
        val historial = listOf(sesion("2026-09-14", 10), sesion("2026-09-16", 11))
        val p = MotorProgresion.evaluar(rutina, ejercicio, historial, autorizada)
        assertTrue(p.tipo != TipoPropuesta.SUBIR_PESO)
    }

    @Test
    fun `no se sube peso sin margen de repeticiones`() {
        val historial = listOf(
            sesion("2026-09-14", 12, rir = 0),
            sesion("2026-09-16", 12, rir = 0),
        )
        val p = MotorProgresion.evaluar(rutina, ejercicio, historial, autorizada)
        assertTrue(p.tipo != TipoPropuesta.SUBIR_PESO, "Sin margen no se progresa: no se entrena al fallo")
    }

    @Test
    fun `no se sube peso con mala tecnica`() {
        val historial = listOf(
            sesion("2026-09-14", 12, tecnica = false),
            sesion("2026-09-16", 12, tecnica = false),
        )
        val p = MotorProgresion.evaluar(rutina, ejercicio, historial, autorizada)
        assertTrue(p.tipo != TipoPropuesta.SUBIR_PESO)
    }

    @Test
    fun `un sintoma congela cualquier progresion aunque haya autorizacion`() {
        val historial = listOf(
            sesion("2026-09-14", 12),
            sesion("2026-09-16", 12, sintoma = ReporteSintoma("2026-09-16", dolor0a10 = 4)),
        )
        val p = MotorProgresion.evaluar(rutina, ejercicio, historial, autorizada)
        assertEquals(TipoPropuesta.CONGELAR_POR_SINTOMA, p.tipo)
        assertTrue(p.mensaje.contains("consulta"))
    }

    @Test
    fun `la tercera serie solo se propone en los dos primeros ejercicios`() {
        val quinto = rutina.ejercicios[4]
        fun sesionDe(id: String, fecha: String) = SesionEntrenamiento(
            fechaIso = fecha, rutinaId = rutina.id, completada = true,
            ejercicios = listOf(
                EjercicioRegistrado(id, (1..2).map { SerieRegistrada(it, 20.0, 10, 3, true) }),
            ),
        )
        val historial = listOf(sesionDe(quinto.id, "2026-09-14"), sesionDe(quinto.id, "2026-09-16"))
        val p = MotorProgresion.evaluar(rutina, quinto, historial, autorizada)
        assertEquals(TipoPropuesta.MANTENER, p.tipo)

        val segundo = rutina.ejercicios[1]
        val historial2 = listOf(sesionDe(segundo.id, "2026-09-14"), sesionDe(segundo.id, "2026-09-16"))
        val p2 = MotorProgresion.evaluar(rutina, segundo, historial2, autorizada)
        assertEquals(TipoPropuesta.ANADIR_SERIE, p2.tipo)
        assertEquals(3, p2.nuevoNumeroDeSeries)
    }

    @Test
    fun `los parametros de sesion respetan los limites pedidos`() {
        val p = ParametrosSesion()
        assertEquals(50, p.limiteSesionMin)
        assertEquals(3, p.repeticionesEnReserva)
        assertEquals("90-120 segundos", p.descansoTexto)
    }
}
