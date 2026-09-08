# Arquitectura y decisiones

## El problema que resuelve la estructura

El requisito era una app instalable en un Samsung, con notificaciones nativas y
acceso a Health Connect. Eso descarta una web o PWA sola: **una PWA no tiene acceso
nativo a Health Connect ni a alarmas exactas de Android.** Por eso el objetivo es
Android nativo con Kotlin y Jetpack Compose.

El entorno donde se escribió el código no puede descargar el SDK de Android
(`dl.google.com` bloqueado por política de red). Si toda la lógica viviera dentro
del módulo Android, no se habría podido ejecutar ni una sola prueba.

**Decisión 1: separar el dominio en un módulo Kotlin/JVM puro.**

```
mi-primer-pr/
├── settings.gradle.kts          build raíz: solo :core
├── core/                        Kotlin/JVM puro. Sin dependencias de Android.
│   ├── tiempo/                  formato de 12 horas y zona horaria
│   ├── perfil/                  perfil, preferencias, traslados, ajustes
│   ├── salud/                   autorización, filtro de rodilla, protocolo de síntomas
│   ├── agenda/                  custodia, calendario escolar, reglas de gimnasio
│   ├── alimentacion/            catálogo, menú, sustituciones, adherencia, compra
│   ├── entrenamiento/           rutinas y motor de progresión
│   ├── estadisticas/            series, cobertura, tendencias
│   ├── plan/                    versionado y propuestas externas
│   └── exportacion/             ICS y revisión semanal (Markdown, JSON, CSV)
└── android/                     build compuesto e independiente
    └── app/                     Compose, Room, alarmas, Health Connect
```

`android/settings.gradle.kts` usa `includeBuild("..")` con sustitución de
dependencias: el módulo `:app` declara `implementation("com.misaludyfuerza:core")`
y Gradle lo resuelve al proyecto local sin publicar nada. Beneficios:

1. La lógica del plan se compila y se prueba sin Android SDK.
2. La app no puede saltarse las reglas: están en el dominio, no en la interfaz.
3. Si algún día hace falta un panel web o un servicio, reutiliza `:core` tal cual.

## Decisión 2: las reglas de seguridad son código, no documentación

Las restricciones del usuario no son avisos de texto en una pantalla. Están
implementadas como funciones con pruebas:

| Regla | Dónde vive | Prueba |
|---|---|---|
| Nada de carga de rodilla | `salud/FiltroRodilla` | `SaludTest` |
| Autorización pendiente por defecto | `salud/AutorizacionProfesional` | `SaludTest` |
| Síntoma congela la progresión | `salud/ProtocoloSintomas`, `entrenamiento/MotorProgresion` | `SaludTest`, `ProgresionTest` |
| Prohibidos fuera, también en sustituciones | `alimentacion/Catalogo` | `AlimentacionTest` |
| Lo planeado no cuenta como comido | `alimentacion/RegistroComida` | `AlimentacionTest` |
| Sin gimnasio en custodia; sí el lunes tras dejarlo | `agenda/ReglasGimnasio` | `AgendaTest` |
| No duplicar la entrega del niño | `agenda/GeneradorAgenda` | `AgendaTest` |
| Un día no es una tendencia | `estadisticas/Series` | `EstadisticasTest` |
| No sumar calorías activas y totales | `estadisticas/ReglasEnergia` | `EstadisticasTest` |

`FiltroRodilla` merece una nota: el bloqueo de piernas se evalúa **antes** que la
lista de ejercicios permitidos por el profesional. Aunque el usuario registre
«prensa de piernas» como autorizada, sigue bloqueada. Se hizo así a propósito: el
usuario dijo que puede caminar y correr, y también dijo explícitamente que no
quiere carga en la rodilla; lo segundo manda, y decir que puede correr no es una
autorización médica.

## Decisión 3: identificadores de evento estables

`GeneradorAgenda.uid(tipo, fecha, sufijo)` produce un identificador determinista.
La agenda no se guarda: se **regenera** desde la configuración, y solo se guarda lo
que el usuario hizo con cada evento (`registro_evento`, con el uid como clave
primaria).

Consecuencias:

- Regenerar la agenda es idempotente: mismos eventos, mismos identificadores.
- Reexportar a ICS actualiza los eventos existentes en vez de duplicarlos.
- Cambiar un ajuste (por ejemplo, la hora de recogida) se refleja en toda la
  agenda futura sin migraciones ni filas huérfanas.
- Al posponer, `fechaIso` y `horaOriginalMin` **no se tocan nunca**; la nueva hora
  va en un campo aparte.

## Decisión 4: alarmas exactas con degradación honesta

Los recordatorios usan `AlarmManager.setExactAndAllowWhileIdle`. Se declara
`SCHEDULE_EXACT_ALARM`, no `USE_EXACT_ALARM`: esta app son recordatorios, no un
despertador, y `USE_EXACT_ALARM` es para apps de alarma o calendario cuya función
principal es esa.

Si el usuario no concede el permiso (`canScheduleExactAlarms()` devuelve `false`),
la app **no falla ni miente**: usa `setWindow` con 10 minutos de margen y la
pantalla de Ajustes dice literalmente que los avisos pueden llegar con retraso.

`WorkManager` corre cada 12 horas y solo rellena la ventana de alarmas de los
próximos 3 días. **No se usa como mecanismo de aviso**: WorkManager no garantiza
una hora exacta.

`ReprogramarReceiver` escucha `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`,
`TIMEZONE_CHANGED` y `TIME_SET`, porque un reinicio borra todas las alarmas
pendientes y un cambio de zona horaria las dejaría en la hora equivocada.

## Decisión 5: el reloj no decide nada

Health Connect es **solo lectura**, permiso por tipo, y lo que llega se guarda como
dato con procedencia (`origenApp`, `dispositivo`, `ultimaModificacionIso`), no como
verdad del plan:

- Los pesos importados no pisan un registro manual del mismo día.
- Las sesiones de ejercicio detectadas no marcan una sesión como completada.
- Las calorías activas y totales se muestran por separado; sumarlas lanza una
  excepción por diseño (`ReglasEnergia.sumarActivasYTotales`).
- La ingesta no se ajusta automáticamente
  (`AJUSTE_AUTOMATICO_DE_INGESTA_HABILITADO = false`).

## Decisión 6: las propuestas externas pasan por una lista blanca

`plan/ValidadorPropuesta` acepta cambios solo en rutas conocidas
(`nutricion.kcalObjetivo`, `entrenamiento.series`, `agenda.horaGimnasio`…) y
rechaza de plano `salud.autorizacion`, `salud.restriccionRodilla`,
`preferencias.prohibidos`, `nino.*` y `custodia.*`.

Una propuesta válida todavía no cambia nada: `HistorialPlan.aceptar` exige
`aceptadaPorUsuario = true` y lanza si no lo recibe. Cada aceptación crea una
versión nueva y la anterior se conserva para revertir.

## Elecciones técnicas menores

- **Room** en vez de DataStore para todo: hay relaciones reales (sesión → series) y
  consultas por rango de fechas. El esquema se exporta a `android/app/schemas/`
  para que los cambios de base de datos sean revisables en el repositorio.
- **Enumerados guardados como texto**, no como ordinal: el esquema es legible y no
  se rompe al reordenar un `enum`.
- **`kotlinx.serialization`** para la exportación: el esquema JSON está versionado
  (`misaludyfuerza.revision.v1`) y es el mismo tipo que se valida al importar.
- **Un solo `AppViewModel`**: la app es de un solo usuario y las pantallas comparten
  fecha, configuración y avisos. Varios ViewModels obligarían a duplicar ese estado.
- **`minSdk 26`**: `java.time` sin desugaring, y coincide con el mínimo del SDK de
  Health Connect (Android 8). La app de Health Connect en sí necesita Android 9+.
