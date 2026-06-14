# Noir Assistant — MainActivity

`MainActivity` actúa como el punto de entrada del sistema de navegación de Noir Assistant.

Su responsabilidad no es construir pantallas ni gestionar lógica de interfaz, sino establecer el entorno inicial donde el sistema de navegación puede comenzar a operar.

Desde esta capa se orquesta la transición entre el estado de arranque de la aplicación y el sistema de navegación activo, incluyendo la coordinación del `ViewModel` principal, la inicialización del contenedor de aplicación y la activación progresiva de la interfaz.

En términos arquitectónicos, MainActivity funciona como el puente entre el ciclo de vida del sistema operativo y el flujo de navegación interno de la aplicación.

## Rol dentro del sistema de navegación

Dentro de la arquitectura general, `MainActivity` se ubica en el nivel más alto del sistema y cumple el rol de inicializador del entorno de navegación.

Su relación con los componentes principales es la siguiente:

- Proporciona el `MainViewModel`, que actúa como estado central de navegación.
- Activa `AssistantApp`, donde se construye el `NavHost`.
- Superpone `CurtainOverlay`, que participa en las transiciones visuales.
- Controla la fase inicial en la que el sistema de navegación aún no está disponible.

## Fase de inicialización del sistema de navegación

Antes de que el sistema de navegación pueda operar, la aplicación atraviesa una fase de preparación controlada.

Durante esta fase:

- El sistema aún no expone pantallas funcionales.
- El estado de navegación aún no está activo.
- El `MainViewModel` determina cuándo la aplicación está lista para iniciar el flujo visual.

Este mecanismo introduce el concepto de “ready state” del sistema de navegación, el cual controla cuándo el NavHost puede ser renderizado de forma segura.

Una vez este estado se activa:

- Se habilita `AssistantApp`.
- Se considera que el sistema de navegación ha entrado en estado operativo.

## Relación con el ciclo de vida del sistema operativo

A diferencia de los componentes internos de navegación, MainActivity está directamente acoplada al ciclo de vida del sistema operativo Android.

Esto le permite:

- Mantener el punto de entrada estable de la aplicación.
- Reiniciar el estado de navegación cuando la aplicación vuelve a primer plano.
- Asegurar que el sistema de navegación se sincronice con el estado real del sistema.

Sin embargo, esta interacción no afecta la lógica de navegación interna, que permanece completamente desacoplada dentro de `MainViewModel` y `AssistantApp`.

## Transición hacia el sistema de navegación

Una vez MainActivity completa su fase de inicialización, la responsabilidad de la navegación se transfiere completamente a:

- `MainViewModel` como controlador de estado.
- `AssistantApp` como ejecutor del grafo de navegación.
- `CurtainOverlay` como gestor de transición visual.

A partir de este punto, MainActivity deja de participar activamente en el flujo de navegación y actúa únicamente como contenedor del ciclo de vida de la aplicación.