# Noir Assistant — Decisiones de Ingeniería del Sistema de Recordatorios

## Introducción

Este documento describe las principales decisiones arquitectónicas tomadas durante el diseño del sistema de recordatorios de Noir Assistant.

A diferencia de la documentación de componentes o flujo de datos, el objetivo aquí no es describir cómo funciona la implementación, sino explicar las razones que llevaron a elegir la arquitectura actual y las alternativas que fueron descartadas.

Comprender estas decisiones facilita el mantenimiento futuro del sistema y permite evaluar posibles cambios sin perder de vista los requisitos originales de diseño.

## Objetivos de Diseño

Antes de seleccionar tecnologías o componentes específicos, se definieron varios requisitos funcionales y operativos:

- Mantener precisión razonable en la hora de ejecución.
- Sobrevivir a reinicios del dispositivo.
- Funcionar bajo Doze Mode.
- Mantener compatibilidad con Android moderno.
- Evitar servicios permanentes en segundo plano.
- Minimizar consumo energético.
- Permitir recuperación automática ante errores.
- Mantener una arquitectura desacoplada y mantenible.

La arquitectura final fue construida para satisfacer simultáneamente todos estos requisitos.

## Evaluación de Alternativas

### Uso Exclusivo de WorkManager

`WorkManager` fue una de las primeras alternativas consideradas.

La tecnología proporciona:

- Persistencia automática.
- Reintentos ante fallos.
- Compatibilidad con reinicios.
- Integración con restricciones del sistema.

Sin embargo presenta una limitación importante para este caso de uso:

WorkManager no garantiza ejecución exacta en una hora determinada.

Android puede retrasar la ejecución para optimizar batería, agrupar tareas o aplicar restricciones relacionadas con Doze Mode.

Para tareas periódicas o diferibles esto suele ser aceptable.

Para recordatorios diarios asociados a una hora específica no resulta ideal.

#### Decisión

WorkManager se utiliza únicamente para procesamiento posterior al disparo de la alarma y no como mecanismo principal de programación temporal.

### Uso Exclusivo de AlarmManager

`AlarmManager` proporciona capacidades de programación temporal mucho más precisas.

Permite solicitar la ejecución de eventos en momentos concretos y ofrece integración directa con mecanismos especiales como:

- Alarmas exactas.
- Alarmas compatibles con Doze.
- Alarmas visibles para el sistema.

Sin embargo presenta varias limitaciones:

- No fue diseñado para procesamiento complejo.
- Los BroadcastReceiver poseen ventanas de ejecución reducidas.
- Operaciones largas pueden ser interrumpidas.
- No ofrece mecanismos avanzados de recuperación ante errores.

Delegar toda la lógica de negocio directamente a AlarmManager habría incrementado significativamente la fragilidad del sistema.

#### Decisión

AlarmManager se utiliza exclusivamente para garantizar la activación temporal del flujo.

Todo procesamiento posterior es delegado a WorkManager.

## Arquitectura Híbrida Elegida

La solución final combina varias tecnologías donde cada una resuelve un problema específico.

| Componente      | Responsabilidad             |
| --------------- | --------------------------- |
| `AlarmManager`  | Programación temporal       |
| `AlarmReceiver` | Recepción del evento        |
| `WorkManager`   | Procesamiento resiliente    |
| `Room`          | Persistencia de tareas      |
| `BootReceiver`  | Recuperación tras reinicios |

Ningún componente intenta resolver responsabilidades ajenas a su propósito principal.

Esta separación reduce complejidad y mejora la mantenibilidad del sistema.

## ¿Por Qué Utilizar un BroadcastReceiver Intermedio?

Una alternativa posible habría sido ejecutar la lógica de negocio directamente desde el evento recibido por AlarmManager.

Esta aproximación fue descartada porque Android impone restricciones severas al tiempo de vida de los BroadcastReceiver.

Mantener un receiver liviano aporta varias ventajas:

- Menor riesgo de ANR.
- Menor riesgo de cancelaciones.
- Menor dependencia del ciclo de vida del proceso.
- Mejor compatibilidad con versiones recientes de Android.

Por esta razón el receiver actúa únicamente como puente hacia WorkManager.

## ¿Por Qué Reprogramar desde Datos Persistidos?

La arquitectura evita almacenar estados temporales relacionados con la programación de recordatorios fuera de las preferencias persistidas.

La configuración almacenada por el usuario actúa como única fuente de verdad.

Esto permite:

- Reconstruir el sistema tras reinicios.
- Recuperar programación perdida.
- Evitar inconsistencias entre UI y sistema.
- Simplificar la sincronización de configuración.

La programación puede ser reconstruida en cualquier momento únicamente a partir de los datos persistidos.

## Beneficios de la Arquitectura Actual

La combinación de componentes elegida permite obtener:

- Precisión temporal adecuada para recordatorios diarios.
- Compatibilidad con Doze Mode.
- Persistencia tras reinicios.
- Recuperación automática ante errores.
- Bajo consumo energético.
- Ausencia de servicios permanentes.
- Separación clara de responsabilidades.
- Escalabilidad para futuras funcionalidades.
- Compatibilidad con Android moderno.

## Trade-offs Aceptados

Como toda decisión de ingeniería, la arquitectura actual implica ciertos compromisos.

### Mayor Complejidad

La solución involucra múltiples componentes coordinados entre sí.

Esto incrementa ligeramente la complejidad conceptual frente a una implementación basada en una única tecnología.

### Dependencia del Fabricante

Algunos fabricantes aplican restricciones agresivas sobre procesos en segundo plano.

Aunque la arquitectura minimiza este problema, ningún mecanismo disponible en Android puede garantizar comportamiento idéntico en todos los dispositivos.

### Precisión Variable Bajo Restricciones Extremas

Cuando el sistema no permite utilizar alarmas exactas, se utilizan mecanismos compatibles con ahorro energético.

En estos escenarios pueden existir pequeñas desviaciones respecto a la hora programada.

Estos compromisos fueron considerados aceptables frente a los beneficios obtenidos en resiliencia y compatibilidad.

## Filosofía General

La arquitectura de recordatorios fue diseñada siguiendo un principio simple:

Cada componente debe resolver únicamente el problema para el que fue creado.

AlarmManager programa.

BroadcastReceiver activa.

WorkManager procesa.

Room persiste.

BootReceiver recupera.

Esta separación permite que el sistema permanezca robusto, mantenible y preparado para evolucionar sin necesidad de rediseñar la infraestructura completa.