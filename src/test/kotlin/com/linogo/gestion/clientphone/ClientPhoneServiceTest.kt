package com.linogo.gestion.clientphone

import com.linogo.gestion.client.domain.Client
import com.linogo.gestion.client.infrastructure.ClientRepository
import com.linogo.gestion.clientphone.application.ClientPhoneService
import com.linogo.gestion.clientphone.application.CreateClientPhoneRequest
import com.linogo.gestion.clientphone.application.UpdateClientPhoneRequest
import com.linogo.gestion.clientphone.domain.ClientPhone
import com.linogo.gestion.clientphone.infrastructure.ClientPhoneRepository
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
class ClientPhoneServiceTest {

    @Mock
    private lateinit var clientPhoneRepository: ClientPhoneRepository

    @Mock
    private lateinit var clientRepository: ClientRepository

    @InjectMocks
    private lateinit var clientPhoneService: ClientPhoneService

    private lateinit var client: Client
    private lateinit var clientPhone: ClientPhone
    private lateinit var createRequest: CreateClientPhoneRequest
    private lateinit var updateRequest: UpdateClientPhoneRequest

    @BeforeEach
    fun setUp() {
        client = Client(idUser = "user_123", name = "Juan Pérez",
            createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now())

        clientPhone = ClientPhone(id = 1L, client = client, phone = "+54911223344",
            createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now())

        createRequest = CreateClientPhoneRequest(clientId = "user_123", phone = "+54911223344")

        updateRequest = UpdateClientPhoneRequest(phone = "+54911998877")
    }

    @Test
    fun `create should save client phone when data is valid`() {
        `when`(clientRepository.findById("user_123")).thenReturn(Optional.of(client))
        `when`(clientPhoneRepository.save(any())).thenReturn(clientPhone)

        val response = clientPhoneService.create(createRequest)

        assertNotNull(response)
        assertEquals("user_123", response.clientId)
        assertEquals("+54911223344", response.phone)
        verify(clientRepository).findById("user_123")
        verify(clientPhoneRepository).save(any())
    }

    @Test
    fun `create should throw IllegalArgumentException when client not found`() {
        `when`(clientRepository.findById("user_not_found")).thenReturn(Optional.empty())

        val request = createRequest.copy(clientId = "user_not_found")
        val exception = assertThrows(IllegalArgumentException::class.java) {
            clientPhoneService.create(request)
        }

        assertEquals("Client with id user_not_found not found", exception.message)
    }

    @Test
    fun `findByClientId should return list of ClientPhoneResponse`() {
        `when`(clientPhoneRepository.findByClientId("user_123")).thenReturn(listOf(clientPhone))

        val responses = clientPhoneService.findByClientId("user_123")

        assertEquals(1, responses.size)
        assertEquals("+54911223344", responses[0].phone)
        verify(clientPhoneRepository).findByClientId("user_123")
    }

    @Test
    fun `findByClientId should return empty list when no phones`() {
        `when`(clientPhoneRepository.findByClientId("user_not_found")).thenReturn(emptyList())

        val responses = clientPhoneService.findByClientId("user_not_found")

        assertEquals(0, responses.size)
        verify(clientPhoneRepository).findByClientId("user_not_found")
    }

    @Test
    fun `findAll should return list of ClientPhoneResponse`() {
        `when`(clientPhoneRepository.findAll()).thenReturn(listOf(clientPhone))

        val responses = clientPhoneService.findAll()

        assertEquals(1, responses.size)
        verify(clientPhoneRepository).findAll()
    }

    @Test
    fun `findAll should return empty list when no client phones exist`() {
        `when`(clientPhoneRepository.findAll()).thenReturn(emptyList())

        val responses = clientPhoneService.findAll()

        assertEquals(0, responses.size)
        verify(clientPhoneRepository).findAll()
    }

    @Test
    fun `update should return updated ClientPhoneResponse when exists`() {
        val updatedPhone = clientPhone.copy(phone = "+54911998877")
        `when`(clientPhoneRepository.findById(1L)).thenReturn(Optional.of(clientPhone))
        `when`(clientPhoneRepository.save(any())).thenReturn(updatedPhone)

        val response = clientPhoneService.update(1L, updateRequest)

        assertNotNull(response)
        assertEquals("+54911998877", response.phone)
        verify(clientPhoneRepository).findById(1L)
        verify(clientPhoneRepository).save(any())
    }

    @Test
    fun `update should throw IllegalArgumentException when not found`() {
        `when`(clientPhoneRepository.findById(999L)).thenReturn(Optional.empty())

        val exception = assertThrows(IllegalArgumentException::class.java) {
            clientPhoneService.update(999L, updateRequest)
        }

        assertEquals("ClientPhone with id 999 not found", exception.message)
    }

    @Test
    fun `delete should remove client phone when exists`() {
        `when`(clientPhoneRepository.existsById(1L)).thenReturn(true)

        clientPhoneService.delete(1L)

        verify(clientPhoneRepository).existsById(1L)
        verify(clientPhoneRepository).deleteById(1L)
    }

    @Test
    fun `delete should throw IllegalArgumentException when not found`() {
        `when`(clientPhoneRepository.existsById(999L)).thenReturn(false)

        val exception = assertThrows(IllegalArgumentException::class.java) {
            clientPhoneService.delete(999L)
        }

        assertEquals("ClientPhone with id 999 not found", exception.message)
        verify(clientPhoneRepository).existsById(999L)
    }
}
