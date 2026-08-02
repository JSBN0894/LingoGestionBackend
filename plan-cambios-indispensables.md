# Plan de cambios indispensables — Linogo Gestión

Revisé el modelo actual (`Customer`, `Order`, `OrderProduct`, `Shipment`, `ShipmentState`, `State`, migraciones y `application.properties`) contra lo que describes: catálogo de productos/categorías, catálogo de transportadoras, orden de compra desde el móvil (nombre, cédula, dirección, teléfono, ciudad, productos, precio con posible descuento) y cambio de estado + asignación de transportadora/guía desde el móvil. Esto es lo que ya existe, lo que falta, y lo que hay que corregir para que no se rompa al construir sobre ello.

## 0. Antes que nada: la base de datos no está bajo control de versiones real

`spring.jpa.hibernate.ddl-auto=update` está activo tanto en `application.properties` como en `application-prod.properties`, y `spring.flyway.enabled=false` por defecto. Es decir: **hoy en producción, Hibernate genera y altera el esquema automáticamente**; las migraciones Flyway (`V1__create_tables_and_seed_data.sql`, `V2__audit_log_table.sql`) no se están aplicando salvo que actives el perfil `flyway` explícitamente. De hecho, tablas como `shipments`, `shipment_states`, `shipment_tracking_history`, `users` y `refresh_tokens` no existen en ninguna migración — solo las generó Hibernate en algún momento.

Esto es indispensable de resolver **antes** de tocar el esquema para transportadoras/descuentos, porque cualquier cambio de tipo de columna o de relación bajo `ddl-auto=update` puede alterar o perder datos sin aviso y sin posibilidad de rollback. Cambio: activar Flyway (`spring.flyway.enabled=true`), cambiar `ddl-auto` a `validate`, y escribir una migración `V3` que capture el estado real actual de `shipments`/`shipment_states`/`shipment_tracking_history`/`users`/`refresh_tokens` (usando el esquema que Hibernate ya generó como base) antes de agregar nada nuevo.

## 1. Transportadoras — hoy es un texto libre, no un catálogo

`Shipment.carrier` es un `String` con default `"Inter rapidisimo"` (`shipment/domain/Shipment.kt:26`), editable libremente en `CreateShipmentRequest`/`UpdateShipmentRequest`. No hay entidad, no hay CRUD, no hay forma de listar transportadoras activas desde el móvil o el panel.

Cambios necesarios:
- Nuevo módulo `carrier` (mismo patrón que `category`/`state`): `Carrier(id, name, contactPhone?, active, createdAt, updatedAt)`.
- Migración Flyway para la tabla `carriers`.
- `CarrierController` con `GET` (`@Authenticated`, para que el móvil pueda listar transportadoras al armar el formulario de envío) y `POST/PUT/DELETE` (`@LogisticaOnly` o `@AdminOnly`, a definir con el negocio).
- Cambiar `Shipment.carrier: String` → `Shipment.carrier: Carrier` (FK), igual en los DTOs de creación/actualización (`carrierId` en vez de `carrier` string).
- Migración de datos: mapear los strings de `carrier` ya guardados a filas de `carriers` antes de convertir la columna en FK.

## 2. Precio total — el diseño actual ya es correcto, solo falta (opcional) dejar visible el descuento

Aclarado: el móvil calcula el total sugerido sumando `pricePerUnit × cantidad` de los productos, y le da al vendedor la opción de modificar ese total antes de enviarlo (ahí es donde entra el descuento). El total final (`orderPrice`) es el que manda el POST, y es autoritativo — no hay que recalcularlo ni validarlo contra el catálogo. Las líneas (`OrderProduct.price = product.pricePerUnit`) se quedan al precio de catálogo, que es correcto: representan el precio de lista, no lo que se cobró. Con esto, el comportamiento actual del backend ya está bien — retiro la corrección que propuse antes, estaba mal diagnosticada.

Lo único que queda como mejora **opcional**, no indispensable: hoy no queda registrado en ningún lado cuánto descuento se otorgó (la diferencia entre `Σ(pricePerUnit × cantidad)` y `orderPrice` no se guarda, solo se puede reconstruir si nadie cambió los precios del catálogo después). Si en algún momento quieres reportes de "cuánto se descontó por vendedor/mes", conviene guardar ese total de catálogo (o el descuento ya calculado) junto a la orden al crearla. Si no te interesa ese reporte, no hace falta tocar nada aquí.

## 3. Cambiar el estado de la orden — el paso a "Enviado" debe crear el envío en el mismo movimiento

Aclarado: el envío **no** se crea junto con la orden — se crea en el momento en que alguien cambia el estado de la orden a "Enviado" y en ese mismo momento aporta transportadora + número de guía. Eso confirma que está bien que `CreateOrderCompleteService` no cree un `Shipment` (punto que yo tenía como pregunta abierta, ya no aplica).

Pero hoy esas dos acciones están completamente desacopladas en el código: `OrderController.update()` cambia `operationStateId` sin saber nada de envíos, y `ShipmentController.create()` crea un `Shipment` sin tocar el estado de la orden. Nada impide hoy que una orden quede en estado "Enviado" sin ningún `Shipment` asociado, o que se cree un `Shipment` con guía y transportadora mientras la orden sigue en "Pendiente" — quedan desincronizados porque ninguna operación garantiza que ocurran juntas.

Cambios necesarios:
- Un endpoint dedicado para esta transición específica — por ejemplo `PATCH /api/orders/{id}/ship` — que reciba `carrierId` + `guideNumber` (obligatorios) y opcionalmente `shippingCost`, `estimateDeliveryDate`, `weight`, `isCashOnDelivery`. En un solo `@Transactional`: crea (o actualiza, si ya existía) el `Shipment` del pedido, mueve `Order.operationState` al id de "Enviado", y registra la entrada correspondiente en `ShipmentTrackingHistory`. Así la orden nunca puede quedar en "Enviado" sin envío asociado, ni viceversa.
- Para el resto de transiciones de estado que no implican envío (p. ej. marcar "Novedad", volver a "Disponible"), sigue siendo útil un `PATCH /api/orders/{id}/status` liviano que solo reciba `operationStateId`, en vez de tener que reenviar todo el `UpdateOrderRequest` (`orderPrice`, direcciones, teléfono) como exige hoy `PUT /api/orders/{id}`.
- Los endpoints ya existentes de `ShipmentController` (`PUT /api/shipments/{id}`, `PATCH /api/shipments/{id}/guide`) siguen siendo útiles para ajustar un envío después de creado (cambiar de transportadora, corregir la guía, actualizar el estado de tránsito) — no hay que tocarlos, solo agregar el endpoint de "marcar como enviado" que los complementa.
- Definir qué rol ejecuta `PATCH /api/orders/{id}/ship` desde el móvil: hoy `ShipmentController` usa `@LogisticaOnly`, pero si es el mismo vendedor quien despacha el paquete, puede que también necesite `VENTAS`. Esto es una decisión de negocio, no técnica.
- Aparte de esto, `OrderCompleteController.createOrder()` (el endpoint que usa el móvil para crear la orden) **no tiene ninguna anotación de rol** — cualquier usuario autenticado, sin importar el rol, puede crear órdenes. `OrderController.create()`, en cambio, exige `@VentasOnly`. Hay que decidir a propósito qué rol(es) puede crear órdenes desde el móvil y anotarlo explícitamente — hoy es un descuido, no una decisión.

## 4. Datos del cliente — mayormente ya cubiertos

Cédula, nombre, dirección, teléfono y ciudad **ya se capturan** en `CreateOrderCompleteRequest` y se usan para crear o actualizar el `Customer` (`find-or-create` por cédula, acumulando teléfonos/direcciones). No es necesario agregar campos aquí. Dos detalles menores:
- `Order.customerId` en realidad guarda la cédula (`customer.cedula`), no un id interno — el nombre del campo confunde; si se va a tocar el módulo igual, vale la pena renombrarlo a `customerCedula`.
- `Customer` no tiene `city` como campo propio (cada dirección va con su ciudad implícita dentro del string combinado `"$address, $city"` que arma `CreateOrderCompleteService`). Si más adelante quieres filtrar clientes por ciudad, hay que separarlo — hoy no es indispensable porque `Order` ya guarda `orderCity` de forma independiente.

## 5. Deuda técnica que conviene resolver de una vez, ya que se va a tocar Order/Shipment

Del audit anterior, esto es lo que se cruza directamente con este trabajo:
- Sacar la lógica de `ShipmentStateController.create()` (búsqueda de `State`, validación, mutación del request) hacia el service — se va a tocar este mismo módulo al normalizar estados de envío.
- Las anotaciones `@AdminOrSeller` y `@AuthenticatedUser` en `SecurityAnnotations.kt` referencian roles (`SELLER`, `USER`) que no existen en el resto del sistema (los roles reales son `ADMIN`, `VENTAS`, `LOGISTICA`, `PRODUCCION`) y no se usan en ningún controller — bórralas antes de decidir qué rol(es) pueden gestionar transportadoras, para no dejar dudas sobre qué anotación usar.
- Escribir tests de integración para el nuevo módulo `Carrier` y para los endpoints de cambio de estado/descuento desde el arranque, ya que quedaron como el punto más débil de cobertura en la auditoría anterior.

## Orden sugerido de implementación

1. Activar Flyway + congelar el esquema actual en una migración (sección 0) — todo lo demás depende de poder migrar con seguridad.
2. Módulo `Carrier` + migración + cambio de `Shipment.carrier` a FK (sección 1) — lo necesita el punto 3.
3. (Opcional, no bloqueante) Si se quiere reportar descuentos, guardar el total de catálogo junto a `orderPrice` al crear la orden (sección 2).
4. `PATCH /api/orders/{id}/ship` (crea/actualiza el envío + mueve la orden a "Enviado" en una sola transacción) y, si hace falta, `PATCH /api/orders/{id}/status` para el resto de transiciones; decidir y anotar el rol de `OrderCompleteController` (sección 3).
5. Limpieza de anotaciones muertas y el fix de `ShipmentStateController` (sección 5), aprovechando que ya estás ahí.
