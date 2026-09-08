# Estado por etapa y próximos pasos

El orden de ejecución pedido era: 1) inspeccionar y explicar arquitectura,
2) construir Hoy, calendario, menú y registro persistente, 3) alertas nativas y
pruebas, 4) Health Connect, 5) evaluar Crunch sin bloquear lo demás,
6) estadísticas y exportación, 7) instrucciones de instalación y pruebas.

| # | Etapa | Estado |
|---|---|---|
| 1 | Inspeccionar y explicar arquitectura | ✅ Hecho. Repositorio vacío (solo README de práctica de Git); se diseñó desde cero. Ver [`ARQUITECTURA.md`](ARQUITECTURA.md). |
| 2 | Hoy, calendario, menú y registro persistente | ✅ Lógica probada · 🟡 Interfaz y Room implementados, sin compilar. |
| 3 | Alertas nativas y pruebas | 🟡 Implementado (alarmas exactas, acciones, reinicio, zona horaria, «Probar notificación»). Falta el teléfono. |
| 4 | Health Connect con datos autorizados | 🟡 Implementado y contrastado con la documentación oficial del 08/09/2026. Falta conceder permisos en tu teléfono. |
| 5 | Evaluar Crunch sin bloquear lo demás | ✅ Evaluado. 🔴 No hay acceso autorizado. La app funciona entera sin ello. |
| 6 | Estadísticas y exportación para ChatGPT | ✅ Lógica probada (tendencias, cobertura, Markdown/JSON/CSV, ICS) · 🟡 Pantalla sin compilar. |
| 7 | Instalación y pruebas en el teléfono | ✅ Documentado en [`INSTALACION.md`](INSTALACION.md) · 🔴 APK no generado aquí. |

## Próximos pasos, en orden de importancia

### 1. Compilar e instalar (bloqueante para todo lo demás)

Nada de lo marcado 🟡 se puede confirmar sin esto. Sigue
[`INSTALACION.md`](INSTALACION.md). Espera errores de compilación la primera vez:
el módulo Android **nunca se ha compilado**, así que es probable que haya algún
import que falte o alguna firma de Compose que ajustar. Son errores de compilación,
no de diseño; la lógica que sostiene las reglas ya está probada aparte.

### 2. Recorrer la lista de verificación

La de [`PRUEBAS.md`](PRUEBAS.md) y [`INSTALACION.md`](INSTALACION.md). Especialmente
los avisos tras reiniciar y la entrega al Galaxy Watch, que son los dos puntos que
no se pueden dar por buenos desde el código.

### 3. Confirmar los tres datos que hoy son estimaciones

La app los marca como «sin confirmar» hasta que tú los ajustes:

- **Traslado gimnasio → casa**: 15 minutos provisionales. Mídelo un miércoles.
- **Recogida de martes y jueves**: 7:15 p. m. es una lectura de tu «7 y algo».
- **Fecha de activación del plan**: por defecto el día de instalación.

Los tres se editan en **Más → Ajustes**, con su interruptor de «confirmado».

### 4. Registrar lo que diga tu traumatólogo o fisioterapeuta

Mientras el estado siga en «Pendiente de confirmar», las rutinas se muestran como
propuestas para validar y **no se propone ninguna subida de carga ni tercera
serie**. En **Más → Autorización profesional** puedes registrar el estado, quién lo
indicó y sus instrucciones, y marcar ejercicios como permitidos o prohibidos.

Recuerda: marcar «autorizada» **no** desbloquea los ejercicios de piernas. Eso es
deliberado y está cubierto por una prueba.

### 5. Pausar tus avisos de ChatGPT (cuando compruebes que estos funcionan)

Primero unos días de uso real. Después lo pides tú allí; esta app no puede hacerlo.

## Mejoras identificadas, sin hacer

Ordenadas por lo que aportan frente a lo que cuestan:

1. **Gráficas en Progreso.** Hoy los datos se muestran como listas y promedios
   semanales con su cobertura. Una gráfica de líneas con la cobertura sombreada
   sería más legible y no cambia la lógica.
2. **Editar porciones al marcar «Cambié».** La base de datos ya guarda las porciones
   reales (`porcion_real`) y el repositorio las escribe; falta el formulario.
3. **Cronómetro de descanso** de 90–120 segundos en la pantalla de sesión, y aviso
   del límite de 50 minutos durante el entrenamiento.
4. **Historial por máquina** con gráfica. El DAO ya lo expone
   (`observarHistorialEjercicio`); falta la pantalla.
5. **Importar la propuesta de cambio desde un archivo.** El validador y el
   versionado están hechos y probados; falta el selector de archivo y la pantalla
   de diferencias.
6. **Sincronización con el calendario de Android o Google Calendar.** Requiere
   decidir credenciales y alcance; es una decisión tuya. El ICS ya cubre el caso
   sin permisos especiales.
7. **Pruebas instrumentadas** (`androidTest`) para Room y para las acciones de la
   notificación. Solo se pueden ejecutar con SDK de Android.
8. **Notificación del resumen del domingo** que abra directamente la vista previa
   de la revisión semanal.

## Deudas conocidas

- **El módulo Android no se ha compilado nunca.** Es la deuda principal.
- **Las estimaciones nutricionales son genéricas**, no de productos concretos. El
  menú da 2089–2207 kcal y 172–180 g de proteína, por encima del objetivo de
  2000 kcal y 150–170 g. Ver [`PRUEBAS.md`](PRUEBAS.md).
- **Sin migraciones de Room todavía.** La base va por la versión 1. En cuanto
  guardes datos reales, cualquier cambio de esquema necesitará una migración; el
  esquema exportado a `android/app/schemas/` está para eso.
- **No hay icono propio**: se usa un vector sencillo de mancuerna generado aquí.
