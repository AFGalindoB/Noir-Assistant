# Noir Assistant — Network Repository

## Introducción

`NetworkRepositoryImpl` es la implementación principal de `NetworkRepository`, responsable de centralizar toda la comunicación entre Noir Assistant y la infraestructura remota.

Su objetivo consiste en abstraer completamente los detalles de red, autenticación y protocolos de comunicación utilizados por el sistema.

La UI y los ViewModels nunca interactúan directamente con:

- OkHttp
- Endpoints HTTP
- Tokens
- Headers
- JSON
- Protocolos de autenticación

Toda operación relacionada con comunicación remota atraviesa `NetworkRepository`.

## Dependencias

### UserPreferencesManager

Responsable de almacenar información persistente relacionada con la conexión:

- URL del servidor.
- Token de sesión.
- Nombre de usuario.

---

### OkHttpClient

Responsable de ejecutar todas las comunicaciones HTTP con la infraestructura remota.

## Arquitectura General

NetworkRepository coordina dos subsistemas principales:

| Componente             | Responsabilidad                        |
|:-----------------------|:---------------------------------------|
| UserPreferencesManager | Persistencia de configuración y sesión |
| OkHttpClient           | Comunicación HTTP                      |

NetworkRepository actúa como fachada entre ambos componentes, exponiendo una interfaz de dominio independiente de los detalles de infraestructura.

Además de coordinar la infraestructura de comunicación, NetworkRepository implementa el protocolo utilizado por Noir Assistant para el registro de dispositivos, autenticación, autorización y acceso a servicios remotos.

## Funcionalidades

### Procesamiento de QR

El protocolo QR funciona como mecanismo de descubrimiento y configuración inicial de la infraestructura remota, permitiendo que la aplicación obtenga dinámicamente la información necesaria para establecer comunicación con el servidor.

NetworkRepository implementa la interpretación del protocolo QR de Noir.

Su responsabilidad consiste en:

- Validar la compatibilidad del protocolo Noir.
- Extraer parámetros de configuración.
- Recuperar endpoints.
- Recuperar identificadores de sesión.
- Inicializar configuraciones de conexión.

Los QR inválidos o incompatibles son rechazados antes de que alcancen capas superiores.

Durante el procesamiento también puede actualizarse la configuración persistente de conexión necesaria para establecer comunicación con la infraestructura remota.

---

### Verificación de Conectividad

Permite comprobar la disponibilidad del servidor remoto mediante un Health Check dedicado.

La verificación utiliza una configuración temporal de red con tiempos de espera reducidos, permitiendo detectar rápidamente indisponibilidad del servidor sin afectar otras configuraciones de comunicación.

---

### Gestión de Autenticación

NetworkRepository implementa el ciclo completo de autenticación utilizado por Noir Assistant.

Esto incluye:

- Solicitud de autorización de dispositivos.
- Obtención de tokens.
- Validación de sesiones existentes.
- Invalidación automática de credenciales locales cuando la infraestructura remota informa que ya no son válidas.

Además de validar usuarios y sesiones, el proceso de autenticación incorpora la identificación del dispositivo que realiza la solicitud, permitiendo que la infraestructura remota administre autorizaciones a nivel de hardware.

NetworkRepository actúa además como guardián del estado de sesión local, eliminando automáticamente credenciales persistidas cuando la infraestructura remota informa que la sesión ya no es válida.

---

### Comunicación con Servicios de IA

NetworkRepository proporciona la infraestructura necesaria para comunicarse con los servicios de procesamiento de audio.

Sus responsabilidades incluyen:

- Construcción automática de solicitudes autenticadas.
- Transmisión autenticada de archivos de audio.
- Autenticación de solicitudes.
- Recuperación de resultados de procesamiento remoto.
- Traducción de respuestas HTTP a resultados de dominio.

---

### Traducción de Errores

Las respuestas de infraestructura nunca son expuestas directamente a capas superiores.

NetworkRepository transforma:

- Códigos HTTP.
- Excepciones de red.
- Errores de protocolo.

Los resultados de comunicación son representados mediante `NetworkResult`:

| Resultado          | Significado                                                                                                               |
|:-------------------|:--------------------------------------------------------------------------------------------------------------------------|
| SUCCESS            | La operación fue completada correctamente.                                                                                |
| LOGIC_ERROR        | El servidor respondió pero la operación no pudo completarse por reglas de negocio, autenticación o errores del protocolo. |
| CONNECTIVITY_ERROR | No fue posible establecer comunicación con la infraestructura remota.                                                     |

`NetworkResult` representa el resultado general de la operación mientras que la información complementaria es proporcionada mediante mensajes de dominio asociados a cada solicitud.

La interpretación de errores se realiza dentro del repository, esto desacopla completamente la lógica de negocio de los detalles específicos del protocolo HTTP, evitando que las capas superiores dependan de códigos HTTP específicos.

---

### Identidad del Dispositivo

Las operaciones relacionadas con autenticación y autorización incorporan información de identificación del dispositivo.

La identificación del dispositivo forma parte de la política de seguridad de la infraestructura remota y permite:

- Autorizar dispositivos específicos.
- Revocar accesos individualmente.
- Asociar sesiones a hardware concreto.
- Aplicar políticas de seguridad por dispositivo.

La identidad del dispositivo forma parte integral del protocolo de autenticación utilizado por Noir Assistant.

## Filosofía de Diseño

NetworkRepository fue diseñado como una capa de integración con infraestructura remota.

Su objetivo no consiste únicamente en ejecutar solicitudes HTTP, sino coordinar múltiples aspectos necesarios para mantener una comunicación segura y consistente con la infraestructura remota:

- Configuración persistente.
- Gestión de sesiones.
- Protocolos de autenticación.
- Comunicación con servicios remotos.
- Traducción de errores.

Actúa como una barrera de aislamiento que impide que detalles de red se propaguen hacia otros repositories, ViewModels, Managers o capas de presentación.