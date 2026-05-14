package com.linogo.gestion.order

import com.linogo.gestion.client.domain.Client
import com.linogo.gestion.client.infrastructure.ClientRepository
import com.linogo.gestion.order.application.CreateOrderRequest
import com.linogo.gestion.order.application.OrderService
import com.linogo.gestion.order.application.UpdateOrderRequest
import com.linogo.gestion.order.domain.Order
import com.linogo.gestion.order.infrastructure.OrderRepository
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
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDateTime
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class OrderServiceTest {

    @Mock
    private lateinit var orderRepository: OrderRepository

    @Mock
    private lateinit var clientRepository: ClientRepository

    @Mock
    private lateinit var stateRepository: StateRepository

    @InjectMocks
    private lateinit var orderService: OrderService

    private lateinit var client: Client
    private lateinit var state: State
    private lateinit var existingOrder: Order
    private lateinit var createRequest: CreateOrderRequest
    private lateinit var updateRequest: UpdateOrderRequest

    @BeforeEach
    fun setUp() {
        client = Client(
            idUser = "user_123",
            name = "Juan Pérez",
            defaultPhone = "+54911223344",
            defaultCity = "Buenos Aires",
            defaultAddress = "Av. Corrientes 1234",
            createdAt = LocalDateTime.now().minusDays(1),
            updatedAt = LocalDateTime.now()
        )

        state = State(
            id = 1L,
            name = "Pendiente",
            priority = 1,
            createdAt = LocalDateTime.now().minusDays(1),
            updatedAt = LocalDateTime.now()
        )

        existingOrder = Order(
            id = 1L,
            client = client,
            operationState = state,
            orderPrice = 50000L,
            orderAddress = "Av. Corrientes 1234",
            orderPhone = "+54911223344",
            orderCity = "Buenos Aires",
            createdAt = LocalDateTime.now().minusDays(1),
            updatedAt = LocalDateTime.now()
        )

        createRequest = CreateOrderRequest(
            clientId = "user_123",
            operationStateId = 1L,
            orderPrice = 50000L,
            orderAddress = "Av. Corrientes 1234",
            orderPhone = "+54911223344",
            orderCity = "Buenos Aires"
        )

        updateRequest = UpdateOrderRequest(
            operationStateId = 2L,
            orderPrice = 60000L,
            orderAddress = "Calle Falsa 123",
            orderPhone = "+54911998877",
            orderCity = "Rosario"
        )
    }

    @Test
    fun `create should save order when data is valid`() {
        val savedOrder = existingOrder.copy(id = 2L)
        `when`(clientRepository.findById("user_123")).thenReturn(Optional.of(client))
        `when`(stateRepository.findById(1L)).thenReturn(Optional.of(state))
        `when`(orderRepository.save(org.mockito.ArgumentMatchers.any())).thenReturn(savedOrder)

        val response = orderService.create(createRequest)

        assertNotNull(response)
        assertEquals("user_123", response.clientId)
        assertEquals(1L, response.operationStateId)
        assertEquals(50000L, response.orderPrice)
        verify(clientRepository).findById("user_123")
        verify(stateRepository).findById(1L)
        verify(orderRepository).save(org.mockito.ArgumentMatchers.any())
    }

    @Test
    fun `create should throw IllegalArgumentException when client not found`() {
        `when`(clientRepository.findById("user_not_found")).thenReturn(Optional.empty())

        val request = createRequest.copy(clientId = "user_not_found")
        val exception = assertThrows(IllegalArgumentException::class.java) {
            orderService.create(request)
        }

        assertEquals("Client with id user_not_found not found", exception.message)
    }

    @Test
    fun `create should throw IllegalArgumentException when state not found`() {
        `when`(clientRepository.findById("user_123")).thenReturn(Optional.of(client))
        `when`(stateRepository.findById(999L)).thenReturn(Optional.empty())

        val request = createRequest.copy(operationStateId = 999L)
        val exception = assertThrows(IllegalArgumentException::class.java) {
            orderService.create(request)
        }

        assertEquals("State with id 999 not found", exception.message)
    }

    @Test
    fun `findById should return OrderResponse when order exists`() {
        `when`(orderRepository.findById(1L)).thenReturn(Optional.of(existingOrder))

        val response = orderService.findById(1L)

        assertNotNull(response)
        assertEquals(1L, response.id)
        assertEquals("Juan Pérez", response.clientName)
        verify(orderRepository).findById(1L)
    }

    @Test
    fun `findById should throw IllegalArgumentException when order does not exist`() {
        `when`(orderRepository.findById(999L)).thenReturn(Optional.empty())

        val exception = assertThrows(IllegalArgumentException::class.java) {
            orderService.findById(999L)
        }

        assertEquals("Order with id 999 not found", exception.message)
        verify(orderRepository).findById(999L)
    }

    @Test
    fun `findAll should return list of OrderResponse`() {
        `when`(orderRepository.findAll()).thenReturn(listOf(existingOrder))

        val responses = orderService.findAll()

        assertEquals(1, responses.size)
        assertEquals(1L, responses[0].id)
        verify(orderRepository).findAll()
    }

    @Test
    fun `findAll should return empty list when no orders exist`() {
        `when`(orderRepository.findAll()).thenReturn(emptyList())

        val responses = orderService.findAll()

        assertEquals(0, responses.size)
        verify(orderRepository).findAll()
    }

    @Test
    fun `update should return updated OrderResponse when order exists`() {
        val updatedState = state.copy(id = 2L, name = "En Proceso")
        val updatedOrder = existingOrder.copy(
            operationState = updatedState,
            orderPrice = 60000L,
            orderAddress = "Calle Falsa 123",
            orderPhone = "+54911998877",
            orderCity = "Rosario"
        )
        `when`(orderRepository.findById(1L)).thenReturn(Optional.of(existingOrder))
        `when`(stateRepository.findById(2L)).thenReturn(Optional.of(updatedState))
        `when`(orderRepository.save(org.mockito.ArgumentMatchers.any())).thenReturn(updatedOrder)

        val response = orderService.update(1L, updateRequest)

        assertNotNull(response)
        assertEquals(2L, response.operationStateId)
        assertEquals(60000L, response.orderPrice)
        verify(orderRepository).findById(1L)
        verify(stateRepository).findById(2L)
    }

    @Test
    fun `update should throw IllegalArgumentException when order does not exist`() {
        `when`(orderRepository.findById(999L)).thenReturn(Optional.empty())

        val exception = assertThrows(IllegalArgumentException::class.java) {
            orderService.update(999L, updateRequest)
        }

        assertEquals("Order with id 999 not found", exception.message)
        verify(orderRepository).findById(999L)
    }

    @Test
    fun `delete should remove order when order exists`() {
        `when`(orderRepository.existsById(1L)).thenReturn(true)

        orderService.delete(1L)

        verify(orderRepository).existsById(1L)
        verify(orderRepository).deleteById(1L)
    }

    @Test
    fun `delete should throw IllegalArgumentException when order does not exist`() {
        `when`(orderRepository.existsById(999L)).thenReturn(false)

        val exception = assertThrows(IllegalArgumentException::class.java) {
            orderService.delete(999L)
        }

        assertEquals("Order with id 999 not found", exception.message)
        verify(orderRepository).existsById(999L)
    }
}
