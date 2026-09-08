package com.misaludyfuerza.app.datos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PerfilDao {
    @Query("SELECT * FROM perfil WHERE id = 1")
    fun observar(): Flow<PerfilEntidad?>

    @Query("SELECT * FROM perfil WHERE id = 1")
    suspend fun leer(): PerfilEntidad?

    @Upsert
    suspend fun guardar(p: PerfilEntidad)
}

@Dao
interface AjustesDao {
    @Query("SELECT * FROM ajustes WHERE id = 1")
    fun observar(): Flow<AjustesEntidad?>

    @Query("SELECT * FROM ajustes WHERE id = 1")
    suspend fun leer(): AjustesEntidad?

    @Upsert
    suspend fun guardar(a: AjustesEntidad)
}

@Dao
interface AutorizacionDao {
    @Query("SELECT * FROM autorizacion WHERE id = 1")
    fun observar(): Flow<AutorizacionEntidad?>

    @Query("SELECT * FROM autorizacion WHERE id = 1")
    suspend fun leer(): AutorizacionEntidad?

    @Upsert
    suspend fun guardar(a: AutorizacionEntidad)

    @Query("SELECT * FROM instrucciones_profesional ORDER BY fechaIso DESC")
    fun observarInstrucciones(): Flow<List<InstruccionEntidad>>

    @Query("SELECT * FROM instrucciones_profesional")
    suspend fun leerInstrucciones(): List<InstruccionEntidad>

    @Insert
    suspend fun anadirInstruccion(i: InstruccionEntidad)

    @Query("DELETE FROM instrucciones_profesional WHERE id = :id")
    suspend fun borrarInstruccion(id: Long)
}

@Dao
interface RegistroEventoDao {
    @Query("SELECT * FROM registro_evento WHERE fechaIso = :fechaIso")
    fun observarDia(fechaIso: String): Flow<List<RegistroEventoEntidad>>

    @Query("SELECT * FROM registro_evento WHERE fechaIso BETWEEN :desdeIso AND :hastaIso")
    suspend fun leerRango(desdeIso: String, hastaIso: String): List<RegistroEventoEntidad>

    @Query("SELECT * FROM registro_evento WHERE uid = :uid")
    suspend fun leer(uid: String): RegistroEventoEntidad?

    /** Upsert por uid: reprogramar o resincronizar no crea filas nuevas. */
    @Upsert
    suspend fun guardar(r: RegistroEventoEntidad)
}

@Dao
interface ComidaDao {
    @Query("SELECT * FROM registro_comida WHERE fechaIso = :fechaIso")
    fun observarDia(fechaIso: String): Flow<List<RegistroComidaEntidad>>

    @Query("SELECT * FROM registro_comida WHERE fechaIso BETWEEN :desdeIso AND :hastaIso")
    suspend fun leerRango(desdeIso: String, hastaIso: String): List<RegistroComidaEntidad>

    @Upsert
    suspend fun guardar(r: RegistroComidaEntidad)

    @Query("DELETE FROM porcion_real WHERE fechaIso = :fechaIso AND tipo = :tipo")
    suspend fun borrarPorciones(fechaIso: String, tipo: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun anadirPorciones(porciones: List<PorcionRealEntidad>)

    @Query("SELECT * FROM porcion_real WHERE fechaIso = :fechaIso AND tipo = :tipo")
    suspend fun leerPorciones(fechaIso: String, tipo: String): List<PorcionRealEntidad>

    @Transaction
    suspend fun registrarCambio(
        registro: RegistroComidaEntidad,
        porciones: List<PorcionRealEntidad>,
    ) {
        guardar(registro)
        borrarPorciones(registro.fechaIso, registro.tipo)
        if (porciones.isNotEmpty()) anadirPorciones(porciones)
    }
}

@Dao
interface MedicionDao {
    @Query("SELECT * FROM medicion WHERE tipo = :tipo ORDER BY fechaIso")
    fun observar(tipo: String): Flow<List<MedicionEntidad>>

    @Query("SELECT * FROM medicion WHERE tipo = :tipo AND fechaIso BETWEEN :desdeIso AND :hastaIso ORDER BY fechaIso")
    suspend fun leerRango(tipo: String, desdeIso: String, hastaIso: String): List<MedicionEntidad>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(m: MedicionEntidad)

    @Query("DELETE FROM medicion WHERE id = :id")
    suspend fun borrar(id: Long)
}

data class SesionConSeries(
    val sesion: SesionEntidad,
    val series: List<SerieEntidad>,
)

@Dao
interface EntrenamientoDao {
    @Query("SELECT * FROM sesion ORDER BY fechaIso")
    fun observarSesiones(): Flow<List<SesionEntidad>>

    @Query("SELECT * FROM sesion WHERE fechaIso BETWEEN :desdeIso AND :hastaIso ORDER BY fechaIso")
    suspend fun leerSesiones(desdeIso: String, hastaIso: String): List<SesionEntidad>

    @Query("SELECT * FROM sesion ORDER BY fechaIso")
    suspend fun leerTodasLasSesiones(): List<SesionEntidad>

    @Query("SELECT * FROM sesion WHERE fechaIso = :fechaIso AND rutinaId = :rutinaId LIMIT 1")
    suspend fun leerSesion(fechaIso: String, rutinaId: String): SesionEntidad?

    @Insert
    suspend fun insertarSesion(s: SesionEntidad): Long

    @Upsert
    suspend fun guardarSesion(s: SesionEntidad)

    @Query("SELECT * FROM serie WHERE sesionId = :sesionId ORDER BY ejercicioId, numero")
    suspend fun leerSeries(sesionId: Long): List<SerieEntidad>

    @Query("SELECT * FROM serie WHERE sesionId IN (:sesionIds)")
    suspend fun leerSeriesDe(sesionIds: List<Long>): List<SerieEntidad>

    /** Historial por maquina/ejercicio para ver la progresion real. */
    @Query(
        """
        SELECT serie.* FROM serie
        INNER JOIN sesion ON sesion.id = serie.sesionId
        WHERE serie.ejercicioId = :ejercicioId AND sesion.completada = 1
        ORDER BY sesion.fechaIso DESC, serie.numero
        """,
    )
    fun observarHistorialEjercicio(ejercicioId: String): Flow<List<SerieEntidad>>

    @Upsert
    suspend fun guardarSerie(s: SerieEntidad)

    @Query("DELETE FROM serie WHERE id = :id")
    suspend fun borrarSerie(id: Long)
}

@Dao
interface CalendarioDao {
    @Query("SELECT * FROM excepcion_custodia")
    fun observarExcepciones(): Flow<List<ExcepcionCustodiaEntidad>>

    @Query("SELECT * FROM excepcion_custodia")
    suspend fun leerExcepciones(): List<ExcepcionCustodiaEntidad>

    @Upsert
    suspend fun guardarExcepcion(e: ExcepcionCustodiaEntidad)

    @Query("DELETE FROM excepcion_custodia WHERE sabadoIso = :sabadoIso")
    suspend fun borrarExcepcion(sabadoIso: String)

    @Query("SELECT * FROM dia_escolar")
    fun observarDiasEscolares(): Flow<List<DiaEscolarEntidad>>

    @Query("SELECT * FROM dia_escolar")
    suspend fun leerDiasEscolares(): List<DiaEscolarEntidad>

    @Upsert
    suspend fun guardarDiaEscolar(d: DiaEscolarEntidad)

    @Query("DELETE FROM dia_escolar WHERE fechaIso = :fechaIso")
    suspend fun borrarDiaEscolar(fechaIso: String)
}

@Dao
interface PlanDao {
    @Query("SELECT * FROM version_plan ORDER BY numero")
    fun observarVersiones(): Flow<List<VersionPlanEntidad>>

    @Query("SELECT * FROM version_plan ORDER BY numero DESC LIMIT 1")
    suspend fun versionActual(): VersionPlanEntidad?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarVersion(v: VersionPlanEntidad)

    @Insert
    suspend fun guardarCambios(cambios: List<CambioPlanEntidad>)

    @Query("SELECT * FROM cambio_plan WHERE versionNumero = :numero")
    suspend fun leerCambios(numero: Int): List<CambioPlanEntidad>
}

@Dao
interface SaludDao {
    @Query("SELECT * FROM salud_sincronizacion")
    fun observarEstado(): Flow<List<SaludSyncEntidad>>

    @Query("SELECT * FROM salud_sincronizacion WHERE tipoDato = :tipoDato")
    suspend fun leerEstado(tipoDato: String): SaludSyncEntidad?

    @Upsert
    suspend fun guardarEstado(e: SaludSyncEntidad)

    /** IGNORE, no REPLACE: la clave de deduplicacion evita contar dos veces la misma sesion. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertarSiEsNuevo(registros: List<SaludRegistroEntidad>): List<Long>

    @Upsert
    suspend fun actualizar(registros: List<SaludRegistroEntidad>)

    @Query("DELETE FROM salud_registro WHERE claveDeduplicacion IN (:claves)")
    suspend fun borrar(claves: List<String>)

    @Query("SELECT * FROM salud_registro WHERE tipoDato = :tipoDato AND inicioIso BETWEEN :desdeIso AND :hastaIso ORDER BY inicioIso")
    suspend fun leerRango(tipoDato: String, desdeIso: String, hastaIso: String): List<SaludRegistroEntidad>

    @Query("DELETE FROM salud_registro")
    suspend fun borrarTodo()

    @Query("DELETE FROM salud_sincronizacion")
    suspend fun borrarEstados()
}
