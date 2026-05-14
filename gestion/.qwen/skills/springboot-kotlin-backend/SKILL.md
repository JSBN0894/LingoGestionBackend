---
name: springboot-clean-architecture
description: Implementación de Clean Architecture Estricta en Spring Boot con Kotlin. Úsalo para diseñar sistemas desacoplados donde el Dominio es independiente de la infraestructura, base de datos y frameworks. Aplica cuando el usuario pida "arquitectura limpia", "hexagonal", o "DDD" de forma rigurosa. Incluye patrones para separar entidades de dominio de entidades de persistencia.
---

# Clean Architecture Estricta en Spring Boot + Kotlin

Esta skill guía la implementación de sistemas basados en Clean Architecture (Arquitectura Hexagonal/Onion) de forma rigurosa. El objetivo es mantener el **Núcleo de Negocio (Dominio)** totalmente aislado de detalles técnicos como bases de datos, APIs externas o el propio framework Spring.

---

## Estructura de Capas (Strict CA)

El código se organiza en capas concéntricas donde la dependencia siempre apunta hacia el centro (Dominio).

```
com.example.project.module/
├── domain/                   # Capa de Dominio (EL NÚCLEO)
│   ├── model/                # Entidades de negocio (Data Classes puras, SIN JPA)
│   ├── repository/           # Interfaces de Repositorio (Puertos de Salida)
│   ├── service/              # Servicios de Dominio (Lógica multi-entidad)
│   └── exception/            # Excepciones de negocio (DomainException)
├── application/              # Capa de Aplicación (CASOS DE USO)
│   ├── service/              # Implementación de Casos de Uso (Orquestación)
│   ├── dto/                  # Request/Response DTOs (Contratos de aplicación)
│   └── mapper/               # Mappers (DTO <-> Domain Model)
└── infrastructure/           # Capa de Infraestructura (ADAPTADORES)
    ├── rest/                 # Controllers, ExceptionHandlers (Input Adapters)
    ├── persistence/          # Persistencia (Output Adapters)
    │   ├── jpa/              # JpaRepository (Spring Data)
    │   ├── entity/           # Entities de DB (@Entity, @Table)
    │   └── mapper/           # Mappers (DB Entity <-> Domain Model)
    └── config/               # Beans, Security, External Clients
```

---

## Patrones por Capa

### 1. Dominio (Domain)
Es la capa más estable. No debe depender de ninguna librería externa (excepto librerías básicas de Kotlin/Java).

**Entidad de Dominio (Pura):**
```kotlin
// domain/model/Product.kt
data class Product(
    val id: Long? = null,
    val name: String,
    val price: Double
) {
    fun applyDiscount(percentage: Double): Product {
        require(percentage in 0.0..100.0) { "Descuento inválido" }
        return copy(price = price * (1 - percentage / 100))
    }
}
```

**Interfaz de Repositorio (Puerto):**
```kotlin
// domain/repository/ProductRepository.kt
interface ProductRepository {
    fun save(product: Product): Product
    fun findById(id: Long): Product?
    fun findAll(): List<Product>
}
```

### 2. Aplicación (Application)
Orquesta el flujo de datos desde y hacia las entidades de dominio.

**Caso de Uso / Servicio de Aplicación:**
```kotlin
// application/service/ProductApplicationService.kt
@Service
class ProductApplicationService(
    private val productRepository: ProductRepository, // Usa la interfaz del dominio
    private val mapper: ProductDtoMapper
) {
    @Transactional
    fun createProduct(request: CreateProductRequest): ProductResponse {
        val product = mapper.toDomain(request)
        val savedProduct = productRepository.save(product)
        return mapper.toResponse(savedProduct)
    }
}
```

### 3. Infraestructura (Infrastructure)
Contiene los detalles de implementación y adaptadores.

**Entidad de Persistencia (JPA):**
```kotlin
// infrastructure/persistence/entity/ProductEntity.kt
@Entity
@Table(name = "products")
class ProductEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    val name: String,
    val price: Double
)
```

**Implementación del Repositorio (Adaptador):**
```kotlin
// infrastructure/persistence/ProductRepositoryImpl.kt
@Component
class ProductRepositoryImpl(
    private val jpaRepository: ProductJpaRepository,
    private val mapper: ProductPersistenceMapper
) : ProductRepository { // Implementa la interfaz del dominio

    override fun save(product: Product): Product {
        val entity = mapper.toEntity(product)
        return mapper.toDomain(jpaRepository.save(entity))
    }

    override fun findById(id: Long): Product? {
        return jpaRepository.findById(id).map { mapper.toDomain(it) }.orElse(null)
    }
    
    override fun findAll(): List<Product> {
        return jpaRepository.findAll().map { mapper.toDomain(it) }
    }
}
```

---

## Reglas de Oro

1.  **Independencia del Framework**: El dominio no debe saber que existe Spring.
2.  **Testabilidad**: El dominio y la aplicación deben ser testeables con Unit Tests puros (sin cargar el contexto de Spring).
3.  **Inversión de Dependencia**: Si `Application` necesita persistir datos, llama a una interfaz en `Domain`. La implementación real vive en `Infrastructure` y se inyecta mediante Spring.
4.  **Mapeo Obligatorio**: Cada capa tiene su propio modelo de datos.
    *   `Rest` usa DTOs.
    *   `Domain` usa Modelos de Dominio.
    *   `Persistence` usa @Entities.
    *   *Nota: Aunque parezca redundante, esto evita que un cambio en la tabla de la DB rompa el contrato de la API.*

---

## Testing Strategy

1.  **Dominio**: Unit Tests con JUnit 5 (sin mocks).
2.  **Aplicación**: Unit Tests con Mockito/MockK para mockear los Repositorios (Puertos).
3.  **Infraestructura (Persistencia)**: Integration Tests con `@DataJpaTest` y Testcontainers.
4.  **Infraestructura (Rest)**: Integration Tests con `@WebMvcTest` o `MockMvc`.

---

## Checklist de Arquitectura Estricta

- [ ] ¿La entidad de dominio tiene anotaciones `@Entity` o `@Table`? (Si es sí -> CORREGIR)
- [ ] ¿El servicio de aplicación depende de `JpaRepository` directamente? (Si es sí -> CORREGIR, debe depender de la interfaz en `domain/repository`)
- [ ] ¿Se están exponiendo las entidades de dominio directamente en el Controller? (Si es sí -> USAR DTOs)
- [ ] ¿El dominio tiene lógica de persistencia? (Si es sí -> MOVER a Infrastructure)
- [ ] ¿Existen mappers para convertir entre capas? (Si es no -> CREARLOS)
