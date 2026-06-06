# Noir Assistant — Note Repository

## Introducción

`OfflineNoteRepository` es la implementación principal de `NoteRepository`, responsable de administrar la persistencia y el ciclo de vida de las notas dentro de Noir Assistant.

Su objetivo consiste en proporcionar una interfaz unificada para la creación, modificación, recuperación y eliminación de notas, desacoplando completamente a las capas superiores de los detalles de persistencia.

La UI y los ViewModels nunca interactúan directamente con:

- Room
- DAOs
- Entidades de persistencia
- Consultas SQL

Toda operación relacionada con notas atraviesa NoteRepository.

## Dependencias

`NoteDao`

Responsable de la persistencia estructurada de las notas mediante Room y de las operaciones CRUD asociadas al ciclo de vida de los registros.

## Arquitectura General

NoteRepository coordina un único subsistema de persistencia:

| Componente | Responsabilidad                    |
|:-----------|:-----------------------------------|
| NoteDao    | Persistencia estructurada de notas |

Aunque internamente utiliza Room, las capas superiores interactúan exclusivamente mediante modelos de dominio (`NoteDomain`).

Esto evita que detalles de infraestructura se propaguen fuera de la capa de datos.

<small>Aunque internamente depende únicamente de NoteDao, el repository continúa siendo responsable de coordinar la lógica asociada al ciclo de vida de las notas, incluyendo transformación de modelos, gestión de expiración y mantenimiento de metadatos temporales.</small>

## Funcionalidades

### Conversión de Modelos

NoteRepository expone exclusivamente modelos de dominio (`NoteDomain`).

La conversión entre entidades de persistencia (`NoteEntity`) y modelos de dominio es realizada mediante mappers especializados ubicados en `NoteMapper.kt`.

Estos mappers encapsulan las diferencias de representación entre la base de datos y el dominio, evitando que detalles internos de persistencia se propaguen fuera de la capa de datos.

Por ejemplo, valores centinela utilizados por la base de datos son transformados automáticamente a representaciones más expresivas dentro del dominio.

---

### Gestión de Notas

El repository proporciona operaciones para administrar el contenido de las notas:

- Creación de nuevas notas.
- Actualización de notas existentes.
- Recuperación de notas activas.
- Eliminación permanente.

Durante la creación y modificación se actualizan automáticamente las marcas temporales necesarias para mantener trazabilidad sobre los cambios realizados.

---

### Flujos Reactivos

NoteRepository expone las consultas mediante Flow, permitiendo que las capas consumidoras observen cambios de forma reactiva. La propagación de actualizaciones es gestionada por la infraestructura de persistencia subyacente.

Las notas activas y eliminadas son expuestas mediante Flow.

Las actualizaciones son propagadas automáticamente por la infraestructura de persistencia subyacente reflejandose inmediatamente en la interfaz sin necesidad de consultas manuales adicionales.

### Papelera Lógica

Las notas implementan un mecanismo de eliminación lógica basado en expiración.

Cuando una nota es enviada a la papelera no se mueve a una estructura de almacenamiento diferente:

- El registro permanece almacenado.
- El mecanismo de papelera se implementa mediante una marca temporal de expiración asociada al registro.
- La nota deja de formar parte de las consultas activas.

Posteriormente puede:

- Ser restaurada.
- Ser eliminada automáticamente al expirar.

<small>A diferencia de otros dominios del sistema que utilizan estados explícitos para representar elementos eliminados, las notas implementan la papelera mediante una fecha de expiración asociada al registro.</small>

#### Restauración de Notas

Las notas enviadas a la papelera pueden recuperarse antes de alcanzar su fecha de expiración.

La restauración elimina el estado de eliminación lógica y devuelve la nota al conjunto de registros activos.

#### Limpieza Automática

NoteRepository proporciona mecanismos para eliminar registros cuya fecha de expiración ya fue alcanzada.

Este proceso permite:

- Liberar espacio de almacenamiento.
- Mantener la base de datos limpia.
- Garantizar que la papelera funcione como almacenamiento temporal y no permanente.

---

### Gestión Temporal

Cada nota mantiene información temporal asociada a su ciclo de vida:

- Fecha de creación.
- Fecha de última modificación.
- Fecha de expiración en papelera.

Estas marcas temporales son gestionadas automáticamente por el repository y permiten mantener trazabilidad sobre los cambios realizados.

Las capas superiores no administran directamente estas marcas temporales.

El repository utiliza DateUtils para generar y administrar las marcas temporales asociadas al ciclo de vida de cada nota.

---

### Ejecución en Segundo Plano

Las operaciones de escritura y mantenimiento de persistencia son ejecutadas fuera del hilo principal de Android.

Esto evita bloqueos durante operaciones de actualización, eliminación o mantenimiento de la base de datos.

La estrategia de concurrencia permanece encapsulada dentro del repository, permitiendo que las capas superiores consuman la API sin preocuparse por detalles de ejecución.

## Filosofía de Diseño

NoteRepository fue diseñado como una capa de persistencia especializada para el dominio de notas.

Su objetivo consiste en centralizar todas las operaciones relacionadas con almacenamiento y recuperación de información escrita, proporcionando una interfaz de dominio estable mientras abstrae completamente los detalles de Room y de la estructura interna de la base de datos.

Actúa como una barrera de aislamiento entre la lógica de negocio y la infraestructura de persistencia, garantizando consistencia, trazabilidad y desacoplamiento entre capas.