# Noir Assistant — Sistema de Navegación

## Introducción

El sistema de navegación de Noir Assistant es el encargado de coordinar el recorrido completo que sigue una solicitud de cambio de pantalla desde el momento en que la aplicación inicia hasta que una nueva interfaz queda completamente renderizada y disponible para el usuario.

A diferencia de arquitecturas donde la navegación se reduce a llamadas directas sobre un `NavController`, Noir Assistant implementa un flujo estructurado compuesto por múltiples componentes especializados. Cada uno participa en una etapa específica del proceso, permitiendo mantener separadas las responsabilidades de inicialización, interacción, coordinación de estado, ejecución de navegación y transición visual.

El resultado es un sistema donde la navegación se encuentra controlada por estado, sincronizada visualmente y desacoplada de los componentes individuales de la interfaz.

---

## Visión general del flujo

Toda navegación dentro de Noir Assistant sigue el mismo recorrido general:

```text
Inicio de la aplicación
          ↓
MainActivity
          ↓
AssistantApp
          ↓
Interacción del usuario
          ↓
NavigationActions
          ↓
MainViewModel
          ↓
CurtainOverlay
          ↓
AssistantApp + NavHost
          ↓
Nueva pantalla
          ↓
Notificación de renderizado
          ↓
MainViewModel
```

Cada componente participa en una etapa concreta de este proceso.

---

## Inicio del sistema

El flujo comienza cuando Android crea la actividad principal de la aplicación.

En este momento, `MainActivity` actúa como punto de entrada del sistema y prepara la infraestructura necesaria para que la navegación pueda comenzar a funcionar.

Durante esta fase:

* Se crea el entorno principal de ejecución.
* Se inicializa el estado compartido.
* Se crea la instancia principal de `MainViewModel`.
* Se espera a que el sistema alcance un estado operativo mediante `isAppReady`.

Una vez completada esta etapa, el control pasa a `AssistantApp`, que se convierte en el núcleo operativo de la navegación.

Para más información: [MainActivity](./components/MainActivity.md)

---

## Construcción del entorno de navegación

Cuando la aplicación se encuentra lista, `AssistantApp` construye la estructura completa de navegación utilizada por la interfaz.

Aquí se crean e integran elementos como:

* NavController
* NavHost
* Drawer de navegación
* Scaffold principal
* NavigationActions

Además, se registran los grafos y pantallas que forman parte del sistema de navegación.

En este punto aún no existe una solicitud de navegación activa. Simplemente se construye el entorno que permitirá ejecutarlas posteriormente.

Para más información: [AssistantApp](./components/AssistantApp.md)

---

## Interacción del usuario

Una vez renderizada la interfaz, el usuario puede interactuar con los mecanismos de navegación disponibles.

Actualmente Noir Assistant utiliza dos sistemas principales:

### NavigationSideBar

Permite desplazarse entre las áreas funcionales principales de la aplicación.

Por ejemplo:

* Home
* Trash
* Account
* Voice Task

### NavigationBottomBar

Permite realizar navegación contextual dentro del área principal de productividad.

Por ejemplo:

* TaskList
* NoteList

Estos componentes representan únicamente la capa visual del sistema y no contienen lógica de navegación interna.

Para más información: [Navigation UI](./Navigation_UI.md)

---

## Expresión de la intención de navegación

Cuando el usuario selecciona una opción de navegación, la interfaz no modifica pantallas directamente.

La interacción es enviada a `NavigationActions`, que actúa como una capa de abstracción entre la UI y el sistema de navegación.

Su responsabilidad consiste en:

* Interpretar la intención del usuario.
* Ejecutar operaciones previas necesarias.
* Delegar la solicitud al sistema de navegación.

En este momento aún no ocurre ningún cambio de pantalla.

El sistema únicamente registra que existe una intención de navegar hacia un nuevo destino.

Para más información: [AssistantApp](./components/AssistantApp.md)

---

## Control de la navegación

La solicitud es recibida por `MainViewModel`, que constituye la fuente de verdad del estado de navegación.

Aquí el sistema:

* Valida que no exista una transición activa.
* Registra el destino solicitado.
* Activa el estado de transición.
* Bloquea nuevas solicitudes simultáneas.

A partir de este punto la navegación deja de pertenecer a la interfaz y pasa a estar completamente controlada por el sistema de estado.

Para más información: [Navigation State Management](./components/Navigation_State_Management.md)

---

## Transición visual

Una vez iniciada la transición, entra en funcionamiento `CurtainOverlay`.

Este componente cubre temporalmente la interfaz mediante una animación visual que protege el proceso de cambio de pantalla.

Mientras la cortina permanece activa:

* La interfaz queda bloqueada.
* No se aceptan nuevas interacciones.
* El cambio de pantalla permanece sincronizado visualmente.

Cuando la cortina alcanza su estado completamente visible, el sistema permite continuar con la navegación.

Para más información: [Navigation State Management](./components/Navigation_State_Management.md)

---

## Ejecución de la navegación

Tras completarse la fase visual de transición, `MainViewModel` actualiza el estado de navegación.

`AssistantApp` detecta este cambio y sincroniza el `NavController` con el nuevo destino solicitado.

Como consecuencia:

* El NavHost selecciona el nuevo destino.
* Se construye la pantalla correspondiente.
* Comienza el ciclo de renderizado de la nueva interfaz.

En este momento ocurre el cambio efectivo de pantalla.

Para más información: [AssistantApp](./components/AssistantApp.md)

---

## Finalización de la transición

La navegación no concluye cuando el NavHost cambia de destino.

El sistema espera a que la nueva pantalla termine de renderizarse completamente.

Cuando esto ocurre:

* La pantalla notifica que está lista.
* MainViewModel libera el estado de transición.
* CurtainOverlay desaparece.
* La interfaz vuelve a estar disponible para el usuario.

Solo en este punto el ciclo de navegación se considera finalizado.

Para más información: [Navigation State Management](./components/Navigation_State_Management.md)

---

## Filosofía del sistema

La arquitectura de navegación de Noir Assistant se basa en un principio fundamental:

> La interfaz expresa intenciones de navegación, pero nunca ejecuta navegación directamente.

Cada cambio de pantalla atraviesa una cadena de componentes especializados que coordinan:

* La intención del usuario.
* El estado global de navegación.
* La transición visual.
* La ejecución del NavHost.
* La sincronización del renderizado.

Esta separación permite mantener un sistema consistente, predecible y extensible, donde cada componente participa únicamente en la parte del flujo que le corresponde controlar.
