package com.linogo.gestion.shipmentstate

import com.fasterxml.jackson.databind.ObjectMapper
import com.linogo.gestion.shipmentstate.application.ShipmentStateService
import com.linogo.gestion.security.infrastructure.JwtAuthenticationFilter
import com.linogo.gestion.security.infrastructure.JwtTokenProvider
import com.linogo.gestion.state.infrastructure.StateRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest(com.linogo.gestion.shipmentstate.infrastructure.ShipmentStateController::class)
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
@org.springframework.test.context.ActiveProfiles("test")
class ShipmentStateControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var service: ShipmentStateService

    @MockBean
    private lateinit var stateRepository: StateRepository

    @MockBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    @MockBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `should create shipment state when request is valid`() {
        val request = mapOf("id" to 1, "stateId" to 1, "name" to "En espera")
        org.mockito.Mockito.`when`(stateRepository.findById(1L))
            .thenReturn(java.util.Optional.of(
                com.linogo.gestion.state.domain.State(id = 1L, name = "Pendiente", priority = 1)
            ))

        mockMvc.perform(post("/api/shipment-states")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated)
    }

    @Test
    fun `should return all shipment states`() {
        org.mockito.Mockito.`when`(service.findAll()).thenReturn(emptyList())

        mockMvc.perform(get("/api/shipment-states"))
            .andExpect(status().isOk)
    }

    @Test
    fun `should delete shipment state when exists`() {
        mockMvc.perform(delete("/api/shipment-states/1"))
            .andExpect(status().isNoContent)
    }
}
