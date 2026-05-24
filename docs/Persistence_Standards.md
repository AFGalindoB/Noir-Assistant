# Noir Assistant — Arquitectura de Persistencia y Flujo de Datos

Este documento define el estándar arquitectónico utilizado para integrar nuevas fuentes de datos dentro de Noir Assistant.

La capa `data/` está diseñada bajo un enfoque:

- Reactivo,
- Desacoplado,
- Escalable,
- Orientado a Single Source of Truth.

El objetivo es garantizar consistencia entre UI, lógica de dominio y persistencia, independientemente del mecanismo de almacenamiento utilizado (Room, DataStore, File System o APIs externas).

---

## Arquitectura General de la Capa data/

La aplicación sigue una **arquitectura basada en el patrón Repository**, centralizada mediante un contenedor manual de dependencias.

### Estructura
`data/`
- `container`: Singleton que gestiona la vida de los repositorios.
- `local`: Acceso directo a fuentes de datos locales (DAO, DataStore, File APIs)
- `mapper`: Transformación entre modelos persistentes y modelos de dominio
- `repository`: Contratos e implementación de lógica de acceso a datos
- `worker`: Procesamiento en segundo plano mediante WorkManager

---

## Principios Arquitectónicos
La arquitectura de Noir Assistant se basa en los siguientes principios:

### Single Source of Truth
La UI nunca interactúa directamente con:

- DAOs
- File APIs
- DataStore
- APIs externas

Toda lectura o escritura debe pasar por el repositorio correspondiente.

### Reactividad como Base del Sistema
La capa de datos está diseñada alrededor de `Flow<T>`.

Esto permite:

- Actualización automática de la UI,
- Sincronización desacoplada entre capas,
- Reducción de estados inconsistentes,
- Observación eficiente de cambios.

### Separación por Responsabilidad
Cada capa tiene una única responsabilidad clara:

| Capa             | Responsabilidad            |
|:-----------------|:---------------------------|
| **Local Source** | Persistencia física        |
| **Repository**   | Orquestación y abstracción |
| **ViewModel**    | Transformación a estado UI |
| **UI**           | Renderizado y eventos      |

---

## Protocolo para Integrar un Nuevo Almacén
Todo nuevo módulo (Audio, Cloud, Settings, etc.) debe seguir este flujo de implementación:

### 1. Crear el Local Data Source `local/`:
El Local Source representa la fuente de verdad técnica del módulo. Nunca debe exponerse directamente a la UI o ViewModels.
- Si es **Room** crear: `Entity`, `Dao`, registro en `AppDatabase`. 
- Si es **DataStore**: Implementar un manager que encapsule (Lecturas, Escrituras y Claves de Preferencias). 
- Si es **File System**: Crear un manager especializado en operaciones I/O

### 2. Definir el Contrato del Repository `repository/`:
El repositorio define qué puede hacerse, no cómo se implementa. Por ejemplo: `interface AudioRepository`

- **Regla de Oro:** Toda operación reactiva o asíncrona debe exponer un `Flow` esto garantiza integración natural con: (Compose, StateFlow y actualización automática de UI).

### 3. Implementar el Repository `repository/`:
Crear la implementación concreta: `OfflineMyEntityRepository`

**Responsabilidades:**

- Consumir el Local Source,
- Encapsular lógica de acceso,
- Aislar detalles de persistencia,
- Manejar concurrencia correctamente.

**Reglas:**
- Operaciones pesadas → Dispatchers.IO
- La UI nunca debe conocer detalles de persistencia
- El repositorio actúa como punto de acceso único

### 4. Registro en AppContainer `container/`:
El contenedor centraliza dependencias y controla su ciclo de vida.

**Contrato:**
```kotlin
interface AppContainer {
   val myRepository: MyRepository
}
```
**Implementación:**
```kotlin
override val myRepository: MyRepository by lazy {
   OfflineMyRepository(dataSource)
}
```

---

## Flujo de Datos Reactivo (UDF)
La aplicación sigue un flujo unidireccional de datos:

1. Local Source
2. Repository
3. ViewModel
4. UI (Compose)

### Pipeline Reactivo
**Repository:**
Expone `Flow<T>` desde la fuente local.

**ViewModel:**
Transforma `Flow` en `StateFlow` mediante: `stateIn(viewModelScope, ...)`

**UI:**
Consume estados reactivos usando: `collectAsStateWithLifecycle()`. Esto permite que la interfaz reaccione automáticamente ante cualquier cambio persistente.

---

## Ejemplo Rápido: Módulo de Audio

| Componente                    | Implementación                      |
|:------------------------------|:------------------------------------|
| **Data Source**               | `java.io.File` (`Context.filesDir`) |
| **Repository Contract**       | `AudioRepository`                   |
| **Repository Implementación** | `OfflineAudioRepository`            |
| **Exposición**                | `audioRepository` en `AppContainer` |

---

## Reglas de Oro

### Nunca pases un `Context` al ViewModel
El repositorio ya debe tener lo que necesita del contexto vía constructor en el `AppContainer`.

### Usar Mappers cuando sea necesario
Si el modelo persistente contiene información irrelevante para UI:

```kotlin
    fun Entity.toDomain(): DomainModel
```

Esto evita acoplar la interfaz a detalles de persistencia.

### La UI no modifica estado persistente directamente: 
La UI trabaja sobre:

- Estados temporales
- Borradores 
- Eventos

La persistencia siempre ocurre a través del repositorio.

---

## Documentación Relacionada
La arquitectura segmenta el flujo de datos según:

- Estructura
- Volatilidad
- Comportamiento reactivo

Para comprender implementaciones específicas:
* [Flujo de Datos con Room (Persistencia de Tareas y Notas)](Room_Data_Flow.md)
* [Flujo de Preferencias con DataStore (Perfil de Usuario)](DataStore_Data_Flow.md)

## Filosofía de Diseño
Noir Assistant prioriza:

- Reactividad
- Desacoplamiento
- Consistencia de estado
- Escalabilidad incremental

Cada módulo debe poder evolucionar independientemente sin comprometer:

- La UI,
- La lógica de dominio,
- Ni el sistema de persistencia existente.