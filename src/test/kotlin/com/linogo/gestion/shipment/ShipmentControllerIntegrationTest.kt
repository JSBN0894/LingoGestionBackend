package com.linogo.gestion.shipment

import com.fasterxml.jackson.databind.ObjectMapper
import com.linogo.gestion.shipment.application.CreateShipmentRequest
import com.linogo.gestion.shipment.application.ShipmentService
import com.linogo.gestion.shipment.application.UpdateShipmentRequest
import com.linogo.gestion.security.infrastructure.JwtAuthenticationFilter
import com.linogo.gestion.security.infrastructure.JwtTokenProvider
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest(com.linogo.gestion.shipment.infrastructure.ShipmentController::class)
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
@org.springframework.test.context.ActiveProfiles("test")
class ShipmentControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var shipmentService: ShipmentService

    @MockBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    @MockBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `should create shipment when request is valid`() {
        val request = CreateShipmentRequest(
            orderId = 1L, shippingStateId = 1L, carrier = "Inter rapidisimo",
            isCashOnDelivery = true, shippingCost = 5000L, weight = 2000L)

        mockMvc.perform(post("/api/shipments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated)
    }

    @Test
    fun `should return shipment when exists`() {
        mockMvc.perform(get("/api/shipments/1"))
            .andExpect(status().isOk)
    }

    @Test
    fun `should return all shipments`() {
        org.mockito.Mockito.`when`(shipmentService.findAll()).thenReturn(emptyList())

        mockMvc.perform(get("/api/shipments"))
            .andExpect(status().isOk)
    }

    @Test
    fun `should update shipment when request is valid`() {
        val request = UpdateShipmentRequest(
            shippingStateId = 2L, carrier = "DHL", isCashOnDelivery = false)

        mockMvc.perform(put("/api/shipments/1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk)
    }

    @Test
    fun `should delete shipment when exists`() {
        mockMvc.perform(delete("/api/shipments/1"))
            .andExpect(status().isNoContent)
    }
}
