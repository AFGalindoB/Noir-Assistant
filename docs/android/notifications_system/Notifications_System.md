# Noir Assistant — Sistema de Notificaciones

## Introducción

El sistema de notificaciones de Noir Assistant es el encargado de gestionar la programación, ejecución y presentación de recordatorios dentro de la aplicación.

Su diseño sigue la misma filosofía arquitectónica utilizada en el resto del proyecto:

- Persistencia como fuente única de verdad.
- Componentes desacoplados.
- Flujo de datos reactivo.
- Recuperación automática ante fallos.
- Compatibilidad con las restricciones modernas de Android.

La infraestructura fue diseñada para proporcionar recordatorios confiables sin depender de servicios permanentes en segundo plano, manteniendo un equilibrio entre precisión temporal, consumo energético y resiliencia operativa.

## Objetivos del Sistema

El sistema de recordatorios busca garantizar que las notificaciones continúen funcionando de forma consistente incluso ante situaciones como:

- Reinicios del dispositivo.
- Reinicios de la aplicación.
- Cambios de configuración.
- Doze Mode.
- Restricciones energéticas del sistema.
- Interrupciones temporales de procesos.

Para lograrlo, la arquitectura combina mecanismos nativos de Android junto con la infraestructura de persistencia de Noir Assistant.

## Arquitectura General

A nivel conceptual, el flujo completo de un recordatorio puede representarse de la siguiente manera:

```text
Configuración Persistida
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

En caso de reinicio del dispositivo, `BootReceiver` participa restaurando automáticamente la programación previamente configurada.

## Organización de la Documentación

La documentación del sistema se encuentra dividida en varios documentos especializados, cada uno enfocado en un aspecto concreto de la arquitectura.

| Documento                                                                          | Descripción                                                                                            |
| ---------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------ |
| [`Notification_Initialization.md`](./Notification_Initialization.md)               | Describe el proceso de inicialización, sincronización y gestión de permisos.                           |
| [`Notification_Data_Flow.md`](./Notification_Data_Flow.md)                         | Explica el recorrido de los datos desde la configuración del usuario hasta la programación de alarmas. |
| [`Notification_Components.md`](./Notification_Components.md)                       | Documenta los componentes principales del sistema y sus responsabilidades.                             |
| [`Notification_Engineering_Decisions.md`](./Notification_Engineering_Decisions.md) | Expone las decisiones arquitectónicas, alternativas evaluadas y criterios de diseño.                   |

## Principios Arquitectónicos

El sistema fue construido siguiendo varios principios fundamentales:

### Persistencia como Fuente de Verdad

La configuración de recordatorios se almacena de forma persistente y constituye la única fuente de verdad para la programación del sistema.

### Sincronización Reactiva

Las alarmas no se sincronizan manualmente. Los cambios en la configuración se propagan automáticamente mediante observación de estado.

### Responsabilidad Única

Cada componente posee una función claramente delimitada:

- Programar.
- Activar.
- Procesar.
- Presentar.
- Recuperar.

### Recuperación Automática

La infraestructura está preparada para reconstruir su estado operativo utilizando únicamente la información persistida por el usuario.

## Consideraciones Futuras

La arquitectura actual permite evolucionar fácilmente hacía:

- Múltiples recordatorios simultáneos
- Recordatorios recurrentes complejos
- Notificaciones enriquecidas
- Acciones rápidas desde notificación

La separación entre programación, procesamiento y presentación facilita futuras expansiones sin comprometer estabilidad.