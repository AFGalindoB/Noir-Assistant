# Noir Assistant — Inicialización del Sistema de Recordatorios

## Introducción

El sistema de recordatorios forma parte de la infraestructura global de Noir Assistant.

A diferencia de componentes asociados a pantallas específicas, los recordatorios deben permanecer sincronizados con la configuración persistida del usuario independientemente del estado actual de la interfaz o de la navegación activa.

Por esta razón, la inicialización del sistema ocurre durante el arranque de la aplicación y no dentro de una pantalla particular.

Su objetivo consiste en garantizar que la programación de recordatorios permanezca alineada con las preferencias almacenadas y con el estado actual del sistema operativo.

## Arquitectura de Inicialización

Durante el arranque participan varios componentes especializados.

| Componente           | Responsabilidad                                                  |
| -------------------- | ---------------------------------------------------------------- |
| `MainActivity`       | Coordina la inicialización del sistema.                          |
| `SettingsRepository` | Proporciona la configuración persistida de recordatorios.        |
| `AlarmScheduler`     | Programa la siguiente ejecución válida.                          |
| `GlobalStateManager` | Centraliza estados globales relacionados con permisos y sistema. |
| `AndroidManifest`    | Declara permisos y receptores requeridos por Android.            |

Cada componente cumple una responsabilidad específica y permanece desacoplado del resto de la infraestructura.

## Flujo de Inicialización

Durante el arranque de la aplicación se ejecuta el siguiente proceso:

```text
MainActivity
      ↓
SettingsRepository
      ↓
Hora de recordatorio persistida
      ↓
AlarmScheduler
      ↓
AlarmManager
```

La aplicación recupera automáticamente la configuración almacenada y solicita a `AlarmScheduler` recalcular la siguiente ejecución válida.

De esta forma cualquier modificación realizada previamente por el usuario permanece activa incluso después de:

* Reinicios de la aplicación.
* Reinicios del dispositivo.
* Actualizaciones de configuración.
* Restauración de estado.

La lógica de programación permanece completamente desacoplada de la interfaz de usuario.

## Sincronización de Configuración

La programación de recordatorios se encuentra directamente vinculada a las preferencias persistidas del usuario.

Cuando la hora configurada cambia:

1. La nueva configuración es almacenada mediante DataStore.
2. El valor actualizado es expuesto por `SettingsRepository`.
3. La capa de inicialización detecta el cambio.
4. `AlarmScheduler` recalcula la próxima ejecución válida.

Este enfoque garantiza que la configuración persistida actúe como única fuente de verdad para el sistema de recordatorios.

## Gestión de Permisos

El sistema requiere varios permisos proporcionados por Android para garantizar compatibilidad con las distintas versiones del sistema operativo.

| Permiso                                | Propósito                                                                               |
| -------------------------------------- | --------------------------------------------------------------------------------------- |
| `POST_NOTIFICATIONS`                   | Permite mostrar notificaciones en Android 13 y versiones posteriores.                   |
| `SCHEDULE_EXACT_ALARM`                 | Permite utilizar mecanismos de programación de alta precisión cuando están disponibles. |
| `RECEIVE_BOOT_COMPLETED`               | Permite restaurar la programación tras reinicios del dispositivo.                       |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Permite solicitar exclusión de restricciones energéticas agresivas.                     |

Todos estos permisos son declarados centralmente dentro del manifiesto de la aplicación.

## Permisos de Notificación

Desde Android 13 (API 33), las notificaciones requieren autorización explícita del usuario.

La verificación y seguimiento de este permiso se centralizan mediante la infraestructura global de la aplicación, evitando que pantallas individuales interactúen directamente con APIs del sistema operativo.

Este enfoque proporciona:

* Una única fuente de verdad para estados globales.
* Menor acoplamiento entre UI y plataforma.
* Mayor consistencia durante el ciclo de vida de la aplicación.
* Simplificación de la gestión de permisos en capas superiores.

## Filosofía de Diseño

La inicialización del sistema de recordatorios fue diseñada para garantizar que la programación permanezca sincronizada con la configuración persistida del usuario sin depender de pantallas específicas ni de interacciones manuales.

La responsabilidad de recuperación, sincronización y programación permanece concentrada en la infraestructura global de la aplicación, permitiendo que el resto de componentes interactúen con recordatorios sin conocer detalles relacionados con permisos, inicialización o APIs específicas de Android.
