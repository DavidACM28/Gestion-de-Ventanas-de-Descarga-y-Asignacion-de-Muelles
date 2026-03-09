# Lab 04 Gestion de Ventanas de Descarga y Asignacion de Muelles en Mercado Mayorista de Santa Anita

## Contexto

En el Mercado Mayorista de Santa Anita ingresan diariamente camiones con frutas, verduras, abarrotes y productos refrigerados para descarga en distintos muelles. Cuando la programacion se gestiona por llamadas, hojas de calculo o mensajes dispersos, aparecen problemas frecuentes:

- doble reserva del mismo muelle en el mismo horario
- camiones asignados a muelles incompatibles con su tipo de carga
- llegadas tardias sin control ni trazabilidad
- cierres operativos por limpieza, mantenimiento o emergencias
- alta demanda que requiere cola de espera
- necesidad de reasignar atenciones cuando un muelle queda libre

## Alcance

Se trabajara con seis entidades relacionadas:

- Muelle
- EmpresaTransportista
- Camion
- ReservaDescarga
- CierreOperativo
- ColaEspera

## Dominio

### Entidad Muelle

#### Campos

- `id` Long autoincrement
- `codigo` String unico
- `nombre` String
- `tipoCargaPermitida` String `SECA` `REFRIGERADA` `MIXTA`
- `capacidadToneladas` BigDecimal
- `activo` boolean

### Entidad EmpresaTransportista

#### Campos

- `id` Long autoincrement
- `ruc` String unico
- `razonSocial` String
- `contactoNombre` String
- `contactoTelefono` String
- `email` String
- `activo` boolean

### Entidad Camion

#### Campos

- `id` Long autoincrement
- `placa` String unica
- `empresaId` FK a EmpresaTransportista
- `tipoCarga` String `SECA` `REFRIGERADA`
- `capacidadToneladas` BigDecimal
- `activo` boolean

### Entidad ReservaDescarga

#### Campos

- `id` Long autoincrement
- `muelleId` FK a Muelle
- `camionId` FK a Camion
- `fecha` LocalDate `yyyy-MM-dd`
- `horaInicio` LocalTime `HH:mm`
- `duracionMin` int `30` `60` `90` `120`
- `pesoEstimadoToneladas` BigDecimal
- `estado` String `SOLICITADA` `CONFIRMADA` `EN_COLA` `CHECK_IN` `EN_DESCARGA` `FINALIZADA` `CANCELADA` `NO_SHOW`
- `tipoMercaderia` String
- `nota` String opcional
- `createdAt` Instant
- `updatedAt` Instant
- `version` Long para optimistic locking

### Entidad CierreOperativo

#### Campos

- `id` Long autoincrement
- `muelleId` FK a Muelle
- `fecha` LocalDate `yyyy-MM-dd`
- `horaInicio` LocalTime `HH:mm`
- `horaFin` LocalTime `HH:mm`
- `motivo` String
- `tipo` String `LIMPIEZA` `MANTENIMIENTO` `INSPECCION` `EMERGENCIA`

### Entidad ColaEspera

#### Campos

- `id` Long autoincrement
- `fecha` LocalDate `yyyy-MM-dd`
- `camionId` FK a Camion
- `tipoCarga` String `SECA` `REFRIGERADA`
- `prioridad` int `1` a `5`
- `estado` String `ACTIVA` `NOTIFICADA` `ASIGNADA` `CANCELADA`
- `observacion` String opcional
- `createdAt` Instant

## Reglas de negocio

### Conflicto por rango horario

Una reserva ocupa el rango:

- `inicio = horaInicio`
- `fin = horaInicio + duracionMin`

No se permite solapamiento con:

- otra reserva del mismo muelle en estado `SOLICITADA`, `CONFIRMADA`, `CHECK_IN` o `EN_DESCARGA`
- un cierre operativo del mismo muelle

### Regla de solapamiento

Hay conflicto si:

- `inicioNueva < finExistente`
- y `finNueva > inicioExistente`

### Compatibilidad del muelle

No se puede crear una reserva si:

- el muelle esta inactivo
- el camion esta inactivo
- el tipo de carga del camion no es compatible con el muelle
- `pesoEstimadoToneladas` excede la capacidad del muelle
- la capacidad del camion es menor al peso estimado

### Ejemplos de compatibilidad

- un camion con carga refrigerada no puede reservar un muelle de carga seca
- un camion con 12 toneladas estimadas no puede reservar un muelle con capacidad de 8 toneladas
- un muelle `MIXTA` acepta carga `SECA` y `REFRIGERADA`

### Transiciones de estado

- `SOLICITADA` puede pasar a `CONFIRMADA` o `CANCELADA`
- `CONFIRMADA` puede pasar a `CHECK_IN`, `CANCELADA` o `NO_SHOW`
- `CHECK_IN` puede pasar a `EN_DESCARGA` o `CANCELADA`
- `EN_DESCARGA` puede pasar a `FINALIZADA`
- `FINALIZADA` no puede cambiar
- `CANCELADA` no puede cambiar
- `NO_SHOW` no puede cambiar

### Politica de cancelacion

- si faltan menos de 3 horas para la reserva, solo rol `OPERADOR` o `ADMIN` puede cancelar
- si faltan 3 horas o mas, `TRANSPORTISTA` puede cancelar su propia reserva

### Check-in

Una reserva confirmada puede pasar a `CHECK_IN` solo si el camion llega dentro de la ventana permitida:

- desde 30 minutos antes del inicio
- hasta 20 minutos despues del inicio

Si supera ese margen:

- puede marcarse como `NO_SHOW`
- o reasignarse segun decision operativa de `OPERADOR` o `ADMIN`

### Cierres operativos

- un cierre no puede solaparse con otro cierre del mismo muelle
- no se permite crear cierre si ya hay reservas activas en ese rango

### Mejora avanzada para ADMIN

Permitir crear cierre con un flag `force` que:

- cancela reservas afectadas
- registra nota automatica
- intenta moverlas a cola de espera

### Cola de espera

Si se intenta reservar y no hay disponibilidad compatible, se puede crear una entrada en cola de espera.

Cuando una reserva se cancela o un cierre termina, se puede promover automaticamente al primer camion en espera compatible segun:

1. fecha
2. prioridad mas alta
3. orden de creacion mas antiguo

## Seguridad

### Autenticacion

Spring Security con JWT

### Roles

- `TRANSPORTISTA` puede crear y ver sus reservas y sus camiones
- `OPERADOR` puede confirmar reservas, registrar check-in y gestionar la operacion diaria
- `ADMIN` puede gestionar todo, incluidos cierres forzados

### Reglas de acceso

- `TRANSPORTISTA` solo accede a recursos asociados a su empresa o sus camiones
- `OPERADOR` y `ADMIN` acceden a todos los recursos

## Endpoints sugeridos

### Muelles

- `GET /api/v1/muelles`
- `GET /api/v1/muelles/{id}`
- `POST /api/v1/muelles` `ADMIN`
- `PUT /api/v1/muelles/{id}` `ADMIN`

### Empresas transportistas

- `GET /api/v1/empresas`
- `GET /api/v1/empresas/{id}`
- `POST /api/v1/empresas`
- `PUT /api/v1/empresas/{id}`

### Camiones

- `GET /api/v1/camiones`
- `GET /api/v1/camiones/{id}`
- `POST /api/v1/camiones`
- `PUT /api/v1/camiones/{id}`

### Reservas

- `POST /api/v1/reservas` `TRANSPORTISTA` `OPERADOR`
- `GET /api/v1/reservas/{id}`
- `GET /api/v1/reservas` con filtros y paginacion
  - `muelleId`
  - `camionId`
  - `empresaId`
  - `fechaDesde`
  - `fechaHasta`
  - `estado`
  - `tipoCarga`
  - `page`
  - `size`
  - `sort`
- `PUT /api/v1/reservas/{id}`
- `PATCH /api/v1/reservas/{id}/confirmar`
- `PATCH /api/v1/reservas/{id}/check-in`
- `PATCH /api/v1/reservas/{id}/iniciar-descarga`
- `PATCH /api/v1/reservas/{id}/finalizar`
- `PATCH /api/v1/reservas/{id}/cancelar`
- `PATCH /api/v1/reservas/{id}/no-show`

### Cierres operativos

- `POST /api/v1/cierres` `OPERADOR` `ADMIN`
- `GET /api/v1/cierres` con filtros
  - `muelleId`
  - `fechaDesde`
  - `fechaHasta`
  - `tipo`
- `DELETE /api/v1/cierres/{id}`

### Cola de espera

- `POST /api/v1/cola-espera`
- `GET /api/v1/cola-espera` con filtros
  - `fecha`
  - `tipoCarga`
  - `estado`
  - `prioridad`
- `PATCH /api/v1/cola-espera/{id}/cancelar`
- `POST /api/v1/cola-espera/promover` `OPERADOR` `ADMIN`

## Validaciones minimas

### Validaciones de empresa transportista

- `ruc` obligatorio y valido en longitud
- `razonSocial` obligatoria minimo 3 caracteres
- `contactoNombre` obligatorio
- `email` formato valido recomendado
- `contactoTelefono` formato valido recomendado

### Validaciones de camion

- `placa` obligatoria y unica
- `empresaId` debe existir si no `404 EMPRESA_NOT_FOUND`
- `tipoCarga` obligatorio
- `capacidadToneladas` mayor a 0

### Validaciones de muelle

- `codigo` obligatorio y unico
- `nombre` obligatorio minimo 3 caracteres
- `tipoCargaPermitida` obligatorio
- `capacidadToneladas` mayor a 0

### Validaciones de reserva

- `muelleId` debe existir si no `404 MUELLE_NOT_FOUND`
- `camionId` debe existir si no `404 CAMION_NOT_FOUND`
- `fecha` obligatoria formato `yyyy-MM-dd`
- `horaInicio` obligatoria formato `HH:mm`
- `duracionMin` debe ser uno de `30`, `60`, `90`, `120`
- `pesoEstimadoToneladas` mayor a 0
- `tipoMercaderia` obligatorio minimo 3 caracteres
- `estado` se define por backend al crear como `SOLICITADA`
- al crear o actualizar validar conflicto por rango y cierres
- al crear o actualizar validar compatibilidad del muelle con el camion

### Validaciones de cierre operativo

- `muelleId` debe existir
- `fecha` obligatoria
- `horaInicio` y `horaFin` obligatorias
- `horaFin` debe ser mayor a `horaInicio`
- no debe solaparse con otros cierres del mismo muelle

### Validaciones de cola de espera

- `camionId` debe existir
- `fecha` obligatoria
- `tipoCarga` obligatoria
- `prioridad` entre `1` y `5`
- `estado` se crea como `ACTIVA`

## Concurrencia y prevencion real de doble reserva

### Estrategia recomendada con tabla de slots

Crear tabla `ReservaSlot`:

- `muelleId`
- `fecha`
- `horaSlot` por ejemplo cada 30 minutos
- `reservaId`
- indice unico `muelleId`, `fecha`, `horaSlot`

Al crear una reserva dentro de transaccion:

1. calcular slots que cubren la duracion
2. insertar slots
3. si falla la restriccion unica devolver `409 RESERVA_CONFLICT`

### Mejora adicional frente al laboratorio anterior

Validar tambien compatibilidad del muelle y estado activo del camion dentro de la misma transaccion de negocio.

### Alternativa con bloqueo pesimista

- consultar reservas del muelle para la fecha con `SELECT FOR UPDATE`
- verificar solapamiento
- insertar o actualizar
- requiere manejo cuidadoso de transacciones

## Errores estandar JSON

Responder siempre con:

```json
{
  "code": "SOME_CODE",
  "message": "Mensaje humano"
}
```

### Codigos sugeridos

- `400 VALIDATION_ERROR`
- `401 UNAUTHORIZED`
- `403 FORBIDDEN`
- `404 MUELLE_NOT_FOUND`
- `404 CAMION_NOT_FOUND`
- `404 EMPRESA_NOT_FOUND`
- `404 RESERVA_NOT_FOUND`
- `409 RESERVA_CONFLICT`
- `409 CIERRE_CONFLICT`
- `409 ESTADO_INVALIDO`
- `422 BUSINESS_RULE_VIOLATION`
- `422 MUELLE_INCOMPATIBLE`
- `422 CHECKIN_WINDOW_EXPIRED`

## Checklist de aceptacion

- proyecto levanta con `./mvnw spring-boot:run`
- Swagger disponible en `/swagger-ui/index.html`
- MySQL conectado y migraciones aplicadas
- CRUD de muelles, empresas y camiones operativo
- `POST /api/v1/reservas` crea en estado `SOLICITADA`
- conflicto por solapamiento devuelve `409 RESERVA_CONFLICT`
- cierres operativos impiden crear reservas en rango
- no se permite reservar muelles incompatibles con la carga
- transiciones de estado cumplen reglas y violaciones devuelven `409 ESTADO_INVALIDO`
- politica de cancelacion aplicada y violaciones devuelven `422 BUSINESS_RULE_VIOLATION`
- `TRANSPORTISTA` no puede ver reservas de otras empresas
- filtros con paginacion y orden funcionan
- cola de espera se crea cuando no hay disponibilidad
- promocion desde cola de espera funciona tras cancelacion o liberacion
- check-in solo funciona en la ventana permitida
- `NO_SHOW` se marca correctamente cuando corresponde

## Recomendaciones

Se recomienda trabajar con:

- Spring Boot
- Spring Web
- Spring Data JPA
- Spring Security
- JWT
- Validation
- MySQL
- Flyway o Liquibase
- Lombok opcional
- OpenAPI Swagger
