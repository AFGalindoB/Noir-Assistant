# Noir Assistant — Componentes de Navegación

## Introducción

Los componentes de navegación constituyen la capa visual encargada de exponer las funcionalidades de navegación al usuario.

A diferencia de los mecanismos de navegación, cuya responsabilidad consiste en definir destinos y coordinar solicitudes de cambio de pantalla, estos componentes proporcionan los elementos gráficos mediante los cuales el usuario interactúa con el sistema.

Actualmente Noir Assistant utiliza dos mecanismos visuales complementarios:

| Componente            | Función                                                             |
| --------------------- | ------------------------------------------------------------------- |
| `NavigationBottomBar` | Navegación contextual dentro del área principal de trabajo.         |
| `NavigationSideBar`   | Navegación global entre las secciones principales de la aplicación. |

Ambos componentes utilizan la infraestructura descrita en `Navigation_Mechanisms.md` y permanecen desacoplados de la implementación interna del sistema de navegación.

## Arquitectura General

Los componentes visuales no realizan navegación directamente.

Toda interacción se delega a `NavigationActions`.

```text
       Usuario
          ↓
Componente de Navegación
          ↓
   NavigationActions
          ↓
  Flujo de Navegación
```

Esta separación permite mantener la interfaz independiente de la infraestructura interna de navegación.

# NavigationBottomBar

`NavigationBottomBar` proporciona navegación rápida entre las vistas principales del área de productividad de Noir Assistant.

Su objetivo consiste en facilitar cambios frecuentes entre funcionalidades estrechamente relacionadas sin obligar al usuario a abrir el menú lateral.

## Alcance

La barra inferior se encuentra limitada al contexto del grafo principal de trabajo.

Actualmente permite alternar entre:

| Destino    | Función            |
| ---------- | ------------------ |
| `TaskList` | Gestión de tareas. |
| `NoteList` | Gestión de notas.  |

Estas pantallas representan las actividades que el usuario consulta con mayor frecuencia durante el uso cotidiano de la aplicación.

## Comportamiento

La barra inferior mantiene sincronizado su estado con el destino actualmente activo.

Cuando una pantalla se encuentra seleccionada:

* El elemento correspondiente aparece resaltado.
* Se evita solicitar una navegación redundante.
* El estado visual refleja la ubicación actual del usuario.

Adicionalmente, la barra puede deshabilitar temporalmente la interacción durante procesos de transición para evitar solicitudes simultáneas de navegación.

## Filosofía de Diseño

La barra inferior fue concebida como un mecanismo de navegación contextual.

Su responsabilidad se limita a facilitar el movimiento entre herramientas relacionadas dentro de una misma área funcional.

No gestiona navegación global ni acceso a secciones administrativas de la aplicación.

# NavigationSideBar

`NavigationSideBar` proporciona acceso a las distintas secciones principales de Noir Assistant.

Representa el mecanismo principal de navegación global de la aplicación.

## Alcance

El menú lateral permite acceder a funcionalidades pertenecientes a distintos grafos de navegación.

Actualmente incluye:

| Destino    | Función                          |
| ---------- | -------------------------------- |
| Home       | Área principal de productividad. |
| Voice Task | Captura rápida mediante voz.     |
| Trash      | Gestión de elementos eliminados. |
| Account    | Configuración y preferencias.    |

Cada opción representa un cambio de contexto dentro de la aplicación.

## Navegación Global

A diferencia de la barra inferior, el menú lateral opera sobre áreas funcionales completas.

Esto permite que el usuario acceda rápidamente a cualquier sección principal desde cualquier punto de la aplicación.

## Contenido Adaptativo

El contenido mostrado por el menú lateral puede modificarse dinámicamente según el estado global de la aplicación.

Determinadas funcionalidades solo se muestran cuando los requisitos necesarios se encuentran disponibles.

Este comportamiento permite mantener una interfaz más limpia y evitar exponer funciones inaccesibles al usuario.

## Gestión de Estado

El menú lateral mantiene sincronizado el elemento seleccionado con el destino actualmente activo.

Cuando el usuario intenta acceder a la sección donde ya se encuentra:

* No se inicia una nueva navegación.
* El menú simplemente se cierra.
* Se evita reconstruir innecesariamente la interfaz.

## Filosofía de Diseño

El menú lateral fue diseñado para representar la estructura global de Noir Assistant.

Mientras la barra inferior facilita la navegación dentro de un contexto específico, el menú lateral permite desplazarse entre los distintos dominios funcionales de la aplicación.

Ambos mecanismos son complementarios y cumplen responsabilidades claramente diferenciadas.

# Resumen Arquitectónico

| Componente          | Tipo de Navegación | Alcance                                 |
| ------------------- | ------------------ | --------------------------------------- |
| NavigationBottomBar | Contextual         | Pantallas del área principal de trabajo |
| NavigationSideBar   | Global             | Secciones principales de la aplicación  |

Esta separación permite optimizar la experiencia de navegación manteniendo accesibles tanto las acciones frecuentes como las funcionalidades de nivel superior.
