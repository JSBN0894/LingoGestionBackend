# Guía de Integración Android - Linogo Backend API

Documentación técnica para integrar la aplicación **Android** con el backend de gestión de Linogo.

**Versión de la API:** `v4`  
**Deploy:** Railway  
**Última actualización:** Agosto 2026

> ⚠️ **Breaking change (v3 → v4):** el sistema de roles fijos (`role: "ADMIN" | "VENTAS" | "PRODUCCION" | "LOGISTICA" | "USER"`) fue reemplazado por **roles dinámicos con permisos**. Los objetos de usuario (`AuthResponse.user`, `UserMeResponse`, etc.) ya **no tienen un campo `role`**: ahora exponen `roles: RoleSummary[]` y `permissions: string[]`. Cualquier cliente que dependa de un campo `role` fijo debe migrarse a chequear `permissions`. Ver la sección [Autenticación y Roles](#autenticación-y-roles) actualizada.

---

## 📋 Índice

0. ⚠️ [Breaking change v4: roles → permisos](#autenticación-y-roles)
1. [Configuración Inicial](#configuración-inicial)
2. [Dependencias](#dependencias)
3. [Autenticación y Roles](#autenticación-y-roles)
4. [Endpoints](#endpoints)
5. [Modelos de Datos](#modelos-de-datos)
6. [Máquina de Estados](#máquina-de-estados)
7. [Manejo de Errores](#manejo-de-errores)
8. [Ejemplo Completo Android](#ejemplo-completo-android)

---

## Configuración Inicial

### Base URL

| Entorno | URL |
|---------|-----|
| Desarrollo (local) | `http://10.0.2.2:8080` |
| Producción (Railway) | `BASE_URL` |

> Para Android Emulator, usa `10.0.2.2` en lugar de `localhost`.

### Configuración de la Base URL

En `build.gradle.kts` (app level):

```kotlin
android {
    defaultConfig {
        buildConfigField("String", "BASE_URL", "\"https://tu-app.railway.app\"")
    }
    buildTypes {
        debug {
            buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8080\"")
        }
        release {
            buildConfigField("String", "BASE_URL", "\"https://tu-app.railway.app\"")
        }
    }
}
```

### Permisos

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

### Network Security Config

```xml
<!-- res/xml/network_security_config.xml -->
<network-security-config>
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">10.0.2.2</domain>
        <domain includeSubdomains="true">localhost</domain>
    </domain-config>
</network-security-config>
```

```xml
<application android:networkSecurityConfig="@xml/network_security_config" ... />
```

---

### Inicio Rápido (Quickstart)

1. Clonar el repositorio
2. Tener PostgreSQL corriendo en `localhost:5432` con base `gestion_db`
3. Ejecutar: `.\gradlew.bat bootRun --args='--spring.profiles.active=dev'`
4. El backend inicia y **se siembran datos iniciales automáticamente** (usuarios, estados, categorías)
5. Credenciales por defecto:
   - **Admin:** `admin` / `Admin@123456`
   - **Usuario demo:** `user` / `User@123456`
6. Swagger UI: `http://localhost:8080/swagger-ui/index.html` (requiere autenticación ADMIN)
7. Admin Panel Web: `http://localhost:8080/admin/` (React SPA)
8. App Android: configurar `BASE_URL = http://10.0.2.2:8080`

---

## Dependencias

```kotlin
dependencies {
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.7")
}
```

---

## Autenticación y Roles

### Flujo JWT

```
┌─────────────┐     ┌──────────────┐     ┌───────────────┐
│   Login     │────▶│  Access      │────▶│  Peticiones   │
│             │     │  Token (15m) │     │  con Bearer   │
└─────────────┘     └──────────────┘     └───────────────┘
                           │
                           ▼
                    ┌──────────────┐
                    │  Refresh     │
                    │  Token (7d)  │
                    └──────────────┘
```

### Sistema de Roles y Permisos (v4)

> ⚠️ El enum fijo `Role` (`ADMIN`/`VENTAS`/`PRODUCCION`/`LOGISTICA`/`USER`) **ya no existe** en el backend. Fue reemplazado por un sistema dinámico: los **roles** son filas editables en base de datos (tabla `roles`, gestionable desde el Admin Panel Web vía `/api/admin/roles`), cada rol tiene un conjunto de **códigos de permiso** (tabla `role_permissions`), y cada usuario puede tener **cero, uno o varios roles** (tabla `user_roles`, many-to-many). La columna `users.role` fue eliminada de la base de datos.
>
> Para el cliente Android esto significa:
> - `AuthResponse.user`, `GET /api/auth/me` y todas las respuestas de usuario ya **no traen `role: String`**. Traen `roles: List<RoleSummary>` (id + nombre, solo informativo/UI) y **`permissions: List<String>`** (la lista plana de códigos de permiso, es la fuente de verdad para decidir qué puede hacer el usuario).
> - El control de acceso en la UI de Android debe basarse en `permissions.contains("ORDERS_SHIP")`, etc., **no** en comparar un nombre de rol.
> - El JWT (access/refresh token) **no lleva roles ni permisos en sus claims** — solo `sub` (username), `userId` y `type`. La autorización real se recalcula en cada request contra la base de datos (ver `CustomUserDetailsService`), así que **no se puede decodificar el JWT para saber los permisos**: hay que llamar a `GET /api/auth/me` después de loguear (o cuando se necesite refrescar el estado de permisos).

#### Roles por defecto (seed)

El backend sigue sembrando estos 4 roles en desarrollo (`DataInitializer`, perfil `dev`), con estos permisos de fábrica. Los `id` son autogenerados (`BIGSERIAL`) y **no deben hardcodearse** — hay que resolverlos vía `GET /api/auth/me` o `GET /api/admin/roles`.

| Rol (nombre) | Permisos |
|---|---|
| `Administrador` | Todos los permisos (`Permission.entries` completo) |
| `Ventas` | `CUSTOMERS_MANAGE`, `ORDERS_MANAGE`, `ORDERS_SHIP`, `DASHBOARD_VIEW` |
| `Producción` | `PRODUCTS_MANAGE` |
| `Logística` | `CARRIERS_MANAGE`, `SHIPMENTS_MANAGE`, `ORDERS_SHIP` |

En producción (`DataInitializer`, perfil `prod`) solo se siembra el rol `Administrador` y el usuario `admin` (contraseña temporal `123456`, distinta de la de desarrollo — cámbiala de inmediato).

#### Catálogo completo de permisos (`Permission` enum)

| Código | Descripción |
|---|---|
| `USERS_MANAGE` | Listar, crear, editar, deshabilitar y eliminar usuarios; restablecer contraseñas y asignar roles |
| `AUDIT_VIEW` | Ver el log de auditoría |
| `ROLES_MANAGE` | Crear, editar y eliminar roles y sus permisos |
| `DASHBOARD_VIEW` | Ver el dashboard de ventas e ingresos |
| `SYSTEM_DOCS_VIEW` | Ver la documentación Swagger / OpenAPI |
| `CATEGORIES_MANAGE` | Crear, editar y eliminar categorías de producto |
| `STATES_MANAGE` | Crear, editar y eliminar estados de operación |
| `SHIPMENT_STATES_MANAGE` | Crear y eliminar estados de envío |
| `CARRIERS_MANAGE` | Crear y editar transportadoras |
| `CARRIERS_DELETE` | Eliminar transportadoras |
| `CUSTOMERS_MANAGE` | Crear y editar clientes |
| `CUSTOMERS_DELETE` | Eliminar clientes |
| `ORDERS_MANAGE` | Crear y editar órdenes (incluye alta completa con productos) |
| `ORDERS_DELETE` | Eliminar órdenes |
| `ORDERS_SHIP` | Marcar una orden como enviada (crea/actualiza el envío asociado) |
| `PRODUCTS_MANAGE` | Crear, editar y eliminar productos |
| `SHIPMENTS_MANAGE` | Crear, editar envíos y asignar número de guía |
| `SHIPMENTS_DELETE` | Eliminar envíos |

En las tablas de endpoints de abajo, la columna **Permiso** indica el código de `Permission` requerido (anotación `@RequiresPermission`). **Autenticado** significa que solo requiere estar logueado (anotación `@Authenticated`), sin permiso específico — normalmente los GET.

**Nota:** Todos los endpoints de escritura (POST/PUT/DELETE/PATCH) ahora requieren un permiso específico. Los endpoints de lectura (GET) generalmente solo requieren autenticación (`@Authenticated`).

### Admin Panel Web

Existe un panel de administración web disponible en `/admin/` que utiliza autenticación por cookies httpOnly (separada del flujo mobile), servido por `WebAuthController` (`/api/web/login`, `/api/web/logout`, `/api/web/refresh`, `/api/web/auth/me`), `CsrfCookieController` (`/api/web/csrf-token`) y `SpaForwardController` (sirve el `index.html` de la SPA de React para cualquier ruta bajo `/admin/**`).
- URL: `http://localhost:8080/admin/`
- Autenticación: Cookies httpOnly (`access_token`, `refresh_token`) + cookie `XSRF-TOKEN` (no Bearer tokens)
- Acceso: cualquier usuario válido puede loguearse; qué secciones ve depende de sus `permissions` (gate en el frontend React, no un rol fijo de backend)
- **Estos tres controladores son exclusivos del panel web y no aplican al flujo Bearer-token de Android** — la app Android debe seguir usando `/api/auth/**` con `Authorization: Bearer <token>`, no `/api/web/**`.

### Endpoints de Autenticación

---

## Endpoints

### 🔐 Auth

| Método | Ruta | Auth | Permiso | Descripción |
|--------|------|------|---------|-------------|
| POST | `/api/auth/login` | ❌ (público) | - | Iniciar sesión |
| POST | `/api/auth/register` | ✅ | `USERS_MANAGE` | Crear usuario (sin roles asignados, ver nota) |
| POST | `/api/auth/refresh` | ❌ (público, requiere `refreshToken` en el body) | - | Renovar token |
| POST | `/api/auth/logout` | ✅ | Autenticado | Cerrar sesión |
| GET | `/api/auth/me` | ✅ | Autenticado | Obtener perfil del usuario actual (roles + permisos) |
| PATCH | `/api/auth/change-password` | ✅ | Autenticado | Cambiar contraseña propia |

> ⚠️ `/api/auth/login` y `/api/auth/refresh` están en la lista `permitAll()` de Spring Security: no requieren header `Authorization`. Los demás requieren `Bearer <accessToken>`.

#### POST /api/auth/login

```json
// Request
{ "username": "admin", "password": "Admin@123456" }

// Response 200
{
  "accessToken": "eyJhbGci...",
  "refreshToken": "eyJhbGci...",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "user": {
    "id": "uuid-123",
    "username": "admin",
    "email": "admin@linogo.com",
    "fullName": "Administrador",
    "roles": [ { "id": 1, "name": "Administrador" } ],
    "permissions": [
      "AUDIT_VIEW", "CARRIERS_DELETE", "CARRIERS_MANAGE", "CATEGORIES_MANAGE",
      "CUSTOMERS_DELETE", "CUSTOMERS_MANAGE", "DASHBOARD_VIEW", "ORDERS_DELETE",
      "ORDERS_MANAGE", "ORDERS_SHIP", "PRODUCTS_MANAGE", "ROLES_MANAGE",
      "SHIPMENTS_DELETE", "SHIPMENTS_MANAGE", "SHIPMENT_STATES_MANAGE",
      "STATES_MANAGE", "SYSTEM_DOCS_VIEW", "USERS_MANAGE"
    ],
    "isEnabled": true
  }
}
```

#### POST /api/auth/register (requiere permiso `USERS_MANAGE`, típicamente ADMIN)

```json
// Request
{
  "fullName": "Juan Pérez",
  "username": "juanperez",
  "email": "juan@example.com",
  "password": "segura123"
}

// Response: mismo formato que login, pero el usuario creado NO tiene roles
// ("roles": [], "permissions": []) hasta que un admin se los asigne.
```

> ⚠️ **Discrepancia a confirmar con backend:** a diferencia del v3 anterior, `RegisterRequest` ya **no acepta `role`** — el usuario queda creado sin ningún rol/permiso. Para crear un usuario **con** roles desde el arranque, usar `POST /api/admin/users` (ver sección Admin), que sí recibe `roleIds`. Si el flujo de registro de la app Android depende de asignar un rol al crear el usuario, debe usarse ese endpoint de admin en vez de `/api/auth/register`, o pedirle al backend que lo confirme/ajuste.

#### POST /api/auth/refresh

```json
// Request
{ "refreshToken": "eyJhbGci..." }

// Response: mismo formato que login
```

#### POST /api/auth/logout

```
Header: Authorization: Bearer <accessToken>
Body: vacío → 200 OK
```

#### GET /api/auth/me

Obtiene el perfil del usuario autenticado, con sus roles y permisos actuales (recalculados desde la base de datos en cada llamada). Es la forma recomendada de refrescar el estado de autorización en el cliente (por ejemplo, tras que un admin le cambie los roles a un usuario logueado).

```json
// Response 200
{
  "id": "uuid-123",
  "username": "admin",
  "email": "admin@linogo.com",
  "fullName": "Administrador",
  "roles": [ { "id": 1, "name": "Administrador" } ],
  "permissions": [ "USERS_MANAGE", "ORDERS_MANAGE", "..." ],
  "isEnabled": true
}
```

---

### 👥 Clientes — Customer

| Método | Ruta | Auth | Permiso | Descripción |
|--------|------|------|---------|-------------|
| GET | `/api/customers` | ✅ | Autenticado | Listar clientes |
| GET | `/api/customers/{cedula}` | ✅ | Autenticado | Obtener por cédula |
| POST | `/api/customers` | ✅ | `CUSTOMERS_MANAGE` | Crear cliente |
| PUT | `/api/customers/{cedula}` | ✅ | `CUSTOMERS_MANAGE` | Actualizar cliente |
| DELETE | `/api/customers/{cedula}` | ✅ | `CUSTOMERS_DELETE` | Eliminar cliente |

#### POST /api/customers

```json
// Request
{
  "cedula": 123456789,
  "name": "Juan Pérez",
  "phones": ["+54911223344"],
  "addresses": ["Av. Corrientes 1234, Buenos Aires"]
}

// Response 201: mismo objeto con cedula como id
```

#### PUT /api/customers/{cedula}

```json
// Request (mismos campos que create)
{
  "cedula": 123456789,
  "name": "Juan Pérez Editado",
  "phones": ["+54911223344", "+54911223355"],
  "addresses": ["Av. Corrientes 1234, Buenos Aires", "Calle Falsa 123"]
}

// Response 200: objeto actualizado
```

---

### 📦 Productos

| Método | Ruta | Auth | Permiso | Descripción |
|--------|------|------|---------|-------------|
| GET | `/api/products` | ✅ | Autenticado | Listar (con paginación) |
| GET | `/api/products/{id}` | ✅ | Autenticado | Obtener por ID |
| GET | `/api/products/category/{categoryId}` | ✅ | Autenticado | Filtrar por categoría |
| POST | `/api/products` | ✅ | `PRODUCTS_MANAGE` | Crear producto |
| PUT | `/api/products/{id}` | ✅ | `PRODUCTS_MANAGE` | Actualizar |
| DELETE | `/api/products/{id}` | ✅ | `PRODUCTS_MANAGE` | Eliminar |

#### GET /api/products?page=0&size=50

Query params opcionales: `page` (default=0), `size` (default=50)

```json
// Response 200: List<ProductResponse>
[
  {
    "id": 1,
    "name": "Molde Tipo A",
    "pricePerUnit": 1500,
    "stock": 50,
    "imageUrl": "/uploads/123456_abc123.jpg",
    "description": "Molde de acero tipo A",
    "category": { "id": 1, "name": "Moldes" },
    "createdAt": "2026-01-01T00:00:00",
    "updatedAt": "2026-01-01T00:00:00"
  }
]
```

#### POST /api/products

```json
// Request
{
  "name": "Molde Tipo A",
  "pricePerUnit": 1500,
  "stock": 50,
  "imageUrl": "/uploads/123456_abc123.jpg",
  "description": "Molde de acero tipo A",
  "categoryId": 1
}
```

> Nota: `imageUrl` ahora es **opcional** (`String?`) tanto al crear como al actualizar un producto. `categoryId` es **obligatorio** (antes era opcional en la doc anterior).

**Flujo de imagen:** Primero subir la imagen a `POST /api/upload`, luego usar la URL devuelta en `imageUrl`.

---

### 📁 Categorías

Ahora soportan **subcategorías** (`parentId`) y tienen CRUD completo (antes solo lectura).

| Método | Ruta | Auth | Permiso | Descripción |
|--------|------|------|---------|-------------|
| GET | `/api/categories` | ✅ | Autenticado | Listar categorías |
| GET | `/api/categories/{id}` | ✅ | Autenticado | Obtener por ID |
| POST | `/api/categories` | ✅ | `CATEGORIES_MANAGE` | Crear categoría |
| PUT | `/api/categories/{id}` | ✅ | `CATEGORIES_MANAGE` | Actualizar categoría |
| DELETE | `/api/categories/{id}` | ✅ | `CATEGORIES_MANAGE` | Eliminar categoría |

#### POST /api/categories

```json
// Request
{ "name": "Accesorios de molde", "parentId": 1 }

// Response 201
{
  "id": 5,
  "name": "Accesorios de molde",
  "parentId": 1,
  "createdAt": "2026-08-01T00:00:00",
  "updatedAt": "2026-08-01T00:00:00"
}
```

> Nota: `CategoryResponse` embebido dentro de `ProductResponse.category` es un tipo distinto y más chico (solo `id` y `name`, sin `parentId`). El endpoint `/api/categories` devuelve el `CategoryResponse` completo con `parentId`, `createdAt`, `updatedAt`.

---

### 🛒 Órdenes

| Método | Ruta | Auth | Permiso | Descripción |
|--------|------|------|---------|-------------|
| GET | `/api/orders` | ✅ | Autenticado | Listar (con paginación) |
| GET | `/api/orders/{id}` | ✅ | Autenticado | Obtener por ID |
| POST | `/api/orders` | ✅ | `ORDERS_MANAGE` | Crear orden |
| PUT | `/api/orders/{id}` | ✅ | `ORDERS_MANAGE` | Actualizar |
| DELETE | `/api/orders/{id}` | ✅ | `ORDERS_DELETE` | Eliminar |
| POST | `/api/orders/complete` | ✅ | `ORDERS_MANAGE` | Crear cliente + orden + productos en un paso |
| PATCH | `/api/orders/{id}/ship` | ✅ | `ORDERS_SHIP` | **Nuevo:** marcar la orden como enviada (crea/actualiza el envío) |

#### GET /api/orders?page=0&size=50

```json
// Response 200: List<OrderResponse>
[
  {
    "id": 1,
    "customerId": 123456789,
    "customerName": "Juan Pérez",
    "operationStateId": 1,
    "operationStateName": "Pendiente",
    "orderPrice": 4500,
    "orderAddress": "Av. Corrientes 1234",
    "orderPhone": "+54911223344",
    "orderCity": "Buenos Aires",
    "observation": null,
    "createdAt": "2026-01-01T00:00:00",
    "updatedAt": "2026-01-01T00:00:00"
  }
]
```

> Nota: `OrderResponse` ganó un campo nuevo `observation: String?` (también presente en `UpdateOrderRequest`, opcional).

#### POST /api/orders

```json
// Request
{
  "customerId": 123456789,
  "operationStateId": 1,
  "orderPrice": 4500,
  "orderAddress": "Av. Corrientes 1234",
  "orderPhone": "+54911223344",
  "orderCity": "Buenos Aires"
}
```

#### POST /api/orders/complete (crear cliente + orden + productos en un solo paso)

```json
// Request
{
  "name": "Juan Pérez",
  "cedula": 123456789,
  "address": "Av. Corrientes 1234",
  "city": "Buenos Aires",
  "phone": "+54911223344",
  "order": {
    "orderPrice": 4500,
    "operationStateId": 1,
    "orderProducts": [
      { "idProduct": 1, "quantity": 2 },
      { "idProduct": 2, "quantity": 1 }
    ]
  }
}
```

#### PATCH /api/orders/{id}/ship (nuevo, permiso `ORDERS_SHIP`)

Reemplaza el flujo manual de "crear un Shipment aparte + cambiar el estado de la orden a mano". En una sola transacción: crea (o actualiza, si ya existía) el `Shipment` de la orden con transportadora + guía, y mueve `Order.operationState` al estado llamado **"Enviado"**. Es el flujo recomendado para marcar una orden como despachada desde la app.

```json
// Request
{
  "carrierId": 1,
  "guideNumber": "GUIDE-ABC-123",
  "shippingStateId": 1,
  "isCashOnDelivery": true,
  "shippingCost": 5000,
  "estimateDeliveryDate": "2026-08-05T00:00:00",
  "weight": 2000
}

// Response 200
{
  "orderId": 1,
  "operationStateId": 3,
  "operationStateName": "Enviado",
  "shipmentId": 10,
  "carrierId": 1,
  "carrierName": "Inter rapidisimo",
  "guideNumber": "GUIDE-ABC-123",
  "shippingStateId": 1,
  "shippingStateName": "Pendiente"
}
```

`carrierId` y `shippingStateId` se obtienen de `GET /api/carriers` y `GET /api/shipment-states` respectivamente (ver esas secciones más abajo). `isCashOnDelivery` por defecto es `true`; `shippingCost`, `estimateDeliveryDate` y `weight` son opcionales.

---

### 🔗 Order Products

> ⚠️ Corrección de rutas respecto a versiones anteriores de esta guía: la ruta de "productos de una orden" es `/order/{orderId}` (no `/by-order/{orderId}`), y **no existe** un `GET /api/order-products/{id}` por id simple — la clave es compuesta (`orderId` + `productId`). Estos endpoints no tienen anotación `@RequiresPermission`/`@Authenticated` explícita en el controller, pero igual requieren estar autenticados (regla por defecto `anyRequest().authenticated()` de Spring Security).

| Método | Ruta | Auth | Descripción |
|--------|------|------|-------------|
| GET | `/api/order-products` | ✅ | Listar todos |
| GET | `/api/order-products/order/{orderId}` | ✅ | Productos de una orden |
| POST | `/api/order-products` | ✅ | Agregar un producto a una orden |
| PUT | `/api/order-products/order/{orderId}/product/{productId}` | ✅ | Actualizar cantidad/precio de un producto en una orden |
| DELETE | `/api/order-products/order/{orderId}/product/{productId}` | ✅ | Quitar un producto de una orden |

#### POST /api/order-products

```json
// Request
{ "orderId": 1, "productId": 2, "quantity": 3, "price": 1500 }

// Response 201
{
  "orderId": 1,
  "productId": 2,
  "productName": "Molde Tipo A",
  "quantity": 3,
  "price": 1500,
  "createdAt": "2026-08-01T00:00:00",
  "updatedAt": "2026-08-01T00:00:00"
}
```

---

### 🚚 Envíos (Shipments)

> ⚠️ **Cambio de modelo importante:** `Shipment.carrier` dejó de ser texto libre. Ahora `carrierId` es una FK obligatoria al nuevo catálogo de [Transportadoras](#-transportadoras-carriers). También se agregaron `isCashOnDelivery`, `shippingCost`, `estimateDeliveryDate` y `weight`. El campo de estado se llama `shippingStateId`/`shippingStateName` (antes `stateId`/`stateName`), y **no existe** un campo `notes` en `Shipment` (las notas viven en el historial de tracking, no en el envío).
>
> Para la mayoría de los casos de uso móviles, considera usar `PATCH /api/orders/{id}/ship` (ver sección Órdenes) en vez de crear el `Shipment` directamente: hace lo mismo pero además mueve el estado de la orden en la misma transacción.

| Método | Ruta | Auth | Permiso | Descripción |
|--------|------|------|---------|-------------|
| GET | `/api/shipments` | ✅ | Autenticado | Listar envíos |
| GET | `/api/shipments/{id}` | ✅ | Autenticado | Obtener por ID |
| POST | `/api/shipments` | ✅ | `SHIPMENTS_MANAGE` | Crear envío |
| PUT | `/api/shipments/{id}` | ✅ | `SHIPMENTS_MANAGE` | Actualizar |
| DELETE | `/api/shipments/{id}` | ✅ | `SHIPMENTS_DELETE` | Eliminar |
| PATCH | `/api/shipments/{id}/guide` | ✅ | `SHIPMENTS_MANAGE` | Asignar/actualizar número de guía |
| GET | `/api/shipments/{id}/tracking` | ✅ | Autenticado | Historial de cambios de estado |

#### POST /api/shipments

```json
// Request
{
  "orderId": 1,
  "carrierId": 1,
  "shippingStateId": 1,
  "isCashOnDelivery": true,
  "shippingCost": 5000,
  "estimateDeliveryDate": "2026-08-05T00:00:00",
  "weight": 2000,
  "guideNumber": null
}

// Response 201
{
  "id": 1,
  "orderId": 1,
  "carrierId": 1,
  "carrierName": "Inter rapidisimo",
  "isCashOnDelivery": true,
  "shippingStateId": 1,
  "shippingStateName": "Pendiente",
  "shippingCost": 5000,
  "estimateDeliveryDate": "2026-08-05T00:00:00",
  "weight": 2000,
  "guideNumber": null,
  "createdAt": "2026-01-01T00:00:00",
  "updatedAt": "2026-01-01T00:00:00"
}
```

#### PATCH /api/shipments/{id}/guide

Asignar o actualizar número de guía (permiso `SHIPMENTS_MANAGE`).

```json
// Request
{ "guideNumber": "GUIDE-ABC-123" }

// Response 200: shipment actualizado con guideNumber
```

---

### 📍 Tracking de Envíos

| Método | Ruta | Auth | Descripción |
|--------|------|------|-------------|
| GET | `/api/shipments/{id}/tracking` | ✅ | Historial de cambios de estado |

> ⚠️ El shape del historial cambió: los campos son `statusFrom`/`statusTo` (antes `previousState`/`newState`) y **ya no incluye `changedBy`** en la respuesta (existe internamente en la entidad pero no se serializa).

```json
// Response 200
[
  {
    "id": 1,
    "statusFrom": null,
    "statusTo": "Pendiente",
    "notes": "Envío creado",
    "changedAt": "2026-01-01T00:00:00"
  },
  {
    "id": 2,
    "statusFrom": "Pendiente",
    "statusTo": "En tránsito",
    "notes": "Estado actualizado",
    "changedAt": "2026-01-02T00:00:00"
  }
]
```

---

### 🚛 Transportadoras (Carriers)

**Nuevo módulo**, no documentado antes. Catálogo administrable de transportadoras — antes `Shipment.carrier` era texto libre, ahora es una FK a esta tabla. Sembrado por defecto con: *Inter rapidisimo, Coordinadora, Servientrega, TCC*.

| Método | Ruta | Auth | Permiso | Descripción |
|--------|------|------|---------|-------------|
| GET | `/api/carriers?onlyActive=true` | ✅ | Autenticado | Listar transportadoras (por defecto solo activas) |
| GET | `/api/carriers/{id}` | ✅ | Autenticado | Obtener por ID |
| POST | `/api/carriers` | ✅ | `CARRIERS_MANAGE` | Crear transportadora |
| PUT | `/api/carriers/{id}` | ✅ | `CARRIERS_MANAGE` | Actualizar (incluye `active`) |
| DELETE | `/api/carriers/{id}` | ✅ | `CARRIERS_DELETE` | Eliminar |

#### GET /api/carriers

```json
// Response 200: List<CarrierResponse>
[
  {
    "id": 1,
    "name": "Inter rapidisimo",
    "contactPhone": "+57 1 800 000 0001",
    "active": true,
    "createdAt": "2026-01-01T00:00:00",
    "updatedAt": "2026-01-01T00:00:00"
  }
]
```

#### POST /api/carriers

```json
// Request
{ "name": "Nueva Transportadora", "contactPhone": "+57 300 000 0000" }

// Response 201: CarrierResponse (ver arriba)
```

#### PUT /api/carriers/{id}

```json
// Request
{ "name": "Nueva Transportadora", "contactPhone": "+57 300 000 0000", "active": true }
```

---

### 🗂️ Estados de Operación (States)

**Nuevo:** antes los estados de operación eran una tabla estática documentada en "Máquina de Estados"; ahora tienen CRUD completo. Los IDs pueden variar entre entornos — consultar `GET /api/states` en vez de hardcodearlos.

| Método | Ruta | Auth | Permiso | Descripción |
|--------|------|------|---------|-------------|
| GET | `/api/states` | ✅ | Autenticado | Listar estados de operación |
| GET | `/api/states/{id}` | ✅ | Autenticado | Obtener por ID |
| POST | `/api/states` | ✅ | `STATES_MANAGE` | Crear estado |
| PUT | `/api/states/{id}` | ✅ | `STATES_MANAGE` | Actualizar |
| DELETE | `/api/states/{id}` | ✅ | `STATES_MANAGE` | Eliminar |

```json
// Response 200 (GET /api/states)
[
  { "id": 1, "name": "PENDIENTE", "priority": 1, "type": "OPERATION", "createdAt": "...", "updatedAt": "..." }
]
```

`CreateStateRequest`/`UpdateStateRequest`: `{ "name": "...", "priority": 1, "type": "OPERATION" }` (`type` por defecto `"OPERATION"`, es el único valor usado actualmente).

---

### 📦 Estados de Envío (Shipment States)

**Nuevo:** sub-estados de envío, cada uno ligado a un `State` (estado de operación) padre. También CRUD completo ahora.

| Método | Ruta | Auth | Permiso | Descripción |
|--------|------|------|---------|-------------|
| GET | `/api/shipment-states` | ✅ | Autenticado | Listar estados de envío |
| POST | `/api/shipment-states` | ✅ | `SHIPMENT_STATES_MANAGE` | Crear (requiere `stateId` del estado padre) |
| DELETE | `/api/shipment-states/{id}` | ✅ | `SHIPMENT_STATES_MANAGE` | Eliminar |

```json
// Response 200 (GET /api/shipment-states)
[
  { "id": 1, "stateId": 1, "stateName": "PENDIENTE", "name": "Pendiente", "createdAt": "...", "updatedAt": "..." }
]
```

---

### 📊 Dashboard

> ⚠️ El shape de la respuesta y los query params cambiaron respecto a versiones anteriores de esta guía.

| Método | Ruta | Auth | Permiso | Descripción |
|--------|------|------|---------|-------------|
| GET | `/api/dashboard/sales?from=2026-01-01T00:00:00&to=2026-12-31T23:59:59` | ✅ | `DASHBOARD_VIEW` | Resumen de ventas en período |

`from`/`to` son **opcionales** y de tipo `LocalDateTime` (formato ISO `yyyy-MM-dd'T'HH:mm:ss`, no solo fecha). Si se omiten, el período por defecto son los últimos 30 días.

```json
// Response 200
{
  "totalOrders": 150,
  "totalRevenue": 675000,
  "avgOrderValue": 4500,
  "totalProducts": 42,
  "totalUsers": 8,
  "ordersByState": [
    { "state": "PENDIENTE", "count": 30 },
    { "state": "ENVIADO", "count": 100 },
    { "state": "FINALIZADO", "count": 20 }
  ]
}
```

---

### 👑 Admin — Gestión de Usuarios

> ⚠️ Rutas y modelos cambiaron respecto a versiones anteriores de esta guía: ya no se asigna un `role: String` único, sino una **lista de `roleIds`** (ids de la tabla `roles`, obtenidos vía `GET /api/admin/roles`). La ruta de cambio de rol pasó de `PATCH .../role` a `PUT .../roles` (reemplaza el conjunto completo). Todas las respuestas de usuario usan el mismo `UserResponse` que `/api/auth/login` y `/api/auth/me` (con `roles`/`permissions`, **sin** `enabled`/`createdAt` — el campo se llama `isEnabled`, no `enabled`, y no se expone `createdAt`).

| Método | Ruta | Auth | Permiso | Descripción |
|--------|------|------|---------|-------------|
| GET | `/api/admin/users` | ✅ | `USERS_MANAGE` | Listar usuarios |
| GET | `/api/admin/users/{id}` | ✅ | `USERS_MANAGE` | Obtener usuario |
| POST | `/api/admin/users` | ✅ | `USERS_MANAGE` | Crear usuario con roles específicos |
| PUT | `/api/admin/users/{id}/roles` | ✅ | `USERS_MANAGE` | Reemplazar los roles de un usuario |
| PATCH | `/api/admin/users/{id}/status` | ✅ | `USERS_MANAGE` | Activar/desactivar usuario |
| PATCH | `/api/admin/users/{id}/password` | ✅ | `USERS_MANAGE` | **Nuevo:** el admin fija una nueva contraseña sin pedir la anterior; cierra sesiones activas del usuario |
| DELETE | `/api/admin/users/{id}` | ✅ | `USERS_MANAGE` | Eliminar usuario (no se puede auto-eliminar) |
| GET | `/api/admin/audit` | ✅ | `AUDIT_VIEW` | Listar logs de auditoría |

#### POST /api/admin/users

```json
// Request
{
  "fullName": "Nuevo Usuario",
  "username": "nuevouser",
  "email": "nuevo@linogo.com",
  "password": "pass123456",
  "roleIds": [2]
}

// Response 201 (UserResponse)
{
  "id": "uuid-456",
  "username": "nuevouser",
  "email": "nuevo@linogo.com",
  "fullName": "Nuevo Usuario",
  "roles": [ { "id": 2, "name": "Ventas" } ],
  "permissions": [ "CUSTOMERS_MANAGE", "ORDERS_MANAGE", "ORDERS_SHIP", "DASHBOARD_VIEW" ],
  "isEnabled": true
}
```

#### PUT /api/admin/users/{id}/roles

Reemplaza **todo** el conjunto de roles del usuario (no es incremental).

```json
// Request
{ "roleIds": [2, 3] }

// Response 200: UserResponse con roles/permissions actualizados
```

#### PATCH /api/admin/users/{id}/status

Activa o desactiva un usuario. Un usuario desactivado no puede iniciar sesión.

```
// Request: vacío
// Response 200: usuario con isEnabled actualizado
```

#### PATCH /api/admin/users/{id}/password

```json
// Request
{ "newPassword": "nuevaClaveSegura123" }

// Response 200
{ "message": "Contraseña de nuevouser actualizada" }
```

---

### 🔑 Roles y Permisos (solo panel admin, permiso `ROLES_MANAGE`)

Gestión del catálogo dinámico de roles. Principalmente para el Admin Panel Web, pero puede ser útil si la app Android necesita mostrar el catálogo de roles/permisos disponibles (por ejemplo, en una pantalla de administración).

| Método | Ruta | Auth | Permiso | Descripción |
|--------|------|------|---------|-------------|
| GET | `/api/admin/permissions` | ✅ | `ROLES_MANAGE` | Catálogo completo de permisos disponibles |
| GET | `/api/admin/roles` | ✅ | `ROLES_MANAGE` | Listar roles |
| POST | `/api/admin/roles` | ✅ | `ROLES_MANAGE` | Crear rol |
| PUT | `/api/admin/roles/{id}` | ✅ | `ROLES_MANAGE` | Editar rol (nombre, descripción, permisos) |
| DELETE | `/api/admin/roles/{id}` | ✅ | `ROLES_MANAGE` | Eliminar rol (falla si algún usuario lo tiene asignado) |

```json
// GET /api/admin/roles → Response 200
[
  {
    "id": 1,
    "name": "Administrador",
    "description": "Acceso total al sistema",
    "permissions": ["AUDIT_VIEW", "CARRIERS_DELETE", "..."],
    "userCount": 1
  }
]

// POST /api/admin/roles → Request
{
  "name": "Soporte",
  "description": "Solo lectura de pedidos",
  "permissions": ["DASHBOARD_VIEW"]
}
```

> Nota: el backend protege el permiso `ROLES_MANAGE` y `USERS_MANAGE` contra "autobloqueo" — al editar/eliminar un rol o usuario, `RoleGuardService` valida que siga existiendo al menos alguien con esos permisos.

#### GET /api/admin/audit

Lista los logs de auditoría con paginación y filtros opcionales.

```
GET /api/admin/audit?page=0&size=20&username=admin&entityType=USER&action=CREATE&fromDate=2026-01-01&toDate=2026-12-31

// Response 200
{
  "content": [
    {
      "id": "uuid-123",
      "username": "admin",
      "action": "CREATE",
      "entityType": "USER",
      "entityId": "uuid-456",
      "oldValues": null,
      "newValues": "{\"username\":\"nuevouser\",\"role\":\"VENTAS\"}",
      "ipAddress": "127.0.0.1",
      "createdAt": "2026-05-25T10:30:00"
    }
  ],
  "totalElements": 150,
  "totalPages": 8,
  "currentPage": 0,
  "pageSize": 20
}
```

---

### 🔄 Sincronización (público, sin auth)

Todo `/api/sync/**` está en la lista `permitAll()` de Spring Security: no requiere `Authorization`.

| Método | Ruta | Auth | Descripción |
|--------|------|------|-------------|
| GET | `/api/sync/version` | ❌ | Versión actual del catálogo |
| GET | `/api/sync/catalog` | ❌ | Catálogo completo (productos + categorías) |
| GET | `/api/sync/states` | ❌ | Estados de operación + estados de envío |
| POST | `/api/sync/validate` | ❌ | Validar si el cliente necesita sincronizar |
| POST | `/api/sync/version/increment?description=...` | ❌ | Uso interno del Admin Panel Web para forzar un incremento de versión tras cambios de catálogo — no es necesario que la app Android lo llame |

#### GET /api/sync/version

```json
// Response 200 (SyncValidateResponse)
{ "needsSync": false, "currentVersion": 3, "description": "Actualización de productos" }
```

#### POST /api/sync/validate

```json
// Request
{ "clientVersion": 2 }

// Response 200 (SyncValidateResponse)
{ "needsSync": true, "currentVersion": 3, "description": "Actualización de productos" }
```

#### GET /api/sync/catalog

```json
// Response 200 (SyncCatalogResponse)
{
  "version": 3,
  "products": [
    {
      "id": 1, "name": "Molde Tipo A", "pricePerUnit": 1500, "stock": 50,
      "imageUrl": "/uploads/123456_abc123.jpg", "description": "Molde de acero tipo A",
      "categoryId": 1, "categoryName": "Moldes"
    }
  ],
  "categories": [
    { "id": 1, "name": "Moldes", "parentId": null }
  ]
}
```

#### GET /api/sync/states

```json
// Response 200 (SyncStatesResponse)
{
  "version": 3,
  "states": [ { "id": 1, "name": "PENDIENTE", "priority": 1 } ],
  "shipmentStates": [ { "id": 1, "name": "Pendiente", "priority": 1 } ]
}
```

---

### 🖼️ Subida de Imágenes

| Método | Ruta | Auth | Descripción |
|--------|------|------|-------------|
| POST | `/api/upload` | ✅ | Subir imagen multipart |

**Flujo recomendado:**
1. Seleccionar imagen en la app
2. Subir a `POST /api/upload` como `multipart/form-data`, campo `file`
3. Recibir URL: `{ "url": "/uploads/123456_abc123.jpg" }`
4. Usar esa URL al crear/actualizar producto en `imageUrl`

```
Response 201: { "url": "/uploads/171234567_abc123.jpg" }
```

Para mostrar la imagen, anteponer la Base URL:
- Debug: `http://10.0.2.2:8080/uploads/171234567_abc123.jpg`
- Release: `https://tu-app.railway.app/uploads/171234567_abc123.jpg`

Límite: 10MB por archivo.

---

## Modelos de Datos (Kotlin)

### Auth

```kotlin
data class LoginRequest(
    val username: String,
    val password: String
)

// v4: ya NO tiene campo "role" — el usuario creado no tiene roles asignados.
// Ver nota en la sección Auth sobre usar POST /api/admin/users si se necesita
// crear el usuario CON roles desde el alta.
data class RegisterRequest(
    val fullName: String,
    val username: String,
    val email: String,
    val password: String
)

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long,
    val user: UserResponse
)

// v4: reemplaza el campo "role: String" por "roles"/"permissions".
data class RoleSummary(
    val id: Long,
    val name: String
)

data class UserResponse(
    val id: String,
    val username: String,
    val email: String,
    val fullName: String,
    val roles: List<RoleSummary>,
    val permissions: List<String>,
    val isEnabled: Boolean = true
)

data class RefreshTokenRequest(
    val refreshToken: String
)

data class UserMeResponse(
    val id: String,
    val username: String,
    val email: String,
    val fullName: String,
    val roles: List<RoleSummary>,
    val permissions: List<String>,
    val isEnabled: Boolean
)

data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)
```

### Customers

```kotlin
data class Customer(
    val cedula: Long,
    val name: String,
    val phones: List<String> = emptyList(),
    val addresses: List<String> = emptyList()
)
```

### Productos

```kotlin
data class CreateProductRequest(
    val name: String,
    val pricePerUnit: Long,
    val stock: Int,
    val imageUrl: String? = null,   // v4: ahora opcional
    val description: String,
    val categoryId: Long            // v4: ahora obligatorio (antes era nullable)
)

// UpdateProductRequest tiene exactamente los mismos campos que CreateProductRequest.

data class ProductResponse(
    val id: Long,
    val name: String,
    val pricePerUnit: Long,
    val stock: Int,
    val imageUrl: String,
    val description: String,
    val category: CategoryResponse,   // embebido: solo id/name, ver más abajo
    val createdAt: String,
    val updatedAt: String
)
```

### Categorías

```kotlin
// Versión embebida dentro de ProductResponse.category (solo id + name).
data class CategoryResponse(
    val id: Long,
    val name: String
)

// Versión completa devuelta por /api/categories (con soporte de subcategorías).
data class CategoryFullResponse(
    val id: Long,
    val name: String,
    val parentId: Long?,
    val createdAt: String,
    val updatedAt: String
)

data class CreateCategoryRequest(
    val name: String,
    val parentId: Long? = null
)
// UpdateCategoryRequest tiene los mismos campos.
```

### Transportadoras (Carriers) — nuevo módulo

```kotlin
data class CreateCarrierRequest(
    val name: String,
    val contactPhone: String? = null
)

data class UpdateCarrierRequest(
    val name: String,
    val contactPhone: String? = null,
    val active: Boolean = true
)

data class CarrierResponse(
    val id: Long,
    val name: String,
    val contactPhone: String?,
    val active: Boolean,
    val createdAt: String,
    val updatedAt: String
)
```

### Estados (States) y Estados de Envío (ShipmentStates) — nuevo CRUD

```kotlin
data class CreateStateRequest(
    val name: String,
    val priority: Int,
    val type: String = "OPERATION"
)
// UpdateStateRequest tiene los mismos campos.

data class StateResponse(
    val id: Long,
    val name: String,
    val priority: Int,
    val type: String,
    val createdAt: String,
    val updatedAt: String
)

data class CreateShipmentStateRequest(
    val stateId: Long,
    val name: String
)

data class ShipmentStateResponse(
    val id: Long,
    val stateId: Long,
    val stateName: String,
    val name: String,
    val createdAt: String,
    val updatedAt: String
)
```

### Órdenes

```kotlin
data class CreateOrderRequest(
    val customerId: Long,
    val operationStateId: Long,
    val orderPrice: Long,
    val orderAddress: String,
    val orderPhone: String,
    val orderCity: String
)

data class UpdateOrderRequest(
    val operationStateId: Long,
    val orderPrice: Long,
    val orderAddress: String,
    val orderPhone: String,
    val orderCity: String,
    val observation: String? = null   // v4: campo nuevo
)

data class CreateOrderCompleteRequest(
    val name: String,
    val cedula: Long,
    val address: String,
    val city: String,
    val phone: String,
    val order: OrderData
)

data class OrderData(
    val orderPrice: Long,
    val operationStateId: Long,
    val orderProducts: List<OrderProductData>
)

data class OrderProductData(
    val idProduct: Long,
    val quantity: Int
)

data class OrderResponse(
    val id: Long,
    val customerId: Long,
    val customerName: String,
    val operationStateId: Long,
    val operationStateName: String,
    val orderPrice: Long,
    val orderAddress: String,
    val orderPhone: String,
    val orderCity: String,
    val observation: String?,   // v4: campo nuevo
    val createdAt: String,
    val updatedAt: String
)

// Nuevo: PATCH /api/orders/{id}/ship
data class ShipOrderRequest(
    val carrierId: Long,
    val guideNumber: String,
    val shippingStateId: Long,
    val isCashOnDelivery: Boolean = true,
    val shippingCost: Long? = null,
    val estimateDeliveryDate: String? = null,
    val weight: Long? = null
)

data class OrderShipmentResponse(
    val orderId: Long,
    val operationStateId: Long,
    val operationStateName: String,
    val shipmentId: Long,
    val carrierId: Long,
    val carrierName: String,
    val guideNumber: String?,
    val shippingStateId: Long,
    val shippingStateName: String
)
```

### Order Products

```kotlin
data class CreateOrderProductRequest(
    val orderId: Long,
    val productId: Long,
    val quantity: Int,
    val price: Long
)

data class UpdateOrderProductRequest(
    val quantity: Int,
    val price: Long
)

data class OrderProductResponse(
    val orderId: Long,
    val productId: Long,
    val productName: String,
    val quantity: Int,
    val price: Long,
    val createdAt: String,
    val updatedAt: String
)
```

### Envíos

```kotlin
// v4: "carrier: String" se reemplazó por "carrierId: Long" (FK a Carrier).
// "stateId/stateName" se renombraron a "shippingStateId/shippingStateName".
// Se agregaron isCashOnDelivery, shippingCost, estimateDeliveryDate, weight.
// Ya no existe un campo "notes" en Shipment (las notas viven en el tracking).
data class CreateShipmentRequest(
    val orderId: Long,
    val carrierId: Long,
    val isCashOnDelivery: Boolean = true,
    val shippingStateId: Long,
    val shippingCost: Long? = null,
    val estimateDeliveryDate: String? = null,
    val weight: Long? = null,
    val guideNumber: String? = null
)
// UpdateShipmentRequest tiene los mismos campos (guideNumber conserva el
// valor anterior si se envía null).

data class AssignGuideRequest(
    val guideNumber: String
)

data class ShipmentResponse(
    val id: Long,
    val orderId: Long,
    val carrierId: Long,
    val carrierName: String,
    val isCashOnDelivery: Boolean,
    val shippingStateId: Long,
    val shippingStateName: String,
    val shippingCost: Long?,
    val estimateDeliveryDate: String?,
    val weight: Long?,
    val guideNumber: String?,
    val createdAt: String,
    val updatedAt: String
)

// v4: "previousState/newState/changedBy" se reemplazaron por
// "statusFrom/statusTo/notes"; changedBy ya no se expone en la respuesta.
data class TrackingEntryResponse(
    val id: Long,
    val statusFrom: String?,
    val statusTo: String,
    val notes: String?,
    val changedAt: String
)
```

### Sincronización

```kotlin
data class SyncValidateResponse(
    val needsSync: Boolean,
    val currentVersion: Long,
    val description: String
)

data class SyncValidateRequest(
    val clientVersion: Long
)

data class SyncCatalogResponse(
    val version: Long,
    val products: List<ProductSyncResponse>,
    val categories: List<CategorySyncResponse>
)

data class ProductSyncResponse(
    val id: Long,
    val name: String,
    val pricePerUnit: Long,
    val stock: Int,
    val imageUrl: String,
    val description: String,
    val categoryId: Long,
    val categoryName: String
)

data class CategorySyncResponse(
    val id: Long,
    val name: String,
    val parentId: Long?
)

data class SyncStatesResponse(
    val version: Long,
    val states: List<StateSyncResponse>,
    val shipmentStates: List<ShipmentStateSyncResponse>
)

data class StateSyncResponse(
    val id: Long,
    val name: String,
    val priority: Int
)

data class ShipmentStateSyncResponse(
    val id: Long,
    val name: String,
    val priority: Int   // heredada del State padre
)
```

### Dashboard

```kotlin
// v4: reemplaza por completo al antiguo SalesResponse/PeriodInfo.
data class SalesSummaryResponse(
    val totalOrders: Int,
    val totalRevenue: Long,
    val avgOrderValue: Long,
    val totalProducts: Int,
    val totalUsers: Int,
    val ordersByState: List<StateCount>
)

data class StateCount(
    val state: String,
    val count: Int
)
```

### Admin

```kotlin
// v4: ya no hay un "role: String" — se envían ids de rol. La respuesta de
// usuario reutiliza el mismo UserResponse de la sección Auth (roles +
// permissions + isEnabled, SIN "enabled" ni "createdAt").
data class CreateUserRequest(
    val fullName: String,
    val username: String,
    val email: String,
    val password: String,
    val roleIds: List<Long> = emptyList()
)

data class UpdateUserRolesRequest(
    val roleIds: List<Long>
)

data class ResetPasswordRequest(
    val newPassword: String
)

data class AuditLogEntry(
    val id: String,
    val username: String,
    val action: String,
    val entityType: String,
    val entityId: String,
    val oldValues: String?,
    val newValues: String?,
    val ipAddress: String?,
    val createdAt: String
)

data class AuditLogPageResponse(
    val content: List<AuditLogEntry>,
    val totalElements: Long,
    val totalPages: Int,
    val currentPage: Int,
    val pageSize: Int
)
```

### Roles y Permisos

```kotlin
data class PermissionDto(
    val code: String,
    val description: String
)

data class RoleRequest(
    val name: String,
    val description: String? = null,
    val permissions: Set<String> = emptySet()
)

data class RoleResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val permissions: List<String>,
    val userCount: Long
)
```

### Upload

```kotlin
data class UploadResponse(
    val url: String
)
```

### Error

```kotlin
data class ErrorResponse(
    val timestamp: String,
    val status: Int,
    val error: String,
    val message: String,
    val details: List<ValidationError>? = null
)

data class ValidationError(
    val field: String,
    val message: String
)
```

---

## Máquina de Estados

> ⚠️ **Importante:** desde que `State` y `ShipmentState` tienen CRUD completo (`/api/states`, `/api/shipment-states`, sección Endpoints), los IDs **ya no son fijos ni garantizados entre entornos** — dependen del orden de inserción (`BIGSERIAL`) y pueden ser editados desde el Admin Panel Web. **No hardcodear estos IDs en la app**: resolverlos siempre vía `GET /api/states` / `GET /api/shipment-states` (o `GET /api/sync/states` para el flujo offline-first). Las tablas de abajo muestran los valores **sembrados por defecto** (`DataInitializer`, perfil `dev`) solo como referencia.

### Estados de Operación (Order.operationStateId) — seed por defecto

| ID (seed) | Nombre | Descripción |
|----|--------|-------------|
| 1 | PENDIENTE | Orden creada, pendiente de procesamiento |
| 2 | EN PROCESO | Orden en preparación |
| 3 | ENVIADO | Orden despachada (el estado al que mueve `PATCH /api/orders/{id}/ship`) |
| 4 | DISPONIBLE | Envío disponible en oficina de destino |
| 5 | FINALIZADO | Entregado al cliente |
| 6 | NOVEDAD | Requiere atención |

### Estados de Envío (ShipmentState.shippingStateId) — seed por defecto

Cada estado de envío está ligado a un estado de operación padre (`stateId`).

| ID (seed) | Nombre | Estado de operación padre |
|----|--------|------|
| 1 | Pendiente | PENDIENTE |
| 2 | En tránsito | ENVIADO |
| 3 | En oficina | DISPONIBLE |
| 4 | Entregado | FINALIZADO |
| 5 | Novedad | NOVEDAD |

---

## Manejo de Errores

### Códigos HTTP

| Código | Significado | Acción |
|--------|-------------|--------|
| 200 | Éxito | Continuar |
| 201 | Creado | Navegar a detalle |
| 204 | Sin contenido (eliminado) | Actualizar UI |
| 400 | Bad Request | Mostrar errores de validación |
| 401 | No autorizado | Refrescar token o pedir login |
| 403 | Prohibido | Mostrar "sin permisos" (el usuario no tiene el `permission` requerido) |
| 404 | No encontrado | Mostrar "no encontrado" |
| 409 | Conflicto (recurso ya existe) | Mostrar mensaje de duplicado (p. ej. nombre de categoría/transportadora/rol repetido) |
| 429 | Too Many Requests | Esperar e intentar de nuevo |
| 500 | Error interno | Reintentar más tarde |

### Response de Error

```json
{
  "timestamp": "2026-05-13T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Error de validación",
  "details": [
    { "field": "name", "message": "Name is required" }
  ]
}
```

---

## Ejemplo Completo Android

### 1. ApiService Interface

```kotlin
interface ApiService {

    // ── Auth ──
    @POST("/api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("/api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("/api/auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<AuthResponse>

    @POST("/api/auth/logout")
    suspend fun logout(): Response<Unit>

    @GET("/api/auth/me")
    suspend fun getMe(): Response<UserMeResponse>

    @PATCH("/api/auth/change-password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<Map<String, String>>

    // ── Customers (nuevo) ──
    @GET("/api/customers")
    suspend fun getCustomers(): Response<List<Customer>>

    @GET("/api/customers/{id}")
    suspend fun getCustomer(@Path("id") id: Long): Response<Customer>

    @POST("/api/customers")
    suspend fun createCustomer(@Body customer: Customer): Response<Customer>

    @PUT("/api/customers/{id}")
    suspend fun updateCustomer(@Path("id") id: Long, @Body customer: Customer): Response<Customer>

    @DELETE("/api/customers/{id}")
    suspend fun deleteCustomer(@Path("id") id: Long): Response<Unit>

    // ── Products ──
    @GET("/api/products")
    suspend fun getProducts(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50
    ): Response<List<ProductResponse>>

    @GET("/api/products/{id}")
    suspend fun getProduct(@Path("id") id: Long): Response<ProductResponse>

    @GET("/api/products/category/{categoryId}")
    suspend fun getProductsByCategory(@Path("categoryId") categoryId: Long): Response<List<ProductResponse>>

    @POST("/api/products")
    suspend fun createProduct(@Body request: CreateProductRequest): Response<ProductResponse>

    @PUT("/api/products/{id}")
    suspend fun updateProduct(@Path("id") id: Long, @Body request: UpdateProductRequest): Response<ProductResponse>

    @DELETE("/api/products/{id}")
    suspend fun deleteProduct(@Path("id") id: Long): Response<Unit>

    // ── Categories ──
    @GET("/api/categories")
    suspend fun getCategories(): Response<List<CategoryFullResponse>>

    @POST("/api/categories")
    suspend fun createCategory(@Body request: CreateCategoryRequest): Response<CategoryFullResponse>

    @PUT("/api/categories/{id}")
    suspend fun updateCategory(@Path("id") id: Long, @Body request: CreateCategoryRequest): Response<CategoryFullResponse>

    @DELETE("/api/categories/{id}")
    suspend fun deleteCategory(@Path("id") id: Long): Response<Unit>

    // ── Carriers (nuevo) ──
    @GET("/api/carriers")
    suspend fun getCarriers(@Query("onlyActive") onlyActive: Boolean = true): Response<List<CarrierResponse>>

    @POST("/api/carriers")
    suspend fun createCarrier(@Body request: CreateCarrierRequest): Response<CarrierResponse>

    @PUT("/api/carriers/{id}")
    suspend fun updateCarrier(@Path("id") id: Long, @Body request: UpdateCarrierRequest): Response<CarrierResponse>

    @DELETE("/api/carriers/{id}")
    suspend fun deleteCarrier(@Path("id") id: Long): Response<Unit>

    // ── States / Shipment States (nuevo CRUD) ──
    @GET("/api/states")
    suspend fun getStates(): Response<List<StateResponse>>

    @GET("/api/shipment-states")
    suspend fun getShipmentStates(): Response<List<ShipmentStateResponse>>

    // ── Orders ──
    @GET("/api/orders")
    suspend fun getOrders(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50
    ): Response<List<OrderResponse>>

    @GET("/api/orders/{id}")
    suspend fun getOrder(@Path("id") id: Long): Response<OrderResponse>

    @POST("/api/orders")
    suspend fun createOrder(@Body request: CreateOrderRequest): Response<OrderResponse>

    @POST("/api/orders/complete")
    suspend fun createOrderComplete(@Body request: CreateOrderCompleteRequest): Response<OrderResponse>

    @PUT("/api/orders/{id}")
    suspend fun updateOrder(@Path("id") id: Long, @Body request: UpdateOrderRequest): Response<OrderResponse>

    @DELETE("/api/orders/{id}")
    suspend fun deleteOrder(@Path("id") id: Long): Response<Unit>

    // Nuevo: marcar la orden como enviada (crea/actualiza el Shipment y mueve el estado en un paso)
    @PATCH("/api/orders/{id}/ship")
    suspend fun shipOrder(@Path("id") id: Long, @Body request: ShipOrderRequest): Response<OrderShipmentResponse>

    // ── Order Products ──
    @GET("/api/order-products")
    suspend fun getOrderProducts(): Response<List<OrderProductResponse>>

    @GET("/api/order-products/order/{orderId}")
    suspend fun getOrderProductsByOrder(@Path("orderId") orderId: Long): Response<List<OrderProductResponse>>

    @POST("/api/order-products")
    suspend fun addOrderProduct(@Body request: CreateOrderProductRequest): Response<OrderProductResponse>

    @PUT("/api/order-products/order/{orderId}/product/{productId}")
    suspend fun updateOrderProduct(
        @Path("orderId") orderId: Long,
        @Path("productId") productId: Long,
        @Body request: UpdateOrderProductRequest
    ): Response<OrderProductResponse>

    @DELETE("/api/order-products/order/{orderId}/product/{productId}")
    suspend fun deleteOrderProduct(@Path("orderId") orderId: Long, @Path("productId") productId: Long): Response<Unit>

    // ── Shipments ──
    @GET("/api/shipments")
    suspend fun getShipments(): Response<List<ShipmentResponse>>

    @GET("/api/shipments/{id}")
    suspend fun getShipment(@Path("id") id: Long): Response<ShipmentResponse>

    @POST("/api/shipments")
    suspend fun createShipment(@Body request: CreateShipmentRequest): Response<ShipmentResponse>

    @PATCH("/api/shipments/{id}/guide")
    suspend fun assignGuide(@Path("id") id: Long, @Body request: AssignGuideRequest): Response<ShipmentResponse>

    @GET("/api/shipments/{id}/tracking")
    suspend fun getTracking(@Path("id") id: Long): Response<List<TrackingEntryResponse>>

    // ── Dashboard ──
    // "from"/"to" son opcionales y de tipo LocalDateTime ISO (yyyy-MM-dd'T'HH:mm:ss);
    // si se omiten, el backend usa los últimos 30 días.
    @GET("/api/dashboard/sales")
    suspend fun getSales(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null
    ): Response<SalesSummaryResponse>

    // ── Admin: usuarios ──
    @GET("/api/admin/users")
    suspend fun getUsers(): Response<List<UserResponse>>

    @POST("/api/admin/users")
    suspend fun createUser(@Body request: CreateUserRequest): Response<UserResponse>

    @PUT("/api/admin/users/{id}/roles")
    suspend fun updateUserRoles(@Path("id") id: String, @Body request: UpdateUserRolesRequest): Response<UserResponse>

    @DELETE("/api/admin/users/{id}")
    suspend fun deleteUser(@Path("id") id: String): Response<Unit>

    @PATCH("/api/admin/users/{id}/status")
    suspend fun toggleUserStatus(@Path("id") id: String): Response<UserResponse>

    @PATCH("/api/admin/users/{id}/password")
    suspend fun resetUserPassword(@Path("id") id: String, @Body request: ResetPasswordRequest): Response<Map<String, String>>

    @GET("/api/admin/audit")
    suspend fun getAuditLogs(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("username") username: String? = null,
        @Query("entityType") entityType: String? = null,
        @Query("action") action: String? = null,
        @Query("fromDate") fromDate: String? = null,
        @Query("toDate") toDate: String? = null
    ): Response<AuditLogPageResponse>

    // ── Admin: roles y permisos ──
    @GET("/api/admin/permissions")
    suspend fun getPermissionCatalog(): Response<List<PermissionDto>>

    @GET("/api/admin/roles")
    suspend fun getRoles(): Response<List<RoleResponse>>

    @POST("/api/admin/roles")
    suspend fun createRole(@Body request: RoleRequest): Response<RoleResponse>

    @PUT("/api/admin/roles/{id}")
    suspend fun updateRole(@Path("id") id: Long, @Body request: RoleRequest): Response<RoleResponse>

    @DELETE("/api/admin/roles/{id}")
    suspend fun deleteRole(@Path("id") id: Long): Response<Unit>

    // ── Sync (sin auth) ──
    @GET("/api/sync/version")
    suspend fun getSyncVersion(): Response<SyncValidateResponse>

    @POST("/api/sync/validate")
    suspend fun validateSync(@Body request: SyncValidateRequest): Response<SyncValidateResponse>

    @GET("/api/sync/catalog")
    suspend fun getCatalog(): Response<SyncCatalogResponse>

    @GET("/api/sync/states")
    suspend fun getSyncStates(): Response<SyncStatesResponse>

    // ── Upload ──
    @Multipart
    @POST("/api/upload")
    suspend fun uploadImage(@Part file: MultipartBody.Part): Response<UploadResponse>
}
```

### 2. Token Manager

> v4: ya no existe un `user_role` único. Se guarda la lista de `permissions` (serializada como `Set<String>` separado por comas, o con Gson/kotlinx.serialization si preferís JSON) y se expone un helper `hasPermission(...)` para gatear la UI. El JWT no lleva esta información en sus claims, así que hay que persistirla en el login/`getMe()` y refrescarla cuando haga falta.

```kotlin
object TokenManager {
    private lateinit var context: Context
    private val prefs by lazy {
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    }

    fun init(appContext: Context) {
        context = appContext.applicationContext
    }

    fun saveTokens(authResponse: AuthResponse) {
        prefs.edit().apply {
            putString("access_token", authResponse.accessToken)
            putString("refresh_token", authResponse.refreshToken)
            putLong("expires_at", System.currentTimeMillis() + authResponse.expiresIn * 1000)
            putStringSet("user_permissions", authResponse.user.permissions.toSet())
            apply()
        }
    }

    // Llamar tras GET /api/auth/me para refrescar permisos sin volver a loguear
    // (por ejemplo si un admin le cambió los roles al usuario logueado).
    fun updatePermissions(permissions: List<String>) {
        prefs.edit().putStringSet("user_permissions", permissions.toSet()).apply()
    }

    fun getAccessToken(): String? = prefs.getString("access_token", null)
    fun getRefreshToken(): String? = prefs.getString("refresh_token", null)
    fun getPermissions(): Set<String> = prefs.getStringSet("user_permissions", emptySet()) ?: emptySet()
    fun hasPermission(code: String): Boolean = getPermissions().contains(code)
    fun isTokenExpired(): Boolean =
        System.currentTimeMillis() >= prefs.getLong("expires_at", 0)

    fun clearTokens() { prefs.edit().clear().apply() }
}
```

### 3. Auth Interceptor

```kotlin
class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = TokenManager.getAccessToken()
        val request = if (token != null) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}
```

### 4. Retrofit Client

```kotlin
object RetrofitClient {
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
        else HttpLoggingInterceptor.Level.NONE
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor())
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
```

### 5. Uso: Subir imagen y crear producto

```kotlin
// 1. Subir imagen
suspend fun uploadImage(uri: Uri): String {
    val file = File(uri.path) // o resolver content resolver
    val requestFile = RequestBody.create(MediaType.parse("image/*"), file)
    val multipart = MultipartBody.Part.createFormData("file", file.name, requestFile)

    val response = RetrofitClient.apiService.uploadImage(multipart)
    if (response.isSuccessful) {
        return response.body()!!.url  // "/uploads/123456_abc123.jpg"
    }
    throw Exception("Upload failed")
}

// 2. Crear producto con la URL (categoryId es obligatorio desde v4)
suspend fun createProduct(name: String, price: Long, imageUrl: String, categoryId: Long) {
    val request = CreateProductRequest(
        name = name,
        pricePerUnit = price,
        stock = 0,
        imageUrl = imageUrl,
        description = "",
        categoryId = categoryId
    )
    RetrofitClient.apiService.createProduct(request)
}
```

---

## 📞 Soporte

**Swagger UI:** `BASE_URL/swagger-ui/index.html` (requiere autenticación ADMIN)  
**Admin Panel Web:** `BASE_URL/admin/` (React SPA, requiere ADMIN)  
**OpenAPI Spec:** `BASE_URL/api-docs`

---

*Última actualización: Agosto 2026*
