# Guía de Integración para Desarrolladores Móviles

## Linogo Backend API

Documentación técnica para integrar la aplicación móvil con el backend de gestión de Linogo.

---

## 📋 Índice

1. [Información General](#información-general)
2. [Autenticación](#autenticación)
3. [Endpoints](#endpoints)
4. [Modelos de Datos](#modelos-de-datos)
5. [Sincronización](#sincronización)
6. [Manejo de Errores](#manejo-de-errores)
7. [Ejemplos de Código](#ejemplos-de-código)

---

## Información General

### Base URL

| Entorno | URL |
|---------|-----|
| Desarrollo | `http://localhost:8080` |
| Producción | `https://tu-dominio.com` |

### Autenticación

La API utiliza **JWT (JSON Web Tokens)** con el esquema `Bearer`.

**Headers requeridos:**
```
Authorization: Bearer <access_token>
Content-Type: application/json
```

### Tokens

| Token | Duración | Uso |
|-------|----------|-----|
| `accessToken` | 15 minutos | Cada petición a la API |
| `refreshToken` | 7 días | Obtener nuevo access token |

---

## Autenticación

### 1. Registro de Usuario

```http
POST /api/auth/register
Content-Type: application/json
```

**Request:**
```json
{
  "fullName": "Juan Pérez",
  "username": "juanperez",
  "email": "juan@example.com",
  "password": "contraseña123"
}
```

**Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 900000,
  "user": {
    "id": "1",
    "username": "juanperez",
    "email": "juan@example.com",
    "fullName": "Juan Pérez",
    "role": "USER"
  }
}
```

### 2. Login

```http
POST /api/auth/login
Content-Type: application/json
```

**Request:**
```json
{
  "username": "juanperez",
  "password": "contraseña123"
}
```

**Response:** (Mismo formato que registro)

### 3. Refresh Token

```http
POST /api/auth/refresh
Content-Type: application/json
```

**Request:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response:** (Mismo formato)

### 4. Logout

```http
POST /api/auth/logout
Authorization: Bearer <access_token>
```

**Response (200 OK):**
```json
{
  "message": "Sesión cerrada exitosamente"
}
```

---

## Endpoints

### 👥 Clientes

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/api/clients` | Crear cliente |
| GET | `/api/clients` | Listar todos |
| GET | `/api/clients/{idUser}` | Obtener por ID |
| PUT | `/api/clients/{idUser}` | Actualizar |
| DELETE | `/api/clients/{idUser}` | Eliminar |

**Crear Cliente:**
```http
POST /api/clients
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{
  "name": "Juan Pérez",
  "idUser": "user_123",
  "defaultPhone": "+54911223344",
  "defaultCity": "Buenos Aires",
  "defaultAddress": "Av. Corrientes 1234"
}
```

### 📦 Productos

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/api/products` | Crear producto |
| GET | `/api/products` | Listar todos |
| GET | `/api/products/{id}` | Obtener por ID |
| GET | `/api/products/category/{categoryId}` | Filtrar por categoría |
| PUT | `/api/products/{id}` | Actualizar |
| DELETE | `/api/products/{id}` | Eliminar |

**Crear Producto:**
```http
POST /api/products
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{
  "id": 1001,
  "name": "Molde Tipo A",
  "pricePerUnit": 1500,
  "stock": 50,
  "imageUrl": "https://ejemplo.com/imagen.jpg",
  "description": "Molde de acero tipo A",
  "categoryId": 1
}
```

### 🛒 Órdenes

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/api/orders` | Crear orden |
| GET | `/api/orders` | Listar todas |
| GET | `/api/orders/{id}` | Obtener por ID |
| PUT | `/api/orders/{id}` | Actualizar |
| DELETE | `/api/orders/{id}` | Eliminar |

**Crear Orden:**
```http
POST /api/orders
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{
  "client": {
    "idUser": "user_123"
  },
  "operationState": {
    "id": 1
  },
  "orderPrice": 4500,
  "orderAddress": "Av. Corrientes 1234",
  "orderPhone": "+54911223344",
  "orderCity": "Buenos Aires"
}
```

### 🚚 Envíos (Shipments)

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/api/shipments` | Crear envío |
| GET | `/api/shipments` | Listar todos |
| GET | `/api/shipments/{id}` | Obtener por ID |
| PUT | `/api/shipments/{id}` | Actualizar |
| DELETE | `/api/shipments/{id}` | Eliminar |

**Crear Envío:**
```http
POST /api/shipments
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{
  "orderId": 1,
  "shipmentDate": "2026-03-04T10:00:00",
  "trackingCode": "TRACK123456",
  "carrier": "Correo Argentino"
}
```

### 🔄 Sincronización

Endpoints para sincronizar datos estáticos (catálogos, estados, etc.)

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/sync/version` | Obtener versión actual |
| GET | `/api/sync/catalog` | Obtener catálogo completo |
| GET | `/api/sync/states` | Obtener estados |
| POST | `/api/sync/validate` | Validar si necesita sync |

**Validar Sincronización:**
```http
POST /api/sync/validate
Content-Type: application/json
```

```json
{
  "clientVersion": 5
}
```

**Response:**
```json
{
  "needsSync": true,
  "currentVersion": 6,
  "description": "Actualización de catálogo de productos"
}
```

---

## Modelos de Datos

### Client

```typescript
interface Client {
  idUser: string;
  name: string;
  defaultPhone?: string;
  defaultCity?: string;
  defaultAddress?: string;
  createdAt: string; // ISO 8601
  updatedAt: string; // ISO 8601
}
```

### Product

```typescript
interface Product {
  id: number;
  name: string;
  pricePerUnit: number;
  stock: number;
  imageUrl: string;
  description: string;
  category?: {
    id: number;
    name: string;
  };
  createdAt: string;
  updatedAt: string;
}
```

### Order

```typescript
interface Order {
  id: number;
  client: Client;
  operationState: State;
  orderPrice: number;
  orderAddress: string;
  orderPhone: string;
  orderCity: string;
  createdAt: string;
  updatedAt: string;
}
```

### Shipment

```typescript
interface Shipment {
  id: number;
  order: Order;
  shipmentState: ShipmentState;
  shipmentDate: string;
  trackingCode?: string;
  carrier?: string;
  createdAt: string;
  updatedAt: string;
}
```

### State

```typescript
interface State {
  id: number;
  name: string;
  type: string; // "OPERATION" o "SHIPMENT"
}
```

---

## Sincronización

### Flujo recomendado

1. **Al iniciar la app**, consultar `/api/sync/version`
2. Comparar `clientVersion` local con `currentVersion` del servidor
3. Si `needsSync === true`, descargar datos nuevos:
   - `/api/sync/catalog` → Productos y categorías
   - `/api/sync/states` → Estados de operaciones y envíos

### Catálogo

```json
{
  "categories": [
    { "id": 1, "name": "Moldes" }
  ],
  "products": [
    {
      "id": 1001,
      "name": "Molde Tipo A",
      "pricePerUnit": 1500,
      "stock": 50,
      "imageUrl": "https://...",
      "description": "...",
      "categoryId": 1
    }
  ]
}
```

### Estados

```json
{
  "operationStates": [
    { "id": 1, "name": "Pendiente" }
  ],
  "shipmentStates": [
    { "id": 1, "name": "En preparación" }
  ]
}
```

---

## Manejo de Errores

### Códigos HTTP

| Código | Significado |
|--------|-------------|
| 200 | Éxito |
| 201 | Creado exitosamente |
| 204 | Eliminado exitosamente |
| 400 | Datos inválidos |
| 401 | No autorizado (token expirado/inválido) |
| 403 | Prohibido (permisos insuficientes) |
| 404 | No encontrado |
| 500 | Error interno del servidor |

### Response de Error

```json
{
  "timestamp": "2026-03-04T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "details": [
    {
      "field": "name",
      "message": "Name is required"
    }
  ]
}
```

---

## Ejemplos de Código

### Kotlin (Android)

```kotlin
// Retrofit setup
val retrofit = Retrofit.Builder()
    .baseUrl("https://api.linogo.com")
    .addConverterFactory(GsonConverterFactory.create())
    .build()

val apiService = retrofit.create(ApiService::class.java)

// Interceptor para JWT
class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = PreferencesManager.getAccessToken()
        val request = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer $token")
            .build()
        return chain.proceed(request)
    }
}

// ApiService
interface ApiService {
    @POST("/api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>
    
    @GET("/api/products")
    suspend fun getProducts(): Response<List<Product>>
    
    @POST("/api/orders")
    suspend fun createOrder(@Body request: CreateOrderRequest): Response<Order>
}

// Manejo de token expirado
suspend fun refreshTokenAndRetry(): AuthResponse {
    val refreshToken = PreferencesManager.getRefreshToken()
    val response = apiService.refreshToken(RefreshTokenRequest(refreshToken))
    if (response.isSuccessful) {
        PreferencesManager.saveTokens(response.body()!!)
        return response.body()!!
    }
    throw AuthenticationException("Refresh token expired")
}
```

### Swift (iOS)

```swift
// URLSession setup
class APIClient {
    static let shared = APIClient()
    
    private let session: URLSession
    private let baseURL = URL(string: "https://api.linogo.com")!
    
    init() {
        let config = URLSessionConfiguration.default
        config.httpAdditionalHeaders = [
            "Content-Type": "application/json"
        ]
        session = URLSession(configuration: config)
    }
    
    func addAuthHeader(to request: inout URLRequest) {
        if let token = Keychain.getAccessToken() {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
    }
}

// Modelos
struct LoginRequest: Codable {
    let username: String
    let password: String
}

struct AuthResponse: Codable {
    let accessToken: String
    let refreshToken: String
    let tokenType: String
    let expiresIn: Int
    let user: User
}

// Uso
func login(username: String, password: String) async throws -> AuthResponse {
    let request = LoginRequest(username: username, password: password)
    return try await APIClient.shared.post("/api/auth/login", body: request)
}

func getProducts() async throws -> [Product] {
    return try await APIClient.shared.get("/api/products")
}
```

### Dart (Flutter)

```dart
// Dio setup
class ApiClient {
  final Dio _dio = Dio();
  
  ApiClient() {
    _dio.options.baseUrl = 'https://api.linogo.com';
    _dio.interceptors.add(InterceptorsWrapper(
      onRequest: (options, handler) {
        final token = SecureStorage.getAccessToken();
        if (token != null) {
          options.headers['Authorization'] = 'Bearer $token';
        }
        return handler.next(options);
      },
      onError: (error, handler) async {
        if (error.response?.statusCode == 401) {
          await _refreshToken();
          return handler.resolve(await _retry(error.requestOptions));
        }
        return handler.next(error);
      },
    ));
  }
  
  Future<Response<T>> get<T>(String path) => _dio.get(path);
  Future<Response<T>> post<T>(String path, dynamic data) => _dio.post(path, data: data);
}

// Uso
final api = ApiClient();

// Login
final loginResponse = await api.post('/api/auth/login', {
  'username': 'juanperez',
  'password': 'contraseña123',
});

// Obtener productos
final productsResponse = await api.get('/api/products');
```

---

## 📞 Soporte

Para consultas o reportar bugs, contactar al equipo de backend.

**Swagger UI:** `http://localhost:8080/swagger-ui.html`

**OpenAPI Spec:** `http://localhost:8080/api-docs`

---

*Última actualización: Marzo 2026*
