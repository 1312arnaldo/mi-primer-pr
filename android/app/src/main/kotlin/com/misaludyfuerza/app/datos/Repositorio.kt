package com.misaludyfuerza.app.datos

import android.content.Context
import com.misaludyfuerza.core.agenda.CalendarioEscolar
import com.misaludyfuerza.core.agenda.ConfiguracionPlan
import com.misaludyfuerza.core.agenda.Custodia
import com.misaludyfuerza.core.agenda.CustodiaEntreSemana
import com.misaludyfuerza.core.agenda.EstadoEvento
import com.misaludyfuerza.core.agenda.EventoPlan
import com.misaludyfuerza.core.agenda.GeneradorAgenda
import com.misaludyfuerza.core.alimentacion.CatalogoBase
import com.misaludyfuerza.core.alimentacion.EstadoComida
import com.misaludyfuerza.core.alimentacion.MenuBase
import com.misaludyfuerza.core.alimentacion.Porcion
import com.misaludyfuerza.core.alimentacion.RegistroComida
import com.misaludyfuerza.core.alimentacion.TipoComida
import com.misaludyfuerza.core.entrenamiento.EjercicioRegistrado
import com.misaludyfuerza.core.entrenamiento.OrigenRegistro
import com.misaludyfuerza.core.entrenamiento.SerieRegistrada
import com.misaludyfuerza.core.entrenamiento.SesionEntrenamiento
import com.misaludyfuerza.core.estadisticas.Medicion
import com.misaludyfuerza.core.perfil.Perfil
import com.misaludyfuerza.core.perfil.PreferenciasAlimentarias
import com.misaludyfuerza.core.perfil.Traslados
import com.misaludyfuerza.core.salud.AutorizacionProfesional
import com.misaludyfuerza.core.salud.EstadoAutorizacion
import com.misaludyfuerza.core.salud.ReporteSintoma
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime

/**
 * Une la base de datos local con el dominio de :core. La UI y las alarmas hablan
 * solo con esta clase, nunca con Room directamente.
 */
class Repositorio(private val db: BaseDatos) {

    val catalogo = CatalogoBase.catalogo()

    // ---------------------------------------------------------------- configuracion

    /** Configuracion viva del plan: reacciona a ajustes, autorizacion y calendario. */
    fun observarConfiguracion(): Flow<ConfiguracionPlan> = combine(
        db.ajustesDao().observar(),
        db.autorizacionDao().observar(),
        db.autorizacionDao().observarInstrucciones(),
        db.calendarioDao().observarExcepciones(),
        db.calendarioDao().observarDiasEscolares(),
    ) { ajustes, autorizacion, instrucciones, excepciones, diasEscolares ->
        construirConfiguracion(ajustes, autorizacion, instrucciones, excepciones, diasEscolares)
    }

    suspend fun configuracion(): ConfiguracionPlan = construirConfiguracion(
        db.ajustesDao().leer(),
        db.autorizacionDao().leer(),
        db.autorizacionDao().leerInstrucciones(),
        db.calendarioDao().leerExcepciones(),
        db.calendarioDao().leerDiasEscolares(),
    )

    private fun construirConfiguracion(
        ajustes: AjustesEntidad?,
        autorizacion: AutorizacionEntidad?,
        instrucciones: List<InstruccionEntidad>,
        excepciones: List<ExcepcionCustodiaEntidad>,
        diasEscolares: List<DiaEscolarEntidad>,
    ): ConfiguracionPlan {
        val a = ajustes ?: AjustesEntidad(fechaActivacionIso = LocalDate.now().toString())
        return ConfiguracionPlan(
            fechaActivacion = LocalDate.parse(a.fechaActivacionIso),
            traslados = Traslados(
                gimnasioACasaMin = a.gimnasioACasaMin,
                gimnasioACasaConfirmado = a.gimnasioACasaConfirmado,
            ),
            custodia = Custodia(
                excepciones = excepciones.associate { LocalDate.parse(it.sabadoIso) to it.tieneCustodia },
            ),
            custodiaEntreSemana = CustodiaEntreSemana(
                horaRecogidaEstimada = LocalTime.ofSecondOfDay(
                    a.horaRecogidaMinutosDesdeMedianoche * 60L,
                ),
                horaRecogidaConfirmada = a.horaRecogidaConfirmada,
            ),
            escuela = CalendarioEscolar(
                cierres = diasEscolares.filter { it.tipo == "CIERRE" }
                    .map { LocalDate.parse(it.fechaIso) }.toSet(),
                festivos = diasEscolares.filter { it.tipo == "FESTIVO" }
                    .map { LocalDate.parse(it.fechaIso) }.toSet(),
            ),
            autorizacion = autorizacionDominio(autorizacion, instrucciones),
        )
    }

    private fun autorizacionDominio(
        e: AutorizacionEntidad?,
        instrucciones: List<InstruccionEntidad>,
    ) = AutorizacionProfesional(
        estado = runCatching { EstadoAutorizacion.valueOf(e?.estado ?: "") }
            .getOrDefault(EstadoAutorizacion.PENDIENTE_DE_CONFIRMAR),
        profesional = e?.profesional,
        fecha = e?.fechaIso,
        instrucciones = instrucciones.filter { it.tipo == "INSTRUCCION" }.map { it.texto },
        ejerciciosPermitidos = instrucciones.filter { it.tipo == "EJERCICIO_PERMITIDO" }
            .map { it.texto }.toSet(),
        ejerciciosProhibidos = instrucciones.filter { it.tipo == "EJERCICIO_PROHIBIDO" }
            .map { it.texto }.toSet(),
    )

    suspend fun autorizacion(): AutorizacionProfesional =
        autorizacionDominio(db.autorizacionDao().leer(), db.autorizacionDao().leerInstrucciones())

    suspend fun guardarAutorizacion(estado: EstadoAutorizacion, profesional: String?, fechaIso: String?) {
        db.autorizacionDao().guardar(
            AutorizacionEntidad(estado = estado.name, profesional = profesional, fechaIso = fechaIso),
        )
    }

    suspend fun anadirInstruccion(texto: String, tipo: String, fechaIso: String) {
        db.autorizacionDao().anadirInstruccion(InstruccionEntidad(texto = texto, fechaIso = fechaIso, tipo = tipo))
    }

    suspend fun perfil(): Perfil {
        val p = db.perfilDao().leer() ?: return Perfil()
        return Perfil(
            nombre = p.nombre, edad = p.edad, alturaM = p.alturaM,
            pesoInicialKg = p.pesoInicialKg, fechaPesoInicial = p.fechaPesoInicialIso,
            zonaHoraria = p.zonaHoraria,
        )
    }

    val preferencias = PreferenciasAlimentarias()

    suspend fun ajustes(): AjustesEntidad =
        db.ajustesDao().leer() ?: AjustesEntidad(fechaActivacionIso = LocalDate.now().toString())
            .also { db.ajustesDao().guardar(it) }

    suspend fun guardarAjustes(a: AjustesEntidad) = db.ajustesDao().guardar(a)

    // ---------------------------------------------------------------- agenda

    /** Eventos del dia con el estado que el usuario les haya dado. */
    fun observarDia(fecha: LocalDate): Flow<List<EventoConEstado>> = combine(
        observarConfiguracion(),
        db.registroEventoDao().observarDia(fecha.toString()),
    ) { cfg, registros ->
        val porUid = registros.associateBy { it.uid }
        GeneradorAgenda.generarDia(fecha, cfg).map { evento ->
            val r = porUid[evento.uid]
            EventoConEstado(
                evento = evento,
                estado = runCatching { EstadoEvento.valueOf(r?.estado ?: "") }
                    .getOrDefault(EstadoEvento.PENDIENTE),
                nuevaHora = r?.nuevaHoraMin?.let { LocalTime.ofSecondOfDay(it * 60L) },
                nota = r?.nota,
            )
        }
    }

    suspend fun eventosDe(fecha: LocalDate): List<EventoPlan> =
        GeneradorAgenda.generarDia(fecha, configuracion())

    /** Estados guardados de los eventos de un dia, indexados por uid. */
    suspend fun registrosDeEventos(fecha: LocalDate): Map<String, RegistroEventoEntidad> =
        db.registroEventoDao().leerRango(fecha.toString(), fecha.toString()).associateBy { it.uid }

    /**
     * Guarda el estado de un evento conservando SIEMPRE la fecha y la hora
     * originales, aunque se posponga.
     */
    suspend fun registrarEvento(
        evento: EventoPlan,
        estado: EstadoEvento,
        nuevaHora: LocalTime? = null,
        nota: String? = null,
        ahoraIso: String,
    ) {
        val existente = db.registroEventoDao().leer(evento.uid)
        db.registroEventoDao().guardar(
            RegistroEventoEntidad(
                uid = evento.uid,
                fechaIso = existente?.fechaIso ?: evento.fecha.toString(),
                horaOriginalMin = existente?.horaOriginalMin ?: (evento.inicio.toSecondOfDay() / 60),
                estado = estado.name,
                nuevaHoraMin = nuevaHora?.let { it.toSecondOfDay() / 60 } ?: existente?.nuevaHoraMin,
                nota = nota ?: existente?.nota,
                actualizadoIso = ahoraIso,
            ),
        )
    }

    /** Version por uid, para las acciones de la notificacion (no hay objeto evento a mano). */
    suspend fun registrarEventoPorUid(
        uid: String,
        fecha: LocalDate,
        horaOriginal: LocalTime,
        estado: EstadoEvento,
        nuevaHora: LocalTime? = null,
        ahoraIso: String,
    ) {
        val existente = db.registroEventoDao().leer(uid)
        db.registroEventoDao().guardar(
            RegistroEventoEntidad(
                uid = uid,
                fechaIso = existente?.fechaIso ?: fecha.toString(),
                horaOriginalMin = existente?.horaOriginalMin ?: (horaOriginal.toSecondOfDay() / 60),
                estado = estado.name,
                nuevaHoraMin = nuevaHora?.let { it.toSecondOfDay() / 60 },
                nota = existente?.nota,
                actualizadoIso = ahoraIso,
            ),
        )
    }

    // ---------------------------------------------------------------- comidas

    fun observarComidas(fecha: LocalDate): Flow<List<RegistroComida>> =
        db.comidaDao().observarDia(fecha.toString()).map { lista ->
            val porTipo = lista.associateBy { it.tipo }
            MenuBase.dia(fecha).comidas.map { c ->
                val r = porTipo[c.tipo.name]
                RegistroComida(
                    fechaIso = fecha.toString(),
                    tipo = c.tipo,
                    estado = runCatching { EstadoComida.valueOf(r?.estado ?: "") }
                        .getOrDefault(EstadoComida.PLANEADA),
                    nota = r?.nota,
                )
            }
        }

    /**
     * Registra lo que realmente paso con una comida. Solo se llama desde una accion
     * explicita del usuario: notificar una comida nunca la marca como comida.
     */
    suspend fun registrarComida(
        fecha: LocalDate,
        tipo: TipoComida,
        estado: EstadoComida,
        porcionesReales: List<Porcion> = emptyList(),
        nota: String? = null,
        ahoraIso: String,
    ) {
        db.comidaDao().registrarCambio(
            RegistroComidaEntidad(
                fechaIso = fecha.toString(), tipo = tipo.name, estado = estado.name,
                nota = nota, actualizadoIso = ahoraIso,
            ),
            porcionesReales.map {
                PorcionRealEntidad(
                    fechaIso = fecha.toString(), tipo = tipo.name,
                    alimentoId = it.alimentoId, gramos = it.gramos,
                )
            },
        )
    }

    suspend fun registrosComidaRango(desde: LocalDate, hasta: LocalDate): List<RegistroComida> =
        db.comidaDao().leerRango(desde.toString(), hasta.toString()).map {
            RegistroComida(
                fechaIso = it.fechaIso,
                tipo = TipoComida.valueOf(it.tipo),
                estado = runCatching { EstadoComida.valueOf(it.estado) }
                    .getOrDefault(EstadoComida.PLANEADA),
                nota = it.nota,
            )
        }

    // ---------------------------------------------------------------- mediciones

    fun observarMediciones(tipo: String): Flow<List<Medicion>> =
        db.medicionDao().observar(tipo).map { lista ->
            lista.map { Medicion(it.fechaIso, it.valor, it.origen) }
        }

    suspend fun guardarMedicion(tipo: String, fecha: LocalDate, valor: Double, origen: String = "Manual") {
        db.medicionDao().guardar(
            MedicionEntidad(tipo = tipo, fechaIso = fecha.toString(), valor = valor, origen = origen),
        )
    }

    suspend fun medicionesRango(tipo: String, desde: LocalDate, hasta: LocalDate): List<Medicion> =
        db.medicionDao().leerRango(tipo, desde.toString(), hasta.toString())
            .map { Medicion(it.fechaIso, it.valor, it.origen) }

    // ---------------------------------------------------------------- entrenamiento

    suspend fun historialSesiones(): List<SesionEntrenamiento> {
        val sesiones = db.entrenamientoDao().leerTodasLasSesiones()
        if (sesiones.isEmpty()) return emptyList()
        val series = db.entrenamientoDao().leerSeriesDe(sesiones.map { it.id }).groupBy { it.sesionId }
        return sesiones.map { s -> aDominio(s, series[s.id].orEmpty()) }
    }

    suspend fun sesionesRango(desde: LocalDate, hasta: LocalDate): List<SesionEntrenamiento> {
        val sesiones = db.entrenamientoDao().leerSesiones(desde.toString(), hasta.toString())
        if (sesiones.isEmpty()) return emptyList()
        val series = db.entrenamientoDao().leerSeriesDe(sesiones.map { it.id }).groupBy { it.sesionId }
        return sesiones.map { s -> aDominio(s, series[s.id].orEmpty()) }
    }

    private fun aDominio(s: SesionEntidad, series: List<SerieEntidad>) = SesionEntrenamiento(
        fechaIso = s.fechaIso,
        rutinaId = s.rutinaId,
        completada = s.completada,
        duracionMin = s.duracionMin,
        sintoma = ReporteSintoma(
            fechaIso = s.fechaIso, dolor0a10 = s.dolor0a10,
            hinchazon = s.hinchazon, inestabilidad = s.inestabilidad, nota = s.notaSintoma,
        ),
        origen = runCatching { OrigenRegistro.valueOf(s.origen) }.getOrDefault(OrigenRegistro.MANUAL),
        ejercicios = series.groupBy { it.ejercicioId }.map { (id, lista) ->
            EjercicioRegistrado(
                id,
                lista.sortedBy { it.numero }.map {
                    SerieRegistrada(
                        it.numero, it.pesoKg, it.repeticiones,
                        it.repeticionesEnReserva, it.tecnicaBuena, it.nota,
                    )
                },
            )
        },
    )

    suspend fun sesionDelDia(fecha: LocalDate, rutinaId: String): Long {
        db.entrenamientoDao().leerSesion(fecha.toString(), rutinaId)?.let { return it.id }
        return db.entrenamientoDao().insertarSesion(
            SesionEntidad(fechaIso = fecha.toString(), rutinaId = rutinaId),
        )
    }

    suspend fun guardarSerie(
        sesionId: Long, ejercicioId: String, numero: Int,
        pesoKg: Double?, repeticiones: Int, repeticionesEnReserva: Int?,
        tecnicaBuena: Boolean, nota: String?,
    ) {
        db.entrenamientoDao().guardarSerie(
            SerieEntidad(
                sesionId = sesionId, ejercicioId = ejercicioId, numero = numero,
                pesoKg = pesoKg, repeticiones = repeticiones,
                repeticionesEnReserva = repeticionesEnReserva,
                tecnicaBuena = tecnicaBuena, nota = nota,
            ),
        )
    }

    /** Completar una sesion es SIEMPRE una accion del usuario. */
    suspend fun completarSesion(
        sesionId: Long, fecha: LocalDate, rutinaId: String,
        duracionMin: Int?, sintoma: ReporteSintoma?,
    ) {
        db.entrenamientoDao().guardarSesion(
            SesionEntidad(
                id = sesionId, fechaIso = fecha.toString(), rutinaId = rutinaId,
                completada = true, duracionMin = duracionMin,
                dolor0a10 = sintoma?.dolor0a10 ?: 0,
                hinchazon = sintoma?.hinchazon ?: false,
                inestabilidad = sintoma?.inestabilidad ?: false,
                notaSintoma = sintoma?.nota,
            ),
        )
    }

    fun observarHistorialEjercicio(ejercicioId: String) =
        db.entrenamientoDao().observarHistorialEjercicio(ejercicioId)

    // ---------------------------------------------------------------- calendario

    suspend fun guardarExcepcionCustodia(sabado: LocalDate, tieneCustodia: Boolean, nota: String?) {
        db.calendarioDao().guardarExcepcion(
            ExcepcionCustodiaEntidad(sabado.toString(), tieneCustodia, nota),
        )
    }

    suspend fun guardarDiaEscolar(fecha: LocalDate, tipo: String, nota: String?) {
        db.calendarioDao().guardarDiaEscolar(DiaEscolarEntidad(fecha.toString(), tipo, nota))
    }

    // ---------------------------------------------------------------- salud

    val saludDao get() = db.saludDao()
    val planDao get() = db.planDao()

    /** Desconectar Health Connect borra lo importado y su estado de sincronizacion. */
    suspend fun olvidarDatosDeSalud() {
        db.saludDao().borrarTodo()
        db.saludDao().borrarEstados()
    }

    companion object {
        const val TIPO_PESO = "PESO"
        const val TIPO_CINTURA = "CINTURA"

        @Volatile
        private var instancia: Repositorio? = null

        fun obtener(context: Context): Repositorio = instancia ?: synchronized(this) {
            instancia ?: Repositorio(BaseDatos.obtener(context)).also { instancia = it }
        }
    }
}

data class EventoConEstado(
    val evento: EventoPlan,
    val estado: EstadoEvento,
    val nuevaHora: LocalTime? = null,
    val nota: String? = null,
) {
    /** Hora efectiva tras posponer; la original se conserva en [evento]. */
    val horaEfectiva: LocalTime get() = nuevaHora ?: evento.inicio
}
