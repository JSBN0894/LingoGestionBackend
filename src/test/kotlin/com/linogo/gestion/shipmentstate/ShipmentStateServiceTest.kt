package com.linogo.gestion.shipmentstate

import com.linogo.gestion.shipmentstate.application.CreateShipmentStateRequest
import com.linogo.gestion.shipmentstate.application.ShipmentStateService
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

@ExtendWith(MockitoExtension::class)
class ShipmentStateServiceTest {

    @Mock
    private lateinit var repository: ShipmentStateRepository

    @InjectMocks
    private lateinit var shipmentStateService: ShipmentStateService

    private lateinit var state: State
    private lateinit var shipmentState: ShipmentState
    private lateinit var createRequest: CreateShipmentStateRequest

    @BeforeEach
    fun setUp() {
        state = State(id = 1L, name = "Pendiente", priority = 1,
            createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now())

        shipmentState = ShipmentState(id = 1L, state = state, name = "En espera",
            createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now())

        createRequest = CreateShipmentStateRequest(id = 1L, stateId = 1L, name = "En espera")
        createRequest.setState(state)
    }

    @Test
    fun `create should save shipment state when data is valid`() {
        `when`(repository.save(any())).thenReturn(shipmentState)

        val response = shipmentStateService.create(createRequest)

        assertNotNull(response)
        assertEquals(1L, response.id)
        assertEquals("En espera", response.name)
        verify(repository).save(any())
    }

    @Test
    fun `findAll should return list of ShipmentStateResponse`() {
        `when`(repository.findAll()).thenReturn(listOf(shipmentState))

        val responses = shipmentStateService.findAll()

        assertEquals(1, responses.size)
        assertEquals("En espera", responses[0].name)
        verify(repository).findAll()
    }

    @Test
    fun `findAll should return empty list when none exist`() {
        `when`(repository.findAll()).thenReturn(emptyList())

        val responses = shipmentStateService.findAll()

        assertEquals(0, responses.size)
        verify(repository).findAll()
    }

    @Test
    fun `delete should remove shipment state when exists`() {
        `when`(repository.existsById(1L)).thenReturn(true)

        shipmentStateService.delete(1L)

        verify(repository).existsById(1L)
        verify(repository).deleteById(1L)
    }

    @Test
    fun `delete should throw IllegalArgumentException when not found`() {
        `when`(repository.existsById(999L)).thenReturn(false)

        val exception = assertThrows(IllegalArgumentException::class.java) {
            shipmentStateService.delete(999L)
        }

        assertEquals("ShipmentState with id 999 not found", exception.message)
        verify(repository).existsById(999L)
    }
}
