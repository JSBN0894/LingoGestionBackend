# Guía de Integración Android - Linogo Backend API

Documentación técnica para integrar la aplicación **Android** con el backend de gestión de Linogo.

**Versión de la API:** `v2`  
**Deploy:** Railway  
**Última actualización:** Mayo 2026

---

## 📋 Índice

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
6. Swagger UI: `http://localhost:8080/swagger-ui.html` (solo perfil `dev`)
7. App Android: configurar `BASE_URL = http://10.0.2.2:8080`

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

### Sistema de Roles

| Rol | Acceso |
|-----|--------|
| `ADMIN` | Acceso total + gestión de usuarios |
| `VENTAS` | Pedidos, productos, clientes |
| `PRODUCCION` | Productos, categorías, dashboard |
| `LOGISTICA` | Envíos, guías, tracking |
| `USER` | Lectura básica |

Cada endpoint indica el rol mínimo requerido en la tabla de endpoints.

### Endpoints de Autenticación

---

## Endpoints

### 🔐 Auth

| Método | Ruta | Auth | Roles | Descripción |
|--------|------|------|-------|-------------|
| POST | `/api/auth/login` | ❌ | - | Iniciar sesión |
| POST | `/api/auth/register` | ✅ | ADMIN | Crear usuario |
| POST | `/api/auth/refresh` | ✅ | Cualquiera | Renovar token |
| POST | `/api/auth/logout` | ✅ | Cualquiera | Cerrar sesión |

#### POST /api/auth/login

```json
// Request
{ "username": "admin", "password": "Admin@123456" }

// Response 200
{
  "accessToken": "eyJhbGci...",
  "refreshToken": "eyJhbGci...",
  "expiresIn": 900,
  "user": {
    "id": "uuid-123",
    "username": "admin",
    "email": "admin@linogo.com",
    "fullName": "Administrador",
    "role": "ADMIN"
  }
}
```

#### POST /api/auth/register (solo ADMIN)

```json
// Request
{
  "fullName": "Juan Pérez",
  "username": "juanperez",
  "email": "juan@example.com",
  "password": "segura123",
  "role": "VENTAS"
}

// Response: mismo formato que login
```

Roles válidos para registro: `USER`, `VENTAS`, `PRODUCCION`, `LOGISTICA`

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

---

### 👥 Clientes — Customer

| Método | Ruta | Auth | Roles | Descripción |
|--------|------|------|-------|-------------|
| GET | `/api/customers` | ✅ | Cualquiera | Listar clientes |
| GET | `/api/customers/{cedula}` | ✅ | Cualquiera | Obtener por cédula |
| POST | `/api/customers` | ✅ | VENTAS, ADMIN | Crear cliente |
| PUT | `/api/customers/{cedula}` | ✅ | VENTAS, ADMIN | Actualizar cliente |
| DELETE | `/api/customers/{cedula}` | ✅ | ADMIN | Eliminar cliente |

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

| Método | Ruta | Auth | Roles | Descripción |
|--------|------|------|-------|-------------|
| GET | `/api/products` | ✅ | Cualquiera | Listar (con paginación) |
| GET | `/api/products/{id}` | ✅ | Cualquiera | Obtener por ID |
| GET | `/api/products/category/{categoryId}` | ✅ | Cualquiera | Filtrar por categoría |
| POST | `/api/products` | ✅ | VENTAS, PRODUCCION, ADMIN | Crear producto |
| PUT | `/api/products/{id}` | ✅ | VENTAS, PRODUCCION, ADMIN | Actualizar |
| DELETE | `/api/products/{id}` | ✅ | ADMIN | Eliminar |

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

**Flujo de imagen:** Primero subir la imagen a `POST /api/upload`, luego usar la URL devuelta en `imageUrl`.

---

### 📁 Categorías

| Método | Ruta | Auth | Descripción |
|--------|------|------|-------------|
| GET | `/api/categories` | ✅ | Listar categorías |
| GET | `/api/categories/{id}` | ✅ | Obtener por ID |

---

### 🛒 Órdenes

| Método | Ruta | Auth | Roles | Descripción |
|--------|------|------|-------|-------------|
| GET | `/api/orders` | ✅ | Cualquiera | Listar (con paginación) |
| GET | `/api/orders/{id}` | ✅ | Cualquiera | Obtener por ID |
| POST | `/api/orders` | ✅ | VENTAS, ADMIN | Crear orden |
| PUT | `/api/orders/{id}` | ✅ | VENTAS, ADMIN | Actualizar |
| DELETE | `/api/orders/{id}` | ✅ | ADMIN | Eliminar |
| POST | `/api/orders/complete` | ✅ | VENTAS, ADMIN | Crear orden con productos |

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
    "createdAt": "2026-01-01T00:00:00",
    "updatedAt": "2026-01-01T00:00:00"
  }
]
```

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

---

### 🔗 Order Products

| Método | Ruta | Auth | Descripción |
|--------|------|------|-------------|
| GET | `/api/order-products` | ✅ | Listar todos |
| GET | `/api/order-products/{id}` | ✅ | Obtener por ID |
| GET | `/api/order-products/by-order/{orderId}` | ✅ | Productos de una orden |

---

### 🚚 Envíos (Shipments)

| Método | Ruta | Auth | Roles | Descripción |
|--------|------|------|-------|-------------|
| GET | `/api/shipments` | ✅ | Cualquiera | Listar envíos |
| GET | `/api/shipments/{id}` | ✅ | Cualquiera | Obtener por ID |
| POST | `/api/shipments` | ✅ | LOGISTICA, ADMIN | Crear envío |
| PUT | `/api/shipments/{id}` | ✅ | LOGISTICA, ADMIN | Actualizar |
| DELETE | `/api/shipments/{id}` | ✅ | ADMIN | Eliminar |
| PATCH | `/api/shipments/{id}/guide` | ✅ | LOGISTICA, ADMIN | Asignar guía |

#### POST /api/shipments

```json
// Request
{
  "orderId": 1,
  "carrier": "Cooperativa",
  "guideNumber": null,
  "stateId": 1,
  "notes": "Envío urgente"
}

// Response 201
{
  "id": 1,
  "orderId": 1,
  "carrier": "Cooperativa",
  "guideNumber": null,
  "stateId": 1,
  "stateName": "Pendiente",
  "notes": "Envío urgente",
  "createdAt": "2026-01-01T00:00:00",
  "updatedAt": "2026-01-01T00:00:00"
}
```

#### PATCH /api/shipments/{id}/guide

Asignar o actualizar número de guía (solo LOGISTICA).

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

```json
// Response 200
[
  {
    "id": 1,
    "shipmentId": 1,
    "previousState": null,
    "newState": "Pendiente",
    "changedBy": "admin",
    "changedAt": "2026-01-01T00:00:00"
  },
  {
    "id": 2,
    "shipmentId": 1,
    "previousState": "Pendiente",
    "newState": "En tránsito",
    "changedBy": "logistica1",
    "changedAt": "2026-01-02T00:00:00"
  }
]
```

---

### 📊 Dashboard

| Método | Ruta | Auth | Descripción |
|--------|------|------|-------------|
| GET | `/api/dashboard/sales?from=2026-01-01&to=2026-12-31` | ✅ | Ventas en período |

```json
// Response 200
{
  "totalOrders": 150,
  "totalRevenue": 675000,
  "averageOrderValue": 4500,
  "period": { "from": "2026-01-01", "to": "2026-12-31" }
}
```

---

### 👑 Admin — Gestión de Usuarios

| Método | Ruta | Auth | Roles | Descripción |
|--------|------|------|-------|-------------|
| GET | `/api/admin/users` | ✅ | ADMIN | Listar usuarios |
| GET | `/api/admin/users/{id}` | ✅ | ADMIN | Obtener usuario |
| POST | `/api/admin/users` | ✅ | ADMIN | Crear usuario |
| PATCH | `/api/admin/users/{id}/role` | ✅ | ADMIN | Cambiar rol |
| DELETE | `/api/admin/users/{id}` | ✅ | ADMIN | Eliminar usuario |

#### POST /api/admin/users

```json
// Request
{
  "fullName": "Nuevo Usuario",
  "username": "nuevouser",
  "email": "nuevo@linogo.com",
  "password": "pass123",
  "role": "VENTAS"
}

// Response 201
{
  "id": "uuid-456",
  "username": "nuevouser",
  "email": "nuevo@linogo.com",
  "fullName": "Nuevo Usuario",
  "role": "VENTAS",
  "enabled": true,
  "createdAt": "2026-01-01T00:00:00"
}
```

#### PATCH /api/admin/users/{id}/role

```json
// Request
{ "role": "LOGISTICA" }

// Response 200: usuario con rol actualizado
```

---

### 🔄 Sincronización (público, sin auth)

| Método | Ruta | Auth | Descripción |
|--------|------|------|-------------|
| GET | `/api/sync/version` | ❌ | Versión actual del catálogo |
| GET | `/api/sync/catalog` | ❌ | Catálogo completo (productos + categorías) |
| GET | `/api/sync/states` | ❌ | Lista de estados |
| POST | `/api/sync/validate` | ❌ | Validar si necesita sync |

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

data class RegisterRequest(
    val fullName: String,
    val username: String,
    val email: String,
    val password: String,
    val role: String? = "USER"  // USER, VENTAS, PRODUCCION, LOGISTICA
)

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
    val user: UserResponse
)

data class UserResponse(
    val id: String,
    val username: String,
    val email: String,
    val fullName: String,
    val role: String
)

data class RefreshTokenRequest(
    val refreshToken: String
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
    val imageUrl: String,
    val description: String,
    val categoryId: Long? = null
)

data class ProductResponse(
    val id: Long,
    val name: String,
    val pricePerUnit: Long,
    val stock: Int,
    val imageUrl: String,
    val description: String,
    val category: CategoryResponse?,
    val createdAt: String,
    val updatedAt: String
)
```

### Categorías

```kotlin
data class CategoryResponse(
    val id: Long,
    val name: String
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
    val createdAt: String,
    val updatedAt: String
)
```

### Envíos

```kotlin
data class CreateShipmentRequest(
    val orderId: Long,
    val carrier: String,
    val guideNumber: String? = null,
    val stateId: Long,
    val notes: String? = null
)

data class AssignGuideRequest(
    val guideNumber: String
)

data class ShipmentResponse(
    val id: Long,
    val orderId: Long,
    val carrier: String,
    val guideNumber: String?,
    val stateId: Long,
    val stateName: String,
    val notes: String?,
    val createdAt: String,
    val updatedAt: String
)

data class TrackingHistoryResponse(
    val id: Long,
    val shipmentId: Long,
    val previousState: String?,
    val newState: String,
    val changedBy: String,
    val changedAt: String
)
```

### Dashboard

```kotlin
data class SalesResponse(
    val totalOrders: Long,
    val totalRevenue: Long,
    val averageOrderValue: Double,
    val period: PeriodInfo
)

data class PeriodInfo(
    val from: String,
    val to: String
)
```

### Admin

```kotlin
data class CreateUserRequest(
    val fullName: String,
    val username: String,
    val email: String,
    val password: String,
    val role: String
)

data class UpdateRoleRequest(
    val role: String
)

data class AdminUserResponse(
    val id: String,
    val username: String,
    val email: String,
    val fullName: String,
    val role: String,
    val enabled: Boolean,
    val createdAt: String
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

### Estados de Operación (Order.operationStateId)

| ID | Nombre | Descripción |
|----|--------|-------------|
| 1 | Pendiente | Orden creada, pendiente de procesamiento |
| 2 | Procesado | Orden procesada, lista para envío |
| 3 | Guía asociada | Se asignó guía de envío |
| 4 | En tránsito | Envío en ruta |
| 5 | En oficina | Envío en oficina de destino |
| 6 | Entregado | Entregado al cliente |
| 7 | Novedad | Requiere atención |

### Estados de Envío (Shipment.stateId / shipment_states)

| ID | Nombre |
|----|--------|
| 1 | Pendiente |
| 2 | En tránsito |
| 3 | En oficina |
| 4 | Entregado |
| 5 | Novedad |

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
| 403 | Prohibido | Mostrar "sin permisos" |
| 404 | No encontrado | Mostrar "no encontrado" |
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
    suspend fun getCategories(): Response<List<CategoryResponse>>

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
    suspend fun getTracking(@Path("id") id: Long): Response<List<TrackingHistoryResponse>>

    // ── Dashboard ──
    @GET("/api/dashboard/sales")
    suspend fun getSales(
        @Query("from") from: String,
        @Query("to") to: String
    ): Response<SalesResponse>

    // ── Admin ──
    @GET("/api/admin/users")
    suspend fun getUsers(): Response<List<AdminUserResponse>>

    @POST("/api/admin/users")
    suspend fun createUser(@Body request: CreateUserRequest): Response<AdminUserResponse>

    @PATCH("/api/admin/users/{id}/role")
    suspend fun updateUserRole(@Path("id") id: String, @Body request: UpdateRoleRequest): Response<AdminUserResponse>

    @DELETE("/api/admin/users/{id}")
    suspend fun deleteUser(@Path("id") id: String): Response<Unit>

    // ── Sync (sin auth) ──
    @GET("/api/sync/version")
    suspend fun getSyncVersion(): Response<SyncVersionResponse>

    @GET("/api/sync/catalog")
    suspend fun getCatalog(): Response<CatalogResponse>

    // ── Upload ──
    @Multipart
    @POST("/api/upload")
    suspend fun uploadImage(@Part file: MultipartBody.Part): Response<UploadResponse>
}
```

### 2. Token Manager

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
            putString("user_role", authResponse.user.role)
            apply()
        }
    }

    fun getAccessToken(): String? = prefs.getString("access_token", null)
    fun getRefreshToken(): String? = prefs.getString("refresh_token", null)
    fun getUserRole(): String? = prefs.getString("user_role", null)
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

// 2. Crear producto con la URL
suspend fun createProduct(name: String, price: Long, imageUrl: String) {
    val request = CreateProductRequest(
        name = name,
        pricePerUnit = price,
        stock = 0,
        imageUrl = imageUrl,
        description = ""
    )
    RetrofitClient.apiService.createProduct(request)
}
```

---

## 📞 Soporte

**Swagger UI:** `BASE_URL/swagger-ui.html` (solo en perfil `dev`)  
**OpenAPI Spec:** `BASE_URL/api-docs`

---

*Última actualización: Mayo 2026*
