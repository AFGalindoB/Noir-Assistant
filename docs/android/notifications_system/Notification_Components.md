# Noir Assistant — Componentes del Sistema de Notificaciones

## Introducción

El sistema de recordatorios de Noir Assistant está construido sobre una arquitectura desacoplada donde cada componente participa en una etapa específica del ciclo de vida de una notificación.

El objetivo principal de esta separación consiste en garantizar que la programación temporal, el procesamiento de datos y la presentación visual permanezcan independientes entre sí, permitiendo que cada capa evolucione sin introducir dependencias innecesarias sobre el resto del sistema.

La arquitectura se apoya sobre componentes nativos de Android como `AlarmManager`, `BroadcastReceiver` y `WorkManager`, complementados por la infraestructura interna de persistencia y gestión de dependencias de la aplicación.

---

### Arquitectura General

El flujo completo de un recordatorio puede resumirse de la siguiente forma:

```text
Preferencias Persistidas
           ↓
     AlarmScheduler
           ↓
      AlarmManager
           ↓
     AlarmReceiver
           ↓
     ReminderWorker
           ↓
   NotificationHelper
           ↓
      Notificación
```
Cuando el dispositivo reinicia, `BootReceiver` participa restaurando la programación previamente configurada.

## AlarmScheduler

`AlarmScheduler` es el responsable de traducir la configuración de recordatorios almacenada por el usuario en alarmas reales programadas dentro del sistema Android.

Actúa como la única capa autorizada para interactuar directamente con AlarmManager, encapsulando toda la lógica relacionada con fechas, horarios y mecanismos de programación.

---

### Responsabilidades

- Interpretar la hora configurada por el usuario.
- Calcular la siguiente ejecución válida.
- Registrar alarmas en Android.
- Evitar programaciones duplicadas.
- Adaptar dinámicamente la estrategia de programación según el estado energético del dispositivo.

---

### Programación Adaptativa

La implementación actual utiliza dos mecanismos distintos dependiendo del contexto del sistema:


| Condición                                          | Estrategia                            |
| -------------------------------------------------- | ------------------------------------- |
| Optimizaciones de batería deshabilitadas           | `AlarmManager.setAlarmClock()`        |
| Optimizaciones activas o restricciones del sistema | `AlarmManager.setAndAllowWhileIdle()` |

Esta estrategia permite priorizar precisión cuando el entorno lo permite sin sacrificar compatibilidad en dispositivos más restrictivos.

---

### Reprogramación Automática

Si la hora configurada ya fue superada durante el día actual, el scheduler desplaza automáticamente la ejecución al siguiente día disponible.

Esto garantiza que siempre exista una próxima alarma válida independientemente del momento en que se inicie la aplicación o se modifique la configuración.

## AlarmReceiver

`AlarmReceiver` constituye el punto de entrada del flujo cuando Android dispara una alarma previamente registrada.

Su responsabilidad se limita exclusivamente a reaccionar ante el evento y transferir el procesamiento a una infraestructura más adecuada para operaciones de mayor duración.

---

### Flujo de Activación

Cuando Android ejecuta la alarma:

1. El sistema despierta el receiver.
2. El receiver valida la acción recibida.
3. Se crea una solicitud de trabajo para `ReminderWorker`.
4. WorkManager continúa el procesamiento.

---

### Filosofía de Diseño

El receiver fue diseñado como un componente extremadamente liviano.

No realiza:

- Consultas a base de datos.
- Acceso a almacenamiento.
- Generación de notificaciones.
- Procesamiento de lógica de negocio.

Esta decisión minimiza riesgos asociados a las restricciones temporales impuestas por Android sobre los `BroadcastReceiver`.

## ReminderWorker

`ReminderWorker` onstituye el núcleo lógico del sistema de recordatorios.

Implementado mediante `CoroutineWorker`, es responsable de analizar el estado actual de las tareas y generar la información que finalmente será presentada al usuario.

---

### Responsabilidades

- Recuperar tareas pendientes desde el repositorio.
- Identificar tareas correspondientes al día actual.
- Detectar tareas vencidas.
- Construir mensajes contextuales.
- Solicitar la creación de la notificación final.

---

### Clasificación Inteligente de Tareas

Durante la ejecución, las tareas pendientes se agrupan según su contexto temporal:

| Categoría       | Descripción                            |
| --------------- | -------------------------------------- |
| Tareas del día  | Actividades programadas para hoy       |
| Tareas vencidas | Actividades cuya fecha ya fue superada |

Esta clasificación permite construir mensajes más relevantes que una simple notificación genérica.

#### Generación Dinámica de Mensajes

Dependiendo del estado actual de la agenda, el sistema puede producir mensajes como:

- Una tarea específica para hoy.
- Varias tareas pendientes para el día.
- Recordatorios sobre tareas atrasadas.
- Combinaciones de tareas actuales y vencidas.

De esta forma la notificación refleja la situación real del usuario en el momento exacto de ejecución.

---

### Recuperación ante Fallos

Si ocurre un error durante el procesamiento, WorkManager puede reintentar automáticamente la ejecución.

Esto permite que WorkManager reprograme automáticamente la ejecución utilizando sus mecanismos internos de recuperación.

## NotificationHelper

`NotificationHelper` centraliza toda la construcción visual de notificaciones.

Ningún otro componente del sistema interactúa directamente con las APIs de notificaciones de Android.

---

### Responsabilidades

* Crear canales de notificación.
* Configurar prioridad y comportamiento.
* Construir notificaciones consistentes.
* Aplicar la identidad visual de Noir Assistant.

---

### Características

Las notificaciones generadas utilizan:

* Canal de alta importancia.
* Soporte para vibración.
* Categoría de recordatorio.
* Iconografía oficial de la aplicación.
* Configuración homogénea para toda la plataforma.

---

### Beneficios

Centralizar esta responsabilidad permite modificar la experiencia visual sin alterar la lógica de programación ni el procesamiento de tareas.

## BootReceiver

Android elimina las alarmas programadas después de un reinicio del dispositivo.

`BootReceiver` existe para reconstruir automáticamente la programación persistida por el usuario.

---

### Flujo de Recuperación

Después de un reinicio:

1. Android emite un evento de arranque.
2. `BootReceiver` recibe el broadcast.
3. Se recuperan las preferencias persistidas mediante la infraestructura de dependencias de la aplicación.
4. Se solicita una nueva programación a `AlarmScheduler`.

---

### Beneficios

Gracias a este mecanismo los recordatorios sobreviven a:

- Reinicios completos.
- Apagados del dispositivo.
- Actualizaciones del sistema.
- Limpieza de memoria por parte del fabricante.

Todo ello sin necesidad de mantener servicios residentes ejecutándose permanentemente en segundo plano.

## Filosofía General de la Arquitectura

El sistema sigue una estrategia basada en responsabilidad única y desacoplamiento funcional.

| Componente         | Responsabilidad Principal   |
| ------------------ | --------------------------- |
| AlarmScheduler     | Programación temporal       |
| AlarmReceiver      | Activación del flujo        |
| ReminderWorker     | Procesamiento de negocio    |
| NotificationHelper | Presentación visual         |
| BootReceiver       | Recuperación tras reinicios |

La combinación de estos componentes permite construir una infraestructura de recordatorios compatible con Android moderno, resiliente ante interrupciones y preparada para futuras extensiones sin comprometer la mantenibilidad del sistema.