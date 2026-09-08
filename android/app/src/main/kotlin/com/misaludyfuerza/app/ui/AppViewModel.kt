package com.misaludyfuerza.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.misaludyfuerza.app.datos.AjustesEntidad
import com.misaludyfuerza.app.datos.EventoConEstado
import com.misaludyfuerza.app.datos.Repositorio
import com.misaludyfuerza.app.exportar.Exportador
import com.misaludyfuerza.app.notificaciones.Avisos
import com.misaludyfuerza.app.notificaciones.EstadoAvisos
import com.misaludyfuerza.app.notificaciones.ProgramadorAlarmas
import com.misaludyfuerza.app.salud.EstadoHealthConnect
import com.misaludyfuerza.app.salud.EstadoTipo
import com.misaludyfuerza.app.salud.GestorSalud
import com.misaludyfuerza.app.salud.TipoDatoSalud
import com.misaludyfuerza.core.agenda.ConfiguracionPlan
import com.misaludyfuerza.core.agenda.EstadoEvento
import com.misaludyfuerza.core.agenda.EventoPlan
import com.misaludyfuerza.core.alimentacion.EstadoComida
import com.misaludyfuerza.core.alimentacion.ListaDeCompra
import com.misaludyfuerza.core.alimentacion.MenuBase
import com.misaludyfuerza.core.alimentacion.RegistroComida
import com.misaludyfuerza.core.alimentacion.TipoComida
import com.misaludyfuerza.core.entrenamiento.MotorProgresion
import com.misaludyfuerza.core.entrenamiento.PropuestaProgresion
import com.misaludyfuerza.core.entrenamiento.RutinasBase
import com.misaludyfuerza.core.estadisticas.Medicion
import com.misaludyfuerza.core.estadisticas.Series
import com.misaludyfuerza.core.estadisticas.Tendencia
import com.misaludyfuerza.core.salud.AutorizacionProfesional
import com.misaludyfuerza.core.salud.EstadoAutorizacion
import com.misaludyfuerza.core.salud.ProtocoloSintomas
import com.misaludyfuerza.core.salud.ReporteSintoma
import com.misaludyfuerza.core.salud.RespuestaSintoma
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = Repositorio.obtener(app)
    private val gestorSalud = GestorSalud(app, repo)
    private val exportador = Exportador(repo, gestorSalud)

    private val _fecha = MutableStateFlow(LocalDate.now(ZoneId.of("America/New_York")))
    val fecha: StateFlow<LocalDate> = _fecha.asStateFlow()

    private val _mensaje = MutableStateFlow<String?>(null)
    val mensaje: StateFlow<String?> = _mensaje.asStateFlow()

    private val _estadoAvisos = MutableStateFlow(Avisos.estado(app))
    val estadoAvisos: StateFlow<EstadoAvisos> = _estadoAvisos.asStateFlow()

    private val _estadoSalud = MutableStateFlow<EstadoHealthConnect?>(null)
    val estadoSalud: StateFlow<EstadoHealthConnect?> = _estadoSalud.asStateFlow()

    private val _tiposSalud = MutableStateFlow<List<EstadoTipo>>(emptyList())
    val tiposSalud: StateFlow<List<EstadoTipo>> = _tiposSalud.asStateFlow()

    private val _vistaPrevia = MutableStateFlow<String?>(null)
    val vistaPrevia: StateFlow<String?> = _vistaPrevia.asStateFlow()

    val configuracion: StateFlow<ConfiguracionPlan> = repo.observarConfiguracion()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ConfiguracionPlan())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val eventosDelDia: StateFlow<List<EventoConEstado>> = _fecha
        .flatMapLatest { repo.observarDia(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val comidasDelDia: StateFlow<List<RegistroComida>> = _fecha
        .flatMapLatest { repo.observarComidas(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val pesos: StateFlow<List<Medicion>> = repo.observarMediciones(Repositorio.TIPO_PESO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val cinturas: StateFlow<List<Medicion>> = repo.observarMediciones(Repositorio.TIPO_CINTURA)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val autorizacion: StateFlow<AutorizacionProfesional> = configuracion
        .map { it.autorizacion }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AutorizacionProfesional())

    private val _ajustes = MutableStateFlow<AjustesEntidad?>(null)
    val ajustes: StateFlow<AjustesEntidad?> = _ajustes.asStateFlow()

    init {
        viewModelScope.launch {
            _ajustes.value = repo.ajustes()
            refrescarSalud()
        }
    }

    // ------------------------------------------------------------------ agenda

    fun irA(fecha: LocalDate) { _fecha.value = fecha }

    fun marcarEvento(evento: EventoPlan, estado: EstadoEvento) = viewModelScope.launch {
        val zona = ZoneId.of(repo.perfil().zonaHoraria)
        repo.registrarEvento(evento, estado, ahoraIso = LocalDateTime.now(zona).toString())
        if (estado == EstadoEvento.HECHO || estado == EstadoEvento.OMITIDO) {
            ProgramadorAlarmas.cancelar(getApplication(), evento.uid)
        }
        // No se mueve la sesion a otro dia ni se duplica la carga: solo se registra.
        if (evento.tipo == com.misaludyfuerza.core.agenda.TipoEvento.GIMNASIO &&
            estado == EstadoEvento.OMITIDO
        ) {
            _mensaje.value = "Sesion marcada como no realizada. No se mueve a otro dia ni se " +
                "duplica la carga manana: la progresion cuenta sesiones hechas."
        }
    }

    fun posponerEvento(evento: EventoPlan) = viewModelScope.launch {
        val zona = ZoneId.of(repo.perfil().zonaHoraria)
        val minutos = repo.ajustes().minutosPosponer.toLong()
        val nueva = LocalTime.now(zona).plusMinutes(minutos)
        repo.registrarEvento(
            evento, EstadoEvento.POSPUESTO, nuevaHora = nueva,
            ahoraIso = LocalDateTime.now(zona).toString(),
        )
        ProgramadorAlarmas.posponer(
            getApplication(), evento.uid, evento.titulo, evento.titulo,
            evento.fecha, evento.inicio, nueva, zona,
        )
    }

    // ------------------------------------------------------------------ comidas

    /** Solo desde una accion explicita: notificar una comida no la marca como comida. */
    fun registrarComida(tipo: TipoComida, estado: EstadoComida, nota: String? = null) =
        viewModelScope.launch {
            val zona = ZoneId.of(repo.perfil().zonaHoraria)
            repo.registrarComida(
                _fecha.value, tipo, estado, nota = nota,
                ahoraIso = LocalDateTime.now(zona).toString(),
            )
        }

    fun sustitucionesPara(tipo: TipoComida) = MenuBase.dia(_fecha.value).comidas
        .first { it.tipo == tipo }
        .porciones
        .associateWith { repo.catalogo.sugerirSustituciones(it) }

    fun listaDeCompra(dias: Int = 7) =
        ListaDeCompra.generar(_fecha.value, _fecha.value.plusDays((dias - 1).toLong()), repo.catalogo)

    fun totalesDelDia() = repo.catalogo.totales(MenuBase.dia(_fecha.value).comidas)

    // ------------------------------------------------------------------ entrenamiento

    private val _propuestas = MutableStateFlow<List<PropuestaProgresion>>(emptyList())
    val propuestas: StateFlow<List<PropuestaProgresion>> = _propuestas.asStateFlow()

    fun calcularPropuestas(rutinaId: String) = viewModelScope.launch {
        val rutina = RutinasBase.TODAS.first { it.id == rutinaId }
        val historial = repo.historialSesiones()
        val autorizacion = repo.autorizacion()
        _propuestas.value = rutina.ejercicios.map {
            MotorProgresion.evaluar(rutina, it, historial, autorizacion)
        }
    }

    fun guardarSerie(
        rutinaId: String, ejercicioId: String, numero: Int,
        pesoKg: Double?, repeticiones: Int, rir: Int?, tecnicaBuena: Boolean, nota: String?,
    ) = viewModelScope.launch {
        val sesionId = repo.sesionDelDia(_fecha.value, rutinaId)
        repo.guardarSerie(sesionId, ejercicioId, numero, pesoKg, repeticiones, rir, tecnicaBuena, nota)
    }

    private val _respuestaSintoma = MutableStateFlow<RespuestaSintoma?>(null)
    val respuestaSintoma: StateFlow<RespuestaSintoma?> = _respuestaSintoma.asStateFlow()

    fun completarSesion(rutinaId: String, duracionMin: Int?, sintoma: ReporteSintoma?) =
        viewModelScope.launch {
            val sesionId = repo.sesionDelDia(_fecha.value, rutinaId)
            repo.completarSesion(sesionId, _fecha.value, rutinaId, duracionMin, sintoma)
            if (sintoma != null && sintoma.haySintoma) {
                _respuestaSintoma.value = ProtocoloSintomas.responder(sintoma)
            }
            calcularPropuestas(rutinaId)
        }

    fun historialDe(ejercicioId: String) = repo.observarHistorialEjercicio(ejercicioId)

    // ------------------------------------------------------------------ mediciones

    fun guardarPeso(valor: Double) = viewModelScope.launch {
        repo.guardarMedicion(Repositorio.TIPO_PESO, _fecha.value, valor)
    }

    fun guardarCintura(valor: Double) = viewModelScope.launch {
        repo.guardarMedicion(Repositorio.TIPO_CINTURA, _fecha.value, valor)
    }

    fun tendenciaPeso(dias: Long = 28): Tendencia {
        val hasta = _fecha.value
        val desde = hasta.minusDays(dias)
        return Series.tendencia(pesos.value, desde, hasta)
    }

    fun tendenciaCintura(dias: Long = 28): Tendencia {
        val hasta = _fecha.value
        val desde = hasta.minusDays(dias)
        return Series.tendencia(cinturas.value, desde, hasta, umbralCambioPorSemana = 0.2)
    }

    // ------------------------------------------------------------------ salud y autorizacion

    fun guardarAutorizacion(estado: EstadoAutorizacion, profesional: String?, fechaIso: String?) =
        viewModelScope.launch { repo.guardarAutorizacion(estado, profesional, fechaIso) }

    fun anadirInstruccion(texto: String, tipo: String) = viewModelScope.launch {
        repo.anadirInstruccion(texto, tipo, LocalDate.now().toString())
    }

    fun refrescarSalud() = viewModelScope.launch {
        _estadoSalud.value = gestorSalud.estado()
        val concedidos = gestorSalud.permisosConcedidos()
        _tiposSalud.value = TipoDatoSalud.entries.map { t ->
            val e = repo.saludDao.leerEstado(t.name)
            EstadoTipo(
                tipo = t,
                permisoConcedido = t.permisoLectura in concedidos,
                hayDatos = e?.disponible == true,
                ultimaSincronizacionIso = e?.ultimaSincronizacionIso,
                error = e?.ultimoErrorMensaje,
            )
        }
    }

    fun sincronizarSalud() = viewModelScope.launch {
        val concedidos = gestorSalud.permisosConcedidos()
        var total = 0
        val fallos = mutableListOf<String>()
        TipoDatoSalud.entries.filter { it.permisoLectura in concedidos }.forEach { t ->
            gestorSalud.sincronizar(t)
                .onSuccess { total += it }
                .onFailure { fallos += "${t.etiqueta}: ${it.message}" }
        }
        gestorSalud.importarPesos()
        refrescarSalud()
        _mensaje.value = when {
            concedidos.isEmpty() -> "Todavia no has concedido ningun permiso de Health Connect."
            fallos.isEmpty() -> "Sincronizacion terminada: $total registros nuevos o cambiados."
            else -> "Sincronizado con incidencias. ${fallos.joinToString("; ")}"
        }
    }

    fun permisosDeSalud() = gestorSalud.permisosSolicitados(TipoDatoSalud.entries.toSet())

    fun contratoPermisosSalud() = gestorSalud.contratoDePermisos()

    fun desconectarSalud() = viewModelScope.launch {
        repo.olvidarDatosDeSalud()
        refrescarSalud()
        _mensaje.value = "Datos importados borrados. Revoca tambien el permiso desde Health Connect."
    }

    // ------------------------------------------------------------------ avisos

    fun refrescarAvisos() { _estadoAvisos.value = Avisos.estado(getApplication()) }

    fun probarNotificacion() {
        Avisos.enviarPrueba(getApplication())
        _mensaje.value = "Notificacion de prueba enviada. Comprueba tambien el Galaxy Watch."
    }

    fun reprogramarAvisos() = viewModelScope.launch {
        ProgramadorAlarmas.reprogramarTodo(getApplication())
        refrescarAvisos()
        _mensaje.value = "Recordatorios reprogramados."
    }

    fun guardarAjustes(nuevos: AjustesEntidad) = viewModelScope.launch {
        repo.guardarAjustes(nuevos)
        _ajustes.value = nuevos
        ProgramadorAlarmas.reprogramarTodo(getApplication())
    }

    // ------------------------------------------------------------------ exportacion

    fun prepararRevision(desde: LocalDate, hasta: LocalDate) = viewModelScope.launch {
        _vistaPrevia.value = exportador.previsualizar(exportador.construirRevision(desde, hasta))
    }

    fun compartirRevision(desde: LocalDate, hasta: LocalDate) = viewModelScope.launch {
        val revision = exportador.construirRevision(desde, hasta)
        val archivos = exportador.escribirArchivos(getApplication(), revision)
        Exportador.compartir(getApplication(), archivos, "Compartir revision semanal")
    }

    fun compartirCalendario(desde: LocalDate, hasta: LocalDate) = viewModelScope.launch {
        val ics = exportador.escribirIcs(getApplication(), desde, hasta)
        Exportador.compartir(getApplication(), listOf(ics), "Compartir calendario (ICS)")
    }

    fun limpiarMensaje() { _mensaje.value = null }
    fun limpiarVistaPrevia() { _vistaPrevia.value = null }
    fun limpiarRespuestaSintoma() { _respuestaSintoma.value = null }
}
