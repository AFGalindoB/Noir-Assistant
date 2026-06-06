# Noir Assistant — Arquitectura de Persistencia Android

Este documento define la arquitectura de persistencia utilizada dentro del cliente Android de Noir Assistant.

Su objetivo es describir cómo la aplicación:

- Organiza el acceso a datos
- Mantiene consistencia de estado
- Gestiona almacenamiento local
- Propaga cambios reactivos hacia la UI
- Coordina múltiples mecanismos de persistencia

La persistencia representa el núcleo operativo de la aplicación. Toda la información crítica del sistema es administrada localmente mediante una infraestructura desacoplada y reactiva diseñada para mantener continuidad operativa incluso ante fallos parciales del sistema.

La arquitectura de persistencia fue diseñada para:

- Centralizar el acceso a datos
- Evitar acoplamiento entre capas
- Mantener un flujo consistente de información
- Facilitar escalabilidad incremental
- Permitir evolución independiente de cada mecanismo de almacenamiento

Este documento se enfoca exclusivamente en persistencia local, organización de repositories, managers y flujo interno de almacenamiento dentro de la aplicación Android.

## Estructura de Persistencia Local

Toda la infraestructura de persistencia local de Noir Assistant se encuentra centralizada dentro de la carpeta `data/local/`

Esta capa representa el nivel más cercano al almacenamiento físico de la aplicación y contiene las implementaciones responsables de administrar el acceso directo a los distintos mecanismos de persistencia utilizados por el sistema.

La arquitectura fue diseñada para separar cada mecanismo de almacenamiento según su responsabilidad y naturaleza de operación, permitiendo mantener una infraestructura desacoplada, mantenible y escalable.

Actualmente la persistencia local se divide en tres áreas principales:

| Carpeta        | Responsabilidad                                  |
|:---------------|:-------------------------------------------------|
| `room/`        | Persistencia estructurada mediante SQLite y Room |
| `preferences/` | Configuración persistente mediante DataStore     |
| `audio/`       | Persistencia y acceso a archivos multimedia      |

Cada módulo encapsula completamente su lógica de almacenamiento evitando exponer detalles internos de infraestructura al resto de la aplicación.

## Persistencia Estructurada `room/`

Esta carpeta contiene toda la infraestructura relacionada con persistencia estructurada basada en SQLite mediante Room.

Este sistema constituye el mecanismo principal de almacenamiento persistente de la aplicación y es utilizado para manejar:

- Tareas
- Notas
- Solicitudes de audio

La arquitectura interna de `room/` se organiza mediante separación por responsabilidad:

| Carpeta  | Responsabilidad                             |
|:---------|:--------------------------------------------|
| `dao/`   | Operaciones de acceso a datos               |
| `entity/`| Modelos persistentes de base de datos       |
| `db/`    | Configuración y acceso centralizado de Room |

---

### entity/

Contiene las estructuras persistentes que representan tablas dentro de la base de datos.

Las entidades definen:

- Estructura de las tablas
- Relaciones persistentes
- Claves primarias
- Tipado persistente

Estas estructuras permanecen desacopladas de los modelos de dominio utilizados por la UI o la lógica de negocio.

---

### dao/

Contiene las interfaces encargadas de definir las operaciones de acceso a datos.

Los DAO encapsulan:

- Queries
- Inserciones
- Actualizaciones
- Eliminaciones
- Observación reactiva mediante Flow<T>

Toda interacción con Room debe realizarse exclusivamente a través de los DAO correspondientes.

---

### db/

Contiene la definición central de la base de datos de la aplicación.

Esta capa es responsable de:

- Registrar entidades
- Exponer DAO
- Configurar Room
- Gestionar migraciones
- Centralizar acceso SQLite

AppDatabase representa el punto único de entrada hacia la persistencia estructurada del sistema.

## Persistencia de Configuración `preferences/`

La carpeta contiene toda la infraestructura relacionada con persistencia ligera de configuración y estado de usuario mediante DataStore.

Este sistema es utilizado para almacenar información persistente de baja complejidad estructural que requiere acceso reactivo, desacoplado y seguro.

La estructura actual se divide en:

| Archivo                     | Responsabilidad                     |
|:----------------------------|:------------------------------------|
| `UserPreferences.kt`        | Modelo persistente de configuración |
| `UserPreferencesManager.kt` | Encapsulación de acceso a DataStore |

---

### `UserPreferences.kt`

Representa la estructura central de configuración persistente utilizada por la aplicación.

Este modelo encapsula información relacionada con:

- Perfil de usuario
- Configuración visual
- Idioma
- Preferencias generales
- Configuración de recordatorios
- Información básica de conexión

La estructura permanece desacoplada de la UI y funciona como representación persistente especializada para configuración de usuario.

**Información Persistida**

Actualmente el sistema almacena:

- Nombre
- Biografía
- Imagen de perfil
- Posicionamiento visual de imagen
- Nivel de zoom
- Idioma
- Hora de recordatorios
- Nombre de usuario asignado en el servidor
- URL al servidor

---

### `UserPreferencesManager.kt`

Encapsula completamente el acceso a DataStore evitando exponer detalles internos de persistencia al resto de la aplicación.

Su responsabilidad consiste en centralizar:

- Lectura persistente
- Escritura de configuración
- Exposición reactiva mediante Flow<T>
- Gestión de tokens
- Estados de configuración
- Validación básica de persistencia

La arquitectura evita que otras capas interactúen directamente con claves, estructuras internas o APIs de DataStore.

#### Flujos Reactivos

La configuración persistente es expuesta mediante flujos reactivos utilizando Flow<T>.

Esto permite que los cambios de configuración se propaguen automáticamente hacia otras capas del sistema sin necesidad de recargas manuales.

**Ejemplos de Estado Reactivo**

```kotlin
val userPreferencesFlow: Flow<UserPreferences>

val languageFlow: Flow<String>

val isUrlConfiguredFlow: Flow<Boolean>

val isTokenConfiguredFlow: Flow<Boolean>
```

Este enfoque permite:

- Reactividad persistente
- Observación desacoplada
- Actualización automática de estado
- Reducción de inconsistencias
- Persistencia transparente

#### Encapsulación de Persistencia

Toda operación sobre DataStore debe realizarse exclusivamente mediante UserPreferencesManager.

Esto permite:

- Centralizar acceso a configuración
- Evitar duplicación de lógica
- Desacoplar infraestructura
- Mantener consistencia de acceso
- Facilitar evolución futura del sistema de persistencia

La aplicación nunca interactúa directamente con claves persistentes o instancias de DataStore fuera de esta capa.

## Persistencia de Audio `audio/`

La carpeta contiene toda la infraestructura relacionada con persistencia y manejo especializado de archivos de audio .m4a.

A diferencia de la persistencia estructurada basada en Room o la configuración almacenada mediante DataStore, este módulo administra almacenamiento físico directo sobre disco utilizando File API y componentes multimedia nativos de Android.

El sistema se encuentra centralizado mediante un único componente especializado: `SmartAudioFileManager.kt`

---

### Objetivo del Sistema de Audio
El sistema de audio fue diseñado para encapsular completamente toda la infraestructura relacionada con:

- Grabación de audio
- Persistencia física de archivos
- Reproducción multimedia
- Observación reactiva de almacenamiento
- Integridad de archivos
- Gestión de recursos multimedia nativos

Esto permite desacoplar completamente al resto de la aplicación de detalles físicos relacionados con:

- Rutas de almacenamiento
- APIs multimedia Android
- MediaRecorder
- ExoPlayer
- Observadores de disco
- Manejo manual de archivos

---

### `SmartAudioFileManager.kt`

Representa el núcleo especializado de persistencia multimedia del sistema manejando toda interacción con:

- ExoPlayer
- MediaRecorder
- FileObserver
- File API

Este componente encapsula toda la lógica relacionada con almacenamiento y control de audios .m4a. Ninguna otra capa del sistema interactúa directamente con APIs multimedia nativas o rutas físicas de almacenamiento.

La infraestructura fue diseñada bajo una filosofía:

- Reactiva
- Resiliente
- Encapsulada
- Orientada a estado
- Desacoplada de UI

Esto permite:

- Mayor mantenibilidad
- Menor acoplamiento
- Sustitución futura de infraestructura
- Mejor control de recursos
- Mayor resiliencia operativa

#### Responsabilidades

**Persistencia Física y Observación Reactiva de Disco**

El manager administra directamente el almacenamiento de audios dentro del directorio privado interno de la aplicación: `filesDir/smart_audio/`

Además, el sistema implementa monitoreo reactivo de archivos mediante `FileObserver`.

Esto permite detectar automáticamente:

- Creación de archivos
- Eliminación de audios
- Movimientos de almacenamiento
- Cambios persistentes en disco

Los cambios son propagados mediante `StateFlow<List<String>>`.

Toda interacción con archivos físicos permanece encapsulada dentro de esta capa.

**Grabación de Audio**

La grabación se encuentra completamente centralizada mediante MediaRecorder.

El sistema encapsula:

- Inicialización del recorder
- Configuración de codificación
- Generación de archivos temporales
- Consolidación de grabaciones válidas
- Limpieza automática ante fallos

Los audios son almacenados utilizando formato:

- MPEG-4 (.m4a)
- Codificación AAC

**Reproducción Multimedia**

La reproducción de audio es gestionada mediante ExoPlayer.

El manager expone estados reactivos relacionados con:

- Archivo actualmente reproduciéndose
- Duración
- Progreso temporal
- Estado de reproducción

Esto permite mantener consistencia reactiva entre el estado multimedia persistente y la representación visual de la aplicación.

**Integridad y Recuperación**

La infraestructura incorpora mecanismos básicos de resiliencia para evitar inconsistencias multimedia.

El sistema puede:

- Detectar archivos corruptos
- Limpiar grabaciones inválidas
- Recuperar estado tras fallos
- Reiniciar controladores multimedia
- Eliminar residuos temporales

Esto reduce inconsistencias entre almacenamiento físico y estado reactivo interno.
