# Guía de Integración Android - Linogo Backend API

## Linogo Backend API

Documentación técnica para integrar la aplicación **Android** con el backend de gestión de Linogo.

**Versión de la API:** `v1`  
**Deploy:** Railway  
**Última actualización:** Marzo 2026

---

## 📋 Índice

1. [Configuración Inicial](#configuración-inicial)
2. [Dependencias](#dependencias)
3. [Autenticación](#autenticación)
4. [Endpoints](#endpoints)
5. [Modelos de Datos](#modelos-de-datos)
6. [Manejo de Errores](#manejo-de-errores)
7. [Ejemplo Completo Android](#ejemplo-completo-android)

---

## Configuración Inicial

### Base URL

| Entorno | URL |
|---------|-----|
| Desarrollo (local) | `http://10.0.2.2:8080` |
| Producción (Railway) | `BASE_URL` |

> **Nota:** Para Android Emulator, usa `10.0.2.2` en lugar de `localhost`.
> 
> **⚠️ Seguridad:** `BASE_URL` es una variable de configuración. Consulta la sección [Configuración](#configuración) para más detalles.

## Configuración Inicial

### Configuración de la Base URL

Por seguridad, la URL de producción no está hardcodeada en el repositorio.

**Opción 1: BuildConfig (Recomendada)**

En tu `build.gradle.kts` (app level):

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

Uso en el código:

```kotlin
object RetrofitClient {
    private val BASE_URL = BuildConfig.BASE_URL
}
```

**Opción 2: Gradle Properties (local.properties)**

En `local.properties` (no commitear a Git):

```properties
api.base.url.debug=http://10.0.2.2:8080
api.base.url.release=https://tu-app.railway.app
```

En `build.gradle.kts`:

```kotlin
val apiBaseUrlDebug = project.findProperty("api.base.url.debug") as? String ?: "http://10.0.2.2:8080"
val apiBaseUrlRelease = project.findProperty("api.base.url.release") as? String ?: "https://tu-app.railway.app"

android {
    defaultConfig {
        buildConfigField("String", "BASE_URL", "\"$apiBaseUrlDebug\"")
    }
    buildTypes {
        debug {
            buildConfigField("String", "BASE_URL", "\"$apiBaseUrlDebug\"")
        }
        release {
            buildConfigField("String", "BASE_URL", "\"$apiBaseUrlRelease\"")
        }
    }
}
```

**Opción 3: Secrets en CI/CD**

Si usas GitHub Actions o similar, inyecta la variable en tiempo de build:

```yaml
- name: Set BASE_URL
  run: |
    echo "api.base.url.release=${{ secrets.API_BASE_URL }}" >> local.properties
```

### Permisos de Internet

Agrega en tu `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

### Network Security Config

Crea `res/xml/network_security_config.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <!-- Solo para desarrollo -->
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">10.0.2.2</domain>
        <domain includeSubdomains="true">localhost</domain>
    </domain-config>
    
    <!-- Producción -->
    <domain-config>
        <domain includeSubdomains="true">railway.app</domain>
    </domain-config>
</network-security-config>
```

Referencia en el `AndroidManifest.xml`:

```xml
<application
    android:networkSecurityConfig="@xml/network_security_config"
    ... >
</application>
```

---

## Dependencias

Agrega en tu `build.gradle` (app level):

```kotlin
dependencies {
    // Retrofit para HTTP
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    
    // OkHttp para interceptors
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    
    // DataStore para guardar tokens
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    
    // Lifecycle (ViewModel, LiveData)
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.7")
}
```

---

## Autenticación

### JWT Token Flow

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Login     │────▶│  Access     │────▶│  Peticiones │
│             │     │  Token (15m)│     │  a la API   │
└─────────────┘     └─────────────┘     └─────────────┘
                           │
                           ▼
                    ┌─────────────┐
                    │  Refresh    │
                    │  Token (7d) │
                    └─────────────┘
```

### Endpoints de Autenticación

#### 1. Registro

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
  "expiresIn": 900,
  "user": {
    "id": "uuid-123",
    "username": "juanperez",
    "email": "juan@example.com",
    "fullName": "Juan Pérez",
    "role": "USER"
  }
}
```

#### 2. Login

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

#### 3. Refresh Token

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

#### 4. Logout

```http
POST /api/auth/logout
Authorization: Bearer <access_token>
```

---

## Endpoints

### 👥 Clientes

| Método | Endpoint | Auth | Descripción |
|--------|----------|------|-------------|
| POST | `/api/clients` | ✅ | Crear cliente |
| GET | `/api/clients` | ✅ | Listar todos |
| GET | `/api/clients/{idUser}` | ✅ | Obtener por ID |
| PUT | `/api/clients/{idUser}` | ✅ | Actualizar |
| DELETE | `/api/clients/{idUser}` | ✅ | Eliminar |

**Crear Cliente:**
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

| Método | Endpoint | Auth | Descripción |
|--------|----------|------|-------------|
| POST | `/api/products` | ✅ | Crear producto |
| GET | `/api/products` | ✅ | Listar todos |
| GET | `/api/products/{id}` | ✅ | Obtener por ID |
| GET | `/api/products/category/{categoryId}` | ✅ | Filtrar por categoría |
| PUT | `/api/products/{id}` | ✅ | Actualizar |
| DELETE | `/api/products/{id}` | ✅ | Eliminar |

**Crear Producto:**
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

| Método | Endpoint | Auth | Descripción |
|--------|----------|------|-------------|
| POST | `/api/orders` | ✅ | Crear orden |
| GET | `/api/orders` | ✅ | Listar todas |
| GET | `/api/orders/{id}` | ✅ | Obtener por ID |
| PUT | `/api/orders/{id}` | ✅ | Actualizar |
| DELETE | `/api/orders/{id}` | ✅ | Eliminar |

**Crear Orden:**
```json
{
  "clientId": "user_123",
  "operationStateId": 1,
  "orderPrice": 4500,
  "orderAddress": "Av. Corrientes 1234",
  "orderPhone": "+54911223344",
  "orderCity": "Buenos Aires"
}
```

### 🚚 Envíos (Shipments)

| Método | Endpoint | Auth | Descripción |
|--------|----------|------|-------------|
| POST | `/api/shipments` | ✅ | Crear envío |
| GET | `/api/shipments` | ✅ | Listar todos |
| GET | `/api/shipments/{id}` | ✅ | Obtener por ID |
| PUT | `/api/shipments/{id}` | ✅ | Actualizar |
| DELETE | `/api/shipments/{id}` | ✅ | Eliminar |

### 🔄 Sincronización (Público)

| Método | Endpoint | Auth | Descripción |
|--------|----------|------|-------------|
| GET | `/api/sync/version` | ❌ | Obtener versión actual |
| GET | `/api/sync/catalog` | ❌ | Obtener catálogo completo |
| GET | `/api/sync/states` | ❌ | Obtener estados |
| POST | `/api/sync/validate` | ❌ | Validar si necesita sync |

---

## Modelos de Datos (Kotlin)

### Auth Models

```kotlin
data class LoginRequest(
    val username: String,
    val password: String
)

data class RegisterRequest(
    val fullName: String,
    val username: String,
    val email: String,
    val password: String
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

### Client Models

```kotlin
data class CreateClientRequest(
    @field:NotBlank(message = "Name is required")
    val name: String,
    
    @field:NotBlank(message = "ID User is required")
    val idUser: String,
    
    val defaultPhone: String? = null,
    val defaultCity: String? = null,
    val defaultAddress: String? = null
)

data class ClientResponse(
    val idUser: String,
    val name: String,
    val defaultPhone: String?,
    val defaultCity: String?,
    val defaultAddress: String?,
    val createdAt: String,
    val updatedAt: String
)
```

### Product Models

```kotlin
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

data class CategoryResponse(
    val id: Long,
    val name: String
)
```

### Order Models

```kotlin
data class CreateOrderRequest(
    val clientId: String,
    val operationStateId: Long,
    val orderPrice: Long,
    val orderAddress: String,
    val orderPhone: String,
    val orderCity: String
)

data class OrderResponse(
    val id: Long,
    val clientId: String,
    val clientName: String,
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

### Error Response

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

## Manejo de Errores

### Códigos HTTP

| Código | Significado | Acción Recomendada |
|--------|-------------|-------------------|
| 200 | Éxito | Continuar normal |
| 201 | Creado | Navegar a detalle o limpiar formulario |
| 204 | Eliminado | Actualizar UI |
| 400 | Bad Request | Mostrar errores de validación |
| 401 | Unauthorized | Refrescar token o pedir login |
| 403 | Forbidden | Mostrar mensaje de permisos |
| 404 | Not Found | Mostrar "No encontrado" |
| 409 | Conflict | Mostrar "Ya existe" |
| 500 | Internal Server Error | Reintentar más tarde |

### Response de Error

```json
{
  "timestamp": "2026-03-09T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Error de validación en los datos enviados",
  "details": [
    {
      "field": "name",
      "message": "Name is required"
    },
    {
      "field": "idUser",
      "message": "ID User is required"
    }
  ]
}
```

---

## Ejemplo Completo Android

### 1. API Service Interface

```kotlin
interface ApiService {
    
    // Auth
    @POST("/api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>
    
    @POST("/api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>
    
    @POST("/api/auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<AuthResponse>
    
    @POST("/api/auth/logout")
    @Headers("Content-Type: application/json")
    suspend fun logout(@Header("Authorization") token: String): Response<Unit>
    
    // Clients
    @POST("/api/clients")
    suspend fun createClient(
        @Header("Authorization") token: String,
        @Body request: CreateClientRequest
    ): Response<ClientResponse>
    
    @GET("/api/clients")
    suspend fun getClients(
        @Header("Authorization") token: String
    ): Response<List<ClientResponse>>
    
    @GET("/api/clients/{idUser}")
    suspend fun getClient(
        @Header("Authorization") token: String,
        @Path("idUser") idUser: String
    ): Response<ClientResponse>
    
    @PUT("/api/clients/{idUser}")
    suspend fun updateClient(
        @Header("Authorization") token: String,
        @Path("idUser") idUser: String,
        @Body request: UpdateClientRequest
    ): Response<ClientResponse>
    
    @DELETE("/api/clients/{idUser}")
    suspend fun deleteClient(
        @Header("Authorization") token: String,
        @Path("idUser") idUser: String
    ): Response<Unit>
    
    // Products
    @GET("/api/products")
    suspend fun getProducts(
        @Header("Authorization") token: String
    ): Response<List<ProductResponse>>
    
    @GET("/api/products/category/{categoryId}")
    suspend fun getProductsByCategory(
        @Header("Authorization") token: String,
        @Path("categoryId") categoryId: Long
    ): Response<List<ProductResponse>>
    
    // Sync (sin auth)
    @GET("/api/sync/version")
    suspend fun getSyncVersion(): Response<SyncVersionResponse>
    
    @GET("/api/sync/catalog")
    suspend fun getCatalog(): Response<CatalogResponse>
}
```

### 2. Retrofit Instance

```kotlin
object RetrofitClient {
    
    private val BASE_URL = BuildConfig.BASE_URL
    
    private val authInterceptor = AuthInterceptor()
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }
    
    private val client = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
```

### 3. Auth Interceptor

```kotlin
class AuthInterceptor : Interceptor {
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val context = MyApplication.context
        val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        val token = prefs.getString("access_token", null)
        
        val request = chain.request().newBuilder()
        
        token?.let {
            request.addHeader("Authorization", "Bearer $it")
        }
        
        return chain.proceed(request.build())
    }
}
```

### 4. Token Manager

```kotlin
object TokenManager {
    
    private lateinit var context: Context
    
    fun init(appContext: Context) {
        context = appContext.applicationContext
    }
    
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    }
    
    fun saveTokens(authResponse: AuthResponse) {
        prefs.edit().apply {
            putString("access_token", authResponse.accessToken)
            putString("refresh_token", authResponse.refreshToken)
            putLong("expires_at", System.currentTimeMillis() + authResponse.expiresIn * 1000)
            apply()
        }
    }
    
    fun getAccessToken(): String? = prefs.getString("access_token", null)
    
    fun getRefreshToken(): String? = prefs.getString("refresh_token", null)
    
    fun isTokenExpired(): Boolean {
        val expiresAt = prefs.getLong("expires_at", 0)
        return System.currentTimeMillis() >= expiresAt
    }
    
    fun clearTokens() {
        prefs.edit().clear().apply()
    }
    
    suspend fun refreshTokenOrLogout(apiService: ApiService): Boolean {
        val refreshToken = getRefreshToken() ?: return false
        
        return try {
            val response = apiService.refreshToken(RefreshTokenRequest(refreshToken))
            if (response.isSuccessful) {
                response.body()?.let { saveTokens(it) }
                true
            } else {
                clearTokens()
                false
            }
        } catch (e: Exception) {
            clearTokens()
            false
        }
    }
}
```

### 5. Repository Pattern

```kotlin
class ClientRepository {
    
    private val apiService = RetrofitClient.apiService
    
    suspend fun getClients(): Result<List<ClientResponse>> {
        return try {
            val response = apiService.getClients("Bearer ${TokenManager.getAccessToken()}")
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                if (response.code() == 401) {
                    // Intentar refresh token
                    if (TokenManager.refreshTokenOrLogout(apiService)) {
                        // Reintentar
                        val retryResponse = apiService.getClients("Bearer ${TokenManager.getAccessToken()}")
                        if (retryResponse.isSuccessful) {
                            Result.success(retryResponse.body() ?: emptyList())
                        } else {
                            Result.failure(ApiException("Error al obtener clientes"))
                        }
                    } else {
                        Result.failure(ApiException("Sesión expirada"))
                    }
                } else {
                    Result.failure(ApiException("Error: ${response.message()}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun createClient(request: CreateClientRequest): Result<ClientResponse> {
        return try {
            val response = apiService.createClient(
                "Bearer ${TokenManager.getAccessToken()}",
                request
            )
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(ApiException("Error al crear cliente"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

### 6. ViewModel

```kotlin
@HiltViewModel
class ClientViewModel @Inject constructor(
    private val repository: ClientRepository
) : ViewModel() {
    
    private val _clients = MutableLiveData<List<ClientResponse>>()
    val clients: LiveData<List<ClientResponse>> = _clients
    
    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading
    
    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error
    
    fun loadClients() {
        viewModelScope.launch {
            _loading.value = true
            repository.getClients()
                .onSuccess { _clients.value = it }
                .onFailure { _error.value = it.message }
            _loading.value = false
        }
    }
    
    fun createClient(request: CreateClientRequest) {
        viewModelScope.launch {
            _loading.value = true
            repository.createClient(request)
                .onSuccess { 
                    loadClients() // Recargar lista
                }
                .onFailure { _error.value = it.message }
            _loading.value = false
        }
    }
}
```

### 7. Activity/Fragment

```kotlin
@AndroidEntryPoint
class ClientsFragment : Fragment() {
    
    private val viewModel: ClientViewModel by viewModels()
    private lateinit var adapter: ClientAdapter
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        observeViewModel()
        
        viewModel.loadClients()
    }
    
    private fun observeViewModel() {
        viewModel.clients.observe(viewLifecycleOwner) { clients ->
            adapter.submitList(clients)
        }
        
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            progressBar.isVisible = isLoading
        }
        
        viewModel.error.observe(viewLifecycleOwner) { error ->
            Snackbar.make(requireView(), error, Snackbar.LENGTH_LONG).show()
        }
    }
}
```

---

## 📞 Soporte

**Swagger UI:** `BASE_URL/swagger-ui.html`  
**OpenAPI Spec:** `BASE_URL/api-docs`

Para consultas o reportar bugs, contactar al equipo de backend.

---

*Última actualización: Marzo 2026*
