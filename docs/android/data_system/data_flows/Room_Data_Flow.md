# Noir Assistant — Room Data Flow

## Introducción

Room constituye el mecanismo principal de persistencia estructurada utilizado por Noir Assistant.

Su responsabilidad consiste en almacenar información perteneciente al dominio de la aplicación de forma local, persistente y reactiva.

La persistencia mediante Room es utilizada para entidades cuya información forma parte del estado principal de la aplicación, incluyendo:

* Tareas.
* Notas.
* Solicitudes de audio.
* Estados asociados al procesamiento local.

La interfaz nunca interactúa directamente con Room.

Todo acceso a persistencia ocurre a través de repositories especializados.

## Arquitectura General

La comunicación entre la interfaz y la persistencia sigue una estructura de capas desacopladas.

```text
UI
 ↓
ViewModel
 ↓
Repository
 ↓
DAO
 ↓
Room
```

Cada capa posee responsabilidades específicas y únicamente conoce las dependencias necesarias para cumplir su función.

## Modelos de Datos

La arquitectura diferencia claramente entre los modelos utilizados por cada capa.

| Capa         | Modelo    | Responsabilidad                 |
| ------------ | --------- | ------------------------------- |
| UI           | FormState | Captura de datos de entrada     |
| Dominio      | Domain    | Representación del negocio      |
| Persistencia | Entity    | Representación física para Room |

Esta separación permite que cambios en la persistencia no afecten directamente al dominio ni a la interfaz.

## Flujo de Lectura

La lectura de información sigue un modelo reactivo basado en `Flow` y `StateFlow`.

### Persistencia

Room almacena entidades estructuradas mediante tablas SQLite.

Las modificaciones realizadas sobre dichas tablas generan nuevas emisiones de datos.

### DAO

Los DAOs exponen flujos reactivos que representan el contenido persistido.

```text
Room
 ↓
Flow<Entity>
```

Cada modificación realizada sobre la base de datos produce automáticamente una nueva emisión.

### Repository

Los repositories consumen entidades provenientes de Room y realizan las transformaciones necesarias para convertirlas en modelos de dominio.

```text
Entity
 ↓
Repository
 ↓
Domain
```

Los repositories constituyen la única capa autorizada para conocer simultáneamente modelos de persistencia y modelos de dominio.

### ViewModel

Los ViewModels consumen modelos de dominio expuestos por los repositories.

Su responsabilidad consiste en:

* Mantener estado reactivo.
* Aplicar transformaciones orientadas a presentación.
* Preparar estructuras consumibles por Compose.

Ejemplos comunes incluyen:

* Agrupaciones.
* Filtros.
* Ordenamientos.
* Estados derivados.

### UI

La interfaz consume exclusivamente estados reactivos provenientes de ViewModels.

```text
StateFlow<Domain>
```

La UI no conoce:

* Room.
* DAO.
* Entities.
* Mecanismos de persistencia.

---

## Flujo de Escritura

La escritura sigue el recorrido inverso.

```text
UI
 ↓
FormState
 ↓
ViewModel
 ↓
Domain
 ↓
Repository
 ↓
Entity
 ↓
DAO
 ↓
Room
```

### Captura de Datos

La interfaz recopila información mediante componentes visuales y formularios.

El resultado de esta interacción se representa mediante modelos FormState.

### ViewModel

El ViewModel recibe la información capturada por la interfaz.

Su responsabilidad consiste en:

* Validar datos.
* Construir modelos de dominio.
* Iniciar operaciones de escritura.

### Repository

Los repositories reciben modelos de dominio y deciden cómo deben persistirse.

Esta capa realiza:

```text
Domain → Entity
```

Encapsulando completamente la representación utilizada por Room.

### Persistencia

Finalmente el DAO aplica las operaciones necesarias sobre la base de datos.

Estas operaciones pueden incluir:

* Inserción.
* Actualización.
* Eliminación.
* Restauración.
* Marcado para eliminación lógica.

---

## Reactividad del Sistema

Una característica fundamental de la arquitectura consiste en que las operaciones de escritura no actualizan directamente la interfaz.

Cuando una modificación es persistida:

```text
Escritura
 ↓
Room
 ↓
DAO
 ↓
Repository
 ↓
ViewModel
 ↓
UI
```

La actualización visual ocurre automáticamente mediante el flujo reactivo de lectura.

Esto garantiza que toda representación visual provenga siempre de la fuente de verdad persistida.

---

## Papel de Room dentro de la Arquitectura

Room actúa exclusivamente como mecanismo de persistencia estructurada.

No contiene:

* Lógica de negocio.
* Reglas de presentación.
* Coordinación de estados globales.
* Comunicación con servicios remotos.

Su responsabilidad se limita a almacenar y recuperar información estructurada de forma eficiente y reactiva.

Los detalles de acceso permanecen encapsulados detrás de repositories especializados, permitiendo que el resto del sistema opere sobre modelos de dominio independientes de la tecnología de persistencia utilizada internamente.
