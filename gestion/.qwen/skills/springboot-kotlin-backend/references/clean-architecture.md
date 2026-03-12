# Clean Architecture para Spring Boot + Kotlin

## Visión General

Clean Architecture es un patrón de diseño que organiza el código en capas concéntricas, donde las dependencias apuntan hacia adentro. Esto permite:

- **Independencia del framework**: La lógica de negocio no depende de frameworks
- **Testabilidad**: La lógica de negocio se puede probar en aislamiento
- **Independencia de la UI**: La lógica de negocio no depende de la interfaz
- **Independencia de la base de datos**: La lógica de negocio no depende del persistence layer

## Capas

```
┌─────────────────────────────────────────────────────────────┐
│                     Interface Layer                          │
│  (Controllers, DTOs, Request/Response, Mappers)             │
├─────────────────────────────────────────────────────────────┤
│                   Application Layer                          │
│  (Use Cases, Services, Application Services)                │
├─────────────────────────────────────────────────────────────┤
│                      Domain Layer                            │
│  (Entities, Value Objects, Domain Services, Repositories)   │
├─────────────────────────────────────────────────────────────┤
│                 Infrastructure Layer                         │
│  (Repository Implementations, External Services, DB)        │
└─────────────────────────────────────────────────────────────┘
```

## Domain Layer (Capa de Dominio)

### Entidades

Las entidades son objetos de negocio que tienen identidad propia. Deben ser:

- **Ricas en comportamiento**: No solo datos, también lógica
- **Independientes**: Sin dependencias de frameworks
- **Auto-validadas**: Validan su propio estado

```kotlin
// ✅ Correcto: Entidad rica en comportamiento
class Order(
    val id: Long? = null,
    val clientId: Long,
    val status: OrderStatus = OrderStatus.PENDING,
    val items: MutableList<OrderItem> = mutableListOf(),
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    fun addItem(product: Product, quantity: Int) {
        require(quantity > 0) { "La cantidad debe ser mayor a 0" }
        require(status == OrderStatus.PENDING) { 
            "Solo se pueden agregar items a órdenes pendientes" 
        }
        items.add(OrderItem(product, quantity))
    }

    fun cancel() {
        require(status == OrderStatus.PENDING) {
            "Solo se pueden cancelar órdenes pendientes"
        }
        status = OrderStatus.CANCELLED
    }

    val total: BigDecimal
        get() = items.sumOf { it.subtotal }
}
```

### Value Objects

Objetos definidos por sus atributos, sin identidad propia:

```kotlin
@Embeddable
class Money(
    @Column(nullable = false)
    val amount: BigDecimal,
    
    @Column(nullable = false, length = 3)
    val currency: String = "USD"
) {
    init {
        require(amount >= BigDecimal.ZERO) { "El monto no puede ser negativo" }
    }

    fun add(other: Money): Money {
        require(currency == other.currency) { "Monedas diferentes" }
        return Money(amount + other.amount, currency)
    }
}
```

### Domain Services

Lógica de negocio que no pertenece a una entidad específica:

```kotlin
interface DomainService {
    fun calculateShipping(order: Order): Money
}

class ShippingService : DomainService {
    override fun calculateShipping(order: Order): Money {
        // Lógica compleja de cálculo de envío
    }
}
```

## Application Layer (Capa de Aplicación)

### Use Cases / Services

Orquestan el flujo de la aplicación:

```kotlin
@Service
class CreateOrderService(
    private val orderRepository: OrderRepository,
    private val productRepository: ProductRepository,
    private val eventPublisher: ApplicationEventPublisher
) {
    @Transactional
    fun execute(request: CreateOrderRequest): OrderResponse {
        // 1. Validar reglas de negocio
        val customer = customerRepository.findById(request.customerId)
            ?: throw CustomerNotFoundException(request.customerId)

        // 2. Crear entidad de dominio
        val order = Order(customerId = customer.id!!)

        // 3. Agregar items
        request.items.forEach { itemRequest ->
            val product = productRepository.findById(itemRequest.productId)
                ?: throw ProductNotFoundException(itemRequest.productId)
            order.addItem(product, itemRequest.quantity)
        }

        // 4. Persistir
        val savedOrder = orderRepository.save(order)

        // 5. Publicar evento de dominio
        eventPublisher.publishEvent(OrderCreatedEvent(savedOrder))

        // 6. Retornar response
        return OrderResponse.from(savedOrder)
    }
}
```

### DTOs (Data Transfer Objects)

```kotlin
// Request DTO
data class CreateOrderRequest(
    @field:NotNull(message = "El customerId es requerido")
    val customerId: Long,

    @field:NotEmpty(message = "La orden debe tener al menos un item")
    val items: List<OrderItemRequest>
)

data class OrderItemRequest(
    @field:NotNull(message = "El productId es requerido")
    val productId: Long,

    @field:Min(value = 1, message = "La cantidad mínima es 1")
    val quantity: Int
)

// Response DTO
data class OrderResponse(
    val id: Long,
    val customerId: Long,
    val status: String,
    val items: List<OrderItemResponse>,
    val total: BigDecimal,
    val createdAt: LocalDateTime
)
```

## Interface Layer (Capa de Interfaz)

### Controllers

```kotlin
@RestController
@RequestMapping("/api/orders")
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Órdenes", description = "Gestión de órdenes")
class OrderController(
    private val createOrderService: CreateOrderService,
    private val getOrderService: GetOrderService
) {

    @PostMapping
    @Operation(summary = "Crear orden", description = "Crea una nueva orden de compra")
    @ApiResponse(responseCode = "201", description = "Orden creada")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    fun create(
        @Valid @RequestBody request: CreateOrderRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<OrderResponse> {
        val response = createOrderService.execute(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): ResponseEntity<OrderResponse> {
        val response = getOrderService.execute(id)
        return ResponseEntity.ok(response)
    }
}
```

## Infrastructure Layer (Capa de Infraestructura)

### Repository Implementations

```kotlin
@Repository
interface OrderRepository : JpaRepository<Order, Long> {
    
    fun findByCustomerId(customerId: Long): List<Order>
    
    fun findByStatus(status: OrderStatus): List<Order>
    
    @Query("SELECT o FROM Order o WHERE o.createdAt BETWEEN :start AND :end")
    fun findByDateRange(@Param("start") start: LocalDateTime, @Param("end") end: LocalDateTime): List<Order>
}
```

### External Services

```kotlin
@Component
class PaymentGatewayClient(
    @Value("\${payment.gateway.url}") private val gatewayUrl: String,
    private val restTemplate: RestTemplate
) {
    fun processPayment(payment: PaymentRequest): PaymentResponse {
        val response = restTemplate.postForObject(
            "$gatewayUrl/payments",
            payment,
            PaymentResponse::class.java
        )
        return response ?: throw PaymentGatewayException("Error procesando pago")
    }
}
```

## Dependencias entre Capas

```
Interface → Application → Domain ← Infrastructure
```

**Regla de oro**: Las dependencias siempre apuntan hacia adentro (hacia Domain).

## Beneficios

1. **Testabilidad**: Cada capa se puede testear independientemente
2. **Mantenibilidad**: Cambios en una capa no afectan las demás
3. **Flexibilidad**: Fácil cambiar frameworks o bases de datos
4. **Claridad**: Cada capa tiene una responsabilidad clara

## Estructura de Paquetes Recomendada

```
com.example.project/
├── order/
│   ├── application/
│   │   ├── CreateOrderService.kt
│   │   ├── GetOrderService.kt
│   │   ├── dto/
│   │   │   ├── CreateOrderRequest.kt
│   │   │   └── OrderResponse.kt
│   │   └── event/
│   │       └── OrderCreatedEvent.kt
│   ├── domain/
│   │   ├── Order.kt
│   │   ├── OrderItem.kt
│   │   ├── OrderStatus.kt
│   │   └── exception/
│   │       ├── OrderNotFoundException.kt
│   │       └── InvalidOrderException.kt
│   └── infrastructure/
│       ├── OrderController.kt
│       ├── OrderRepository.kt
│       └── OrderMapper.kt
```
