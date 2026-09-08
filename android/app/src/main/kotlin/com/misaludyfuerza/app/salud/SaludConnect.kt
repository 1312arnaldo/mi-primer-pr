package com.misaludyfuerza.app.salud

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.changes.DeletionChange
import androidx.health.connect.client.changes.UpsertionChange
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.ChangesTokenRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.misaludyfuerza.app.datos.Repositorio
import com.misaludyfuerza.app.datos.SaludRegistroEntidad
import com.misaludyfuerza.app.datos.SaludSyncEntidad
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.reflect.KClass

/**
 * Tipos de dato que la app PUEDE pedir. Cada uno se solicita por separado: el
 * usuario puede conceder solo lo que quiera y revocar cuando quiera.
 *
 * No se promete ninguna metrica propietaria de Samsung (puntuacion de energia,
 * detalle de fases de sueno, HRV): esta app solo lee lo que Health Connect
 * expone y la fuente comparte.
 */
enum class TipoDatoSalud(
    val etiqueta: String,
    val recordClass: KClass<out Record>,
    val unidad: String?,
) {
    PASOS("Pasos", StepsRecord::class, "pasos"),
    SESIONES("Sesiones de ejercicio", ExerciseSessionRecord::class, "minutos"),
    SUENO("Sueno", SleepSessionRecord::class, "minutos"),
    FRECUENCIA_CARDIACA("Frecuencia cardiaca", HeartRateRecord::class, "lpm"),
    PESO("Peso", WeightRecord::class, "kg"),
    ENERGIA_ACTIVA("Energia activa", ActiveCaloriesBurnedRecord::class, "kcal"),
    ENERGIA_TOTAL("Energia total", TotalCaloriesBurnedRecord::class, "kcal"),
    ;

    val permisoLectura: String get() = HealthPermission.getReadPermission(recordClass)
}

sealed interface EstadoHealthConnect {
    data object NoDisponible : EstadoHealthConnect
    data object RequiereActualizacion : EstadoHealthConnect
    data class Disponible(val permisosConcedidos: Set<String>) : EstadoHealthConnect
}

data class EstadoTipo(
    val tipo: TipoDatoSalud,
    val permisoConcedido: Boolean,
    val hayDatos: Boolean,
    val ultimaSincronizacionIso: String?,
    val error: String?,
) {
    /**
     * Sin datos NO significa cero: puede que la fuente no los comparta, que no haya
     * permiso o que simplemente no se registrara nada.
     */
    val descripcion: String
        get() = when {
            !permisoConcedido -> "Sin permiso concedido."
            !hayDatos -> "Con permiso, pero todavia sin datos. Sin dato no es lo mismo que cero."
            else -> "Sincronizado."
        }
}

/**
 * Lector de Health Connect. Ruta prevista:
 * Galaxy Watch -> Samsung Health -> Health Connect -> esta app.
 *
 * La app NO habla con Samsung Health directamente ni pide credenciales de Samsung:
 * usa el permiso oficial de Health Connect y lee lo que ya este sincronizado en el
 * telefono.
 */
class GestorSalud(private val context: Context, private val repo: Repositorio) {

    private val cliente: HealthConnectClient? by lazy {
        if (disponibilidad() == HealthConnectClient.SDK_AVAILABLE) {
            runCatching { HealthConnectClient.getOrCreate(context) }.getOrNull()
        } else {
            null
        }
    }

    fun disponibilidad(): Int = HealthConnectClient.getSdkStatus(context)

    fun contratoDePermisos() = PermissionController.createRequestPermissionResultContract()

    /** Permisos que se piden: uno por tipo, mas historial y lectura en segundo plano. */
    fun permisosSolicitados(tipos: Set<TipoDatoSalud>): Set<String> =
        tipos.map { it.permisoLectura }.toSet() +
            setOf(
                HealthPermission.PERMISSION_READ_HEALTH_DATA_HISTORY,
                HealthPermission.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND,
            )

    suspend fun estado(): EstadoHealthConnect = when (disponibilidad()) {
        HealthConnectClient.SDK_UNAVAILABLE -> EstadoHealthConnect.NoDisponible
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED ->
            EstadoHealthConnect.RequiereActualizacion
        else -> {
            val c = cliente ?: return EstadoHealthConnect.NoDisponible
            EstadoHealthConnect.Disponible(
                runCatching { c.permissionController.getGrantedPermissions() }.getOrDefault(emptySet()),
            )
        }
    }

    suspend fun permisosConcedidos(): Set<String> =
        (estado() as? EstadoHealthConnect.Disponible)?.permisosConcedidos ?: emptySet()

    /**
     * Sincroniza un tipo de dato.
     *
     * Primera vez: lee la ventana indicada y guarda un token de cambios.
     * Siguientes: usa el token para procesar altas, modificaciones y borrados sin
     * releer todo, y sin duplicar lo ya guardado.
     */
    suspend fun sincronizar(tipo: TipoDatoSalud, diasHaciaAtras: Long = 30): Result<Int> {
        val c = cliente ?: return Result.failure(
            IllegalStateException("Health Connect no esta disponible en este telefono."),
        )
        val concedidos = permisosConcedidos()
        if (tipo.permisoLectura !in concedidos) {
            guardarEstado(tipo, permisoConcedido = false, error = "Permiso no concedido")
            return Result.failure(SecurityException("Sin permiso para ${tipo.etiqueta}."))
        }

        return runCatching {
            val previo = repo.saludDao.leerEstado(tipo.name)
            var cambiados = 0

            if (previo?.tokenCambios == null) {
                val fin = Instant.now()
                val inicio = fin.minus(Duration.ofDays(diasHaciaAtras))
                val registros = leer(c, tipo, inicio, fin)
                repo.saludDao.insertarSiEsNuevo(registros)
                cambiados = registros.size
                val token = c.getChangesToken(ChangesTokenRequest(setOf(tipo.recordClass)))
                guardarEstado(tipo, true, token = token, hayDatos = registros.isNotEmpty())
            } else {
                var token: String? = previo.tokenCambios
                while (token != null) {
                    val respuesta = c.getChanges(token)
                    if (respuesta.changesTokenExpired) {
                        // Token caducado: se reinicia la ventana en la proxima sincronizacion.
                        guardarEstado(tipo, true, token = null, error = "Token caducado; se releera la ventana.")
                        return@runCatching cambiados
                    }
                    val altas = respuesta.changes.filterIsInstance<UpsertionChange>()
                        .map { aEntidad(tipo, it.record) }
                    val bajas = respuesta.changes.filterIsInstance<DeletionChange>()
                        .map { clave(tipo, it.recordId) }
                    if (altas.isNotEmpty()) repo.saludDao.actualizar(altas)
                    if (bajas.isNotEmpty()) repo.saludDao.borrar(bajas)
                    cambiados += altas.size + bajas.size
                    token = if (respuesta.hasMore) respuesta.nextChangesToken else null
                    if (!respuesta.hasMore) {
                        guardarEstado(tipo, true, token = respuesta.nextChangesToken, hayDatos = true)
                    }
                }
            }
            cambiados
        }.onFailure { e ->
            guardarEstado(tipo, true, error = e.message ?: e::class.simpleName)
        }
    }

    private suspend fun leer(
        c: HealthConnectClient,
        tipo: TipoDatoSalud,
        inicio: Instant,
        fin: Instant,
    ): List<SaludRegistroEntidad> {
        val filtro = TimeRangeFilter.between(inicio, fin)
        return when (tipo) {
            TipoDatoSalud.PASOS ->
                c.readRecords(ReadRecordsRequest(StepsRecord::class, filtro)).records.map { aEntidad(tipo, it) }
            TipoDatoSalud.SESIONES ->
                c.readRecords(ReadRecordsRequest(ExerciseSessionRecord::class, filtro)).records.map { aEntidad(tipo, it) }
            TipoDatoSalud.SUENO ->
                c.readRecords(ReadRecordsRequest(SleepSessionRecord::class, filtro)).records.map { aEntidad(tipo, it) }
            TipoDatoSalud.FRECUENCIA_CARDIACA ->
                c.readRecords(ReadRecordsRequest(HeartRateRecord::class, filtro)).records.map { aEntidad(tipo, it) }
            TipoDatoSalud.PESO ->
                c.readRecords(ReadRecordsRequest(WeightRecord::class, filtro)).records.map { aEntidad(tipo, it) }
            TipoDatoSalud.ENERGIA_ACTIVA ->
                c.readRecords(ReadRecordsRequest(ActiveCaloriesBurnedRecord::class, filtro)).records.map { aEntidad(tipo, it) }
            TipoDatoSalud.ENERGIA_TOTAL ->
                c.readRecords(ReadRecordsRequest(TotalCaloriesBurnedRecord::class, filtro)).records.map { aEntidad(tipo, it) }
        }
    }

    /**
     * Clave de deduplicacion: tipo + identificador del registro en Health Connect.
     * Ese identificador es unico, asi que la misma sesion vista dos veces no se
     * cuenta dos veces, y un borrado (que solo trae el id) encuentra su fila.
     */
    private fun clave(tipo: TipoDatoSalud, id: String): String = "${tipo.name}|$id"

    private fun aEntidad(tipo: TipoDatoSalud, record: Record): SaludRegistroEntidad {
        val meta = record.metadata
        val paquete = meta.dataOrigin.packageName
        val (inicio, fin, valor) = when (record) {
            is StepsRecord -> Triple(record.startTime, record.endTime, record.count.toDouble())
            is ExerciseSessionRecord -> Triple(
                record.startTime, record.endTime,
                Duration.between(record.startTime, record.endTime).toMinutes().toDouble(),
            )
            is SleepSessionRecord -> Triple(
                record.startTime, record.endTime,
                Duration.between(record.startTime, record.endTime).toMinutes().toDouble(),
            )
            is HeartRateRecord -> Triple(
                record.startTime, record.endTime,
                record.samples.map { it.beatsPerMinute }.average().takeIf { !it.isNaN() },
            )
            is WeightRecord -> Triple(record.time, null, record.weight.inKilograms)
            is ActiveCaloriesBurnedRecord -> Triple(record.startTime, record.endTime, record.energy.inKilocalories)
            is TotalCaloriesBurnedRecord -> Triple(record.startTime, record.endTime, record.energy.inKilocalories)
            else -> Triple(Instant.now(), null, null)
        }
        return SaludRegistroEntidad(
            claveDeduplicacion = clave(tipo, meta.id),
            tipoDato = tipo.name,
            inicioIso = inicio.toString(),
            finIso = fin?.toString(),
            valor = valor,
            unidad = tipo.unidad,
            origenApp = paquete,
            dispositivo = meta.device?.model,
            ultimaModificacionIso = meta.lastModifiedTime.toString(),
        )
    }

    private suspend fun guardarEstado(
        tipo: TipoDatoSalud,
        permisoConcedido: Boolean,
        token: String? = null,
        hayDatos: Boolean? = null,
        error: String? = null,
    ) {
        val previo = repo.saludDao.leerEstado(tipo.name)
        val ahora = LocalDateTime.now(ZoneId.systemDefault()).toString()
        repo.saludDao.guardarEstado(
            SaludSyncEntidad(
                tipoDato = tipo.name,
                disponible = hayDatos ?: previo?.disponible ?: false,
                permisoConcedido = permisoConcedido,
                tokenCambios = token ?: previo?.tokenCambios,
                ultimaSincronizacionIso = if (error == null) ahora else previo?.ultimaSincronizacionIso,
                ultimoErrorMensaje = error,
                ultimoErrorIso = if (error != null) ahora else null,
            ),
        )
    }

    /**
     * Copia a mediciones los pesos leidos del reloj o la bascula conectada, sin
     * pisar nunca un registro manual del mismo dia.
     */
    suspend fun importarPesos() {
        val registros = repo.saludDao.leerRango(
            TipoDatoSalud.PESO.name,
            Instant.now().minus(Duration.ofDays(90)).toString(),
            Instant.now().toString(),
        )
        val zona = ZoneId.of(repo.perfil().zonaHoraria)
        registros.forEach { r ->
            val valor = r.valor ?: return@forEach
            val fecha = Instant.parse(r.inicioIso).atZone(zona).toLocalDate()
            val existentes = repo.medicionesRango(Repositorio.TIPO_PESO, fecha, fecha)
            if (existentes.none { it.origen == "Manual" }) {
                repo.guardarMedicion(Repositorio.TIPO_PESO, fecha, valor, origen = "Health Connect")
            }
        }
    }
}

/**
 * Integracion con el gimnasio.
 *
 * Crunch Fitness no publica una API abierta para que aplicaciones de terceros lean
 * la cuenta de un socio. Mientras no exista un acceso aprobado, la app NO muestra
 * un boton que finja conectar: registra los entrenamientos a mano, permite importar
 * archivos legitimos y ofrece abrir la app de Crunch.
 */
object IntegracionGimnasio {
    const val NOMBRE = "Crunch Fitness"
    const val ESTADO = "Sin integracion directa disponible"
    const val EXPLICACION =
        "No hay una API publica ni un mecanismo autorizado de Crunch Fitness para que esta " +
            "app lea tu cuenta. No se extraen tokens de su aplicacion ni se hace scraping de " +
            "zonas privadas. Registra las sesiones a mano o importa un archivo que tu mismo " +
            "hayas exportado. Si Crunch habilita un acceso oficial, se anadira aqui."
    const val PAQUETE_APP = "com.crunch.crunchfitness"
}
