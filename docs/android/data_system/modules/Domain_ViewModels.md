# Noir Assistant — Domain ViewModels

## Introducción

Los ViewModels de Noir Assistant constituyen la capa de adaptación entre la interfaz de usuario y el dominio de la aplicación.

Su responsabilidad consiste en transformar interacciones de UI en operaciones de dominio consumibles por los repositories correspondientes, manteniendo un modelo reactivo basado en StateFlow.

Los ViewModels no implementan persistencia ni lógica de infraestructura.

Estas responsabilidades permanecen encapsuladas dentro de repositories y managers especializados.

## Responsabilidades Comunes

Todos los ViewModels del sistema comparten un conjunto de responsabilidades comunes:

### Exposición de Estado Reactivo

Los datos consumidos por la interfaz son expuestos mediante StateFlow.

Esto permite:

- Observación reactiva.
- Sincronización automática con Compose.
- Persistencia temporal de estado durante cambios de configuración.

---

### Coordinación de Operaciones de Dominio

Los ViewModels reciben acciones provenientes de la interfaz y las traducen en operaciones sobre repositories.

Por ejemplo:

- Crear entidades.
- Actualizar entidades.
- Restaurar elementos.
- Eliminar elementos.
- Ejecutar procesos de sincronización.

---

### Gestión de Coroutines

Las operaciones asíncronas son ejecutadas mediante viewModelScope.

Esto garantiza:

- Cancelación automática.
- Integración con el ciclo de vida.
- Aislamiento de concurrencia respecto a la interfaz.

## ViewModels Especializados

### TaskViewModel

Responsable del dominio de tareas.

Permite:

- Exponer tareas organizadas mediante filtros y agrupaciones por sección.
- Crear tareas.
- Actualizar tareas.
- Gestionar estados de completado.
- Enviar tareas a eliminación lógica.
- Restaurar tareas recientemente eliminadas.
- Recuperar tareas procesadas desde audio.

### NoteViewModel

Responsable del dominio de notas.

Permite:

- Crear notas.
- Actualizar notas.
- Enviar notas a eliminación lógica.
- Restaurar notas recientemente eliminadas.

### AudioViewModel

Responsable del dominio de audio.

Coordina:

- Grabación.
- Reproducción.
- Control de reproducción y posicionamiento temporal de audio.
- Procesamiento remoto.
- Gestión de solicitudes pendientes.
- Enviar solicitudes de audio a eliminación lógica.
- Restaurar solicitudes eliminadas.

### SettingsViewModel

Responsable de configuración y autenticación.

Permite:

- Administrar preferencias.
- Actualizar configuración persistente.
- Gestionar autenticación mediante QR.
- Coordinar validaciones de infraestructura.
- Exponer estado global de conexión.

### TrashViewModel

Responsable de la administración unificada de elementos eliminados.

Permite:

- Consultar papelera de tareas.
- Consultar papelera de notas.
- Consultar papelera de audios.
- Restaurar elementos.
- Eliminar permanentemente.
- Administrar reingresos a papelera.

Actúa como adaptador de presentación para TrashRepository.

## Integración con GlobalStateManager

Algunos ViewModels colaboran con GlobalStateManager para exponer información compartida por toda la aplicación.

Entre los estados compartidos se encuentran:

- Estado del servidor.
- Estado de autenticación.
- Disponibilidad de infraestructura.
- Mensajes globales.

Esto evita duplicar lógica de estado entre dominios independientes.

<small>No todos los ViewModels dependen de GlobalStateManager. Únicamente aquellos dominios que requieren información global relacionada con conectividad, autenticación o estado de infraestructura consumen dichos estados compartidos.</small>

## Eliminación Lógica y Restauración Inmediata

Los ViewModels de dominio no administran directamente la papelera global del sistema.

Cuando un elemento es eliminado:

- El repository marca la entidad para eliminación lógica.
- La entidad continúa existiendo en persistencia.
- Mientras la referencia al elemento permanezca disponible en la interfaz, el ViewModel puede solicitar su restauración inmediata sin necesidad de consultar nuevamente la papelera global.

La exploración y administración completa de elementos eliminados es responsabilidad de TrashViewModel y TrashRepository.

## Filosofía de Diseño

Los ViewModels de Noir Assistant fueron diseñados como una capa de adaptación entre la interfaz y el dominio.

Su objetivo consiste en mantener la UI libre de lógica de acceso a datos, infraestructura y coordinación de estados complejos, delegando dichas responsabilidades a repositories y managers especializados.

Actúan como intermediarios reactivos entre la presentación y el dominio, proporcionando una API estable orientada exclusivamente a las necesidades de la interfaz.

Los ViewModels de Noir Assistant fueron diseñados como adaptadores del dominio hacia la interfaz, manteniendo una separación estricta entre presentación, coordinación de estado y lógica de negocio.