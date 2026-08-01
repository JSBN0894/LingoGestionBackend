package com.linogo.gestion.order

import com.linogo.gestion.category.domain.Category
import com.linogo.gestion.customer.domain.Customer
import com.linogo.gestion.customer.domain.CustomerRepository
import com.linogo.gestion.order.application.CreateOrderCompleteRequest
import com.linogo.gestion.order.application.CreateOrderCompleteService
import com.linogo.gestion.order.application.OrderData
import com.linogo.gestion.order.application.OrderProductData
import com.linogo.gestion.order.domain.Order
import com.linogo.gestion.order.infrastructure.OrderRepository
import com.linogo.gestion.orderproduct.infrastructure.OrderProductRepository
import com.linogo.gestion.product.infrastructure.persistence.entity.ProductEntity
import com.linogo.gestion.product.infrastructure.ProductRepository
import com.linogo.gestion.state.domain.State
import com.linogo.gestion.state.infrastructure.StateRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import java.time.LocalDateTime
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class CreateOrderCompleteServiceTest {

    @Mock
    private lateinit var orderRepository: OrderRepository

    @Mock
    private lateinit var customerRepository: CustomerRepository

    @Mock
    private lateinit var stateRepository: StateRepository

    @Mock
    private lateinit var productRepository: ProductRepository

    @Mock
    private lateinit var orderProductRepository: OrderProductRepository

    @InjectMocks
    private lateinit var createOrderCompleteService: CreateOrderCompleteService

    private lateinit var existingCustomer: Customer
    private lateinit var state: State
    private lateinit var category: Category
    private lateinit var product: ProductEntity
    private lateinit var request: CreateOrderCompleteRequest
    private lateinit var savedOrder: Order

    @BeforeEach
    fun setUp() {
        existingCustomer = Customer(
            cedula = 123456789L, name = "Juan Pérez",
            phones = listOf("+54911223344"),
            addresses = listOf("Av. Corrientes 1234, Buenos Aires")
        )

        state = State(id = 1L, name = "Pendiente", priority = 1,
            createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now())

        category = Category(id = 1L, name = "Moldes")

        product = ProductEntity(id = 1L, name = "Molde Silicona", pricePerUnit = 15000L, stock = 10,
            imageUrl = "img.jpg", description = "Molde de silicona", category = category,
            createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now())

        savedOrder = Order(id = 1L, customerId = 123456789L, customerName = "Juan Pérez",
            operationState = state, orderPrice = 30000L,
            orderAddress = "Av. Corrientes 1234", orderPhone = "+54911223344",
            orderCity = "Buenos Aires",
            createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now())

        request = CreateOrderCompleteRequest(
            name = "Juan Pérez",
            cedula = 123456789L,
            address = "Av. Corrientes 1234",
            city = "Buenos Aires",
            phone = "+54911223344",
            order = OrderData(
                orderPrice = 30000L,
                operationStateId = 1L,
                orderProducts = listOf(OrderProductData(idProduct = 1L, quantity = 2))
            )
        )
    }

    @Test
    fun `create should create complete order when customer exists`() {
        `when`(customerRepository.findByCedula(123456789L)).thenReturn(existingCustomer)
        `when`(stateRepository.findById(1L)).thenReturn(Optional.of(state))
        `when`(productRepository.findById(1L)).thenReturn(Optional.of(product))
        `when`(orderRepository.save(any())).thenReturn(savedOrder)

        val response = createOrderCompleteService.create(request)

        assertNotNull(response)
        assertEquals(123456789L, response.customerId)
        assertEquals(30000L, response.orderPrice)
        verify(customerRepository).findByCedula(123456789L)
        verify(orderRepository).save(any())
        verify(orderProductRepository).saveAll(any<Iterable<com.linogo.gestion.orderproduct.domain.OrderProduct>>())
    }

    @Test
    fun `create should create new customer when customer does not exist`() {
        `when`(customerRepository.findByCedula(123456789L)).thenReturn(null)
        `when`(customerRepository.save(any())).thenReturn(existingCustomer)
        `when`(stateRepository.findById(1L)).thenReturn(Optional.of(state))
        `when`(productRepository.findById(1L)).thenReturn(Optional.of(product))
        `when`(orderRepository.save(any())).thenReturn(savedOrder)

        val response = createOrderCompleteService.create(request)

        assertNotNull(response)
        assertEquals(123456789L, response.customerId)
        verify(customerRepository).findByCedula(123456789L)
        verify(customerRepository).save(any())
    }

    @Test
    fun `create should add new phone when customer exists but phone is different`() {
        `when`(customerRepository.findByCedula(123456789L)).thenReturn(existingCustomer)
        `when`(stateRepository.findById(1L)).thenReturn(Optional.of(state))
        `when`(productRepository.findById(1L)).thenReturn(Optional.of(product))
        `when`(orderRepository.save(any())).thenReturn(savedOrder)
        `when`(customerRepository.save(any())).thenReturn(existingCustomer)

        val requestWithNewPhone = request.copy(phone = "+57 300 987 6543")
        val response = createOrderCompleteService.create(requestWithNewPhone)

        assertNotNull(response)
        verify(customerRepository).save(any())
    }

    @Test
    fun `create should throw IllegalArgumentException when state not found`() {
        `when`(customerRepository.findByCedula(123456789L)).thenReturn(existingCustomer)
        `when`(stateRepository.findById(1L)).thenReturn(Optional.empty())

        val exception = assertThrows(IllegalArgumentException::class.java) {
            createOrderCompleteService.create(request)
        }

        assertEquals("State with id 1 not found", exception.message)
    }

    @Test
    fun `create should throw IllegalArgumentException when product not found`() {
        `when`(customerRepository.findByCedula(123456789L)).thenReturn(existingCustomer)
        `when`(stateRepository.findById(1L)).thenReturn(Optional.of(state))
        `when`(orderRepository.save(any())).thenReturn(savedOrder)
        `when`(productRepository.findById(999L)).thenReturn(Optional.empty())

        val requestWithBadProduct = request.copy(
            order = request.order.copy(orderProducts = listOf(OrderProductData(idProduct = 999L, quantity = 1)))
        )

        val exception = assertThrows(IllegalArgumentException::class.java) {
            createOrderCompleteService.create(requestWithBadProduct)
        }

        assertEquals("Product with id 999 not found", exception.message)
    }
}
