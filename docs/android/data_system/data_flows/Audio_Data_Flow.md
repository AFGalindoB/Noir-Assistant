# Noir Assistant — Audio Data Flow

## Introducción

Este documento describe cómo fluye la información dentro del sistema de audio de Noir Assistant.

A diferencia de otros dominios de la aplicación, el sistema de audio no depende de una única fuente de datos. Cada solicitud de audio involucra simultáneamente:

* Persistencia estructurada mediante Room.
* Archivos físicos almacenados en el dispositivo.
* Componentes multimedia nativos de Android.
* Procesamiento remoto.
* Estados reactivos consumidos por la interfaz.

La coordinación de todos estos componentes se encuentra centralizada dentro de `AudioRepository`.

Las capas superiores nunca interactúan directamente con archivos físicos, reproductores multimedia, grabadores de audio o servicios remotos.

## Arquitectura General

El flujo principal puede representarse mediante la siguiente estructura:

```text
UI
 ↓
AudioViewModel
 ↓
AudioRepository
 ├── AudioRequestDao
 ├── SmartAudioFileManager
 └── NetworkRepository
```

AudioRepository actúa como fachada y coordinador entre todos los subsistemas involucrados en el ciclo de vida de los audios.

## Componentes del Sistema

### SmartAudioFileManager

Responsable de administrar la infraestructura multimedia local.

Incluye:

* Grabación de audio.
* Reproducción multimedia.
* Gestión de archivos físicos.
* Observación reactiva de almacenamiento.

---

### AudioRequestDao

Responsable de almacenar el estado persistente asociado a cada solicitud de audio.

Incluye:

* Estado de procesamiento.
* Fechas de creación.
* Fechas de expiración.
* Información de papelera.
* Relación con archivos físicos.

---

### NetworkRepository

Responsable de la comunicación con la infraestructura remota.

Permite:

* Enviar audios para procesamiento.
* Recuperar resultados procesados.
* Gestionar respuestas del servidor.

---

## Flujo de Lectura

La visualización de audios se construye a partir de la información persistida en Room.

### 1. Persistencia

AudioRequestDao expone flujos reactivos de solicitudes activas.

```text
Room
 ↓
Flow<List<AudioRequestEntity>>
```

---

### 2. AudioRepository

El repository transforma las entidades persistentes en modelos de dominio.

```text
AudioRequestEntity
 ↓
AudioDomain
```

Además aplica lógica relacionada con:

* Estados de procesamiento.
* Visibilidad de elementos.
* Consistencia entre persistencia y almacenamiento físico.

---

### 3. AudioViewModel

AudioViewModel transforma los flujos del repository en StateFlow observables por Compose.

Además puede aplicar transformaciones adicionales para la presentación.

Por ejemplo:

```text
AudioDomain
 ↓
Agrupación por sección
 ↓
Map<Int, List<AudioDomain>>
```

---

### 4. UI

La interfaz consume exclusivamente estados expuestos por AudioViewModel.

La UI nunca interactúa directamente con:

* Room.
* Archivos físicos.
* ExoPlayer.
* MediaRecorder.
* Servicios remotos.

---

## Flujo de Grabación

La creación de una nueva solicitud de audio combina persistencia física y persistencia estructurada.

### 1. UI

El usuario pulsa el botón de grabación.

---

### 2. AudioViewModel

La intención es delegada a AudioRepository.

---

### 3. AudioRepository

Solicita a SmartAudioFileManager iniciar la grabación.

---

### 4. SmartAudioFileManager

MediaRecorder crea un nuevo archivo de audio dentro del almacenamiento privado de la aplicación.

---

### 5. Finalización

Cuando la grabación concluye:

1. Se consolida el archivo físico.
2. Se crea un registro persistente en Room.
3. El nuevo audio aparece automáticamente en la interfaz mediante el flujo reactivo de lectura.

---

## Flujo de Reproducción

La reproducción es administrada completamente por SmartAudioFileManager mediante ExoPlayer.

### Estados Reactivos

AudioRepository expone:

* Audio actualmente reproducido.
* Duración total.
* Posición actual.
* Estado de reproducción.

Estos estados son consumidos por AudioViewModel y posteriormente observados por Compose.

### Control de Reproducción

La interfaz puede solicitar:

* Reproducir.
* Pausar.
* Avanzar.
* Retroceder.
* Buscar una posición específica.

Todas estas operaciones atraviesan AudioRepository antes de llegar a SmartAudioFileManager.

---

## Flujo de Procesamiento Remoto

El procesamiento remoto funciona mediante una cola persistente almacenada en Room.

### Creación de Solicitud

Cada audio nuevo genera una entrada persistente con estado:

```text
WAITING
```

---

### Procesamiento

Cuando el usuario solicita procesar audios pendientes:

1. AudioRepository recupera la siguiente solicitud pendiente.
2. Actualiza su estado a PROCESSING.
3. Verifica que el archivo exista físicamente.
4. Envía el audio al servidor mediante NetworkRepository.
5. Actualiza el estado resultante.

---

### Estados de Procesamiento

```text
WAITING
 ↓
PROCESSING
 ↓
SENT
```

o

```text
WAITING
 ↓
PROCESSING
 ↓
FAILED
```

---

### Recuperación de Fallos

Si ocurre un error:

* El estado cambia a FAILED.
* La cola permanece consistente.
* El procesamiento puede reintentarse posteriormente.

---

## Flujo de Papelera

La eliminación de audios utiliza un modelo de eliminación lógica.

### Envío a Papelera

Cuando un audio es eliminado:

1. El archivo físico permanece intacto.
2. Room actualiza el estado de la solicitud.
3. El audio desaparece de las consultas activas.

---

### Restauración

Mientras el registro exista en persistencia:

* El audio puede restaurarse inmediatamente.
* El archivo físico continúa disponible.

---

### Eliminación Permanente

La eliminación permanente elimina simultáneamente:

* El registro en Room.
* El archivo físico correspondiente.

Garantizando consistencia entre ambas fuentes.

---

## Auto-Reconciliación

Uno de los mecanismos fundamentales del sistema consiste en la conciliación automática entre Room y el almacenamiento físico.

AudioRepository observa continuamente los cambios reportados por SmartAudioFileManager.

Cuando detecta inconsistencias:

| Archivo   | Room      | Acción                          |
| --------- | --------- | ------------------------------- |
| Existe    | No existe | Crear registro faltante         |
| No existe | Existe    | Eliminar registro inconsistente |

Este mecanismo permite mantener sincronizadas ambas fuentes de información sin intervención manual.

---

## Expiración Automática

Los audios enviados a papelera poseen una fecha de expiración.

Cuando dicha fecha es alcanzada:

1. El archivo físico es eliminado.
2. La conciliación detecta la ausencia del archivo.
3. El registro persistente es eliminado automáticamente.

Esto evita duplicar lógica de limpieza y reutiliza el mismo mecanismo de consistencia utilizado por el sistema.

---

## Filosofía de Diseño

El sistema de audio fue diseñado como una infraestructura especializada capaz de coordinar múltiples mecanismos de almacenamiento y procesamiento bajo una única interfaz de dominio.

AudioRepository actúa como una capa de aislamiento que permite que la aplicación interactúe con audio mediante estados reactivos y operaciones de dominio sin depender de detalles relacionados con:

* Room.
* File API.
* ExoPlayer.
* MediaRecorder.
* Comunicación remota.

De esta manera el resto de la aplicación puede tratar el audio como un dominio coherente mientras la complejidad de infraestructura permanece completamente encapsulada.
