package com.misaludyfuerza.app.datos

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Todos los enumerados se guardan como texto para que el esquema sea legible y
 * estable entre versiones. La conversion vive en el repositorio.
 */

@Entity(tableName = "perfil")
data class PerfilEntidad(
    @PrimaryKey val id: Int = 1,
    val nombre: String = "Arnaldo",
    val edad: Int = 33,
    val alturaM: Double = 1.67,
    val pesoInicialKg: Double = 91.0,
    val fechaPesoInicialIso: String? = null,
    val zonaHoraria: String = "America/New_York",
)

@Entity(tableName = "ajustes")
data class AjustesEntidad(
    @PrimaryKey val id: Int = 1,
    val fechaActivacionIso: String,
    val margenAvisoMin: Int = 10,
    val minutosPosponer: Int = 10,
    val anonimizarNinoAlExportar: Boolean = true,
    /** Traslado gimnasio-casa: estimacion hasta que el usuario la confirme. */
    val gimnasioACasaMin: Int = 15,
    val gimnasioACasaConfirmado: Boolean = false,
    /** Recogida de martes y jueves: "7 y algo", estimacion editable. */
    val horaRecogidaMinutosDesdeMedianoche: Int = 19 * 60 + 15,
    val horaRecogidaConfirmada: Boolean = false,
    val alarmasExactasDisponibles: Boolean = false,
    val avisosDeChatGptPausados: Boolean = false,
)

@Entity(tableName = "autorizacion")
data class AutorizacionEntidad(
    @PrimaryKey val id: Int = 1,
    /** PENDIENTE_DE_CONFIRMAR | AUTORIZADA_CON_LIMITES | NO_AUTORIZADA */
    val estado: String = "PENDIENTE_DE_CONFIRMAR",
    val profesional: String? = null,
    val fechaIso: String? = null,
)

@Entity(tableName = "instrucciones_profesional")
data class InstruccionEntidad(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val texto: String,
    val fechaIso: String,
    /** INSTRUCCION | EJERCICIO_PERMITIDO | EJERCICIO_PROHIBIDO */
    val tipo: String = "INSTRUCCION",
)

/** Lo que paso con un evento del plan. La fecha y hora originales no se pierden. */
@Entity(tableName = "registro_evento", indices = [Index("fechaIso")])
data class RegistroEventoEntidad(
    @PrimaryKey val uid: String,
    val fechaIso: String,
    val horaOriginalMin: Int,
    /** PENDIENTE | HECHO | POSPUESTO | OMITIDO */
    val estado: String = "PENDIENTE",
    val nuevaHoraMin: Int? = null,
    val nota: String? = null,
    val actualizadoIso: String? = null,
)

@Entity(tableName = "registro_comida", primaryKeys = ["fechaIso", "tipo"])
data class RegistroComidaEntidad(
    val fechaIso: String,
    /** DESAYUNO | MERIENDA_MANANA | ALMUERZO | MERIENDA_TARDE | CENA */
    val tipo: String,
    /** PLANEADA | COMI | CAMBIE | NO_COMI. Nunca pasa sola a COMI. */
    val estado: String = "PLANEADA",
    val nota: String? = null,
    val actualizadoIso: String? = null,
)

/** Porciones reales cuando el usuario marca "Cambie". */
@Entity(
    tableName = "porcion_real",
    indices = [Index("fechaIso", "tipo")],
)
data class PorcionRealEntidad(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fechaIso: String,
    val tipo: String,
    val alimentoId: String,
    val gramos: Double,
)

@Entity(tableName = "medicion", indices = [Index("tipo", "fechaIso", unique = true)])
data class MedicionEntidad(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** PESO | CINTURA */
    val tipo: String,
    val fechaIso: String,
    val valor: Double,
    /** Manual | Health Connect | Importado */
    val origen: String = "Manual",
)

@Entity(tableName = "sesion", indices = [Index("fechaIso")])
data class SesionEntidad(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fechaIso: String,
    val rutinaId: String,
    /** Solo el usuario la marca: asistir al gimnasio no la completa. */
    val completada: Boolean = false,
    val duracionMin: Int? = null,
    val dolor0a10: Int = 0,
    val hinchazon: Boolean = false,
    val inestabilidad: Boolean = false,
    val notaSintoma: String? = null,
    val origen: String = "MANUAL",
)

@Entity(
    tableName = "serie",
    foreignKeys = [
        ForeignKey(
            entity = SesionEntidad::class,
            parentColumns = ["id"],
            childColumns = ["sesionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sesionId")],
)
data class SerieEntidad(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sesionId: Long,
    val ejercicioId: String,
    val numero: Int,
    val pesoKg: Double?,
    val repeticiones: Int,
    val repeticionesEnReserva: Int?,
    val tecnicaBuena: Boolean = true,
    val nota: String? = null,
)

/** Excepciones de custodia: true fuerza el fin de semana, false lo cancela. */
@Entity(tableName = "excepcion_custodia")
data class ExcepcionCustodiaEntidad(
    @PrimaryKey val sabadoIso: String,
    val tieneCustodia: Boolean,
    val nota: String? = null,
)

@Entity(tableName = "dia_escolar")
data class DiaEscolarEntidad(
    @PrimaryKey val fechaIso: String,
    /** CIERRE | FESTIVO */
    val tipo: String,
    val nota: String? = null,
)

@Entity(tableName = "version_plan")
data class VersionPlanEntidad(
    @PrimaryKey val numero: Int,
    val fechaIso: String,
    val descripcion: String,
    val propuestaId: String? = null,
)

@Entity(tableName = "cambio_plan", indices = [Index("versionNumero")])
data class CambioPlanEntidad(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val versionNumero: Int,
    val ruta: String,
    val valorAnterior: String?,
    val valorNuevo: String,
    val justificacion: String?,
)

/**
 * Estado de sincronizacion por tipo de dato de Health Connect. Guarda el token de
 * cambios para procesar altas, modificaciones y borrados sin releer todo.
 */
@Entity(tableName = "salud_sincronizacion")
data class SaludSyncEntidad(
    @PrimaryKey val tipoDato: String,
    val disponible: Boolean = false,
    val permisoConcedido: Boolean = false,
    val tokenCambios: String? = null,
    val ultimaSincronizacionIso: String? = null,
    val ultimoErrorMensaje: String? = null,
    val ultimoErrorIso: String? = null,
)

/**
 * Registro leido de Health Connect. [claveDeduplicacion] combina origen y id del
 * registro para no duplicar la misma sesion vista dos veces.
 */
@Entity(tableName = "salud_registro", indices = [Index("tipoDato", "inicioIso")])
data class SaludRegistroEntidad(
    @PrimaryKey val claveDeduplicacion: String,
    val tipoDato: String,
    val inicioIso: String,
    val finIso: String?,
    val valor: Double?,
    val unidad: String?,
    /** Paquete de la app que origino el dato (por ejemplo Samsung Health). */
    val origenApp: String?,
    val dispositivo: String?,
    val ultimaModificacionIso: String?,
)
