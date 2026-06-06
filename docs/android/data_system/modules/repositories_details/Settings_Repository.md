# Noir Assistant — Settings Repository

## Introducción

`OfflineSettingsRepository` es la implementación principal de `SettingsRepository`, responsable de administrar el estado de configuración de la aplicación y coordinar las operaciones relacionadas con autenticación y conectividad.

A diferencia de otros repositories orientados principalmente a persistencia estructurada, SettingsRepository centraliza tanto la configuración local de la aplicación como las operaciones necesarias para interactuar con la infraestructura remota.

Su responsabilidad consiste en:

* Administrar preferencias persistentes.
* Gestionar configuración de conexión.
* Administrar credenciales de sesión.
* Exponer estado reactivo de configuración.
* Coordinar procesos de autenticación.
* Validar disponibilidad de infraestructura remota.

La UI y los ViewModels nunca interactúan directamente con:

* DataStore
* UserPreferencesManager
* Tokens de autenticación
* Configuración persistente
* Protocolos de autenticación
* Operaciones de conectividad

Toda operación relacionada con configuración, sesión y autenticación iniciada desde las capas superiores atraviesa `SettingsRepository`.

SettingsRepository constituye además una de las principales fuentes de estado global reactivo de la aplicación, permitiendo que diferentes componentes observen cambios de configuración sin depender directamente de DataStore.

## Dependencias

### UserPreferencesManager

Responsable de la persistencia de configuración local mediante DataStore.

Permite administrar:

* Preferencias de usuario.
* Idioma.
* Hora de recordatorios.
* Usuario del servidor.
* URL de conexión.
* Token de autenticación.

---

### NetworkRepository

Responsable de la comunicación con la infraestructura remota.

Permite:

* Verificar disponibilidad del servidor.
* Validar credenciales.
* Procesar protocolos de autenticación.
* Gestionar solicitudes de acceso.
* Obtener tokens de sesión.

## Arquitectura General

SettingsRepository coordina dos subsistemas principales:

| Componente             | Responsabilidad                              |
| :--------------------- | :------------------------------------------- |
| UserPreferencesManager | Persistencia de configuración y sesión local |
| NetworkRepository      | Comunicación y autenticación remota          |

SettingsRepository actúa como fachada entre ambos componentes, proporcionando una interfaz unificada para administrar configuración local, estado de sesión e integración con infraestructura remota.

## Funcionalidades

### Gestión de Preferencias

Permite administrar la configuración persistente de la aplicación.

Entre las preferencias disponibles se encuentran:

* Idioma.
* Hora de recordatorios.
* Información general de usuario.
* Configuración de conexión.

La persistencia de estas configuraciones es delegada a UserPreferencesManager.

---

### Flujos Reactivos

SettingsRepository expone información de configuración mediante Flow, permitiendo que la interfaz observe cambios de manera reactiva.

Entre los flujos disponibles se encuentran:

* Preferencias completas del usuario.
* Idioma configurado.
* Usuario del servidor.
* Estado de configuración de URL.
* Estado de configuración de token.

Las actualizaciones realizadas sobre la configuración son propagadas automáticamente hacia las capas consumidoras.

---

### Configuración de Conectividad

SettingsRepository administra la información necesaria para establecer comunicación con la infraestructura remota.

Esto incluye:

* URL del servidor.
* Usuario asociado al servidor.
* Estado de configuración de conexión.

La configuración remota permanece desacoplada de las capas superiores mediante la interfaz del repository.

---

### Gestión de Sesión

El repository administra el ciclo de vida de las credenciales utilizadas para comunicarse con la infraestructura remota.

Permite:

* Guardar tokens de autenticación.
* Recuperar credenciales persistidas.
* Eliminar sesiones almacenadas.
* Consultar el estado actual de autenticación.
* Verificar la existencia de configuración mínima necesaria para autenticación.

Las capas superiores nunca interactúan directamente con mecanismos de almacenamiento de credenciales.

---

### Verificación de Infraestructura

SettingsRepository proporciona mecanismos para validar el estado de la infraestructura remota.

Permite:

* Verificar disponibilidad del servidor.
* Validar credenciales almacenadas.
* Detectar sesiones inválidas.
* Confirmar que la configuración actual es funcional.

Estas operaciones son delegadas internamente a NetworkRepository.

---

### Integración con el Protocolo QR

Una de las responsabilidades más especializadas del repository consiste en coordinar el flujo de autenticación basado en códigos QR.

El proceso general consiste en:

1. Recepción de datos QR.
2. Validación del protocolo Noir.
3. Extracción de parámetros de autenticación.
4. Actualización de configuración local cuando sea necesario.
5. Ejecución de la operación remota correspondiente.

Este mecanismo permite desacoplar completamente a las capas superiores de los detalles del protocolo de autenticación utilizado por Noir Assistant.

---

### Solicitud de Autorización

SettingsRepository permite iniciar procesos de autorización de dispositivos mediante códigos QR de registro.

Durante este flujo:

* Se valida la estructura del protocolo.
* Se recuperan los parámetros de conexión.
* Se actualiza la configuración local necesaria.
* Se solicita autorización a la infraestructura remota.

---

### Obtención de Tokens

Una vez autorizado un dispositivo, SettingsRepository permite solicitar credenciales de acceso mediante códigos QR de autenticación.

Durante este proceso:

* Se valida el protocolo QR.
* Se recuperan los parámetros necesarios.
* Se solicita un nuevo token de acceso.
* El token obtenido es persistido localmente.

Esto permite mantener desacoplado el mecanismo de autenticación del resto de la aplicación.

---

### Delegación de Infraestructura

SettingsRepository no implementa directamente protocolos de autenticación ni lógica de comunicación remota.

Las operaciones relacionadas con infraestructura son delegadas a NetworkRepository, mientras que SettingsRepository coordina el flujo de configuración y sesión necesario para que dichas operaciones puedan ejecutarse.

Esto permite mantener separadas las responsabilidades de:

- Persistencia de configuración.
- Gestión de sesión.
- Comunicación remota.


## Filosofía de Diseño

SettingsRepository fue diseñado como una capa de coordinación para la configuración global de la aplicación.

Su objetivo no consiste únicamente en almacenar preferencias, sino proporcionar una interfaz unificada capaz de administrar:

* Configuración local.
* Estado de sesión.
* Preferencias persistentes.
* Integración con autenticación.
* Conectividad remota.
* Estado reactivo de configuración.

Actúa como una barrera de aislamiento que impide que detalles de DataStore, autenticación o configuración de infraestructura se propaguen hacia ViewModels, Managers o capas de presentación.

De esta manera la aplicación puede interactuar con la configuración y el estado de sesión mediante una API de dominio estable, independiente de los mecanismos concretos de persistencia y comunicación utilizados internamente.
