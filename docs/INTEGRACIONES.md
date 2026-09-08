# Integraciones: lo que se puede, lo que no y qué falta comprobar

Documentación oficial consultada el **8 de septiembre de 2026**. Vuelve a
comprobarla antes de dar por buena cualquier afirmación de esta página.

## Health Connect

**Ruta prevista:** Galaxy Watch → Samsung Health → Health Connect (en el teléfono)
→ esta app. La app **no habla con Samsung Health directamente** ni pide
credenciales de Samsung: usa el permiso oficial de Health Connect y lee lo que ya
esté sincronizado en el teléfono.

### Verificado contra la documentación oficial

| Punto | Comprobado el 08/09/2026 |
|---|---|
| Disponibilidad | `HealthConnectClient.getSdkStatus(context)` → `SDK_AVAILABLE`, `SDK_UNAVAILABLE`, `SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED`. |
| Versión del artefacto | La documentación muestra `androidx.health.connect:connect-client:1.2.0-alpha06`. Es la versión fijada en `android/app/build.gradle.kts`. |
| Mínimos | SDK: Android 8 (API 26). App de Health Connect: Android 9 (API 28) o superior. |
| Permisos | `<uses-permission android:name="android.permission.health.READ_*" />` en el manifiesto, y en ejecución con `PermissionController.createRequestPermissionResultContract()`. |
| Segundo plano | `android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND`, con `HealthConnectFeatures.FEATURE_READ_HEALTH_DATA_IN_BACKGROUND` para comprobar si el teléfono lo admite. |
| Historial | `PERMISSION_READ_HEALTH_DATA_HISTORY`. **Sin él solo se pueden leer 30 días.** Por eso la ventana inicial de la app es de 30 días y solo se amplía a 365 si el permiso está concedido. |

### Lo que la app implementa

- Permiso **por tipo**: pasos, sesiones de ejercicio, sueño, frecuencia cardiaca,
  peso, energía activa y energía total. Cada uno se concede o se niega por separado.
- Estado por tipo en pantalla: permiso concedido, si hay datos, última
  sincronización y último error, con su texto.
- **Deduplicación** por `TIPO|idDeHealthConnect`.
- **Cambios y borrados** mediante token (`getChangesToken` / `getChanges`), con
  manejo de token caducado: se reinicia la ventana en vez de perder datos en
  silencio.
- **Desconectar** borra todo lo importado y su estado de sincronización.

### Lo que la app NO promete

- **Puntuación de energía de Samsung, detalle de fases de sueño y HRV:** son
  métricas propietarias. La app no las declara disponibles. Si aparecen en Health
  Connect y la fuente las comparte, se pueden añadir; hasta comprobarlo en tu
  teléfono, no se afirma nada.
- **Sincronización continua en tiempo real:** Android restringe la ejecución en
  segundo plano y el fabricante añade su propio ahorro de batería. La app
  sincroniza cuando la abres y cuando se lo pides.
- **Que haya datos:** «sin datos» no es «cero». La pantalla lo dice con esas
  palabras.

### Samsung Health Data SDK

No se usa. Requiere aprobación de partner de Samsung y no se ha comprobado que
esté concedida. Usar un modo de desarrollo y presentarlo como conexión lista para
producción sería mentir. La ruta vía Health Connect no la necesita.

Referencias a revisar si algún día se plantea:
<https://developer.samsung.com/health/data> y
<https://developer.samsung.com/health/blog/en/accessing-samsung-health-data-through-health-connect>.

### App independiente de Wear OS

Opcional. **No hace falta** para leer datos que ya están sincronizados en el
teléfono. No se ha construido.

## Galaxy Watch: notificaciones

Los avisos de Android se reflejan en el reloj a través de **Galaxy Wearable**. La
app no puede garantizarlo desde el código, así que:

1. La pantalla **Más** incluye el botón **«Probar notificación»**.
2. El texto de esa notificación te dice exactamente qué hacer si llega al teléfono
   pero no al reloj: abrir Galaxy Wearable → Notificaciones → activar
   «Mi Salud y Fuerza».

**Estado: falta probar en tu teléfono y tu reloj.** Hasta que lo hagas, no se
afirma que la entrega al reloj funcione.

## Crunch Fitness

**Estado: «Sin integración directa disponible».**

No existe una API pública ni un mecanismo autorizado de Crunch Fitness para que una
aplicación de terceros lea la cuenta de un socio. En consecuencia, la app:

- **No** inventa endpoints.
- **No** extrae tokens de la aplicación de Crunch.
- **No** hace scraping de zonas privadas.
- **No** muestra un botón que simule una conexión con éxito.
- **No** pide tu sucursal ni tu cuenta, porque hoy no sirven para nada real.

Lo que sí hace: registro manual completo de la sesión (ejercicio, máquina, series,
repeticiones, peso, descanso y notas), historial por máquina, importación de
archivos que tú mismo exportes y un enlace para abrir la app de Crunch.

Si Crunch habilita un acceso oficial, se añade aquí. Hasta entonces la app funciona
al 100 % sin ello.

## Calendario

- **Exportación ICS**: calendario separado, con `VTIMEZONE` de `America/New_York` y
  UID estables. Es la vía que ya funciona y no necesita ningún permiso especial.
- **Google Calendar con OAuth o el calendario de Android**: previsto, no
  implementado. Requiere decidir credenciales y alcance, y al ser una decisión de
  acceso y de coste, se deja para que la autorices tú.

Sea cual sea la vía, la fuente de verdad es la app: los identificadores estables
garantizan una sincronización idempotente y evitan avisos duplicados.

## Los avisos que ya tienes en ChatGPT

Tienes recordatorios a las 6:00 a. m., a las 7:40 a. m. los lunes, miércoles y
viernes, y a las 9:00 p. m. **Esta app no puede modificarlos ni pausarlos**: no hay
integración autorizada entre ambas.

El plan es tuyo y en dos pasos: primero compruebas con «Probar notificación» y unos
días de uso que estos avisos llegan bien al teléfono y al reloj; después pides tú
en ChatGPT que se pausen los suyos. La pantalla **Más** te lo recuerda con ese
mismo orden.

## ChatGPT y Codex

**Claude Code y ChatGPT no están conectados.** Nadie puede leer tu reloj ni tu app
sin un conector autorizado que hoy no existe.

Lo que sí funciona desde la primera versión: exportas la revisión semanal
(Markdown, JSON y CSV), la revisas en la vista previa y la compartes tú por donde
quieras. Ver [`PARA_CHATGPT.md`](PARA_CHATGPT.md).
