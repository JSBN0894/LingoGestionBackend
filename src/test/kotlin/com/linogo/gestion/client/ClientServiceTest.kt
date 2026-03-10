package com.linogo.gestion.client

import com.linogo.gestion.client.application.ClientResponse
import com.linogo.gestion.client.application.ClientService
import com.linogo.gestion.client.application.CreateClientRequest
import com.linogo.gestion.client.application.UpdateClientRequest
import com.linogo.gestion.client.domain.Client
import com.linogo.gestion.client.infrastructure.ClientRepository
import com.linogo.gestion.exception.AlreadyExistsException
import com.linogo.gestion.exception.NotFoundException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDateTime
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class ClientServiceTest {

    @Mock
    private lateinit var clientRepository: ClientRepository

    @InjectMocks
    private lateinit var clientService: ClientService

    private lateinit var existingClient: Client
    private lateinit var createRequest: CreateClientRequest
    private lateinit var updateRequest: UpdateClientRequest

    @BeforeEach
    fun setUp() {
        existingClient = Client(
            idUser = "user_123",
            name = "Juan Pérez",
            defaultPhone = "+54911223344",
            defaultCity = "Buenos Aires",
            defaultAddress = "Av. Corrientes 1234",
            createdAt = LocalDateTime.now().minusDays(1),
            updatedAt = LocalDateTime.now()
        )

        createRequest = CreateClientRequest(
            idUser = "user_456",
            name = "María García",
            defaultPhone = "+54911556677",
            defaultCity = "Córdoba",
            defaultAddress = "Calle Falsa 123"
        )

        updateRequest = UpdateClientRequest(
            name = "Juan Pérez Actualizado",
            defaultPhone = "+54911998877",
            defaultCity = "Rosario",
            defaultAddress = "San Martín 567"
        )
    }

    @Test
    fun `create should save client when data is valid`() {
        // Given
        val savedClient = existingClient.copy(idUser = createRequest.idUser, name = createRequest.name)
        `when`(clientRepository.existsById(createRequest.idUser)).thenReturn(false)
        `when`(clientRepository.save(org.mockito.ArgumentMatchers.any())).thenReturn(savedClient)

        // When
        val response = clientService.create(createRequest)

        // Then
        assertNotNull(response)
        assertEquals(createRequest.idUser, response.idUser)
        assertEquals(createRequest.name, response.name)
        verify(clientRepository).existsById(createRequest.idUser)
        verify(clientRepository).save(org.mockito.ArgumentMatchers.any())
    }

    @Test
    fun `create should throw AlreadyExistsException when client already exists`() {
        // Given
        `when`(clientRepository.existsById(createRequest.idUser)).thenReturn(true)

        // When & Then
        val exception = assertThrows(AlreadyExistsException::class.java) {
            clientService.create(createRequest)
        }

        assertEquals("Client con idUser 'user_456' ya existe", exception.message)
        verify(clientRepository).existsById(createRequest.idUser)
        org.mockito.Mockito.verifyNoMoreInteractions(clientRepository)
    }

    @Test
    fun `findById should return ClientResponse when client exists`() {
        // Given
        `when`(clientRepository.findById("user_123")).thenReturn(Optional.of(existingClient))

        // When
        val response = clientService.findById("user_123")

        // Then
        assertNotNull(response)
        assertEquals(existingClient.idUser, response.idUser)
        assertEquals(existingClient.name, response.name)
        verify(clientRepository).findById("user_123")
    }

    @Test
    fun `findById should throw NotFoundException when client does not exist`() {
        // Given
        `when`(clientRepository.findById("user_not_found")).thenReturn(Optional.empty())

        // When & Then
        val exception = assertThrows(NotFoundException::class.java) {
            clientService.findById("user_not_found")
        }

        assertEquals("Client con id 'user_not_found' no encontrado", exception.message)
        verify(clientRepository).findById("user_not_found")
    }

    @Test
    fun `findAll should return list of ClientResponse`() {
        // Given
        val clients = listOf(existingClient)
        `when`(clientRepository.findAll()).thenReturn(clients)

        // When
        val responses = clientService.findAll()

        // Then
        assertEquals(1, responses.size)
        assertEquals(existingClient.idUser, responses[0].idUser)
        verify(clientRepository).findAll()
    }

    @Test
    fun `findAll should return empty list when no clients exist`() {
        // Given
        `when`(clientRepository.findAll()).thenReturn(emptyList())

        // When
        val responses = clientService.findAll()

        // Then
        assertEquals(0, responses.size)
        verify(clientRepository).findAll()
    }

    @Test
    fun `update should return updated ClientResponse when client exists`() {
        // Given
        val updatedClient = existingClient.copy(
            name = updateRequest.name,
            defaultPhone = updateRequest.defaultPhone,
            defaultCity = updateRequest.defaultCity,
            defaultAddress = updateRequest.defaultAddress
        )
        `when`(clientRepository.findById("user_123")).thenReturn(Optional.of(existingClient))
        `when`(clientRepository.save(org.mockito.ArgumentMatchers.any())).thenReturn(updatedClient)

        // When
        val response = clientService.update("user_123", updateRequest)

        // Then
        assertNotNull(response)
        assertEquals(updateRequest.name, response.name)
        assertEquals(updateRequest.defaultPhone, response.defaultPhone)
        verify(clientRepository).findById("user_123")
        verify(clientRepository).save(org.mockito.ArgumentMatchers.any())
    }

    @Test
    fun `update should throw NotFoundException when client does not exist`() {
        // Given
        `when`(clientRepository.findById("user_not_found")).thenReturn(Optional.empty())

        // When & Then
        val exception = assertThrows(NotFoundException::class.java) {
            clientService.update("user_not_found", updateRequest)
        }

        assertEquals("Client con id 'user_not_found' no encontrado", exception.message)
        verify(clientRepository).findById("user_not_found")
    }

    @Test
    fun `delete should remove client when client exists`() {
        // Given
        `when`(clientRepository.existsById("user_123")).thenReturn(true)

        // When
        clientService.delete("user_123")

        // Then
        verify(clientRepository).existsById("user_123")
        verify(clientRepository).deleteById("user_123")
    }

    @Test
    fun `delete should throw NotFoundException when client does not exist`() {
        // Given
        `when`(clientRepository.existsById("user_not_found")).thenReturn(false)

        // When & Then
        val exception = assertThrows(NotFoundException::class.java) {
            clientService.delete("user_not_found")
        }

        assertEquals("Client con id 'user_not_found' no encontrado", exception.message)
        verify(clientRepository).existsById("user_not_found")
    }
}
