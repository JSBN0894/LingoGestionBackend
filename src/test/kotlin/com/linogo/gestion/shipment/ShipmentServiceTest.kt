package com.linogo.gestion.shipment

import com.linogo.gestion.client.domain.Client
import com.linogo.gestion.order.domain.Order
import com.linogo.gestion.order.infrastructure.OrderRepository
import com.linogo.gestion.shipment.application.CreateShipmentRequest
import com.linogo.gestion.shipment.application.ShipmentService
import com.linogo.gestion.shipment.application.UpdateShipmentRequest
import com.linogo.gestion.shipment.domain.Shipment
import com.linogo.gestion.shipment.infrastructure.ShipmentRepository
import com.linogo.gestion.shipmentstate.domain.ShipmentState
import com.linogo.gestion.shipmentstate.infrastructure.ShipmentStateRepository
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
class ShipmentServiceTest {

    @Mock
    private lateinit var shipmentRepository: ShipmentRepository

    @Mock
    private lateinit var orderRepository: OrderRepository

    @Mock
    private lateinit var shipmentStateRepository: ShipmentStateRepository

    @InjectMocks
    private lateinit var shipmentService: ShipmentService

    private lateinit var client: Client
    private lateinit var state: State
    private lateinit var order: Order
    private lateinit var shippingState: ShipmentState
    private lateinit var shipment: Shipment
    private lateinit var createRequest: CreateShipmentRequest
    private lateinit var updateRequest: UpdateShipmentRequest

    @BeforeEach
    fun setUp() {
        client = Client(idUser = "user_123", name = "Juan", createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now())
        state = State(id = 1L, name = "Pendiente", priority = 1, createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now())
        order = Order(id = 1L, client = client, operationState = state, orderPrice = 50000L,
            orderAddress = "Addr", orderPhone = "Phone", orderCity = "City",
            createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now())
        shippingState = ShipmentState(id = 1L, state = state, name = "En preparación",
            createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now())

        shipment = Shipment(id = 1L, order = order, shippingState = shippingState, carrier = "Inter rapidisimo",
            isCashOnDelivery = true, shippingCost = 5000L, weight = 2000L,
            createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now())

        createRequest = CreateShipmentRequest(
            orderId = 1L, shippingStateId = 1L, carrier = "Inter rapidisimo",
            isCashOnDelivery = true, shippingCost = 5000L, weight = 2000L)

        updateRequest = UpdateShipmentRequest(
            shippingStateId = 2L, carrier = "DHL", isCashOnDelivery = false,
            shippingCost = 8000L, weight = 3000L)
    }

    @Test
    fun `create should save shipment when data is valid`() {
        `when`(orderRepository.findById(1L)).thenReturn(Optional.of(order))
        `when`(shipmentStateRepository.findById(1L)).thenReturn(Optional.of(shippingState))
        `when`(shipmentRepository.save(any())).thenReturn(shipment)

        val response = shipmentService.create(createRequest)

        assertNotNull(response)
        assertEquals(1L, response.orderId)
        assertEquals("Inter rapidisimo", response.carrier)
        verify(orderRepository).findById(1L)
        verify(shipmentStateRepository).findById(1L)
        verify(shipmentRepository).save(any())
    }

    @Test
    fun `create should throw IllegalArgumentException when order not found`() {
        `when`(orderRepository.findById(999L)).thenReturn(Optional.empty())

        val request = createRequest.copy(orderId = 999L)
        val exception = assertThrows(IllegalArgumentException::class.java) {
            shipmentService.create(request)
        }

        assertEquals("Order with id 999 not found", exception.message)
    }

    @Test
    fun `create should throw IllegalArgumentException when shipment state not found`() {
        `when`(orderRepository.findById(1L)).thenReturn(Optional.of(order))
        `when`(shipmentStateRepository.findById(999L)).thenReturn(Optional.empty())

        val request = createRequest.copy(shippingStateId = 999L)
        val exception = assertThrows(IllegalArgumentException::class.java) {
            shipmentService.create(request)
        }

        assertEquals("ShipmentState with id 999 not found", exception.message)
    }

    @Test
    fun `findById should return ShipmentResponse when exists`() {
        `when`(shipmentRepository.findById(1L)).thenReturn(Optional.of(shipment))

        val response = shipmentService.findById(1L)

        assertNotNull(response)
        assertEquals(1L, response.id)
        assertEquals("Inter rapidisimo", response.carrier)
        verify(shipmentRepository).findById(1L)
    }

    @Test
    fun `findById should throw IllegalArgumentException when not found`() {
        `when`(shipmentRepository.findById(999L)).thenReturn(Optional.empty())

        val exception = assertThrows(IllegalArgumentException::class.java) {
            shipmentService.findById(999L)
        }

        assertEquals("Shipment with id 999 not found", exception.message)
        verify(shipmentRepository).findById(999L)
    }

    @Test
    fun `findAll should return list of ShipmentResponse`() {
        `when`(shipmentRepository.findAll()).thenReturn(listOf(shipment))

        val responses = shipmentService.findAll()

        assertEquals(1, responses.size)
        verify(shipmentRepository).findAll()
    }

    @Test
    fun `findAll should return empty list when no shipments exist`() {
        `when`(shipmentRepository.findAll()).thenReturn(emptyList())

        val responses = shipmentService.findAll()

        assertEquals(0, responses.size)
        verify(shipmentRepository).findAll()
    }

    @Test
    fun `update should return updated ShipmentResponse when exists`() {
        val updatedState = ShipmentState(id = 2L, state = state.copy(id = 2L, name = "En tránsito"), name = "En tránsito",
            createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now())
        val updatedShipment = shipment.copy(shippingState = updatedState, carrier = "DHL", isCashOnDelivery = false,
            shippingCost = 8000L, weight = 3000L)

        `when`(shipmentRepository.findById(1L)).thenReturn(Optional.of(shipment))
        `when`(shipmentStateRepository.findById(2L)).thenReturn(Optional.of(updatedState))
        `when`(shipmentRepository.save(any())).thenReturn(updatedShipment)

        val response = shipmentService.update(1L, updateRequest)

        assertNotNull(response)
        assertEquals("DHL", response.carrier)
        assertEquals(false, response.isCashOnDelivery)
        verify(shipmentRepository).findById(1L)
        verify(shipmentStateRepository).findById(2L)
    }

    @Test
    fun `update should throw IllegalArgumentException when shipment not found`() {
        `when`(shipmentRepository.findById(999L)).thenReturn(Optional.empty())

        val exception = assertThrows(IllegalArgumentException::class.java) {
            shipmentService.update(999L, updateRequest)
        }

        assertEquals("Shipment with id 999 not found", exception.message)
        verify(shipmentRepository).findById(999L)
    }

    @Test
    fun `delete should remove shipment when exists`() {
        `when`(shipmentRepository.existsById(1L)).thenReturn(true)

        shipmentService.delete(1L)

        verify(shipmentRepository).existsById(1L)
        verify(shipmentRepository).deleteById(1L)
    }

    @Test
    fun `delete should throw IllegalArgumentException when not found`() {
        `when`(shipmentRepository.existsById(999L)).thenReturn(false)

        val exception = assertThrows(IllegalArgumentException::class.java) {
            shipmentService.delete(999L)
        }

        assertEquals("Shipment with id 999 not found", exception.message)
        verify(shipmentRepository).existsById(999L)
    }
}
