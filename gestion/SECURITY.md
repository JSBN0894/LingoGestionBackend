# 🔐 Seguridad Empresarial - Backend Linogo

## 📋 Índice

1. [Resumen de Seguridad Implementada](#resumen)
2. [Arquitectura de Seguridad](#arquitectura)
3. [Configuración](#configuración)
4. [Endpoints de Autenticación](#endpoints)
5. [Control de Acceso (RBAC)](#rbac)
6. [Protecciones Implementadas](#protecciones)
7. [Guía para Android](#android)
8. [Producción](#producción)

---

## 🔒 Resumen de Seguridad Implementada

| Característica | Estado | Descripción |
|----------------|--------|-------------|
| **JWT Auth** | ✅ | Access token (15 min) + Refresh token (7 días) |
| **BCrypt** | ✅ | Hash de contraseñas con costo 12 |
| **SecurityFilterChain** | ✅ | Configuración moderna (Spring Security 6+) |
| **RBAC** | ✅ | Roles: ADMIN, SELLER, USER |
| **Rate Limiting** | ✅ | 5 intentos/minuto por IP en login |
| **CORS** | ✅ | Configurado para Android y web |
| **CSRF** | ✅ | Deshabilitado (API stateless con JWT) |
| **SQL Injection** | ✅ | Prevenida con JPA/Hibernate |
| **Validaciones** | ✅ | Jakarta Bean Validation |
| **ExceptionHandler** | ✅ | Respuestas de error consistentes |

---

## 🏗️ Arquitectura

```
src/main/kotlin/com/linogo/gestion/security/
├── config/
│   ├── SecurityConfig.kt          # SecurityFilterChain, CORS, BCrypt
│   ├── SecurityAnnotations.kt     # @AdminOnly, @Authenticated
│   └── SecurityChecks.kt          # Verificaciones SpEL
├── domain/
│   ├── User.kt                    # Entidad + UserDetails
│   └── RefreshToken.kt            # Tokens de refresco
├── application/
│   └── AuthDTOs.kt                # DTOs con validaciones
├── infrastructure/
│   ├── UserRepository.kt          # Acceso a datos
│   ├── RefreshTokenRepository.kt
│   ├── JwtTokenProvider.kt        # Generación/validación JWT
│   ├── JwtAuthenticationFilter.kt # Filtro de tokens
│   ├── AuthController.kt          # Endpoints públicos
│   ├── AdminController.kt         # Ejemplo endpoints protegidos
│   └── GlobalExceptionHandler.kt  # Manejo de errores
└── service/
    ├── AuthService.kt             # Lógica de auth + rate limiting
    └── CustomUserDetailsService.kt # Carga de usuarios
```

---

## ⚙️ Configuración

### Variables de Entorno (Desarrollo)

```properties
# application-dev.properties
app.jwt.secret=DevSecretKeyForDevelopmentOnly...
app.jwt.access-token-expiration=900000      # 15 minutos
app.jwt.refresh-token-expiration=604800000  # 7 días
```

### Variables de Entorno (Producción)

```bash
# Usar variables de entorno, NO hardcodear
export JWT_SECRET=$(openssl rand -base64 64)
export DATABASE_URL=jdbc:postgresql://db:5432/gestion
export DATABASE_USERNAME=linogo_user
export DATABASE_PASSWORD=<secure-password>
export SSL_KEY_STORE_PATH=/certs/keystore.p12
export SSL_KEY_STORE_PASSWORD=<secure-password>
```

---

## 🔑 Endpoints de Autenticación

### 1. Login

```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}
```

**Respuesta exitosa (200):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "user": {
    "id": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
    "username": "admin",
    "email": "admin@linogo.com",
    "fullName": "Administrador",
    "role": "ADMIN"
  }
}
```

### 2. Registro

```http
POST /api/auth/register
Content-Type: application/json

{
  "fullName": "Juan Pérez",
  "username": "juanperez",
  "email": "juan@example.com",
  "password": "SecurePass123!"
}
```

### 3. Refresh Token

```http
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

### 4. Logout

```http
POST /api/auth/logout
Authorization: Bearer <access_token>
```

---

## 🎭 Control de Acceso (RBAC)

### Roles Disponibles

| Rol | Descripción | Permisos |
|-----|-------------|----------|
| `USER` | Cliente básico | Operaciones de usuario |
| `SELLER` | Vendedor | Gestión de productos + USER |
| `ADMIN` | Administrador | Acceso completo |

### Uso en Controllers

```kotlin
// Solo administradores
@GetMapping("/users")
@AdminOnly
fun getAllUsers(): ResponseEntity<List<User>> { ... }

// Admins o vendedores
@GetMapping("/stats")
@PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
fun getStats(): ResponseEntity<Map<String, Any>> { ... }

// Usuario autenticado
@GetMapping("/profile")
@Authenticated
fun getProfile(@AuthenticationPrincipal user: UserDetails): ResponseEntity<User> { ... }

// Verificación personalizada
@PreAuthorize("@securityChecks.isOwner(#userId)")
@GetMapping("/users/{userId}")
fun getUser(@PathVariable userId: String): ResponseEntity<User> { ... }
```

---

## 🛡️ Protecciones Implementadas

### CSRF
**Deshabilitado** - Las APIs REST stateless con JWT no necesitan CSRF. El token se envía en header `Authorization`, no en cookies.

### CORS
Configurado para permitir orígenes específicos:
```kotlin
allowedOrigins = listOf(
    "https://tu-dominio.com",
    "android-app://com.linogo.app"
)
```

### Rate Limiting
```
5 intentos de login por minuto por IP
```
En producción: 3 intentos por 5 minutos.

### SQL Injection
Prevenida automáticamente por JPA/Hibernate con prepared statements.

### Password Hashing
```kotlin
@Bean
fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder(12)
```

---

## 📱 Guía para Android

### Interceptor OkHttp

```kotlin
class AuthInterceptor(
    private val tokenManager: TokenManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // No añadir token a endpoints de auth
        if (originalRequest.url.encodedPath.contains("/api/auth/")) {
            return chain.proceed(originalRequest)
        }

        val accessToken = tokenManager.getAccessToken()
        val request = if (accessToken != null) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $accessToken")
                .build()
        } else {
            originalRequest
        }

        val response = chain.proceed(request)

        // Manejar 401 - Token expirado
        if (response.code == 401) {
            response.close()
            
            val newToken = tokenManager.refreshToken()
            
            if (newToken != null) {
                val newRequest = originalRequest.newBuilder()
                    .header("Authorization", "Bearer $newToken")
                    .build()
                return chain.proceed(newRequest)
            }
        }

        return response
    }
}
```

### TokenManager

```kotlin
class TokenManager @Inject constructor(
    private val authApi: AuthApi,
    private val preferences: Preferences
) {
    private val lock = Any()

    fun getAccessToken(): String? = preferences.accessToken

    fun saveTokens(accessToken: String, refreshToken: String) {
        synchronized(lock) {
            preferences.accessToken = accessToken
            preferences.refreshToken = refreshToken
        }
    }

    suspend fun refreshToken(): String? {
        return try {
            val refreshToken = preferences.refreshToken ?: return null
            val response = authApi.refreshToken(RefreshTokenRequest(refreshToken))
            
            saveTokens(response.accessToken, response.refreshToken)
            response.accessToken
        } catch (e: Exception) {
            // Refresh fallido - cerrar sesión
            clearTokens()
            null
        }
    }

    fun clearTokens() {
        preferences.clear()
    }

    fun needsLogin(): Boolean = getAccessToken() == null
}
```

### Flujo de Autenticación en Android

```kotlin
sealed class AuthState {
    object Unauthenticated : AuthState()
    object Loading : AuthState()
    object Authenticated : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        checkAuthStatus()
    }

    fun checkAuthStatus() {
        if (tokenManager.getAccessToken() != null) {
            _authState.value = AuthState.Authenticated
        } else {
            _authState.value = AuthState.Unauthenticated
        }
    }

    fun login(username: String, password: String) = viewModelScope.launch {
        _authState.value = AuthState.Loading
        
        try {
            val result = authRepository.login(username, password)
            tokenManager.saveTokens(result.accessToken, result.refreshToken)
            _authState.value = AuthState.Authenticated
        } catch (e: Exception) {
            _authState.value = AuthState.Error(e.message ?: "Error de login")
        }
    }

    fun logout() = viewModelScope.launch {
        try {
            authRepository.logout()
        } finally {
            tokenManager.clearTokens()
            _authState.value = AuthState.Unauthenticated
        }
    }
}
```

---

## 🚀 Producción

### Checklist de Seguridad

- [ ] Generar JWT_SECRET único con `openssl rand -base64 64`
- [ ] Configurar HTTPS con certificado válido
- [ ] Cambiar contraseñas de base de datos
- [ ] Especificar orígenes CORS explícitos
- [ ] Rate limiting más estricto (3 intentos/5 min)
- [ ] Logging en nivel WARN o ERROR
- [ ] Deshabilitar Swagger UI o proteger con auth
- [ ] Configurar HSTS headers
- [ ] Habilitar security headers (X-Content-Type-Options, etc.)
- [ ] Usar variables de entorno para secrets
- [ ] Configurar backup automático de tokens refresh

### Generar Certificado SSL

```bash
# Generar keystore PKCS12
keytool -genkeypair \
  -alias linogo \
  -keyalg RSA \
  -keysize 2048 \
  -storetype PKCS12 \
  -keystore keystore.p12 \
  -validity 365 \
  -dname "CN=tu-dominio.com, OU=Linogo, O=Linogo, L=Ciudad, ST=Estado, C=CO"
```

### Docker Compose para Producción

```yaml
version: '3.8'
services:
  app:
    image: linogo/gestion:latest
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DATABASE_URL=jdbc:postgresql://db:5432/gestion
      - DATABASE_USERNAME=${DB_USER}
      - DATABASE_PASSWORD=${DB_PASSWORD}
      - JWT_SECRET=${JWT_SECRET}
      - SSL_KEY_STORE_PATH=/certs/keystore.p12
      - SSL_KEY_STORE_PASSWORD=${SSL_PASSWORD}
    volumes:
      - ./certs:/certs:ro
    ports:
      - "8443:8443"
    depends_on:
      - db

  db:
    image: postgres:16-alpine
    environment:
      - POSTGRES_DB=gestion
      - POSTGRES_USER=${DB_USER}
      - POSTGRES_PASSWORD=${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data

volumes:
  postgres_data:
```

---

## 📊 Monitoreo y Auditoría

### Logs de Seguridad

```kotlin
// En AuthService
logger.warn("Intento de login fallido para usuario: ${request.username}")
logger.warn("Rate limit excedido para IP: $clientIp")
logger.info("Usuario registrado: ${savedUser.username}")
logger.info("Logout realizado para usuario: $userId")
```

### Métricas Recomendadas

- Intentos de login fallidos por hora
- Tokens refresh revocados
- IPs con rate limit activado
- Usuarios creados por día

---

## 🔗 Recursos

- [Spring Security Documentation](https://spring.io/projects/spring-security)
- [JWT.io](https://jwt.io/)
- [OWASP Security Cheat Sheets](https://cheatsheetseries.owasp.org/)
- [BCrypt Calculator](https://bcrypt-generator.com/)
