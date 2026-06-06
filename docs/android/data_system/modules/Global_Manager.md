# Noir Assistant — Global State Manager

## Introducción

`AndroidGlobalStateManager` es la implementación principal de `GlobalStateManager`, responsable de centralizar el estado global de la aplicación relacionado con permisos, conectividad, autenticación y disponibilidad de infraestructura.

A diferencia de los repositories del sistema, `GlobalStateManager` no administra persistencia ni representa un dominio de negocio específico.

Su responsabilidad consiste en coordinar múltiples fuentes de información para exponer una representación coherente del estado operativo general de la aplicación.

Permite administrar:

- Estado de permisos del sistema.
- Estado de conectividad con la infraestructura remota.
- Estado de autenticación.
- Mensajes globales asociados a la comunicación con el servidor.
- Estado de configuración de conexión.

La UI y los ViewModels nunca interactúan directamente con:

- Permisos Android.
- Validaciones de conectividad.
- Estados de infraestructura remota.
- Verificaciones de sesión.
- Coordinación entre configuración y autenticación.

Toda operación relacionada con estado global atraviesa GlobalStateManager.

## Dependencias

### Context

Responsable de proporcionar acceso a capacidades del sistema Android necesarias para verificar permisos y estados del dispositivo. Permite:

- Consultar permisos de notificaciones.
- Acceder a servicios del sistema.

---

### SettingsRepository

Responsable de proporcionar:

- Estado de configuración.
- Estado de autenticación.
- Configuración persistida.
- Validaciones de conectividad.
- Verificación de credenciales.

GlobalStateManager utiliza esta información para construir una representación global del estado operativo de la aplicación.

---

### Application Scope

Utilizado para ejecutar procesos reactivos de larga duración asociados al ciclo de vida completo de la aplicación.

Permite mantener observadores permanentes sobre cambios de configuración y autenticación.

## Arquitectura General

GlobalStateManager coordina tres subsistemas principales:

| Componente         | Responsabilidad                     |
|:-------------------|:------------------------------------|
| Context            | Estado del sistema Android          |
| SettingsRepository | Configuración y autenticación       |
| CoroutineScope     | Observación reactiva y coordinación |

GlobalStateManager actúa como una capa de agregación de estado que transforma múltiples fuentes de información en una representación única del estado global de la aplicación.

## Funcionalidades

### Estado de Permisos

GlobalStateManager mantiene información reactiva sobre permisos necesarios para el funcionamiento de la aplicación. Actualmente administra permisos de notificaciones.

El estado es expuesto mediante StateFlow y puede actualizarse mediante procesos de refresco explícitos.

---

### Estado de Configuración de Conexión

El manager expone información reactiva relacionada con la configuración de conexión.

Entre los estados disponibles se encuentran:

- URL configurada.
- Token configurado.

Estos estados son derivados directamente desde SettingsRepository y convertidos a StateFlow para facilitar su consumo global.

---

### Estado de Infraestructura

Una de las responsabilidades principales del manager consiste en mantener una representación simplificada del estado de la infraestructura remota.

Este estado es representado mediante ServerStatus.

Los posibles estados incluyen:

- `UNLINKED`
- `NAME_REQUIRED`
- `READY_TO_CONNECT`
- `DISCONNECTED`
- `ONLINE`

Cada estado representa una condición operativa diferente del sistema.

---

### Validación de Conectividad

GlobalStateManager coordina el proceso completo de validación de infraestructura.

El flujo general puede incluir:

- Verificar existencia de URL configurada.
- Verificar disponibilidad del servidor.
- Verificar existencia de credenciales.
- Validar la sesión actual.
- Actualizar el estado global correspondiente.

Esto permite condensar múltiples validaciones técnicas en un único estado consumible por la interfaz.

---

### Validación de Identidad

GlobalStateManager supervisa la existencia de información mínima requerida para operar correctamente.

Actualmente valida el nombre de usuario configurado.

Cuando la información requerida no se encuentra disponible, el estado global es actualizado para reflejar la condición detectada.

---

### Mensajes Globales

Además del estado general de infraestructura, el manager mantiene un canal reactivo de mensajes globales representados mediante ServerMessage.

Estos mensajes permiten comunicar:

- Errores de conectividad.
- Problemas de autenticación.
- Estados de autorización.
- Fallos de infraestructura.

Los mensajes pueden ser publicados, actualizados o eliminados mediante la interfaz del manager.

---

### Observación Reactiva

GlobalStateManager puede mantener observadores permanentes reactivos sobre fuentes de configuración relevantes.

Cuando determinada información de configuración cambia, el manager puede revalidar automáticamente el estado operativo de la aplicación y actualizar los estados globales correspondientes. Por ejemplo, cuando alguno de estos estados cambia:

- Se reevalúa la conectividad.
- Se valida la configuración.
- Se actualiza el estado global.

Esto permite que la aplicación reaccione automáticamente a cambios de configuración sin requerir sincronizaciones manuales.

## Filosofía de Diseño

GlobalStateManager fue diseñado como una capa de coordinación de estado global para la aplicación.

Su objetivo no consiste en almacenar información ni ejecutar lógica de negocio específica, sino consolidar múltiples fuentes de estado dispersas en una representación coherente y reactiva del estado operativo general del sistema.

Actúa como un punto central de observación para:

- Configuración.
- Autenticación.
- Permisos.
- Infraestructura remota.
- Mensajes globales.

De esta manera los ViewModels y componentes de presentación pueden consumir un único modelo de estado global sin depender directamente de las múltiples fuentes de información utilizadas internamente para construirlo.

GlobalStateManager funciona como la principal fuente de verdad (Single Source of Truth) para el estado operativo global de la aplicación.