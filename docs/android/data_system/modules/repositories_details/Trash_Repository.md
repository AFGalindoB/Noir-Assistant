# Noir Assistant — Trash Repository

## Introducción

`OfflineTrashRepository` es la implementación principal de `TrashRepository`, responsable de proporcionar una interfaz unificada para la administración de elementos eliminados dentro de Noir Assistant.

A diferencia del resto de repositories del sistema, `TrashRepository` no posee persistencia propia ni administra un dominio independiente.

Su responsabilidad consiste en coordinar los mecanismos de papelera implementados por otros repositories especializados y exponerlos mediante una API común.

Permite administrar:

- Tareas eliminadas.
- Notas eliminadas.
- Solicitudes de audio eliminadas.

La UI y los ViewModels nunca interactúan directamente con los mecanismos de papelera individuales implementados por cada dominio.

Toda operación relacionada con la papelera atraviesa `TrashRepository`.

## Dependencias

### TaskRepository

Responsable de la administración de tareas eliminadas.

Permite:

- Consultar tareas en papelera.
- Restaurar tareas.
- Reprogramar expiraciones.
- Eliminar permanentemente.

---

### NoteRepository

Responsable de la administración de notas eliminadas.

Permite:

- Consultar notas en papelera.
- Restaurar notas.
- Reprogramar expiraciones.
- Eliminar permanentemente.

---

### AudioRepository

Responsable de la administración de solicitudes de audio eliminadas.

Permite:

- Consultar audios en papelera.
- Restaurar audios.
- Reprogramar expiraciones.
- Eliminar permanentemente.

## Arquitectura General

TrashRepository coordina múltiples dominios independientes:

| Componente      | Responsabilidad    |
|:----------------|:-------------------|
| TaskRepository  | Papelera de tareas |
| NoteRepository  | Papelera de notas  |
| AudioRepository | Papelera de audio  |

TrashRepository actúa como una fachada que unifica estas capacidades bajo una única interfaz de dominio. No replica ni almacena información propia. Todas las operaciones son delegadas al repository propietario del dominio correspondiente.

Por esta razón TrashRepository no implementa lógica de negocio relacionada con papelera. Su responsabilidad consiste exclusivamente en centralizar y exponer capacidades ya existentes en otros repositories.

## Funcionalidades

### Recuperación de Elementos Eliminados

Permite consultar de forma reactiva los elementos presentes en la papelera de cada dominio:

- Tareas eliminadas.
- Notas eliminadas.
- Audios eliminados.

Las consultas continúan siendo administradas por los repositories propietarios de cada dominio.

TrashRepository únicamente centraliza el acceso.

---

### Restauración

Permite restaurar elementos previamente enviados a la papelera.

La restauración es delegada al repository propietario del dominio correspondiente.

---

### Reingreso a Papelera

Permite volver a enviar elementos restaurados a la papelera estableciendo una nueva fecha de expiración.

Esta capacidad resulta útil cuando un elemento recuperado debe regresar nuevamente al estado de eliminación lógica.

---

### Eliminación Permanente

Permite eliminar definitivamente elementos presentes en la papelera.

La operación es delegada al repository responsable de cada dominio.

Dependiendo del tipo de elemento esto puede implicar:

- Eliminación de registros persistentes.
- Eliminación de archivos físicos.
- Limpieza de metadatos asociados.

## Filosofía de Diseño

TrashRepository fue diseñado como una capa de agregación para los mecanismos de papelera del sistema.

Su objetivo consiste en proporcionar un único punto de acceso para la administración de elementos eliminados sin duplicar lógica ni replicar infraestructura ya existente en otros repositories.

Actúa como una fachada (Facade) que unifica múltiples dominios relacionados con elementos eliminados bajo una única interfaz de acceso. Esto permite que las capas superiores interactúen con la papelera como una característica unificada, mientras los detalles específicos de cada dominio permanecen encapsulados dentro de sus repositories especializados.