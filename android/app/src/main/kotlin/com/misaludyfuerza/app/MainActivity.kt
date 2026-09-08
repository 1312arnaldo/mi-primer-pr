package com.misaludyfuerza.app

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import com.misaludyfuerza.app.ui.AplicacionUi

class MainActivity : ComponentActivity() {

    private val pedirNotificaciones = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* El estado real se relee en Ajustes; no se asume nada. */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pedirNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val fechaInicial = intent?.getStringExtra(EXTRA_ABRIR_FECHA)

        setContent {
            AplicacionUi(
                fechaInicialIso = fechaInicial,
                abrirAjustesAlarmas = ::abrirAjustesDeAlarmasExactas,
                abrirAjustesNotificaciones = ::abrirAjustesDeNotificaciones,
            )
        }
    }

    /** Android 13+: el permiso de alarmas exactas se concede desde Ajustes del sistema. */
    private fun abrirAjustesDeAlarmasExactas() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching {
                startActivity(
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                        .setData(Uri.parse("package:$packageName")),
                )
            }
        }
    }

    private fun abrirAjustesDeNotificaciones() {
        runCatching {
            startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, packageName),
            )
        }
    }

    companion object {
        const val EXTRA_ABRIR_FECHA = "abrir_fecha"
    }
}
