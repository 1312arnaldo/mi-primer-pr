# Instalar y verificar en tu Samsung

## Lo que necesitas

- Un ordenador con **Android Studio** (incluye el SDK de Android y un JDK).
- Tu teléfono Samsung con **Android 8 o superior** (para Health Connect, Android 9+).
- Un cable USB.

## 1. Compilar el APK

```bash
git clone <este-repositorio>
cd mi-primer-pr

# Comprueba primero que la lógica pasa sus pruebas (no necesita Android):
./gradlew :core:test

# Ahora la app:
cd android
./gradlew :app:assembleDebug
```

El APK queda en:

```
android/app/build/outputs/apk/debug/app-debug.apk
```

Si prefieres Android Studio: abre la carpeta **`android/`** (no la raíz), espera a
que sincronice y usa *Run*. El build compuesto trae `core/` automáticamente.

## 2. Instalar

Con el teléfono conectado y la depuración USB activada:

```bash
adb install -r android/app/build/outputs/apk/debug/app-debug.apk
```

O copia el APK al teléfono y ábrelo, permitiendo instalar desde esa fuente.

## 3. Permisos, en este orden

1. **Notificaciones** — la app las pide al abrirse. Acéptalas.
2. **Alarmas exactas** — ve a **Más → Recordatorios**. Si dice que no están
   permitidas, pulsa «Permitir alarmas exactas» y actívalo en los ajustes del
   sistema. Sin esto los avisos pueden llegar con minutos de retraso; la app te lo
   dirá en lugar de fingir que son puntuales.
3. **Ahorro de batería** — en los ajustes de Samsung, marca la app como sin
   restricciones. Samsung es agresivo con el segundo plano y puede retrasar avisos.
4. **Health Connect** — en **Más → Health Connect**, pulsa «Conceder permisos» y
   elige tipo por tipo. Puedes conceder solo lo que quieras.
5. **Galaxy Wearable** — abre la app Galaxy Wearable → **Notificaciones** → activa
   «Mi Salud y Fuerza».

## 4. Verificación, paso a paso

Marca cada uno. Si algo falla, es un fallo real que hay que arreglar, no algo que
puedas dar por bueno.

### Horas

- [ ] Recorre Hoy, Calendario, Comidas, Entreno, Progreso y Más. **Ninguna hora**
      debe aparecer en formato de 24 horas. Todas terminan en «a. m.» o «p. m.».
- [ ] Comprueba que el mediodía sale como «12:00 p. m.» y la medianoche como
      «12:00 a. m.».

### Familia y calendario

- [ ] En Calendario, el próximo fin de semana con el niño debe ser un **sábado a
      las 8:00 a. m.**, alternando cada dos semanas desde el 12 de septiembre de 2026.
- [ ] Un **miércoles o viernes** por la mañana debe decir **«Preparar al niño»**,
      no «Recibir al niño» (durmió contigo tras la recogida del día anterior).
- [ ] Un **martes o jueves** por la mañana debe decir **«Recibir al niño»**.
- [ ] El **lunes después de un fin de semana con el niño**: debe decir «Preparar al
      niño» **y aun así tener gimnasio**, porque va después de dejarlo en la escuela.
- [ ] El sábado y el domingo de custodia **no** deben tener gimnasio.
- [ ] Añade un cierre escolar y comprueba que ese día desaparecen la entrega en la
      escuela y el gimnasio, con su motivo.

### Comidas

- [ ] Ninguna comida ni sustitución propone **pollo, huevo hervido o avena**.
- [ ] Marca «Comí» en una comida: se pone en verde. Reabre la app: sigue marcada.
- [ ] Una comida que solo se notificó **no** debe aparecer como comida.

### Entrenamiento

- [ ] Ningún ejercicio de la sesión es de piernas.
- [ ] Registra dolor 3 al cerrar una sesión: debe aparecer el aviso de detener el
      movimiento y consultar, y ninguna propuesta de subir carga.

### Recordatorios

- [ ] **Más → Probar notificación**: debe llegar al teléfono **y al reloj**.
- [ ] Espera a un recordatorio real. Pulsa **«Posponer»**: debe volver a sonar, y en
      Hoy la tarjeta debe mostrar la hora nueva **y la original entre paréntesis**.
- [ ] Pulsa **«Hecho»** en otro: la tarjeta se pone verde y no vuelve a sonar.
- [ ] **Reinicia el teléfono.** Abre la app y comprueba en Más → Recordatorios que
      siguen programados. Espera al siguiente aviso: debe llegar.

### Health Connect

- [ ] Concede permisos y pulsa «Sincronizar». Mira el estado por tipo.
- [ ] Un tipo sin datos debe decir «todavía sin datos», **no cero**.
- [ ] Pulsa «Sincronizar» **dos veces seguidas**: los registros no deben duplicarse.
- [ ] **Revoca** un permiso desde Health Connect, vuelve a la app y sincroniza: debe
      decir «Sin permiso concedido» sin inventarse nada.
- [ ] Pulsa «Desconectar y borrar lo importado» y comprueba que se limpia.

### Exportación

- [ ] **Más → Vista previa**: debe mostrar cobertura, huecos de datos y horas en 12
      horas antes de compartir nada.
- [ ] Comparte y abre los tres archivos: `.md`, `.json` y `.csv`.
- [ ] Con «Omitir datos del niño» activado, exporta el ICS y comprueba que sus
      eventos salen como «Compromiso familiar».
- [ ] Importa el ICS en tu calendario **dos veces**: no deben duplicarse los eventos.

### Datos

- [ ] Registra un peso, cierra la app del todo y vuelve a abrirla: sigue ahí.
- [ ] Pon el teléfono en modo avión y usa la app entera: debe funcionar igual.

## Si algo va mal

- **No compila** → comprueba que abriste `android/` y no la raíz, y que tienes la
  plataforma 35 instalada.
- **No llegan los avisos** → Más → Recordatorios te dice qué permiso falta. Revisa
  también el ahorro de batería de Samsung.
- **Llegan al teléfono pero no al reloj** → Galaxy Wearable → Notificaciones →
  activar «Mi Salud y Fuerza». Es el propio texto de la notificación de prueba.
- **Health Connect no aparece** → en Android 14+ viene integrado; en versiones
  anteriores se instala desde Play Store. La app te dice cuál es tu caso.
