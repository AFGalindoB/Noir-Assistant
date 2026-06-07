# Noir Assistant — Reminder Data Flow

## Introducción

Este documento describe cómo fluye la configuración de recordatorios dentro de Noir Assistant desde que el usuario selecciona una nueva hora hasta que dicha configuración queda sincronizada con el sistema de alarmas de Android.

A diferencia de la documentación de componentes, cuyo objetivo es describir la infraestructura interna de notificaciones, este documento se enfoca exclusivamente en el recorrido de los datos a través de la aplicación.

Para conocer la arquitectura completa del sistema de recordatorios puede consultarse:

[Componentes de Notificaciones](./Notification_Components.md)

## Flujo General

La configuración de recordatorios sigue una arquitectura reactiva basada en persistencia y observación de estado.

**Escritura:**

```text
UI
 ↓
SettingsViewModel
 ↓
SettingsRepository
 ↓
UserPreferencesManager
 ↓
DataStore
```

**Lectura:**

```text
DataStore
 ↓
UserPreferencesManager
 ↓
SettingsRepository
 ↓
MainActivity
 ↓
AlarmScheduler
 ↓
AlarmManager
```

Cada componente posee una responsabilidad específica y desconoce los detalles internos de las demás capas.

## Flujo de Escritura

### UI

El usuario accede a la pantalla de cuenta y selecciona la sección de recordatorios.

La selección se realiza mediante un selector de hora personalizado.

El componente permite elegir:

- Hora
- Minutos

Una vez confirmada la selección la nueva configuración es enviada al ViewModel que sigue el recorrido descrito anteriormente.

### Actualización Persistente

La escritura real ocurre dentro de `UserPreferencesManager`, aquí se actualiza la clave correspondiente al recordatorio.

Una vez almacenado el nuevo valor: `REMINDER_TIME_KEY`

Una vez persistido el cambio, DataStore emite automáticamente una nueva versión de las preferencias.

No existen sincronizaciones manuales ni recargas explícitas.

## Sincronización de Alarmas

Cuando `MainActivity` recibe una nueva configuración de recordatorio, solicita una sincronización con `AlarmScheduler`.

El scheduler se encarga de:

- Cancelar la programación anterior si existe.
- Calcular la siguiente ejecución válida.
- Registrar la nueva alarma en Android.

Los detalles internos de este proceso se encuentran documentados en:

[Componentes de Notificaciones](./Notification_Components.md)

### MainActivity

La actividad principal actúa como punto de sincronización entre la configuración persistida y el sistema Android.

Cuando la hora cambia: `LaunchedEffect(preferences?.reminderTime)` se solicita una nueva programación al `AlarmScheduler`.

### AlarmScheduler

El scheduler traduce la configuración persistida en una alarma real dentro de Android.

Entre sus responsabilidades se encuentran:

- Calcular la próxima ejecución válida.
- Reprogramar alarmas existentes.
- Registrar la nueva programación en el sistema.

Los detalles internos de esta infraestructura se encuentran documentados en:

[Componentes de Notificaciones](./Notification_Components.md)

## Filosofía de Diseño

La configuración de recordatorios sigue la misma filosofía reactiva utilizada por el resto de Noir Assistant.

La interfaz únicamente expresa la intención de cambio.

La persistencia permanece encapsulada por DataStore.

La sincronización con Android ocurre mediante observación reactiva de preferencias persistidas.

Gracias a esta estrategia:

- No existen sincronizaciones manuales.
- No existen estados duplicados.
- La UI no conoce AlarmManager.
- AlarmScheduler no conoce la interfaz.
- La programación permanece sincronizada con la configuración almacenada.

El resultado es un flujo de datos simple, predecible y alineado con la arquitectura general de Noir Assistant.