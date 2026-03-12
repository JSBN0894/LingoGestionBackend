# Convenciones de Kotlin para Spring Boot

## Null Safety

### Reglas Generales

```kotlin
// ✅ Preferir tipos no nulos por defecto
val name: String = "John"

// ✅ Usar nullable solo cuando sea necesario
val middleName: String? = null

// ✅ Safe calls para acceder a propiedades
val length = middleName?.length

// ✅ Elvis operator para valores por defecto
val displayName = middleName ?: "Anonymous"

// ✅ let para ejecutar código solo si no es null
middleName?.let { println("Middle name: $it") }

// ❌ Evitar !! (null assertion)
val dangerous = middleName!!.length // Puede lanzar NullPointerException
```

### En Parámetros de Funciones

```kotlin
// ✅ Parámetros no nulos por defecto
fun createUser(name: String, email: String) { }

// ✅ Nullable solo cuando tenga sentido de negocio
fun updateUser(nickname: String?, bio: String?) { }

// ✅ Valores por defecto cuando aplique
fun createProduct(
    name: String,
    description: String? = null,
    price: BigDecimal = BigDecimal.ZERO
) { }
```

### En Entidades JPA

```kotlin
@Entity
class User(
    @Id
    val id: Long? = null, // Nullable porque es null antes de persistir

    @Column(nullable = false)
    val username: String, // No nullable

    @Column(nullable = true)
    val bio: String? = null // Nullable con default
)
```

## Inmutabilidad

### Preferir `val` sobre `var`

```kotlin
// ✅ Inmutable por defecto
data class User(
    val id: Long,
    val name: String,
    val email: String
)

// ❌ Evitar var mutable
data class User(
    val id: Long,
    var name: String, // ❌ ¿Por qué necesita cambiar?
    var email: String // ❌ ¿Por qué necesita cambiar?
)

// ✅ Si necesita mutación, usar copy()
val updatedUser = user.copy(email = "new@email.com")
```

### Colecciones Inmutables

```kotlin
// ✅ Usar listas inmutables
val items: List<String> = listOf("A", "B", "C")

// ✅ Para mutación controlada, usar toMutableList()
val mutableItems = items.toMutableList()
mutableItems.add("D")
val newItems = mutableItems.toList()

// ❌ Evitar ArrayList mutable
val mutableList = ArrayList<String>() // ❌
```

## Data Classes

### Uso Apropiado

```kotlin
// ✅ Para DTOs
data class UserResponse(
    val id: Long,
    val username: String,
    val email: String
)

// ✅ Para Request DTOs
data class CreateUserRequest(
    val username: String,
    val email: String,
    val password: String
)

// ✅ Para Value Objects
data class Money(
    val amount: BigDecimal,
    val currency: String
)
```

### Métodos Generados

Data classes generan automáticamente:
- `equals()` y `hashCode()`
- `toString()`
- `copy()`
- Getters para propiedades

```kotlin
val user1 = User(1, "john", "john@email.com")
val user2 = user1.copy(email = "new@email.com")

println(user1 == user2) // false (diferente email)
println(user1) // User(id=1, username=john, email=john@email.com)
```

## Extension Functions

### Utilidades

```kotlin
// ✅ Extensiones para validación
fun String.isValidEmail(): Boolean {
    return this.contains("@") && this.contains(".")
}

// ✅ Extensiones para transformación
fun User.toResponse(): UserResponse {
    return UserResponse(id, username, email)
}

// ✅ Extensiones con reified generics
inline fun <reified T> ResponseEntity<*>.isSuccess(): Boolean {
    return this.statusCode.is2xxSuccessful && this.body is T
}
```

### En Controllers

```kotlin
// ✅ Helper para responses
fun <T> success(data: T, message: String = "Success"): ResponseEntity<ApiResponse<T>> {
    return ResponseEntity.ok(ApiResponse.success(data, message))
}

fun <T> created(data: T, location: URI): ResponseEntity<ApiResponse<T>> {
    return ResponseEntity.created(location).body(ApiResponse.success(data))
}

// Uso
@PostMapping
fun create(@RequestBody request: CreateUserRequest) = 
    success(service.create(request))
```

## Scope Functions

### let, run, with, apply, also

```kotlin
// let - ejecutar si no es null, retorna resultado
nullableValue?.let { 
    process(it) 
}

// run - configurar y retornar
val config = run {
    val config = Config()
    config.name = "test"
    config.port = 8080
    config
}

// with - operar en objeto sin modificarlo
with(stringBuilder) {
    append("Hello")
    append(" ")
    append("World")
}

// apply - configurar objeto, retorna el mismo
val user = User().apply {
    username = "john"
    email = "john@email.com"
}

// also - efecto secundario, retorna el mismo
val users = repository.findAll()
    .also { logger.info("Found ${it.size} users") }
    .map { it.toResponse() }
```

## Sealed Classes

### Para Estados y Resultados

```kotlin
// ✅ Para estados de dominio
sealed class OrderStatus {
    object Pending : OrderStatus()
    object Confirmed : OrderStatus()
    object Shipped : OrderStatus()
    object Delivered : OrderStatus()
    object Cancelled : OrderStatus()
}

// ✅ Para resultados de operaciones
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val exception: Throwable? = null) : Result<Nothing>()
}

// ✅ Uso con when exhaustivo
fun processOrder(status: OrderStatus) {
    when (status) {
        is OrderStatus.Pending -> sendConfirmation()
        is OrderStatus.Confirmed -> prepareShipment()
        is OrderStatus.Shipped -> trackDelivery()
        is OrderStatus.Delivered -> completeOrder()
        is OrderStatus.Cancelled -> refund()
    }
}
```

## Coroutines (para operaciones async)

### En Controllers

```kotlin
@RestController
class UserController(
    private val userService: UserService
) {

    @GetMapping("/{id}")
    suspend fun getById(@PathVariable id: Long): ResponseEntity<UserResponse> {
        // Ejecuta en coroutine scope
        val user = userService.findById(id)
        return ResponseEntity.ok(user)
    }

    @PostMapping("/bulk")
    suspend fun createBulk(
        @RequestBody requests: List<CreateUserRequest>
    ): ResponseEntity<List<UserResponse>> {
        // Ejecutar en paralelo
        val users = requests.mapAsync { request ->
            userService.create(request)
        }
        return ResponseEntity.ok(users)
    }
}
```

### En Servicios

```kotlin
@Service
class UserService(
    private val userRepository: UserRepository,
    private val emailService: EmailService
) {

    @Transactional
    suspend fun createWithWelcomeEmail(request: CreateUserRequest): UserResponse {
        // Guardar usuario
        val user = userRepository.save(request.toEntity())

        // Enviar email en background
        coroutineScope {
            launch {
                emailService.sendWelcomeEmail(user.email)
            }
        }

        return user.toResponse()
    }
}
```

## Naming Conventions

### Clases y Objetos

```kotlin
// ✅ PascalCase para clases
class UserService
class CreateUserRequest
class OrderNotFoundException

// ✅ camelCase para funciones y propiedades
fun findUserById(id: Long)
val userName: String

// ✅ SCREAMING_SNAKE_CASE para constantes
const val MAX_RETRY_ATTEMPTS = 3
const val API_VERSION = "v1"
```

### Tests

```kotlin
// ✅ Backticks para nombres descriptivos
@Test
fun `create should return user when data is valid`() { }

@Test
fun `create should throw exception when email already exists`() { }

@Test
fun `findById should return not found when user does not exist`() { }
```

## Pattern Matching con when

```kotlin
// ✅ when exhaustivo con sealed classes
fun getOrderMessage(status: OrderStatus): String {
    return when (status) {
        is OrderStatus.Pending -> "Orden pendiente de confirmación"
        is OrderStatus.Confirmed -> "Orden confirmada, preparando envío"
        is OrderStatus.Shipped -> "Orden en camino"
        is OrderStatus.Delivered -> "Orden entregada"
        is OrderStatus.Cancelled -> "Orden cancelada"
    }
}

// ✅ when con condiciones
fun categorize(amount: BigDecimal): String {
    return when {
        amount < BigDecimal.ZERO -> "Negativo"
        amount == BigDecimal.ZERO -> "Cero"
        amount < BigDecimal("1000") -> "Pequeño"
        amount < BigDecimal("10000") -> "Mediano"
        else -> "Grande"
    }
}
```

## Collection Operations

### Transformaciones

```kotlin
// ✅ map para transformar
val names = users.map { it.name }

// ✅ filter para filtrar
val activeUsers = users.filter { it.isActive }

// ✅ find para encontrar uno
val admin = users.find { it.role == Role.ADMIN }

// ✅ any/all para condiciones
val hasAdmins = users.any { it.role == Role.ADMIN }
val allActive = users.all { it.isActive }

// ✅ groupBy para agrupar
val usersByRole = users.groupBy { it.role }

// ✅ flatMap para colecciones anidadas
val allItems = orders.flatMap { it.items }

// ✅ associate para crear mapas
val userMap = users.associateBy { it.id }

// ✅ chain operations
val activeAdminNames = users
    .filter { it.isActive }
    .filter { it.role == Role.ADMIN }
    .map { it.name }
    .sorted()
```

## Error Handling

### Excepciones Personalizadas

```kotlin
// ✅ Excepciones específicas de dominio
class UserNotFoundException(userId: Long) : 
    RuntimeException("Usuario no encontrado: $userId")

class InvalidEmailException(email: String) : 
    RuntimeException("Email inválido: $email")

class InsufficientStockException(
    productId: Long, 
    requested: Int, 
    available: Int
) : RuntimeException(
    "Stock insuficiente para producto $productId: solicitado $requested, disponible $available"
)
```

### try-catch en Kotlin

```kotlin
// ✅ try como expresión
val result = try {
    riskyOperation()
} catch (e: SpecificException) {
    handleSpecific(e)
} catch (e: Exception) {
    handleGeneric(e)
}

// ✅ runCatching para resultados
val result: Result<String> = runCatching {
    readFromFile(path)
}

result.onSuccess { println("Content: $it") }
      .onFailure { println("Error: ${it.message}") }
```

## Checklist de Código Kotlin

### Antes de Commitear

- [ ] ¿Usé `val` en lugar de `var` donde sea posible?
- [ ] ¿Las propiedades nullable tienen un propósito claro?
- [ ] ¿Evité el uso de `!!` (null assertion)?
- [ ] ¿Usé data classes para DTOs?
- [ ] ¿Los nombres siguen las convenciones (PascalCase, camelCase)?
- [ ] ¿Usé `when` en lugar de múltiples if-else?
- [ ] ¿Las extensiones mejoran la legibilidad?
- [ ] ¿Los tests usan backticks para nombres descriptivos?
