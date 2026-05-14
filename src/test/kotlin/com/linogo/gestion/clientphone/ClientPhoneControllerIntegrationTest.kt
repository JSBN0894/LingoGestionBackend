package com.linogo.gestion.clientphone

import com.fasterxml.jackson.databind.ObjectMapper
import com.linogo.gestion.clientphone.application.ClientPhoneService
import com.linogo.gestion.clientphone.application.CreateClientPhoneRequest
import com.linogo.gestion.clientphone.application.UpdateClientPhoneRequest
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

@WebMvcTest(com.linogo.gestion.clientphone.infrastructure.ClientPhoneController::class)
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
@org.springframework.test.context.ActiveProfiles("test")
class ClientPhoneControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var clientPhoneService: ClientPhoneService

    @MockBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    @MockBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `should create client phone when request is valid`() {
        val request = CreateClientPhoneRequest(clientId = "user_123", phone = "+54911223344")

        mockMvc.perform(post("/api/client-phones")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated)
    }

    @Test
    fun `should return phones by client id`() {
        org.mockito.Mockito.`when`(clientPhoneService.findByClientId("user_123")).thenReturn(emptyList())

        mockMvc.perform(get("/api/client-phones/client/user_123"))
            .andExpect(status().isOk)
    }

    @Test
    fun `should return all client phones`() {
        org.mockito.Mockito.`when`(clientPhoneService.findAll()).thenReturn(emptyList())

        mockMvc.perform(get("/api/client-phones"))
            .andExpect(status().isOk)
    }

    @Test
    fun `should update client phone when request is valid`() {
        val request = UpdateClientPhoneRequest(phone = "+54911998877")

        mockMvc.perform(put("/api/client-phones/1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk)
    }

    @Test
    fun `should delete client phone when exists`() {
        mockMvc.perform(delete("/api/client-phones/1"))
            .andExpect(status().isNoContent)
    }
}
