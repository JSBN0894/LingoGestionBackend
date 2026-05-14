package com.linogo.gestion.orderproduct

import com.linogo.gestion.category.domain.Category
import com.linogo.gestion.order.domain.Order
import com.linogo.gestion.order.infrastructure.OrderRepository
import com.linogo.gestion.orderproduct.application.CreateOrderProductRequest
import com.linogo.gestion.orderproduct.application.OrderProductService
import com.linogo.gestion.orderproduct.application.UpdateOrderProductRequest
import com.linogo.gestion.orderproduct.domain.OrderProduct
import com.linogo.gestion.orderproduct.domain.OrderProductId
import com.linogo.gestion.orderproduct.infrastructure.OrderProductRepository
import com.linogo.gestion.product.infrastructure.persistence.entity.ProductEntity
import com.linogo.gestion.product.infrastructure.ProductRepository
import com.linogo.gestion.state.domain.State
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
class OrderProductServiceTest {

    @Mock
    private lateinit var orderProductRepository: OrderProductRepository

    @Mock
    private lateinit var orderRepository: OrderRepository

    @Mock
    private lateinit var productRepository: ProductRepository

    @InjectMocks
    private lateinit var orderProductService: OrderProductService

    private lateinit var state: State
    private lateinit var order: Order
    private lateinit var category: Category
    private lateinit var product: ProductEntity
    private lateinit var orderProduct: OrderProduct
    private lateinit var createRequest: CreateOrderProductRequest
    private lateinit var updateRequest: UpdateOrderProductRequest

    @BeforeEach
    fun setUp() {
        state = State(id = 1L, name = "Pendiente", priority = 1, createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now())
        order = Order(id = 1L, customerId = 123456789L, customerName = "Juan",
            operationState = state, orderPrice = 50000L,
            orderAddress = "Addr", orderPhone = "Phone", orderCity = "City",
            createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now())
        category = Category(id = 1L, name = "Moldes")
        product = ProductEntity(id = 1L, name = "Molde", pricePerUnit = 10000L, stock = 10,
            imageUrl = "img.jpg", description = "desc", category = category,
            createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now())

        orderProduct = OrderProduct(order = order, product = product, quantity = 2, price = 20000L,
            createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now())

        createRequest = CreateOrderProductRequest(orderId = 1L, productId = 1L, quantity = 2, price = 20000L)

        updateRequest = UpdateOrderProductRequest(quantity = 3, price = 30000L)
    }

    @Test
    fun `create should save order product when data is valid`() {
        `when`(orderRepository.findById(1L)).thenReturn(Optional.of(order))
        `when`(productRepository.findById(1L)).thenReturn(Optional.of(product))
        `when`(orderProductRepository.save(any())).thenReturn(orderProduct)

        val response = orderProductService.create(createRequest)

        assertNotNull(response)
        assertEquals(1L, response.orderId)
        assertEquals(1L, response.productId)
        assertEquals(2, response.quantity)
        verify(orderRepository).findById(1L)
        verify(productRepository).findById(1L)
        verify(orderProductRepository).save(any())
    }

    @Test
    fun `create should throw IllegalArgumentException when order not found`() {
        `when`(orderRepository.findById(999L)).thenReturn(Optional.empty())

        val request = createRequest.copy(orderId = 999L)
        val exception = assertThrows(IllegalArgumentException::class.java) {
            orderProductService.create(request)
        }

        assertEquals("Order with id 999 not found", exception.message)
    }

    @Test
    fun `create should throw IllegalArgumentException when product not found`() {
        `when`(orderRepository.findById(1L)).thenReturn(Optional.of(order))
        `when`(productRepository.findById(999L)).thenReturn(Optional.empty())

        val request = createRequest.copy(productId = 999L)
        val exception = assertThrows(IllegalArgumentException::class.java) {
            orderProductService.create(request)
        }

        assertEquals("Product with id 999 not found", exception.message)
    }

    @Test
    fun `findByOrderId should return list of OrderProductResponse`() {
        `when`(orderProductRepository.findByOrderId(1L)).thenReturn(listOf(orderProduct))

        val responses = orderProductService.findByOrderId(1L)

        assertEquals(1, responses.size)
        assertEquals(1L, responses[0].orderId)
        verify(orderProductRepository).findByOrderId(1L)
    }

    @Test
    fun `findByOrderId should return empty list when no products for order`() {
        `when`(orderProductRepository.findByOrderId(999L)).thenReturn(emptyList())

        val responses = orderProductService.findByOrderId(999L)

        assertEquals(0, responses.size)
        verify(orderProductRepository).findByOrderId(999L)
    }

    @Test
    fun `findAll should return list of OrderProductResponse`() {
        `when`(orderProductRepository.findAll()).thenReturn(listOf(orderProduct))

        val responses = orderProductService.findAll()

        assertEquals(1, responses.size)
        verify(orderProductRepository).findAll()
    }

    @Test
    fun `findAll should return empty list when no order products exist`() {
        `when`(orderProductRepository.findAll()).thenReturn(emptyList())

        val responses = orderProductService.findAll()

        assertEquals(0, responses.size)
        verify(orderProductRepository).findAll()
    }

    @Test
    fun `update should return updated OrderProductResponse when exists`() {
        val updated = orderProduct.copy(quantity = 3, price = 30000L)
        `when`(orderProductRepository.findById(OrderProductId(1L, 1L))).thenReturn(Optional.of(orderProduct))
        `when`(orderProductRepository.save(any())).thenReturn(updated)

        val response = orderProductService.update(1L, 1L, updateRequest)

        assertNotNull(response)
        assertEquals(3, response.quantity)
        assertEquals(30000L, response.price)
        verify(orderProductRepository).findById(OrderProductId(1L, 1L))
    }

    @Test
    fun `update should throw IllegalArgumentException when order product not found`() {
        `when`(orderProductRepository.findById(OrderProductId(999L, 999L))).thenReturn(Optional.empty())

        val exception = assertThrows(IllegalArgumentException::class.java) {
            orderProductService.update(999L, 999L, updateRequest)
        }

        assertEquals("OrderProduct not found for order 999 and product 999", exception.message)
    }

    @Test
    fun `delete should remove order product when exists`() {
        `when`(orderProductRepository.existsById(OrderProductId(1L, 1L))).thenReturn(true)

        orderProductService.delete(1L, 1L)

        verify(orderProductRepository).existsById(OrderProductId(1L, 1L))
        verify(orderProductRepository).deleteById(OrderProductId(1L, 1L))
    }

    @Test
    fun `delete should throw IllegalArgumentException when order product not found`() {
        `when`(orderProductRepository.existsById(OrderProductId(999L, 999L))).thenReturn(false)

        val exception = assertThrows(IllegalArgumentException::class.java) {
            orderProductService.delete(999L, 999L)
        }

        assertEquals("OrderProduct not found for order 999 and product 999", exception.message)
        verify(orderProductRepository).existsById(OrderProductId(999L, 999L))
    }
}
