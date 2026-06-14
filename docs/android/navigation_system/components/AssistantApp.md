# Noir Assistant — AssistantApp

## Introducción

`AssistantApp` constituye el núcleo estructural del sistema de navegación de Noir Assistant.

A diferencia de `MainActivity`, que inicializa el entorno de ejecución, `AssistantApp` es el componente que **define, construye y coordina el sistema completo de navegación dentro de la interfaz visual.**

Este componente no solo refleja el estado de navegación, sino que también actúa como:

- Constructor del grafo de navegación
- Orquestador de la estructura visual (Scaffold + Drawer)
- Puente entre UI y lógica de navegación
- Punto de integración de `NavigationActions`

En términos arquitectónicos, `AssistantApp` representa el nivel donde:

la estructura de navegación, la interfaz y el estado global convergen en un único sistema operativo de UI.

## Rol dentro del sistema de navegación

AssistantApp opera como una capa intermedia entre:

- Estado global (`MainViewModel`)
- Sistema de navegación (`NavController`)
- Interfaz de usuario (Scaffold, Drawer, BottomBar)
- Acciones de navegación (`NavigationActions`)

```text
MainViewModel
      ↓
AssistantApp (orquestación + UI shell)
      ↓
NavController
      ↓
NavHost
      ↓
Pantallas
```

Pero a diferencia de una capa puramente reactiva, AssistantApp también define la estructura visual del sistema de navegación.

## Responsabilidades principales

AssistantApp no es solo sincronización. Sus responsabilidades se dividen en cuatro bloques claros:

### 1. Construcción del sistema de navegación

AssistantApp inicializa y configura:

- `NavController`
- `NavHost`
- Grafos de navegación
- Destinos de la aplicación

Esto lo convierte en el punto donde se materializa la estructura completa de navegación.

### 2. Integración de NavigationActions

AssistantApp inyecta `NavigationActions` como puente entre UI y lógica de navegación.

Este objeto:

- Encapsula acciones de navegación de alto nivel
- Gestiona cierre del drawer
- Dispara solicitudes de navegación hacia `MainViewModel`

Esto permite que los componentes UI no dependan directamente del sistema de navegación.

### 3. Orquestación de la interfaz de navegación

AssistantApp define la estructura visual global mediante:

- `ModalNavigationDrawer`
- `Scaffold`
- `TopBar`
- `BottomBar`
- `NavHost`

Esto lo convierte en el layout principal del sistema de navegación.

### 4. Sincronización con el estado global

AssistantApp observa el estado de `MainViewModel` para:

- Detectar cambios de pantalla (`currentScreen`)
- Sincronizar el `NavController`
- Reflejar el estado de transición (`isTransitioning`)
- Actualizar UI dinámica (TopBar / BottomBar)

## Modelo híbrido: UI + navegación + control

A diferencia de arquitecturas donde la navegación está separada de la UI, aquí AssistantApp funciona como un sistema híbrido:

- No es solo UI
- No es solo navegación
- No es solo sincronización

Es un orquestador estructural del sistema completo de navegación.

## NavigationActions como capa de interacción

Aunque AssistantApp define la estructura de navegación y coordina su ejecución, los componentes visuales no interactúan directamente con el sistema de navegación.

Para ello, AssistantApp crea e inyecta una instancia compartida de `NavigationActions`, que actúa como intermediario entre la interfaz y el flujo de navegación.

Este componente constituye el punto de entrada de todas las solicitudes de cambio de pantalla originadas desde la UI.

### Responsabilidad dentro del sistema

NavigationActions cumple tres funciones principales:

- Exponer acciones de navegación de alto nivel para la interfaz.
- Coordinar operaciones previas a la navegación.
- Delegar la solicitud al sistema de control gestionado por `MainViewModel`.

De esta forma, los componentes visuales no necesitan conocer:

- `NavController`
- `MainViewModel`
- El sistema de transiciones
- El estado interno de navegación

Su única responsabilidad consiste en expresar una intención de navegación.

### Navegación basada en intenciones

Las acciones expuestas por este componente representan operaciones del dominio de navegación, no llamadas directas a rutas o pantallas específicas.

Por ejemplo:

- Navegar al área principal
- Navegar a la papelera
- Navegar a la configuración
- Navegar mediante la barra contextual del área Home

Esto permite que la UI trabaje con conceptos funcionales en lugar de detalles de implementación.

### Coordinación previa a la navegación

Antes de delegar una solicitud al sistema de navegación, NavigationActions puede ejecutar operaciones auxiliares necesarias para mantener la coherencia visual de la interfaz.

Un ejemplo es el cierre automático del menú lateral cuando la navegación se origina desde el `NavigationSideBar`.

De esta manera, la interfaz queda en un estado consistente antes de iniciar la transición.

### Integración con MainViewModel

Una vez completadas las operaciones previas necesarias, NavigationActions delega la solicitud a MainViewModel.

Esta delegación se realiza mediante el callback `onStartTransition`, proporcionado por AssistantApp durante la construcción del sistema.

```text
NavigationActions
        ↓
onStartTransition(...)
        ↓
MainViewModel.navigateTo(...)
```

A partir de este punto, la solicitud abandona la interfaz y pasa a formar parte del flujo de navegación controlado por estado.

### Relación con AssistantApp

AssistantApp actúa como el punto de integración de este mecanismo.

Durante su inicialización:

- Crea la instancia de NavigationActions
- La conecta con el estado del drawer
- La vincula al flujo de navegación gestionado por MainViewModel
- La distribuye a los componentes de navegación de la interfaz

Esto permite que toda la aplicación utilice una única puerta de entrada para solicitar cambios de pantalla.

## ModalNavigationDrawer como capa de navegación global

AssistantApp utiliza ModalNavigationDrawer para representar la navegación global.

Este componente:

- Encapsula la navegación entre grafos principales
- Mantiene accesible el SideBar desde cualquier punto
- Delega acciones a `NavigationActions`

**Importante:** el drawer no ejecuta navegación, solo emite intenciones estructuradas.

## Scaffold como sistema estructural de UI

El Scaffold dentro de AssistantApp define la arquitectura visual del sistema:

- TopBar → acciones dinámicas del sistema
- BottomBar → navegación contextual
- Content → `NavHost`

Además, el `MainViewModel` permite inyectar dinámicamente:

- Acciones del TopBar
- Contenido del BottomBar

Esto permite que cada pantalla:

- Defina su propia configuración global de UI
- Adapte el layout sin acoplarse a la estructura base

### BottomBar dinámico: navegación contextual

AssistantApp implementa lógica condicional para el BottomBar:

- En contexto Home → se muestra NavigationBottomBar
- Fuera de Home → puede ser reemplazado por contenido dinámico

Esto permite diferenciar entre:

- Navegación global (SideBar)
- Navegación contextual (BottomBar)
- Navegación específica de pantalla (custom UI)

## NavHost como núcleo de definición de grafos

El NavHost dentro de AssistantApp actúa como el punto donde se materializa toda la estructura de navegación del sistema.

Su responsabilidad no es decidir qué pantalla debe mostrarse, sino interpretar el estado actual del sistema de navegación y renderizar el destino correspondiente.

Dentro de este componente se:

- Definen los grafos de navegación
- Registran las pantallas concretas
- Establecen relaciones jerárquicas entre destinos
- Se conecta la estructura tipada de navegación con el sistema de ejecución de Jetpack Compose

Aquí es donde el sistema de navegación deja de ser abstracto y se convierte en una estructura ejecutable.

### Estructura de destinos (`NavDestinations`)

La estructura del NavHost está construida sobre un sistema de destinos tipados, que define de forma explícita todos los posibles puntos de navegación dentro de la aplicación.

Estos destinos se organizan en dos niveles:

#### Grafos de navegación

Los grafos representan contextos funcionales completos dentro de la aplicación, agrupando pantallas relacionadas bajo una misma estructura lógica.

```text
RootGraph
 ├── HomeGraph
 ├── TrashGraph
 ├── AccountGraph
 └── VoiceTaskGraph
```

En este nivel se define la estructura global del sistema:

- `HomeGraph` → núcleo de productividad
- `TrashGraph` → gestión de elementos eliminados
- `AccountGraph` → configuración y preferencias
- `VoiceTaskGraph` → captura rápida mediante voz

Estos grafos permiten que la navegación no sea una lista plana de pantallas, sino un sistema jerárquico de contextos.

#### Pantallas individuales

Dentro de cada grafo, existen destinos concretos que representan pantallas específicas del sistema:

```text
TaskList
NoteList
Account
TrashScreen
VoiceTaskScreen
TransitionScreen
```

Cada uno de estos destinos representa un punto final de navegación donde la UI es efectivamente renderizada.

### Integración con el NavHost

El NavHost utiliza estos destinos como base estructural para construir el grafo de navegación ejecutable.

Esto significa que:

- Los grafos definen el contexto
- Las pantallas definen los nodos concretos
- El NavHost une ambos en un sistema navegable

Sin embargo, el NavHost no decide qué destino activar.

Su única responsabilidad es reflejar el estado actual del sistema de navegación y renderizar el destino correspondiente.

### Naturaleza del sistema tipado

Este modelo de destinos está diseñado como un sistema completamente tipado mediante @Serializable, lo que permite:

- Eliminar rutas basadas en strings
- Garantizar consistencia estructural en tiempo de compilación
- Facilitar refactorización segura del sistema de navegación

Esto convierte los destinos en entidades del dominio de navegación, no simples identificadores de UI.

## Integración con ciclo de renderizado

Cada pantalla dentro del NavHost participa en el sistema de navegación mediante notificación de renderizado.

Esto permite:

- Sincronizar `CurtainOverlay`
- Controlar fin de transición
- Habilitar interacción solo cuando la UI está lista

El sistema garantiza que la navegación visual y disponibilidad de UI estén sincronizadas.

## Control de estado de transición

AssistantApp respeta el estado global `isTransitioning` para:

- Bloquear interacción durante transiciones
- Evitar múltiples navegaciones simultáneas
- Mantener consistencia del flujo visual

Esto asegura un comportamiento secuencial del sistema de navegación.

## Filosofía de diseño

AssistantApp sigue una filosofía clara:

Es el punto donde la estructura, la lógica de navegación y la UI convergen en un solo sistema controlado.

Esto implica que:

- No solo refleja estado
- No solo renderiza UI
- Mo solo sincroniza navegación

Sino que actúa como un orquestador estructural del sistema completo de navegación.