# Noir Assistant — Main ViewModel

## Introducción

`MainViewModel` es el coordinador principal de la aplicación.

Su responsabilidad consiste en administrar el ciclo de vida global de la interfaz, coordinar la inicialización de los subsistemas principales y centralizar la navegación de alto nivel utilizada por Noir Assistant.

A diferencia de los ViewModels de dominio, MainViewModel no administra entidades específicas del sistema.

Su función es coordinar:

- Arranque de la aplicación.
- Inicialización de subsistemas.
- Navegación global.
- Estado de transición entre pantallas.
- Configuración dinámica de barras de navegación.
- Acceso centralizado a ViewModels especializados.

## Dependencias

### AppContainer

Proporciona acceso a la infraestructura principal de la aplicación.

Permite resolver:

- Repositories.
- Managers globales.
- Servicios compartidos.

## Arquitectura General

MainViewModel coordina dos áreas principales:

| Área           | Responsabilidad           |
|:---------------|:--------------------------|
| Inicialización | Arranque y mantenimiento  |
| Navegación     | Coordinación de pantallas |

Además expone un conjunto de ViewModels especializados agrupados mediante AssistantViewModels.

| ViewModel         | Dominio       |
|:------------------|:--------------|
| TaskViewModel     | Tareas        |
| NoteViewModel     | Notas         |
| AudioViewModel    | Audio         |
| TrashViewModel    | Papelera      |
| SettingsViewModel | Configuración |

## AssistantViewModels

MainViewModel agrupa los ViewModels especializados mediante la estructura `AssistantViewModels`.

Esta estructura actúa como un contenedor de composición que permite exponer todos los dominios funcionales de la aplicación desde un único punto de acceso.

De esta manera la interfaz puede obtener acceso a los distintos ViewModels especializados sin depender directamente del mecanismo de construcción utilizado por la infraestructura de la aplicación.

AssistantViewModels no implementa lógica propia y su responsabilidad consiste exclusivamente en agrupar dependencias relacionadas con la capa de presentación.

## Funcionalidades

### Arranque de la Aplicación

#### Limpieza del sistema

Durante su inicialización ejecuta tareas de mantenimiento necesarias para garantizar consistencia de datos.

Entre ellas:

- Eliminación de tareas expiradas.
- Eliminación de notas expiradas.
- Eliminación de solicitudes de audio expiradas.

Estas operaciones son ejecutadas en segundo plano antes de liberar la pantalla de inicio.

#### Calentamiento de Persistencia

Una vez completado el mantenimiento inicial, MainViewModel realiza una consulta temprana sobre la base de datos.

El objetivo consiste en:

- Inicializar Room.
- Cargar estructuras internas.
- Reducir latencia en consultas posteriores.

Este proceso actúa como mecanismo de calentamiento (database warm-up).

#### Verificación Inicial de Infraestructura

Durante el arranque también se inicia una validación del estado de comunicación con la infraestructura remota.

La operación es delegada a `GlobalStateManager`.

Esto permite que la aplicación conozca tempranamente:

- Estado del servidor.
- Estado de autenticación.
- Disponibilidad de servicios remotos.

---

### Gestión de Splash Screen

MainViewModel mantiene el estado reactivo `isAppReady`.

Mientras este estado permanezca en falso:

- La interfaz continúa mostrando la pantalla de inicio.

Cuando todas las tareas críticas concluyen:

- El estado cambia a verdadero.
- La aplicación libera la Splash Screen.

---

### Navegación Global

MainViewModel implementa un modelo de navegación basado en estado donde la pantalla visible es representada explícitamente mediante `currentScreen`.

Las transiciones de navegación son coordinadas mediante cambios de estado observables consumidos por la interfaz.

#### Sistema de navegacion

Las solicitudes de navegación son coordinadas mediante: `navigateTo()`. Esto desacopla la navegación de implementaciones específicas de frameworks de navegación.

#### Coordinación de Transiciones

El sistema implementa una transición controlada mediante cortina (curtain transition).

El flujo general consiste en:

- Solicitud de navegación.
- Activación del estado de transición.
- Oscurecimiento de la interfaz.
- Cambio de pantalla.
- Liberación de la transición.

Esto garantiza que los cambios de pantalla ocurran de manera consistente y visualmente controlada.

#### Configuración Dinámica de Barras

MainViewModel permite que cada pantalla configure dinámicamente el contenido de los elementos estructurales compartidos de la interfaz, incluyendo Top Bar y Bottom Bar.

Esta capacidad permite que las pantallas personalicen la estructura visual principal ajustando el Scaffold raíz de la aplicación.

## Filosofía de Diseño

MainViewModel fue diseñado como un coordinador de aplicación.

Su objetivo consiste en centralizar la lógica relacionada con:

- Inicialización.
- Navegación.
- Estado global de interfaz.

Sin asumir responsabilidades de dominio específicas.

Actúa como el punto de entrada principal de la capa de presentación y como el mecanismo de coordinación entre los distintos ViewModels especializados.

MainViewModel funciona además como el punto de composición principal de la capa de presentación, concentrando la inicialización y coordinación de los ViewModels especializados que conforman la experiencia completa de Noir Assistant.