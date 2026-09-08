# Continuar desde ChatGPT o Codex

## Lo primero: no hay conexión automática

**Claude Code y ChatGPT no están conectados.** Ningún asistente puede leer tu reloj,
tu teléfono ni esta app sin un conector autorizado, y hoy ese conector no existe.

Lo que sí funciona desde la primera versión: **tú** exportas y **tú** compartes.

## Cómo pasar tu situación a otro asistente

1. En la app, **Más → Compartir revisión semanal → Vista previa**. Lee lo que vas
   a mandar.
2. **Compartir**. Se generan tres archivos:
   - `revision_<desde>_a_<hasta>.md` — para pegar en una conversación.
   - `revision_<desde>_a_<hasta>.json` — esquema `misaludyfuerza.revision.v1`.
   - `metricas_<desde>_a_<hasta>.csv` — métricas con cobertura y origen.
3. Pega el Markdown o adjunta el JSON.

La exportación ya incluye zona horaria, unidades, fechas, procedencia de cada
bloque, huecos de datos, tu perfil, tus preferencias, tus restricciones y la
versión del plan. Los detalles identificables del niño se omiten por defecto.

## Cómo pedirle una propuesta de cambio

Copia esto en la conversación:

> A partir del JSON adjunto (`misaludyfuerza.revision.v1`), propón cambios en
> formato `misaludyfuerza.propuesta.v1`. Solo puedes usar estas rutas:
> `nutricion.kcalObjetivo`, `nutricion.proteinaMinG`, `nutricion.proteinaMaxG`,
> `nutricion.menu.almuerzo`, `nutricion.menu.cena`, `entrenamiento.series`,
> `entrenamiento.repMin`, `entrenamiento.repMax`, `entrenamiento.descansoSeg`,
> `agenda.horaGimnasio`, `agenda.horaDormirObjetivo`, `agenda.margenAvisoMin`.
> Cada cambio debe llevar `justificacion`. No propongas nada que toque
> restricciones de salud, alimentos prohibidos, al niño o la custodia: se rechaza.
> No propongas ejercicios de piernas ni con impacto. No propongas pollo, huevo
> hervido ni avena.

Formato de respuesta esperado:

```json
{
  "esquema": "misaludyfuerza.propuesta.v1",
  "id": "p-42",
  "origen": "ChatGPT",
  "fechaIso": "2026-09-28",
  "comentario": "Ajuste tras tres semanas de datos",
  "cambios": [
    {
      "ruta": "nutricion.kcalObjetivo",
      "valorAnterior": "2000",
      "valorNuevo": "1900",
      "justificacion": "Peso promedio estable tres semanas con cobertura suficiente"
    }
  ]
}
```

La app valida el esquema, comprueba cada ruta, te enseña las diferencias y **espera
tu aceptación**. Aceptar crea una versión nueva del plan; la anterior se conserva y
puedes revertir.

## Contexto para quien retome el código

- **Empieza por** [`ARQUITECTURA.md`](ARQUITECTURA.md). Explica por qué el dominio
  está separado en `core/` y por qué las reglas son código con pruebas.
- **Las reglas de seguridad no se tocan sin hablarlo.** `FiltroRodilla`,
  `ProtocoloSintomas`, el filtro de alimentos prohibidos y «lo planeado no cuenta
  como comido» tienen pruebas que fallarán si alguien las relaja. Eso es
  intencionado.
- **Antes de cambiar nada:** `./gradlew :core:test`. 90 pruebas, 0 fallos.
- **Lo que falta** está en [`ESTADO_Y_PROXIMOS_PASOS.md`](ESTADO_Y_PROXIMOS_PASOS.md),
  priorizado.
- **Lo que NO se ha probado** está en [`PRUEBAS.md`](PRUEBAS.md). No lo des por
  bueno.

## Interfaz futura (documentada, no expuesta)

El esquema de importación y exportación está pensado para que algún día haya una
API o un servidor MCP autenticado. **Hoy no hay nada expuesto en red**, a propósito:
anticipar esa integración no justifica abrir datos de salud. Cuando exista, se
diseñará con autenticación, autorización por usuario y secretos fuera del cliente
y del repositorio.

Las funciones básicas de la app **no dependen** de pagar ninguna API de IA y no
deben empezar a depender.
