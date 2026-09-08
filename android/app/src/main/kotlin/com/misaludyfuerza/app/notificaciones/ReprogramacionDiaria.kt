package com.misaludyfuerza.app.notificaciones

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Vuelve a llenar la ventana de alarmas una vez al dia.
 *
 * IMPORTANTE: WorkManager no garantiza una hora exacta y NO se usa para disparar
 * los recordatorios. Su unico trabajo es asegurarse de que siempre haya alarmas
 * programadas para los proximos dias.
 */
class ReprogramacionWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        Canales.crear(applicationContext)
        ProgramadorAlarmas.reprogramarTodo(applicationContext)
        Result.success()
    } catch (e: Exception) {
        Result.retry()
    }
}

object ReprogramacionDiaria {

    private const val NOMBRE = "reprogramar-recordatorios"

    fun programar(context: Context) {
        val trabajo = PeriodicWorkRequestBuilder<ReprogramacionWorker>(12, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(false).build())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            NOMBRE, ExistingPeriodicWorkPolicy.UPDATE, trabajo,
        )
    }
}
