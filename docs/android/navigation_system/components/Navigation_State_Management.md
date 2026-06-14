# Noir Assistant — Navigation State & Transition System (MainViewModel + CurtainOverlay)

## Introducción

Este componente del sistema de navegación de Noir Assistant define el núcleo de control del flujo de navegación y sincronización visual entre pantallas.

Está compuesto por dos elementos que funcionan como un sistema único:

- `MainViewModel`: controlador del estado de navegación
- `CurtainOverlay`: mecanismo de transición visual

Juntos forman el mecanismo que garantiza que cada cambio de pantalla ocurra de forma controlada, secuencial y visualmente protegida.

A diferencia de sistemas tradicionales donde la navegación ocurre directamente en el `NavController`, este sistema introduce una capa intermedia que separa:

- Intención de navegación
- Ejecución del cambio
- Confirmación visual de renderizado

## Rol dentro de la arquitectura de navegación

Este sistema representa el núcleo de control del flujo de navegación.

Su posición en la arquitectura es central entre:

- Interfaz de usuario (NavigationActions / AssistantApp)
- Sistema de ejecución (NavController)
- Capa visual de transición (CurtainOverlay)

```text
UI Intent
     ↓
MainViewModel (estado de navegación)
     ↓
CurtainOverlay (transición visual)
     ↓
AssistantApp → NavController
     ↓
Pantalla activa
     ↓
Notificación de renderizado
     ↑
MainViewModel
```

Este flujo convierte la navegación en un sistema controlado por estado y sincronizado por eventos visuales.

# MainViewModel — Núcleo de estado de navegación

## Responsabilidad principal

`MainViewModel` actúa como la fuente de verdad del estado de navegación.

Su función no es únicamente almacenar el destino actual, sino coordinar:

- Solicitudes de navegación
- Bloqueo de transiciones simultáneas
- Sincronización con la UI
- Control del ciclo completo de navegación

## Estado de navegación global

El sistema se compone de tres estados fundamentales:

### 1. Destino actual (`currentScreen`)

Representa la pantalla actualmente activa dentro del sistema de navegación.

Este estado:

- Determina qué renderiza NavHost
- Sincroniza AssistantApp
- Define la coherencia del flujo visual

### 2. Estado de transición (`isTransitioning`)

Indica si el sistema se encuentra en medio de una transición activa.

Este estado garantiza:

- Navegación secuencial
- Bloqueo de interacciones concurrentes
- Sincronización con CurtainOverlay

### 3. Destino pendiente (pendingDestination)

Representa una solicitud de navegación aún no aplicada.

Este mecanismo desacopla:

- Intención de navegación
- Ejecución real del cambio

## Modelo de navegación

El sistema sigue un flujo controlado dividido en etapas:

### Fase 1: Solicitud de navegación

Cuando se solicita una navegación:

- Se valida que no haya transición activa
- Se almacena el destino solicitado
- Se activa `isTransitioning`

En este punto, la navegación aún no ocurre.

### Fase 2: Transición visual (Curtain)

Cuando `CurtainOverlay` alcanza su estado completamente visible:

- Se confirma la transición visual
- Se actualiza `currentScreen`
- Se limpia el destino pendiente
- Se resetean elementos de UI global (TopBar / BottomBar)

Aquí ocurre el cambio real de contexto visual, pero aún no se permite interacción.

### Fase 3: Finalización de renderizado

Cuando la nueva pantalla termina de renderizarse:

- La pantalla notifica al ViewModel
- `isTransitioning` se establece en `false`
- La interfaz vuelve a estado interactivo
- `CurtainOverlay` inicia su salida

Este paso garantiza que la UI solo se habilita cuando está completamente lista.

# CurtainOverlay — Sistema de transición visual

## Responsabilidad

`CurtainOverlay` es el componente encargado de ocultar y proteger visualmente la interfaz durante las transiciones de navegación.

No representa contenido, sino que actúa como una capa de bloqueo y sincronización visual.

## Funcionamiento interno

El sistema funciona mediante una animación controlada de opacidad (alpha):

### Entrada (activación de transición)

Cuando `isVisible = true`:

1. La cortina inicia una animación de aparición (fade in)
2. La opacidad llega a 1 (pantalla completamente cubierta)
3. Se dispara `onFullyVisible()`

**Este evento es crítico:** marca el punto exacto donde la UI está completamente oculta y segura para cambiar de pantalla.

### Salida (finalización de transición)

Cuando `isVisible = false`:

- La cortina inicia una animación de desaparición (fade out)
- La opacidad disminuye gradualmente hasta 0
- La interfaz vuelve a ser interactiva

## Bloqueo de interacción

Durante su visibilidad, CurtainOverlay intercepta todos los eventos de entrada:

- Taps
- Scroll
- Gestos
- Interacción del usuario

Esto garantiza que ninguna interacción ocurra durante el cambio de pantalla.

## Papel dentro del sistema de navegación

CurtainOverlay no decide navegación.

Su rol es exclusivamente:

- Sincronizar visualmente la transición
- Bloquear interacción durante el cambio
- Marcar el punto exacto de “cambio seguro”

# Conclusión

El sistema `MainViewModel + CurtainOverlay` constituye el núcleo de control del flujo de navegación en Noir Assistant.

No solo gestiona estados, sino que define el momento exacto en que una transición de pantalla es válida, segura y visible.

