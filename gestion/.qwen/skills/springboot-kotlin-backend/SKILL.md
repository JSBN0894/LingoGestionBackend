---
name: springboot-kotlin-backend
description: Desarrollo backend con Spring Boot 3.x y Kotlin aplicando Clean Architecture, DDD y mejores prácticas. Usar cuando el usuario necesite crear endpoints, servicios, repositorios, configurar seguridad JWT, implementar APIs REST, escribir tests, o trabajar con JPA/Hibernate.
---

# Spring Boot + Kotlin Backend Development

Skill para desarrollo backend profesional con Spring Boot 3.x y Kotlin. Esta skill aplica Clean Architecture, Domain-Driven Design (DDD), y las mejores prácticas de la industria para crear código mantenible, seguro y escalable.

---

## Principios de Diseño

### 1. Clean Architecture

El código se organiza en capas concéntricas:

```
domain (entity) → application (use case) → infrastructure (adapter) → interface (controller)
```

- **Domain**: Entidades y lógica de negocio pura (sin dependencias de frameworks)
- **Application**: Casos de uso, servicios, DTOs
- **Infrastructure**: Implementaciones de repositorios, configuración externa
- **Interface**: Controllers, endpoints REST

### 2. SOLID

- **Single Responsibility**: Cada clase tiene una única responsabilidad
- **Open/Closed**: Abierto para extensión, cerrado para modificación
- **Liskov Substitution**: Las subclases deben ser sustituibles por sus padres
- **Interface Segregation**: Interfaces pequeñas y específicas
- **Dependency Inversion**: Depender de abstracciones, no de implementaciones

### 3. Convenciones de Kotlin

- Usar **data classes** para DTOs y responses
- **Null safety** explícito (`String?` vs `String`)
- **Inmutabilidad** por defecto (`val` sobre `var`)
- **Extension functions** para utilidades
- **Coroutines** para operaciones async (cuando aplique)

---

## Estructura de Paquetes

```
com.example.project/
├── module/
│   ├── application/          # Casos de uso, servicios, DTOs
│   │   ├── CreateXRequest.kt
│   │   ├── XResponse.kt
│   │   └── XService.kt
│   ├── domain/               # Entidades de negocio
│   │   └── X.kt
│   └── infrastructure/       # Implementaciones
│       ├── XController.kt
│       └── XRepository.kt
├── security/
│   ├── config/
│   ├── domain/
│   ├── infrastructure/
│   └── service/
└── shared/
    ├── exception/
    └── util/
```

---

## Patrones por Capa

### Controller

```kotlin
@RestController
@RequestMapping("/api/resource")
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Recurso", description = "Gestión de recursos")
class ResourceController(
    private val resourceService: ResourceService
) {

    @PostMapping
    @Operation(summary = "Crear recurso", description = "Crea un nuevo recurso")
    @ApiResponse(responseCode = "201", description = "Recurso creado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    fun create(@Valid @RequestBody request: CreateResourceRequest): ResponseEntity<ResourceResponse> {
        val response = resourceService.create(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): ResponseEntity<ResourceResponse> {
        val response = resourceService.findById(id)
        return ResponseEntity.ok(response)
    }
}
```

**Reglas:**
- ✅ Controllers son delgados (solo delegan al service)
- ✅ Usar `@Valid` para validación de inputs
- ✅ Retornar `ResponseEntity<T>` explícito
- ✅ Documentar con OpenAPI (`@Operation`, `@ApiResponse`)
- ✅ `@SecurityRequirement` para endpoints protegidos

### Service

```kotlin
@Service
class ResourceService(
    private val resourceRepository: ResourceRepository,
    private val mapper: ResourceMapper
) {

    @Transactional
    fun create(request: CreateResourceRequest): ResourceResponse {
        // 1. Validar reglas de negocio
        if (resourceRepository.existsByName(request.name)) {
            throw IllegalArgumentException("El nombre ya existe")
        }

        // 2. Mapear request a entidad
        val entity = mapper.toEntity(request)

        // 3. Persistir
        val saved = resourceRepository.save(entity)

        // 4. Mapear a response
        return mapper.toResponse(saved)
    }

    @Transactional(readOnly = true)
    fun findById(id: Long): ResourceResponse {
        val entity = resourceRepository.findById(id)
            ?: throw ResourceNotFoundException("Recurso no encontrado: $id")
        return mapper.toResponse(entity)
    }
}
```

**Reglas:**
- ✅ Servicios contienen lógica de negocio
- ✅ `@Transactional` para operaciones de escritura
- ✅ `@Transactional(readOnly = true)` para lecturas
- ✅ Lanzar excepciones específicas para errores de negocio

### Repository

```kotlin
@Repository
interface ResourceRepository : JpaRepository<Resource, Long> {

    fun findByName(name: String): Resource?

    fun existsByName(name: String): Boolean

    @Query("SELECT r FROM Resource r WHERE r.status = :status ORDER BY r.createdAt DESC")
    fun findByStatusOrderByCreatedAtDesc(status: Status): List<Resource>
}
```

**Reglas:**
- ✅ Interfaces simples (Spring Data JPA genera implementación)
- ✅ Métodos custom con `@Query` para queries complejas
- ✅ Nombres descriptivos siguiendo convención Spring Data

### Entity (Domain)

```kotlin
@Entity
@Table(name = "resources")
class Resource(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, unique = true, length = 100)
    val name: String,

    @Column(nullable = true)
    val description: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: Status = Status.ACTIVE,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
) {
    // Métodos de dominio si aplica
    fun activate() {
        // lógica de negocio
    }
}

enum class Status {
    ACTIVE, INACTIVE, ARCHIVED
}
```

**Reglas:**
- ✅ Entidades son ricas en comportamiento (no solo datos)
- ✅ Inmutabilidad donde sea posible
- ✅ Validaciones de dominio en métodos de la entidad

### DTOs (Data Transfer Objects)

```kotlin
// Request DTO
data class CreateResourceRequest(
    @field:NotBlank(message = "El nombre es requerido")
    @field:Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres")
    val name: String,

    @field:Size(max = 500, message = "La descripción no puede exceder 500 caracteres")
    val description: String? = null
)

// Response DTO
data class ResourceResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val status: String,
    val createdAt: LocalDateTime
)

// Mapper
@Component
class ResourceMapper {
    fun toEntity(request: CreateResourceRequest): Resource {
        return Resource(
            name = request.name,
            description = request.description
        )
    }

    fun toResponse(entity: Resource): ResourceResponse {
        return ResourceResponse(
            id = entity.id!!,
            name = entity.name,
            description = entity.description,
            status = entity.status.name,
            createdAt = entity.createdAt
        )
    }
}
```

**Reglas:**
- ✅ DTOs separados para request y response
- ✅ Validaciones con Jakarta Validation en requests
- ✅ Mappers explícitos (no usar reflection en producción)

---

## Seguridad

### Configuración Base (SecurityConfig.kt)

```kotlin
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
class SecurityConfig {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth.requestMatchers("/api/auth/**").permitAll()
                auth.anyRequest().authenticated()
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder(12)
}
```

### JWT Authentication Flow

1. **Registro**: `POST /api/auth/register` → User + tokens
2. **Login**: `POST /api/auth/login` → Access + Refresh tokens
3. **Refresh**: `POST /api/auth/refresh` → Nuevos tokens
4. **Logout**: `POST /api/auth/logout` → Invalidar tokens

### Endpoints Protegidos

```kotlin
@PostMapping("/protected")
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasRole('ADMIN')")
fun protectedEndpoint(@AuthenticationPrincipal userDetails: UserDetails) {
    // Usuario autenticado y autorizado
}
```

---

## Manejo de Errores

### Global Exception Handler

```kotlin
@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleNotFound(ex: ResourceNotFoundException): ResponseEntity<ErrorResponse> {
        return ResponseEntity(
            ErrorResponse(
                timestamp = LocalDateTime.now(),
                status = HttpStatus.NOT_FOUND.value(),
                error = "Not Found",
                message = ex.message
            ),
            HttpStatus.NOT_FOUND
        )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val errors = ex.bindingResult.fieldErrors.map {
            ValidationError(it.field, it.defaultMessage ?: "Error de validación")
        }

        return ResponseEntity(
            ErrorResponse(
                timestamp = LocalDateTime.now(),
                status = HttpStatus.BAD_REQUEST.value(),
                error = "Bad Request",
                message = "Error de validación",
                details = errors
            ),
            HttpStatus.BAD_REQUEST
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneric(ex: Exception): ResponseEntity<ErrorResponse> {
        // Log error
        return ResponseEntity(
            ErrorResponse(
                timestamp = LocalDateTime.now(),
                status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
                error = "Internal Server Error",
                message = "Error interno del servidor"
            ),
            HttpStatus.INTERNAL_SERVER_ERROR
        )
    }
}

data class ErrorResponse(
    val timestamp: LocalDateTime,
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

## Testing

### Unit Tests (Service)

```kotlin
@ExtendWith(MockitoExtension::class)
class ResourceServiceTest {

    @Mock
    private lateinit var resourceRepository: ResourceRepository

    @InjectMocks
    private lateinit var resourceService: ResourceService

    @Test
    fun `create should save resource when name is unique`() {
        // Arrange
        val request = CreateResourceRequest("Test Resource", "Description")
        given(resourceRepository.existsByName(request.name)).willReturn(false)
        given(resourceRepository.save(any())).willReturn(Resource(id = 1L, name = request.name))

        // Act
        val response = resourceService.create(request)

        // Assert
        assertThat(response.id).isEqualTo(1L)
        assertThat(response.name).isEqualTo("Test Resource")
    }

    @Test
    fun `create should throw exception when name exists`() {
        // Arrange
        val request = CreateResourceRequest("Existing", null)
        given(resourceRepository.existsByName(request.name)).willReturn(true)

        // Act & Assert
        assertThatThrownBy { resourceService.create(request) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("El nombre ya existe")
    }
}
```

### Integration Tests (Controller)

```kotlin
@SpringBootTest
@AutoConfigureMockMvc
class ResourceControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `create should return created resource`() {
        val request = CreateResourceRequest("Test", "Description")

        mockMvc.perform(post("/api/resources")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("Test"))
    }
}
```

**Reglas:**
- ✅ Tests unitarios para servicios (mockear repositorios)
- ✅ Tests de integración para controllers
- ✅ Nombres descriptivos con backticks
- ✅ Patrón Arrange-Act-Assert

---

## Base de Datos

### JPA Best Practices

```kotlin
// ✅ Correcto: Lazy loading por defecto
@OneToMany(mappedBy = "order", fetch = FetchType.LAZY)
val products: List<OrderProduct> = emptyList()

// ✅ Correcto: Cascade solo cuando aplica
@OneToOne(cascade = [CascadeType.PERSIST, CascadeType.MERGE])
val address: Address? = null

// ✅ Correcto: Índices para queries frecuentes
@Table(
    name = "users",
    indexes = [
        Index(name = "idx_users_email", columnList = "email"),
        Index(name = "idx_users_username", columnList = "username")
    ]
)
```

### Migraciones (Flyway)

```sql
-- V1__create_users_table.sql
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    is_enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);
```

---

## Performance

### Caching

```kotlin
@Service
class ProductService(
    private val productRepository: ProductRepository
) {

    @Cacheable(value = ["products"], key = "#id")
    fun findById(id: Long): ProductResponse {
        return productRepository.findById(id)
            ?.let { mapper.toResponse(it) }
            ?: throw ProductNotFoundException("Producto no encontrado: $id")
    }

    @CacheEvict(value = ["products"], key = "#id")
    fun update(id: Long, request: UpdateProductRequest) {
        // Actualizar producto
    }
}
```

### Pagination

```kotlin
@GetMapping
fun getAll(
    @RequestParam(defaultValue = "0") page: Int,
    @RequestParam(defaultValue = "20") size: Int,
    @RequestParam(defaultValue = "createdAt,desc") sort: String
): ResponseEntity<Page<ProductResponse>> {
    val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
    return ResponseEntity.ok(service.findAll(pageable))
}
```

### Rate Limiting

```kotlin
@Service
class AuthService {
    private val loginAttempts: MutableMap<String, Bucket> = ConcurrentHashMap()

    private fun checkRateLimit(clientIp: String) {
        val bucket = loginAttempts.computeIfAbsent(clientIp) { createBucket() }
        if (!bucket.tryConsume(1)) {
            throw RateLimitExceededException("Demasiados intentos. Intente en 1 minuto.")
        }
    }
}
```

---

## Checklists

### Antes de Commitear

- [ ] Tests unitarios pasan
- [ ] Tests de integración pasan
- [ ] No hay código duplicado
- [ ] Nombres de variables/clases son descriptivos
- [ ] Manejo de errores apropiado
- [ ] Logs relevantes agregados
- [ ] Documentación OpenAPI actualizada
- [ ] Validaciones de input implementadas

### Security Checklist

- [ ] Autenticación requerida para endpoints protegidos
- [ ] Autorización verificada (roles/permisos)
- [ ] Inputs validados y sanitizados
- [ ] Passwords hasheados (BCrypt, argon2)
- [ ] Tokens JWT con expiración
- [ ] CORS configurado correctamente
- [ ] Rate limiting en endpoints críticos
- [ ] No hay información sensible en logs

---

## Recursos

- [references/clean-architecture.md](references/clean-architecture.md) - Guía detallada de Clean Architecture
- [references/kotlin-conventions.md](references/kotlin-conventions.md) - Convenciones y estilo Kotlin
- [references/spring-security.md](references/spring-security.md) - Configuración de seguridad
- [templates/entity-template.kt](templates/entity-template.kt) - Plantilla de entidades
- [templates/test-template.kt](templates/test-template.kt) - Plantilla de tests
