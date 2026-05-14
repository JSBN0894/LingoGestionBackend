package com.linogo.gestion.clientaddress

import com.fasterxml.jackson.databind.ObjectMapper
import com.linogo.gestion.clientaddress.application.ClientAddressService
import com.linogo.gestion.clientaddress.application.CreateClientAddressRequest
import com.linogo.gestion.clientaddress.application.UpdateClientAddressRequest
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

@WebMvcTest(com.linogo.gestion.clientaddress.infrastructure.ClientAddressController::class)
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
@org.springframework.test.context.ActiveProfiles("test")
class ClientAddressControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var clientAddressService: ClientAddressService

    @MockBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    @MockBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `should create client address when request is valid`() {
        val request = CreateClientAddressRequest(clientId = "user_123", address = "Av. Corrientes 1234", city = "Buenos Aires")

        mockMvc.perform(post("/api/client-addresses")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated)
    }

    @Test
    fun `should return addresses by client id`() {
        org.mockito.Mockito.`when`(clientAddressService.findByClientId("user_123")).thenReturn(emptyList())

        mockMvc.perform(get("/api/client-addresses/client/user_123"))
            .andExpect(status().isOk)
    }

    @Test
    fun `should return all client addresses`() {
        org.mockito.Mockito.`when`(clientAddressService.findAll()).thenReturn(emptyList())

        mockMvc.perform(get("/api/client-addresses"))
            .andExpect(status().isOk)
    }

    @Test
    fun `should update client address when request is valid`() {
        val request = UpdateClientAddressRequest(address = "Calle Falsa 123", city = "Rosario")

        mockMvc.perform(put("/api/client-addresses/1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk)
    }

    @Test
    fun `should delete client address when exists`() {
        mockMvc.perform(delete("/api/client-addresses/1"))
            .andExpect(status().isNoContent)
    }
}
