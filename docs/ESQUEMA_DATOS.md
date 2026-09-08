# Esquema de datos

## Base de datos local (Room, versión 1)

Archivo: `mi-salud-y-fuerza.db`, solo en el teléfono. Sin copia en la nube.

| Tabla | Clave | Para qué |
|---|---|---|
| `perfil` | `id = 1` | Nombre, edad, altura, peso inicial declarado, zona horaria. |
| `ajustes` | `id = 1` | Fecha de activación, margen de aviso, minutos de posponer, traslado gimnasio–casa y hora de recogida (ambos con su bandera de «confirmado»), anonimizar al exportar. |
| `autorizacion` | `id = 1` | Estado (`PENDIENTE_DE_CONFIRMAR` por defecto), profesional, fecha. |
| `instrucciones_profesional` | auto | Instrucciones y listas de ejercicios permitidos o prohibidos por el profesional. |
| `registro_evento` | `uid` | Qué hizo el usuario con cada evento. Conserva `fechaIso` y `horaOriginalMin` aunque se posponga. |
| `registro_comida` | `fechaIso` + `tipo` | `PLANEADA`, `COMI`, `CAMBIE` o `NO_COMI`. |
| `porcion_real` | auto | Lo que realmente comió cuando marcó «Cambié». |
| `medicion` | `tipo` + `fechaIso` único | Peso y cintura, con `origen` (`Manual`, `Health Connect`, `Importado`). |
| `sesion` | auto | Sesión de entrenamiento, `completada` solo por acción del usuario, más síntomas. |
| `serie` | auto, FK a `sesion` | Peso, repeticiones, repeticiones en reserva, técnica. |
| `excepcion_custodia` | `sabadoIso` | Excepción puntual sin romper la paridad general. |
| `dia_escolar` | `fechaIso` | Cierres y festivos escolares. |
| `version_plan` / `cambio_plan` | `numero` | Historial de versiones del plan, con reversión. |
| `salud_sincronizacion` | `tipoDato` | Por tipo: permiso, disponibilidad, token de cambios, última sincronización, último error. |
| `salud_registro` | `claveDeduplicacion` | Registros leídos de Health Connect con su procedencia. |

La clave de deduplicación es `TIPO|idDeHealthConnect`. Se eligió así a propósito:
un borrado en Health Connect llega solo con el identificador del registro, sin el
paquete de origen, y con esta clave el borrado encuentra su fila.

## Modelo de dominio (`core/`)

Lo importante del dominio, en una frase cada cosa:

- **`Hora12`** — única frontera de presentación de horas. Cualquier hora que ve el
  usuario pasa por aquí; la interfaz nunca formatea horas a mano.
- **`Custodia`** — paridad quincenal anclada al sábado 12 de septiembre de 2026,
  con excepciones por sábado. El lunes hasta las 7:40 a. m. pertenece al fin de
  semana anterior.
- **`CustodiaEntreSemana`** — martes y jueves, recogida estimada a las 7:15 p. m.
  con `horaRecogidaConfirmada = false`.
- **`ReglasGimnasio`** — decide y **explica** por qué hay o no hay gimnasio
  (`DecisionGimnasio.Programado` / `NoProgramado`, ambos con motivo).
- **`GeneradorAgenda`** — la agenda se calcula, no se guarda.
- **`Catalogo`** — filtro duro de alimentos prohibidos, totales y sustituciones.
- **`MotorProgresion`** — cuenta sesiones realizadas, no fechas. Devuelve siempre
  una `PropuestaProgresion` con `requiereConfirmacion = true`.
- **`Series`** — promedios semanales, cobertura y tendencias con mínimos.
- **`HistorialPlan`** — versiones y reversión.

## Esquema de exportación: `misaludyfuerza.revision.v1`

Se exporta en tres formatos a la vez, con el mismo contenido:

- **Markdown** — para leer y pegar en una conversación.
- **JSON** — para analizar. Esquema versionado.
- **CSV** — métricas, una fila por métrica con cobertura y origen.

Estructura del JSON:

```jsonc
{
  "metadatos": {
    "esquema": "misaludyfuerza.revision.v1",
    "generadoEnIso": "2026-09-14T18:00:00-04:00",
    "zonaHoraria": "America/New_York",
    "formatoHoraVisible": "12 horas con a. m. / p. m.",
    "unidades": { "peso": "kg", "cintura": "cm", "energia": "kcal", "proteina": "g",
                  "carga": "kg", "sueno": "horas" },
    "ninoAnonimizado": true,
    "advertencia": "Datos declarados y registrados por el usuario. Las cifras
                    nutricionales son estimaciones, no mediciones..."
  },
  "desdeIso": "2026-09-08",
  "hastaIso": "2026-09-14",
  "versionPlan": 1,
  "perfil": { },
  "preferencias": { "prohibidos": ["pollo", "huevo hervido", "avena"], },
  "autorizacion": { "estado": "PENDIENTE_DE_CONFIRMAR", },
  "restriccionesClave": ["Ligamentos de rodilla rotos: sin carga de piernas..."],
  "nutricion":     { "comidasSinRegistrar": 12, "adherenciaPorcentaje": 66.7,
                     "procedencia": { "fuente": "Registro manual del usuario" } },
  "entrenamiento": { "sesionesCompletadas": 2, "volumenKg": 4820.0,
                     "procedencia": { } },
  "mediciones":    { "coberturaPeso": { "diasConDatos": 2, "diasDelPeriodo": 7 } },
  "salud":         { "conectado": false, "tiposNoDisponibles": ["pasos", "sueno"],
                     "procedencia": { "huecos": ["Sin permisos concedidos"] } },
  "tendencias":    { "peso": { "veredicto": "DATOS_INSUFICIENTES", } }
}
```

Cada bloque lleva su **procedencia** y sus **huecos**. `DATOS_INSUFICIENTES` es un
veredicto de primera clase, no un cero disfrazado.

Los detalles identificables del niño se omiten por defecto: en el ICS sus eventos
salen como «Compromiso familiar».

## Esquema de importación: `misaludyfuerza.propuesta.v1`

Para que ChatGPT/Codex propongan cambios sin poder aplicarlos:

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
      "justificacion": "Peso promedio estable en tres semanas con buena cobertura"
    }
  ]
}
```

**Rutas aceptadas:** `nutricion.kcalObjetivo`, `nutricion.proteinaMinG`,
`nutricion.proteinaMaxG`, `nutricion.menu.almuerzo`, `nutricion.menu.cena`,
`entrenamiento.series`, `entrenamiento.repMin`, `entrenamiento.repMax`,
`entrenamiento.descansoSeg`, `agenda.horaGimnasio`, `agenda.horaDormirObjetivo`,
`agenda.margenAvisoMin`.

**Rutas rechazadas siempre:** `salud.autorizacion`, `salud.restriccionRodilla`,
`preferencias.prohibidos`, cualquier `nino.*` y cualquier `custodia.*`.

Flujo: validar → mostrar diferencias → **el usuario acepta** → nueva versión del
plan, con la anterior conservada.

## Calendario ICS

`ExportadorIcs` genera un calendario separado (`X-WR-CALNAME:Mi Salud y Fuerza`) con
un bloque `VTIMEZONE` completo para `America/New_York` (segundo domingo de marzo y
primer domingo de noviembre). Cada `DTSTART` lleva `TZID=America/New_York`; no hay
horas flotantes. El `UID` de cada evento es el uid estable del dominio, así que
reimportar actualiza en vez de duplicar.
