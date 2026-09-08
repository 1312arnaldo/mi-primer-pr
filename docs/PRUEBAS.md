# Pruebas: qué se ha probado de verdad

Tres categorías, y ninguna se mezcla con otra:

- ✅ **Funciona y está probado** — ejecutado, con resultado verificado.
- 🟡 **Implementado, falta probar en dispositivo** — el código está, no se ha
  ejecutado en un teléfono.
- 🔴 **Requiere conexión o aprobación externa** — no depende del código.

## ✅ Funciona y está probado

**90 pruebas ejecutadas, 0 fallos, 0 omitidas.** Entorno: JDK 21, Gradle 8.14.3,
Linux. Son pruebas de JVM: **no** se ejecutaron en un emulador ni en un Samsung.

```bash
./gradlew :core:test
```

| Archivo | Pruebas | Qué cubre |
|---|---|---|
| `Hora12Test` | 6 | Formato de 12 horas en las 96 combinaciones de hora del día; medianoche como 12:00 a. m. y mediodía como 12:00 p. m.; fechas en español; horario de verano (−04:00 en septiembre, −05:00 en diciembre). |
| `CustodiaTest` | 8 | El ancla del 12/09/2026 es sábado; paridad quincenal hacia delante y hacia atrás; el periodo va de sábado 8:00 a. m. a lunes 7:40 a. m. exactos; el lunes por la mañana pertenece al fin de semana anterior; una excepción no rompe la paridad; martes y jueves generan noche con el niño y su hora es estimación. |
| `AgendaTest` | 16 | Gimnasio solo lunes, miércoles y viernes; nunca en custodia; nunca antes de la entrega en la escuela; sí el lunes tras el fin de semana con el niño; un cierre escolar no lo programa solo; no se duplica la entrega del niño; recogida y traslado marcados «sin confirmar»; peso solo L-X-V; cintura los sábados; identificadores estables y sin duplicados en todo el plan de referencia; ningún conflicto de horario en día laboral; **todas** las horas visibles del plan en formato de 12 horas; el objetivo de dormir avanza gradualmente sin pasarse; activar tarde no genera eventos vencidos. |
| `SaludTest` | 10 | 16 ejercicios de pierna e impacto bloqueados; ningún ejercicio de la propuesta carga la rodilla; jalón sentado y ejercicios de pie marcados para revisión; una prohibición del profesional manda; **una autorización del profesional NO desbloquea la carga de piernas**; cualquier síntoma detiene, congela y sugiere consultar; sin metas de pasos automáticas. |
| `AlimentacionTest` | 11 | Ningún alimento prohibido en el catálogo; huevo revuelto sí y hervido no; las sustituciones nunca proponen un prohibido y mantienen proteína y energía dentro del ±25 %; los siete días caen en rango razonable; las cinco comidas cada día; lo planeado no cuenta como comido; la adherencia separa «no registrado» de «incumplido»; la lista de compra convierte peso cocinado a peso de compra en ambos sentidos. |
| `ProgresionTest` | 10 | Las dos primeras sesiones se mantienen en 2 series; **la progresión cuenta sesiones realizadas, no fechas** (un mes sin sesiones completadas no progresa); sin autorización la subida queda en espera; con autorización se propone el incremento mínimo y siempre pide confirmación; no se sube sin llegar al tope del rango, sin margen o con mala técnica; un síntoma congela todo aunque haya autorización; la tercera serie solo en los dos primeros ejercicios. |
| `EstadisticasTest` | 8 | Un solo día nunca es tendencia; la cobertura se reporta siempre; con datos suficientes detecta bajada; un cambio mínimo se reporta como «sin cambio claro»; la referencia de pérdida es orientativa; **sumar calorías activas y totales lanza excepción**; la ingesta no se ajusta sola. |
| `ExportacionTest` | 8 | El ICS declara zona y reglas de horario de verano; todos los `DTSTART` con `TZID`; reexportar es idempotente y sin UID duplicados; la anonimización oculta los eventos del niño; el JSON lleva esquema versionado, zona, unidades, procedencia y huecos; el Markdown muestra los huecos de datos y recuerda que lo planeado no cuenta; el CSV incluye cobertura y origen. |
| `PlanVersionTest` | 10 | Una propuesta válida muestra diferencias legibles; no puede tocar salud, prohibidos, niño ni custodia; ruta o esquema desconocidos se rechazan; un JSON roto no rompe la app; **nada se aplica sin aceptación explícita**; aceptar crea versión nueva y se puede revertir conservando el historial. |

### Dato honesto sobre las calorías del menú

Con las estimaciones del catálogo, el menú precargado da:

| Día | kcal | Proteína |
|---|---|---|
| lunes | 2124 | 173 g |
| martes | 2175 | 177 g |
| miércoles | 2207 | 172 g |
| jueves | 2093 | 180 g |
| viernes | 2089 | 173 g |
| sábado | 2124 | 173 g |
| domingo | 2175 | 177 g |

El objetivo declarado era **≈2000 kcal y 150–170 g de proteína**. El menú queda
entre **90 y 210 kcal por encima** y **entre 2 y 10 g de proteína por encima** del
tope. No se ha «corregido» solo: son estimaciones de referencia, y ajustar
porciones es una decisión tuya. La pantalla Comidas muestra el objetivo y el total
del menú uno al lado del otro para que la diferencia se vea.

## 🟡 Implementado, falta probar en dispositivo

Nada de esto se ha ejecutado en un teléfono. **La app no se ha compilado**: el
entorno no puede descargar el SDK de Android ni los artefactos de AndroidX
(`dl.google.com` devuelve 403 por política de red).

Lo que sí se verificó del módulo Android sin compilarlo:

- Los **51 símbolos** de `:core` que importa la app existen y están declarados.
- Los 22 archivos Kotlin de `android/` tienen estructura balanceada.
- Las APIs de Health Connect se contrastaron con la documentación oficial el
  08/09/2026 (ver [`INTEGRACIONES.md`](INTEGRACIONES.md)).

Pendiente de comprobar en tu Samsung, en este orden
(ver [`INSTALACION.md`](INSTALACION.md) para los pasos):

1. Que compila y se instala.
2. Formato de 12 horas en toda la interfaz.
3. Que el recordatorio llega a su hora, y que «Hecho», «Posponer» y «Omitir»
   funcionan desde la notificación.
4. Que los avisos siguen ahí **después de reiniciar el teléfono**.
5. Que la notificación llega al **Galaxy Watch** vía Galaxy Wearable.
6. Permisos de Health Connect: conceder, leer, **revocar** y comprobar que la app
   lo refleja sin inventarse datos.
7. Sincronizar dos veces seguidas y comprobar que **no se duplican** registros.
8. Exportar, compartir e importar una propuesta de vuelta.
9. Que la base de datos sobrevive a cerrar y reabrir la app.
10. Comportamiento con el ahorro de batería de Samsung activado.

## 🔴 Requiere conexión o aprobación externa

| Asunto | Por qué |
|---|---|
| APK de prueba | El entorno no puede descargar el SDK de Android. Compílalo tú con Android Studio. |
| Integración con Crunch Fitness | No existe API pública ni acceso autorizado para terceros. |
| Samsung Health Data SDK | Requiere aprobación de partner de Samsung, no concedida. |
| Google Calendar con OAuth | Requiere credenciales y una decisión de alcance que debes autorizar tú. |
| Pausar tus avisos de ChatGPT | Sin integración autorizada. Lo pides tú cuando compruebes que estos funcionan. |

## Lo que estas pruebas NO demuestran

- No demuestran que la app funcione en un teléfono: son pruebas de JVM.
- No demuestran que las estimaciones nutricionales coincidan con tu comida real.
- No demuestran nada médico. Que el filtro de rodilla bloquee ejercicios significa
  que el código hace lo que se le pidió, no que la rutina sea segura para ti. Eso
  lo decide tu traumatólogo o tu fisioterapeuta.
