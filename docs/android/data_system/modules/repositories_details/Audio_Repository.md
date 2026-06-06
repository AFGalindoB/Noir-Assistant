# Noir Assistant — Audio Repository

## Introducción

`OfflineAudioRepository` es la implementación principal de `AudioRepository`, responsable de coordinar el ciclo de vida completo de los recursos de audio dentro de Noir Assistant.

A diferencia de otros repositories orientados principalmente a persistencia estructurada, `AudioRepository` integra múltiples fuentes de datos y recursos especializados del sistema.

Su responsabilidad consiste en actuar como punto único de acceso para:

- Grabación de audio.
- Reproducción multimedia.
- Gestión de archivos físicos.
- Procesamiento remoto.
- Administración de estados.
- Recuperación de inconsistencias.
- Gestión del ciclo de vida de solicitudes de audio.

La UI y los ViewModels nunca interactúan directamente con archivos físicos, reproductores multimedia o componentes de red.

Toda operación relacionada con audio atraviesa `AudioRepository`.

## Dependencias

### `SmartAudioFileManager`

Responsable de:

- Grabación de audio.
- Reproducción multimedia.
- Gestión de archivos físicos.
- Descubrimiento de archivos disponibles.

---

### `AudioRequestDao`

Responsable de la persistencia estructurada de solicitudes de audio mediante Room. Almacena:

- Estado del procesamiento.
- Fechas de creación.
- Fechas de expiración.
- Relación entre registros y archivos físicos.

---

### `NetworkRepository`

Encargado de la comunicación con la infraestructura remota.

Permite:

- Envío de audios.
- Recuperación de resultados procesados.
- Validación de respuestas.

---

### Application Scope

Utilizado para ejecutar procesos reactivos de larga duración asociados al ciclo de vida de la aplicación.

## Arquitectura General

AudioRepository coordina cuatro subsistemas principales:

| Componente            | Responsabilidad                                      |
|:----------------------|:-----------------------------------------------------|
| SmartAudioFileManager | Grabación, reproducción y gestión física de archivos |
| AudioRequestDao       | Persistencia estructurada y estados de procesamiento |
| NetworkRepository     | Comunicación con servicios remotos                   |
| CoroutineScope        | Procesos reactivos de larga duración                 |

<small>Ninguno de estos componentes es consumido directamente por capas superiores. AudioRepository actúa como fachada y coordinador entre todos ellos.</small>

## Funcionalidades

### Grabación de Audio

- Permite conocer si existe una grabación activa mediante el estado reactivo `isRecording`
- Iniciar una nueva captura de audio
- Finalizar la grabación en proceso y devolver el nombre del archivo generado.

La UI nunca interactúa directamente con rutas físicas o mecanismos de almacenamiento.

### Reproducción Multimedia

Permite sincronizar automáticamente la interfaz con el estado actual del reproductor multimedia exponiendo valores como:

- Progreso de reproducción.
- Duración total del audio.
- Nombre del archivo actualmente reproducido.

AudioRepository expone operaciones de reproducción:

- Reproducir/Pausar
- Avanzar o Retroceder

Estas operaciones permiten controlar la reproducción sin exponer detalles internos de ExoPlayer.

### Auto-Reconciliación de Estado

Uno de los mecanismos más importantes del repository es el sistema de conciliación automática.

Durante su inicialización el repository observa continuamente los archivos disponibles con el objetivo de mantener sincronizados:

- Archivos físicos.
- Registros almacenados en Room.

**Correcciones Automáticas**

| Archivo   | Room      | Efecto                                                           |
|:----------|:----------|:-----------------------------------------------------------------|
| Existe    | No Existe | El repository crea automáticamente el registro faltante.         |
| No Existe | Existe    | El repository elimina automáticamente el registro inconsistente. |

Esto garantiza que ambas fuentes permanezcan sincronizadas.

La conciliación se ejecuta de forma reactiva observando continuamente los cambios reportados por `SmartAudioFileManager`.

De esta forma cualquier modificación realizada sobre el almacenamiento físico puede reflejarse automáticamente en la persistencia estructurada.

### Borrado Lógico y Borrado Permanente

Los audios pueden ser administrados de forma lógica mediante:

- Enviar a la papelera mediante una transición de estado persistente.
- Restaurar elementos previamente enviados a la papelera.

O eliminarlos de manera permanente eliminando:

- Archivo físico.
- Registro persistente.

Garantizando consistencia entre ambas fuentes.

### Expiración Automática

AudioRepository implementa un mecanismo de limpieza automática. Su responsabilidad consiste en eliminar archivos cuya fecha de expiración ya fue alcanzada lo que evita acumulación innecesaria de datos en almacenamiento.

Cuando un audio expira:

1. El archivo físico es eliminado.
2. El mecanismo de conciliación detecta la ausencia del archivo.
3. El registro inconsistente es eliminado automáticamente de Room.

Esto permite reutilizar el mismo mecanismo de sincronización para mantener consistencia entre ambas fuentes.

### Cola de Procesamiento Remoto

AudioRepository administra una cola persistente de solicitudes pendientes administrada por Room

Esto permite que las solicitudes pendientes sobrevivan a:

- Reinicios de la aplicación.
- Cierres inesperados.
- Pérdida de conectividad.

Garantizando que el procesamiento pueda continuar posteriormente sin pérdida de información.

Para cada solicitud/audio grabado:

1. Se crea una entidad en Room con estado `WAITING`.
2. Recupera la siguiente solicitud pendiente de la cola.
3. Actualiza su estado a `PROCESSING`.
4. Verifica la existencia física del archivo.
5. Envía el audio al servidor.
6. Actualiza el resultado en Room a `SENT` .

**Recuperación de Fallos**

Si ocurre un error:

1. El estado cambia a `FAILED`.
2. El procesamiento se detiene.
3. La cola permanece consistente.

Y si durante el procesamiento se detecta que el archivo físico ya no existe, el registro inconsistente es eliminado automáticamente de Room y la cola continúa con la siguiente solicitud.

Esto evita pérdidas de información y permite reintentos posteriores.

## Filosofía de Diseño

AudioRepository fue diseñado como un coordinador de infraestructura especializada para el dominio de audio.

Su objetivo no es únicamente almacenar datos, sino presentar una interfaz coherente y reactiva mientras coordina múltiples subsistemas internos con responsabilidades diferentes manteniendo consistencia entre:

- Persistencia estructurada.
- Sistema de archivos.
- Multimedia.
- Procesamiento remoto.
- Estados reactivos.

Actúa como una capa de abstracción que permite que el resto de la aplicación interactúe con audio sin depender de detalles de infraestructura específicos.