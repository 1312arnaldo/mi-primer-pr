package com.misaludyfuerza.app.datos

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        PerfilEntidad::class,
        AjustesEntidad::class,
        AutorizacionEntidad::class,
        InstruccionEntidad::class,
        RegistroEventoEntidad::class,
        RegistroComidaEntidad::class,
        PorcionRealEntidad::class,
        MedicionEntidad::class,
        SesionEntidad::class,
        SerieEntidad::class,
        ExcepcionCustodiaEntidad::class,
        DiaEscolarEntidad::class,
        VersionPlanEntidad::class,
        CambioPlanEntidad::class,
        SaludSyncEntidad::class,
        SaludRegistroEntidad::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class BaseDatos : RoomDatabase() {
    abstract fun perfilDao(): PerfilDao
    abstract fun ajustesDao(): AjustesDao
    abstract fun autorizacionDao(): AutorizacionDao
    abstract fun registroEventoDao(): RegistroEventoDao
    abstract fun comidaDao(): ComidaDao
    abstract fun medicionDao(): MedicionDao
    abstract fun entrenamientoDao(): EntrenamientoDao
    abstract fun calendarioDao(): CalendarioDao
    abstract fun planDao(): PlanDao
    abstract fun saludDao(): SaludDao

    companion object {
        private const val NOMBRE = "mi-salud-y-fuerza.db"

        /**
         * Anade la preferencia de tema sin borrar nada de lo que el usuario ya
         * tuviera guardado.
         */
        private val MIGRACION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE ajustes ADD COLUMN tema TEXT NOT NULL DEFAULT 'AUTOMATICO'",
                )
            }
        }

        @Volatile
        private var instancia: BaseDatos? = null

        fun obtener(context: Context): BaseDatos = instancia ?: synchronized(this) {
            instancia ?: Room.databaseBuilder(
                context.applicationContext, BaseDatos::class.java, NOMBRE,
            )
                // Almacenamiento local persistente: la app funciona sin internet.
                .addMigrations(MIGRACION_1_2)
                .build()
                .also { instancia = it }
        }

        /** Borra la base de datos completa. Lo usa "Borrar mis datos" en Ajustes. */
        fun borrar(context: Context) = synchronized(this) {
            instancia?.close()
            instancia = null
            context.applicationContext.deleteDatabase(NOMBRE)
        }
    }
}
