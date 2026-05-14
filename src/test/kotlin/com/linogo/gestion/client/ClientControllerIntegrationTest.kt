package com.linogo.gestion.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.linogo.gestion.client.application.ClientService
import com.linogo.gestion.client.application.CreateClientRequest
import com.linogo.gestion.client.application.UpdateClientRequest
import com.linogo.gestion.exception.NotFoundException
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.LocalDateTime

@WebMvcTest(com.linogo.gestion.client.infrastructure.ClientController::class)
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
@org.springframework.test.context.ActiveProfiles("test")
class ClientControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var clientService: ClientService

    @MockBean
    private lateinit var jwtAuthenticationFilter: com.linogo.gestion.security.infrastructure.JwtAuthenticationFilter

    @MockBean
    private lateinit var jwtTokenProvider: com.linogo.gestion.security.infrastructure.JwtTokenProvider

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `should create client when request is valid`() {
        // Given
        val request = CreateClientRequest(
            idUser = "user_123",
            name = "Juan Pérez",
            defaultPhone = "+54911223344",
            defaultCity = "Buenos Aires",
            defaultAddress = "Av. Corrientes 1234"
        )

        // When & Then
        mockMvc.perform(post("/api/clients")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated)
    }

    @Test
    fun `should return bad request when client name is blank`() {
        // Given
        val request = mapOf(
            "idUser" to "user_123",
            "name" to ""
        )

        // When & Then
        mockMvc.perform(post("/api/clients")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should return not found when client does not exist`() {
        // Given
        val clientId = "user_not_found"
        org.mockito.Mockito.`when`(clientService.findById(clientId))
            .thenThrow(NotFoundException("Client", clientId))

        // When & Then
        mockMvc.perform(get("/api/clients/{idUser}", clientId))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should return all clients`() {
        org.mockito.Mockito.`when`(clientService.findAll()).thenReturn(emptyList())
        // When & Then
        mockMvc.perform(get("/api/clients"))
            .andExpect(status().isOk)
    }

    @Test
    fun `should update client when client exists`() {
        // Given
        val request = UpdateClientRequest(
            name = "Juan Pérez Actualizado",
            defaultPhone = "+54911998877",
            defaultCity = "Rosario",
            defaultAddress = "San Martín 567"
        )
        org.mockito.Mockito.`when`(clientService.update(any(), any()))
            .thenReturn(com.linogo.gestion.client.application.ClientResponse("user_123", request.name, request.defaultPhone, request.defaultCity, request.defaultAddress, LocalDateTime.now(), LocalDateTime.now()))

        // When & Then
        mockMvc.perform(put("/api/clients/{idUser}", "user_123")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk)
    }

    @Test
    fun `should return not found when updating non-existent client`() {
        // Given
        val clientId = "user_not_found"
        val request = UpdateClientRequest(
            name = "Nombre Actualizado",
            defaultPhone = null,
            defaultCity = null,
            defaultAddress = null
        )
        org.mockito.Mockito.`when`(clientService.update(org.mockito.kotlin.eq(clientId), any()))
            .thenThrow(NotFoundException("Client", clientId))

        // When & Then
        mockMvc.perform(put("/api/clients/{idUser}", clientId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should delete client when client exists`() {
        // When & Then
        mockMvc.perform(delete("/api/clients/{idUser}", "user_123"))
            .andExpect(status().isNoContent)
    }

    @Test
    fun `should return not found when deleting non-existent client`() {
        // Given
        val clientId = "user_not_found"
        org.mockito.Mockito.doThrow(NotFoundException("Client", clientId))
            .`when`(clientService).delete(clientId)

        // When & Then
        mockMvc.perform(delete("/api/clients/{idUser}", clientId))
            .andExpect(status().isNotFound)
    }
}
