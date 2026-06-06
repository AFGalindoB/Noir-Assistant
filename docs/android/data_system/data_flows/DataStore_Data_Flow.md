# Noir Assistant — DataStore Data Flow

## Introducción

Este documento describe cómo fluye la información de configuración y preferencias persistentes dentro de Noir Assistant mediante **Jetpack DataStore**.

A diferencia de Room, DataStore no almacena entidades de dominio complejas ni relaciones estructuradas. Su propósito consiste en persistir configuraciones ligeras, credenciales y preferencias de usuario mediante un modelo clave-valor reactivo.

La infraestructura de DataStore se encuentra encapsulada por `UserPreferencesManager` y es consumida exclusivamente a través de `SettingsRepository`.

## Arquitectura General

El flujo de configuración sigue la misma filosofía arquitectónica utilizada por el resto de la aplicación:

```text
UI
 ↓
SettingsViewModel
 ↓
SettingsRepository
 ↓
UserPreferencesManager
 ↓
DataStore
```

Cada capa posee una responsabilidad específica y desconoce los detalles internos de las demás.

## Información Persistida

Actualmente DataStore almacena información relacionada con:

### Perfil de Usuario

- Nombre
- Biografía
- Imagen de perfil
- Posición horizontal de imagen
- Posición vertical de imagen
- Nivel de zoom

---

### Configuración General

- Idioma
- Hora de recordatorio

---

### Configuración de Conectividad y Estado de Sesión

- URL del servidor
- Nombre de usuario en el servidor
- Token de autenticación

## Flujo de Lectura

La lectura de configuración es completamente reactiva.

### 1. Persistencia

DataStore almacena los valores mediante claves persistentes.

La infraestructura física permanece encapsulada y nunca es consumida directamente por repositories o ViewModels.

---

### 2. UserPreferencesManager

UserPreferencesManager encapsula completamente la interacción con DataStore y transforma los valores persistidos en estructuras tipadas consumibles por capas superiores.

Expone distintos flujos reactivos especializados según el tipo de información requerida. Entre ellos:

```kotlin
Flow<UserPreferences>

Flow<String>

Flow<Boolean>
```

Estos flujos representan:

- Preferencias completas de usuario.
- Idioma.
- Estados de configuración.
- Estados de autenticación.

La información no siempre es expuesta mediante un único flujo agregado.

Dependiendo de la necesidad, UserPreferencesManager puede publicar flujos especializados para evitar que consumidores reaccionen a cambios irrelevantes.

Por ejemplo:

- Un cambio de idioma no requiere reconstruir información de perfil.
- La validación de credenciales no requiere observar preferencias visuales.

---

### 3. SettingsRepository

`SettingsRepository` actúa como frontera entre el dominio y DataStore.

Su responsabilidad consiste en:

Exponer información reactiva de configuración.
Coordinar acceso a preferencias.
Desacoplar las capas superiores de DataStore.

Los consumidores nunca interactúan directamente con `UserPreferencesManager`.

---

### 4. ViewModel

`SettingsViewModel` transforma los flujos obtenidos desde el repository en estados observables mediante `StateFlow`.

Esto permite:

- Integración directa con Compose.
- Conservación del último estado conocido.
- Reactividad alineada con el ciclo de vida.

---

### 5. UI

La interfaz observa exclusivamente estados expuestos por los ViewModels.

La UI nunca conoce:

- Claves de DataStore.
- Operaciones de persistencia.
- Estructuras internas de configuración.

Su responsabilidad consiste únicamente en representar el estado recibido.


## Flujo de Escritura

Las modificaciones de configuración siguen un flujo unidireccional.

### 1. UI

El usuario realiza una acción de configuración.

Por ejemplo:

- Actualizar perfil.
- Cambiar idioma.
- Modificar hora de recordatorio.
- Configurar servidor.

La UI emite únicamente la intención de cambio.

---

### 2. SettingsViewModel

El ViewModel recibe la solicitud y coordina la operación correspondiente.

Ejemplos:

- Actualizar perfil
- Actualizar lenguaje
- Actualizar recordatorio
- Actualizar nombre de usuario

---

### 3. SettingsRepository

El repository interpreta la operación solicitada y delega la persistencia al componente adecuado.

Su responsabilidad consiste en mantener una API estable para las capas superiores.

---

### 4. UserPreferencesManager

`UserPreferencesManager` ejecuta la escritura real sobre DataStore. Dependiendo de la operación puede:

- Actualizar una única clave.
- Actualizar múltiples valores de forma coordinada.

Toda interacción con DataStore ocurre exclusivamente en esta capa.

---

### 5. Propagación Reactiva

Una vez persistido el cambio:

- DataStore emite un nuevo valor.
- UserPreferencesManager recibe la actualización.
- SettingsRepository propaga el cambio.
- SettingsViewModel actualiza sus StateFlow.
- Compose recompone automáticamente la interfaz.

No existen recargas manuales ni sincronizaciones explícitas.

La actualización ocurre de forma reactiva a través de todo el sistema.

## Integración con GlobalStateManager

Algunos valores almacenados en DataStore son utilizados para construir estados globales observados por toda la aplicación.

Entre ellos:

- Estado de autenticación.
- Configuración de conexión.
- Disponibilidad de credenciales.

Algunos valores persistidos en DataStore son utilizados indirectamente para construir estados globales compartidos por toda la aplicación.

La información fluye a través de SettingsRepository antes de ser consolidada por GlobalStateManager.

## Filosofía de Diseño

La infraestructura basada en DataStore fue diseñada para proporcionar persistencia ligera, reactiva y desacoplada para configuraciones de aplicación.

Su objetivo no consiste únicamente en almacenar preferencias, sino proporcionar una fuente confiable de configuración persistente capaz de integrarse naturalmente con el modelo reactivo utilizado por Noir Assistant.

La arquitectura garantiza que detalles relacionados con claves, almacenamiento o APIs de DataStore permanezcan completamente encapsulados detrás de `UserPreferencesManager` y `SettingsRepository`, manteniendo una separación clara entre persistencia, dominio y presentación.