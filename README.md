# Mi Salud y Fuerza

Aplicación personal de alimentación, entrenamiento, calendario y seguimiento de salud
para Arnaldo. Android (Kotlin + Jetpack Compose), almacenamiento local, interfaz
completamente en español y **todas las horas visibles en formato de 12 horas con
a. m. / p. m.**

Zona horaria del plan: `America/New_York`, con horario de verano resuelto por la
base de datos de zonas horarias del sistema.

## Estado actual, sin adornos

| Parte | Estado |
|---|---|
| Lógica del plan (`core/`) | **Funciona y está probado.** 90 pruebas ejecutadas, 0 fallos. |
| App Android (`android/`) | **Implementado, falta compilar y probar en el teléfono.** |
| Notificaciones y entrega al Galaxy Watch | **Falta probar en dispositivo real.** |
| Health Connect | **Implementado; requiere permisos y un teléfono con Health Connect.** |
| Crunch Fitness | **Requiere aprobación externa que hoy no existe.** Sin integración directa. |
| APK de prueba | **No generado.** Ver «Bloqueo concreto» más abajo. |

### Bloqueo concreto para el APK

El entorno donde se escribió este código no puede descargar los artefactos de
Android: `dl.google.com` (y `maven.google.com`, que redirige allí) están
bloqueados por la política de red de salida, igual que el SDK de Android.

```
$ curl -I https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
curl: (56) CONNECT tunnel failed, response 403
```

Sin el Android SDK ni los artefactos de AndroidX no se puede compilar ni generar
un APK aquí. Por eso el proyecto está partido en dos:

- **`core/`** — Kotlin/JVM puro, sin nada de Android. Contiene toda la lógica del
  plan y **sí se compila y se prueba** en cualquier máquina con JDK 17+.
- **`android/`** — la app. Se compila en cualquier equipo con Android Studio.

Los pasos exactos para compilarla están en [`docs/INSTALACION.md`](docs/INSTALACION.md).

## Cómo ejecutar las pruebas ahora mismo

```bash
./gradlew :core:test
```

Requiere solo un JDK 17 o superior. Resultado esperado: 90 pruebas, 0 fallos.

## Cómo compilar la app

```bash
cd android
./gradlew :app:assembleDebug     # genera app/build/outputs/apk/debug/app-debug.apk
```

Requiere Android Studio o el SDK de Android con la plataforma 35.

## Documentación

- [`docs/ARQUITECTURA.md`](docs/ARQUITECTURA.md) — por qué está montado así y qué decisión resuelve cada pieza.
- [`docs/ESQUEMA_DATOS.md`](docs/ESQUEMA_DATOS.md) — modelo de datos y esquema de exportación.
- [`docs/INTEGRACIONES.md`](docs/INTEGRACIONES.md) — Health Connect, Samsung, Galaxy Watch y Crunch: lo que se puede y lo que no.
- [`docs/PRUEBAS.md`](docs/PRUEBAS.md) — qué se ha probado de verdad y qué falta.
- [`docs/INSTALACION.md`](docs/INSTALACION.md) — compilar, instalar y verificar en el teléfono.
- [`docs/PARA_CHATGPT.md`](docs/PARA_CHATGPT.md) — cómo continuar desde ChatGPT/Codex sin reinventar trabajo.
- [`docs/ESTADO_Y_PROXIMOS_PASOS.md`](docs/ESTADO_Y_PROXIMOS_PASOS.md) — dónde está cada etapa.

## Lo que la app nunca hace

Estas reglas están escritas en código y cubiertas por pruebas, no solo en la
documentación:

- **No añade carga a la rodilla.** Sentadillas, prensa, peso muerto, zancadas,
  saltos, carrera, extensiones, femoral, gemelos y similares están bloqueados de
  forma dura. Marcar «autorizada» en la app no los desbloquea.
- **No se autoriza sola.** El estado de partida es «Pendiente de confirmar» y las
  rutinas se presentan como propuestas para validar. No se presenta como
  rehabilitación ni como indicación médica.
- **No da por hecho lo que no confirmaste.** Una comida planeada o notificada
  nunca cuenta como comida. Entrar al gimnasio no completa una sesión.
- **No propone pollo, huevo hervido ni avena**, tampoco como sustitución
  automática.
- **No inventa datos.** No hay lecturas del reloj precargadas, no se convierte un
  día en tendencia, la ausencia de dato no es un cero, no se suman calorías
  activas y totales, y la ingesta no se ajusta sola por lo que estime el reloj.
- **No finge integraciones.** Si algo no está conectado, lo dice.

## Privacidad

Uso individual, almacenamiento local, funcionamiento sin internet. Sin analítica,
sin telemetría, sin copias automáticas en la nube (`allowBackup=false` y reglas de
extracción que excluyen la base de datos). No se piden contraseñas de Samsung ni de
Crunch: solo se usan los permisos oficiales de Health Connect. Puedes exportar,
borrar tus datos y desconectar fuentes desde la propia app.

**No subas datos de salud reales a este repositorio.** El `.gitignore` ya excluye
`datos-personales/` y los archivos `*.export.json` / `*.export.csv`.
