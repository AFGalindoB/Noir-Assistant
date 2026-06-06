# Noir Assistant — Android Data System

## Introducción

Noir Assistant está organizado mediante un conjunto de módulos especializados que colaboran para mantener separadas las responsabilidades de presentación, dominio, persistencia y coordinación global.

La arquitectura fue diseñada para que cada componente conozca únicamente las dependencias necesarias para cumplir su función, evitando que detalles de infraestructura se propaguen hacia capas superiores.

Como resultado, cada capa puede evolucionar de forma independiente sin afectar al resto del sistema. La arquitectura separa explícitamente:

- Presentación.
- Coordinación de estado.
- Dominio.
- Persistencia.
- Infraestructura especializada.
- Integración con servicios remotos.

## Visión General

La arquitectura puede visualizarse como una serie de capas colaborativas:

1. UI
2. ViewModels
3. Repositories
4. Fuentes de Datos e Infraestructura

Complementando estas capas existe un mecanismo de coordinación global encargado de consolidar estados compartidos por toda la aplicación.

GlobalStateManager actúa como una capa transversal responsable de exponer:

- Estado de configuración.
- Estado de autenticación.
- Estado de infraestructura.
- Estado de permisos.

Permitiendo que múltiples módulos consuman una representación coherente del estado operativo general del sistema.

## Capa de Presentación

La capa de presentación contiene las pantallas y componentes visuales construidos con Jetpack Compose.

Su responsabilidad consiste exclusivamente en representar estado y emitir eventos de interacción.

La interfaz no accede directamente a:

- Bases de datos.
- DataStore.
- Servicios remotos.
- Infraestructura Android.

Toda comunicación ocurre mediante ViewModels.

[Ver Domain Viewmodels](modules/Domain_ViewModels.md)

## Capa de ViewModels

Los ViewModels actúan como adaptadores entre la interfaz y el dominio.

Son responsables de:

- Exponer estado reactivo.
- Transformar datos para la UI.
- Ejecutar operaciones asíncronas.
- Coordinar acciones iniciadas por el usuario.

Cada dominio funcional posee su propio ViewModel especializado.

[Ver Domain Viewmodels](modules/Domain_ViewModels.md)

## Capa de Repositories

Los repositories constituyen la principal frontera entre el dominio y la infraestructura.

Su responsabilidad consiste en encapsular:

- Persistencia.
- Procesamiento local.
- Integración con servicios externos.
- Conversión de modelos.

Cada repository representa una capacidad específica del sistema.

### Repositories de Dominio

- TaskRepository
- NoteRepository
- AudioRepository

### Repositories de Coordinación
- TrashRepository
- SettingsRepository

[Ver Repository Architecture](modules/Repository_Architecture.md)

## Persistencia Local

[Ver Persistence Architecture](modules/Persistence_Architecture.md)

### Room

Utilizado para almacenar información estructurada del dominio. Incluye:

- Tareas.
- Notas.
- Solicitudes de audio.

[Ver Room Data Flow](data_flows/Room_Data_Flow.md)

---

### DataStore

Utilizado para almacenar configuración y preferencias persistentes. Incluye:

- Configuración de usuario.
- Idioma.
- Recordatorios
- Credenciales.
- Parámetros de conexión.

[Ver DataStore Data Flow](data_flows/DataStore_Data_Flow.md)

## Infraestructura Especializada

Además de los mecanismos tradicionales de persistencia, Noir Assistant incorpora subsistemas especializados responsables de coordinar recursos complejos del dispositivo.

### Audio

El subsistema de audio constituye una infraestructura especializada que integra múltiples componentes bajo una única interfaz de dominio.

Entre las capacidades administradas se encuentran:

- Grabación de audio.
- Reproducción multimedia.
- Gestión de archivos físicos.
- Persistencia de solicitudes de procesamiento.
- Administración de estados de procesamiento.
- Integración con servicios remotos.

A diferencia de Room o DataStore, el sistema de audio no representa únicamente un mecanismo de almacenamiento, sino una capa de coordinación capaz de mantener consistencia entre archivos físicos, persistencia estructurada, reproducción multimedia y procesamiento remoto.

[Ver Audio Data Flow](data_flows/Audio_Data_Flow.md)

[Ver Audio Repository](repository_details/Audio_Repository.md)

## Estado Global

Algunas capacidades de la aplicación requieren coordinar información proveniente de múltiples módulos.

Para ello existe `GlobalStateManager`.

Su responsabilidad consiste en consolidar:

- Estado de infraestructura.
- Estado de autenticación.
- Permisos.
- Mensajes globales.

[Ver Global Manager](modules/Global_Manager.md)

## Infraestructura Remota

La infraestructura remota utilizada por Noir Assistant no actúa como una fuente principal de datos para la aplicación.

Su responsabilidad consiste en proporcionar capacidades especializadas de procesamiento que serían costosas o inconvenientes de ejecutar localmente.

La información persistente del usuario permanece almacenada en el dispositivo mediante los mecanismos de persistencia locales.

Cuando se requiere una capacidad remota, el flujo general consiste en:

1. Generar una solicitud de procesamiento.
2. Enviar la solicitud al servidor.
3. Permitir que el procesamiento ocurra de forma desacoplada.
4. Consultar posteriormente el resultado.
5. Persistir localmente la información obtenida.

Este modelo permite mantener independencia entre la aplicación y la infraestructura remota, reduciendo dependencias permanentes de conectividad.

## Filosofía General

La arquitectura de Noir Assistant se basa en la separación explícita de responsabilidades.

Cada módulo posee un propósito claramente definido y comunica sus capacidades mediante interfaces estables.

Esto permite que la aplicación permanezca desacoplada de detalles de infraestructura específicos mientras mantiene un modelo reactivo y fácilmente extensible.

La infraestructura se encuentra encapsulada detrás de interfaces y coordinadores especializados, permitiendo que la mayor parte del sistema opere sobre abstracciones de dominio independientes de la tecnología utilizada para implementarlas.