# Noir Assistant — Arquitectura de Repositories y Container

## Introducción

La arquitectura de Noir Assistant utiliza un sistema de inyección manual de dependencias basado en un contenedor centralizado.

El objetivo es desacoplar la creación de recursos de su consumo, permitiendo que repositorios, managers y servicios especializados puedan ser reutilizados por cualquier componente de la aplicación sin depender directamente de implementaciones concretas.

Esta estrategia permite mantener un control explícito sobre la construcción del sistema, reduciendo complejidad innecesaria y evitando dependencias ocultas entre módulos.

## Container

La infraestructura que centraliza el sistema se encuentra dentro del módulo: `data/container`

El Container representa el punto de entrada de toda la infraestructura de datos de la aplicación. Su responsabilidad consiste en construir, conectar y exponer los componentes compartidos necesarios para el funcionamiento del sistema.

---

### AppContainer

`AppContainer` define el contrato principal de dependencias compartidas por toda la aplicación.

Su responsabilidad consiste en exponer los componentes globales necesarios para el funcionamiento del sistema.

```kotlin
interface AppContainer {
    val globalStateManager: GlobalStateManager
    val taskRepository: TaskRepository
    val noteRepository: NoteRepository
    val trashRepository: TrashRepository
    val settingsRepository: SettingsRepository
    val audioRepository: AudioRepository
    val networkRepository: NetworkRepository
}
```

La interfaz desacopla a la aplicación de una implementación específica del contenedor.

Esto permite:

- Sustituir implementaciones durante pruebas.
- Reemplazar repositorios sin afectar capas superiores.
- Centralizar dependencias compartidas.
- Mantener una única fuente de construcción de infraestructura.

---

### AppDataContainer

`AppDataContainer` constituye la implementación principal de `AppContainer`.

Su responsabilidad es construir, inicializar y administrar el ciclo de vida de los componentes compartidos de la aplicación.

Entre los componentes construidos y administrados por el contenedor se encuentran:

**Infraestructura Base**

- AppDatabase
- UserPreferencesManager
- SmartAudioFileManager
- OkHttpClient
- CoroutineScope global

**Repositories**

- NetworkRepository
- SettingsRepository
- TaskRepository
- NoteRepository
- AudioRepository
- TrashRepository

**Managers Globales**

- GlobalStateManager

El Container es responsable de conectar los componentes que dependen unos de otros.

Por ejemplo:

- `TaskRepository` consume:
  - `AppDatabase.taskDao`
  - `AudioRepository`
  - `NetworkRepository`
- `AudioRepository` consume:
  - `NetworkRepository`
  - `SmartAudioFileManager`
  - `AppDatabase.audioRequestDao`
  - `applicationScope`

Esto permite centralizar la construcción de dependencias en un único lugar y evita que las capas superiores conozcan cómo se construye internamente cada módulo.

Todos los componentes son inicializados mediante `lazy`, permitiendo que los recursos sean creados únicamente cuando son requeridos por primera vez.

Esto reduce:

- Tiempo de arranque.
- Consumo inicial de memoria.
- Creación innecesaria de objetos.

---

### Ciclo de Vida

Los componentes expuestos por el Container son compartidos por toda la aplicación.

Una vez inicializados, permanecen disponibles mientras el proceso de la aplicación continúe activo.

Esto permite reutilizar recursos costosos como:

- Base de datos
- Clientes HTTP
- Managers especializados
- Repositories

Sin necesidad de recrearlos constantemente.

## Repositories

El sistema de repositories se encuentra organizado bajo el directorio: `data/repository/`

Cada dominio funcional posee su propio módulo independiente:

- `audio/`
- `network/`
- `note/`
- `settings/`
- `task/`
- `trash/`

Cada módulo contiene dos componentes principales:

- Repository Interface
- Repository Implementation

Esta organización permite que cada dominio evolucione de forma independiente sin afectar el resto del sistema.

<small>**Nota:** La responsabilidad específica de cada repository se documenta de forma individual en su correspondiente documento técnico.</small>

---

### Filosofía de los Repositories

Los repositories representan la frontera entre la lógica de negocio y la infraestructura.

No funcionan como simples wrappers de persistencia. Cada repository encapsula completamente:

- Persistencia local.
- Managers especializados.
- Comunicación remota.
- Conversión entre modelos de persistencia y modelos de dominio.
- Coordinación entre múltiples fuentes.

Por esta razón la UI nunca interactúa directamente con:

- DAOs
- DataStore
- File APIs
- Networking
- Managers

Toda operación relacionada con datos debe atravesar el repository correspondiente, lo que garantiza:

- Consistencia de estado.
- Desacoplamiento entre capas.
- Reutilización de lógica.
- Mantenibilidad a largo plazo.

---

### Diseño

Cada repository se divide en dos componentes claramente diferenciados.

#### Contrato

El contrato define las capacidades públicas del módulo.

Ejemplo:

```kotlin
interface NoteRepository
```

El contrato describe:

- Qué operaciones existen.
- Qué datos expone.
- Qué acciones pueden ejecutarse.

Sin revelar detalles de implementación. Esto permite sustituir implementaciones sin modificar los componentes consumidores.

#### Implementación

La implementación concreta contiene la lógica de infraestructura necesaria para cumplir el contrato.

Ejemplo:

```kotlin
class OfflineNoteRepository( private val noteDao: NoteDao ) : NoteRepository
```

Sus responsabilidades incluyen:

- Consumir fuentes de datos.
- Aplicar transformaciones de dominio.
- Gestionar concurrencia.
- Coordinar infraestructura.
- Mantener consistencia de estado.

La implementación permanece completamente oculta detrás de la interfaz pública del repository.

## Documentación Relacionada

La implementación y responsabilidades específicas de cada repository se documentan de forma independiente:

- [Audio Repository](repositories_details/Audio_Repository.md)
- [Network Repository](repositories_details/Network_Repository.md)
- [Note Repository](repositories_details/Notes_Repository.md)
- [Settings Repository](repositories_details/Settings_Repository.md)
- [Task Repository](repositories_details/Task_Repository.md)
- [Trash Repository](repositories_details/Trash_Repository.md)