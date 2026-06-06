# Noir Assistant — Task Repository

## Introducción

`OfflineTaskRepository` es la implementación principal de `TaskRepository`, responsable de administrar el ciclo de vida completo de las tareas dentro de Noir Assistant.

Además de proporcionar persistencia estructurada para tareas creadas localmente, el repository actúa como punto de integración entre el sistema de procesamiento de audio y el dominio de tareas.

TaskRepository actúa como consumidor final del pipeline materializando el procesamiento de audio y transcripción

Su responsabilidad consiste en:

- Crear tareas.
- Actualizar tareas.
- Administrar estados de completado.
- Gestionar papelera lógica.
- Mantener fechas y metadatos.
- Recuperar tareas pendientes.
- Transformar transcripciones procesadas por IA en tareas persistentes.

La UI y los ViewModels nunca interactúan directamente con:

- Room
- DAOs
- Formatos de respuesta del servicio de IA
- Protocolos de sincronización
- Repositories externos

Toda operación relacionada con tareas atraviesa `TaskRepository`.

## Dependencias

### TaskDao

Responsable de la persistencia estructurada de tareas mediante Room. Permite:

- Consultas reactivas.
- Inserciones.
- Actualizaciones.
- Eliminaciones.
- Gestión de papelera lógica.

---

### NetworkRepository

Responsable de recuperar desde la infraestructura remota las transcripciones estructuradas generadas por los servicios de IA.

---

### AudioRepository 

Responsable de coordinar el estado y ciclo de vida de las solicitudes de audio, incluyendo persistencia, ejecución de acciones y sincronización con procesamiento remoto. Permite:

- Marcar solicitudes como completadas.
- Marcar solicitudes fallidas.
- Mantener consistencia entre tareas generadas y audios procesados.

## Arquitectura General

TaskRepository coordina tres subsistemas principales:

| Componente        | Responsabilidad                                 |
|:------------------|:------------------------------------------------|
| TaskDao           | Persistencia estructurada de tareas             |
| NetworkRepository | Recuperación de transcripciones procesadas      |
| AudioRepository   | Coordinación del estado de solicitudes de audio |

TaskRepository actúa como fachada entre estos componentes exponiendo una interfaz centrada exclusivamente en el dominio de tareas.

## Funcionalidades

### Conversión de Modelos

TaskRepository expone exclusivamente modelos de dominio (`TaskDomain`).

La conversión entre entidades de persistencia (`TaskEntity`) y modelos de dominio es realizada mediante mappers especializados ubicados en `TaskMapper.kt`.

Estos mappers encapsulan las diferencias de representación entre la base de datos y el dominio, evitando que detalles internos de persistencia se propaguen fuera de la capa de datos.

Por ejemplo, valores centinela utilizados por la base de datos son transformados automáticamente a representaciones más expresivas dentro del dominio.

---

### Gestión de Tareas

Permite:

- Crear tareas.
- Actualizar tareas.
- Recuperar tareas activas.
- Recuperar tareas eliminadas.
- Eliminar permanentemente.
- Consultar tareas pendientes para una fecha específica.

Durante la creación y modificación se actualizan automáticamente las marcas temporales necesarias para mantener trazabilidad.

---

### Flujos Reactivos

Las tareas activas y eliminadas son expuestas mediante `Flow`.

Las actualizaciones son propagadas automáticamente por Room, permitiendo sincronización inmediata con la interfaz.

---

### Papelera Lógica

Las tareas implementan un mecanismo de eliminación lógica basado en expiración.

Cuando una tarea es enviada a la papelera:

- Permanece almacenada.
- Deja de formar parte de las consultas activas.
- Puede restaurarse posteriormente.
- Puede eliminarse automáticamente al expirar.

---

### Integración con el Procesamiento de Audio

#### Sincronización de Transcripciones

Una de las responsabilidades más especializadas del repository consiste en transformar resultados generados por IA en tareas persistentes. El flujo general es:

1. Recuperación de transcripciones estructuradas desde el servicio remoto (`NetworkRepository`)
2. Respuesta estructurada
3. Validación de protocolo
4. Conversión a TaskEntity
5. Persistencia local
6. Actualización del estado del audio

#### Validación del Protocolo de Transcripción

Antes de insertar cualquier tarea, TaskRepository valida que la estructura recibida cumpla el protocolo definido por Noir Assistant.

Cada transcripción debe incluir:

- Validación de compatibilidad de protocolo.
- Título.
- Descripción.
- Fecha.
- Identificador del audio asociado.

Las respuestas incompatibles son descartadas automáticamente.

<small>El header actúa como mecanismo de validación del protocolo Noir Assistant y permite descartar respuestas provenientes de formatos incompatibles o versiones no soportadas.</small>

#### Coordinación con AudioRepository

Cuando una transcripción es procesada correctamente:

- La tarea es almacenada.
- El audio asociado se marca como completado.
- Se establece su expiración automática.

Si ocurre un error:

- La tarea no se inserta.
- El audio asociado se marca como fallido.

Esto garantiza consistencia entre ambos dominios.

#### Recuperación ante Errores

El procesamiento de transcripciones es tolerante a fallos parciales.
Si un elemento individual contiene errores:

- Se descarta únicamente dicho elemento.
- El procesamiento continúa con el resto de resultados válidos.

Esto evita que una transcripción corrupta impida procesar el resto de respuestas disponibles.

<small>Los errores detectados en una transcripción individual no interrumpen el procesamiento del resto de elementos disponibles.</small>

## Filosofía de Diseño

TaskRepository fue diseñado como una capa de persistencia e integración para el dominio de tareas.

Además de abstraer completamente la infraestructura de persistencia, coordina la transformación de resultados generados por servicios externos en estructuras de dominio persistentes.

Su objetivo consiste en presentar una interfaz estable para el resto de la aplicación mientras mantiene consistencia entre:

- Persistencia local.
- Procesamiento remoto.
- Solicitudes de audio.
- Estados reactivos.
- Metadatos temporales.