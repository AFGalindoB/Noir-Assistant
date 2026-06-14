# Noir Assistant V2.0.0

![Logo App](docs/images/logo.png)

**Noir Assistant** es una plataforma personal de captura y organización de información diseñada bajo una arquitectura *offline-first*, modular y reactiva.

Su objetivo es permitir que el usuario registre información de forma rápida y natural mientras mantiene control local sobre sus datos, configuración y flujo de trabajo.

La aplicación combina un cliente Android moderno desarrollado con Jetpack Compose junto a una infraestructura de procesamiento desacoplada para ofrecer:

* Gestión de tareas y notas.
* Captura de información mediante audio.
* Procesamiento remoto de voz a texto.
* Persistencia local reactiva.
* Funcionamiento resiliente ante interrupciones de conectividad.

Uno de los pilares del proyecto es la soberanía de los datos. Noir Assistant no depende de servicios propietarios obligatorios ni de infraestructuras centralizadas. El usuario puede desplegar y administrar su propio servidor, manteniendo control sobre la infraestructura utilizada por el sistema y conservando la información persistente dentro de su dispositivo.

La arquitectura adopta un modelo donde el cliente Android constituye la fuente principal de datos y estado de la aplicación. Los servicios remotos actúan exclusivamente como proveedores de capacidades especializadas de procesamiento y no como repositorios centrales de información.

Este enfoque permite reducir dependencias permanentes de conectividad, mejorar la resiliencia operativa y mantener una experiencia consistente incluso cuando la infraestructura remota no se encuentra disponible.

La versión `2.0.0` introduce una evolución completa de la arquitectura incorporando procesamiento remoto desacoplado de audio, autenticación modular, persistencia reactiva unificada y una refactorización integral orientada a escalabilidad, mantenibilidad y resiliencia.

## Funcionalidades

### Gestión de Tareas y Notas

![Pantalla principal](docs/images/home.jpeg)

La aplicación centraliza tareas y notas dentro de una experiencia unificada, rápida y consistente, diseñada para minimizar distracciones y facilitar la organización diaria.

- Creación y edición rápida mediante diálogos unificados
- Organización temporal de actividades y contenido
- Sistema de eliminación reversible con papelera integrada
- Interacciones optimizadas mediante gestos y acciones rápidas
- Persistencia local robusta y reactiva
- Actualización reactiva automática de contenido

---

### Captura Inteligente de Audio

![Pantalla audios](docs/images/audio.jpeg)

Noir Assistant permite capturar información mediante notas de voz y enviarlas posteriormente a una infraestructura de procesamiento especializada.

Las grabaciones permanecen almacenadas localmente y pueden ser procesadas bajo demanda mediante una cola persistente diseñada para tolerar interrupciones de conectividad y reinicios de la aplicación.

- Grabación de audio integrada
- Procesamiento remoto bajo demanda
- Conversión de voz a estructuras organizadas
- Cola persistente de procesamiento
- Recuperación automática ante fallos
- Persistencia reactiva y resiliente

---

### Experiencia de Usuario

La experiencia de usuario fue diseñada bajo una filosofía reactiva y offline-first, priorizando fluidez, claridad visual y continuidad operativa incluso ante cambios de conectividad o reinicios del sistema.

#### Personalización e Internacionalización

![Pantalla de cuenta](docs/images/account.jpeg)

- Cambio dinámico de idioma dentro de la aplicación
- Personalización de perfil e identidad visual
- Configuración persistente de preferencias
- Sistema de foto de perfil con edición interactiva

#### Recuperación y Protección de Información

![Pantalla papelera](docs/images/trash.jpeg)

- Papelera integrada para contenido eliminado
- Restauración de tareas, notas y audios
- Protección ante eliminaciones accidentales
- Conservación temporal antes de eliminación definitiva

---

### Procesamiento Desacoplado y Resiliencia

Noir Assistant adopta una arquitectura offline-first donde la información persistente permanece almacenada localmente en el dispositivo.

Los servicios remotos no actúan como una fuente principal de datos. Su función consiste en proporcionar capacidades especializadas de procesamiento que pueden utilizarse cuando sea necesario.

Este enfoque permite:

- Persistencia local como fuente principal de datos
- Procesamiento remoto bajo demanda
- Cola persistente de solicitudes
- Recuperación automática de estado
- Gestión resiliente de conectividad
- Estados globales de infraestructura
- Continuidad operativa ante reinicios y fallos parciales

---

### Sistema de Recordatorios Persistentes

La aplicación incorpora un sistema de recordatorios diseñado para mantener continuidad operativa incluso ante reinicios del dispositivo o cierres inesperados.

- Recordatorios diarios configurables
- Reprogramación automática tras reinicios
- Notificaciones persistentes
- Ejecución resiliente en segundo plano

## Tecnologías Utilizadas

Noir Assistant combina tecnologías modernas de desarrollo móvil, persistencia local y procesamiento remoto para construir una plataforma reactiva, resiliente y preparada para evolucionar de forma independiente en cada una de sus capas.

### Cliente Android

- **Kotlin** — Lenguaje principal de desarrollo.
- **Jetpack Compose** — Construcción de interfaces declarativas y reactivas.
- **Material Design 3** — Sistema moderno de diseño visual.
- **Room** — Persistencia local estructurada y reactiva.
- **Jetpack DataStore** — Gestión persistente de preferencias y configuración.
- **Coroutines & Flow** — Manejo asíncrono y reactivo de estados.
- **OkHttp** — Comunicación HTTP desacoplada con el backend.

---

### Backend y Procesamiento

- **FastAPI** — Backend principal y gestión de endpoints REST.
- **Python** — Procesamiento de audio y lógica de integración.
- **Rust** — Servicios críticos orientados a rendimiento y seguridad.
- **Redis** — Gestión temporal de sesiones y estados efímeros.
- **SQLite** — Persistencia ligera para servicios y metadatos del backend.
- **Faster-Whisper Large** — Conversión de voz a texto ejecutada dentro de la infraestructura de procesamiento.
- **Structured Data Pipeline** — Transformación automática de transcripciones en estructuras organizadas consumibles por la aplicación.

---

### Arquitectura y Rendimiento Android

- **MVVM + Repository Pattern** — Arquitectura modular y escalable.
- **Repository-Centric Architecture** — Coordinación desacoplada entre dominio, persistencia e infraestructura.
- **Offline-First Architecture** — Persistencia local como núcleo operativo.
- **Reactive State Management** — Propagación reactiva de estado entre persistencia, dominio y presentación.
- **Baseline Profiles** — Optimización avanzada de tiempos de arranque y ejecución.

## Plataformas Validadas

La infraestructura backend ha sido probada exitosamente en:

| Sistema Operativo | Arquitectura |
| ----------------- | ------------ |
| Windows           | x86_64       |
| Linux             | ARM64        |

La arquitectura del servidor fue diseñada para mantener compatibilidad multiplataforma siempre que las dependencias de procesamiento utilizadas por el sistema estén disponibles para la plataforma objetivo.

## Requisitos del Sistema

### Cliente Android

- **Android mínimo:** Android 8.0 Oreo (API 26)
- **Target SDK:** Android 15 (API 35)
- **Java:** 17
- **Kotlin JVM Target:** 17

La aplicación fue desarrollada utilizando tecnologías modernas del ecosistema Android con una arquitectura reactiva y offline-first.

---

### Backend y Procesamiento

El backend está construido sobre una arquitectura desacoplada orientada a procesamiento de audio y gestión distribuida de sesiones.

#### Stack Principal

- **Python 3.13**
- **FastAPI**
- **Rust (Edition 2021)**
- **Redis**
- **SQLite**

#### Dependencias Principales

- **Uvicorn** — Servidor ASGI para FastAPI
- **PyJWT** — Gestión de autenticación basada en JWT
- **Jinja2** — Renderizado de vistas administrativas
- **QRCode + Pillow** — Generación de códigos QR y procesamiento auxiliar de imágenes
- **PyO3** — Integración nativa entre Rust y Python

#### Procesamiento Inteligente de Audio

- **Faster-Whisper Large** — Conversión avanzada de voz a texto mediante inferencia local.
- Pipeline preparado para transformación automática de audio en estructuras JSON organizadas.

## Licencia

Este proyecto se distribuye bajo la **Licencia MIT**.

Noir Assistant sigue una filosofía de desarrollo abierta, modular y extensible, orientada a construir un asistente personal centrado en privacidad, resiliencia y control local de los datos.

---

## Documentación Técnica

La documentación técnica describe la arquitectura, persistencia, procesamiento de audio, infraestructura backend y funcionamiento interno del sistema.

### Cliente Android

* [Sistema de Datos y Persistencia](./docs/android/data_system/Data_System.md)
* [Ciclo de Vida de Notificaciones y Alarmas](./docs/android/notifications_system/Notifications_System.md)
* [Sistema de Navegacion de Pantallas](./docs/android/navigation_system/Navigation_System.md)

### Backend

* Documentación del servidor (próximamente)

---

Noir Assistant es un proyecto enfocado en construir una plataforma personal de captura y organización de información donde la persistencia local, el procesamiento desacoplado y la soberanía de los datos constituyen los pilares fundamentales de la arquitectura.
