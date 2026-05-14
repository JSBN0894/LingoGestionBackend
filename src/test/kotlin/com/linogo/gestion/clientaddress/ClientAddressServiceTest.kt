package com.linogo.gestion.clientaddress

import com.linogo.gestion.client.domain.Client
import com.linogo.gestion.client.infrastructure.ClientRepository
import com.linogo.gestion.clientaddress.application.ClientAddressService
import com.linogo.gestion.clientaddress.application.CreateClientAddressRequest
import com.linogo.gestion.clientaddress.application.UpdateClientAddressRequest
import com.linogo.gestion.clientaddress.domain.ClientAddress
import com.linogo.gestion.clientaddress.infrastructure.ClientAddressRepository
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
class ClientAddressServiceTest {

    @Mock
    private lateinit var clientAddressRepository: ClientAddressRepository

    @Mock
    private lateinit var clientRepository: ClientRepository

    @InjectMocks
    private lateinit var clientAddressService: ClientAddressService

    private lateinit var client: Client
    private lateinit var clientAddress: ClientAddress
    private lateinit var createRequest: CreateClientAddressRequest
    private lateinit var updateRequest: UpdateClientAddressRequest

    @BeforeEach
    fun setUp() {
        client = Client(idUser = "user_123", name = "Juan Pérez",
            createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now())

        clientAddress = ClientAddress(id = 1L, client = client, address = "Av. Corrientes 1234", city = "Buenos Aires",
            createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now())

        createRequest = CreateClientAddressRequest(clientId = "user_123", address = "Av. Corrientes 1234", city = "Buenos Aires")

        updateRequest = UpdateClientAddressRequest(address = "Calle Falsa 123", city = "Rosario")
    }

    @Test
    fun `create should save client address when data is valid`() {
        `when`(clientRepository.findById("user_123")).thenReturn(Optional.of(client))
        `when`(clientAddressRepository.save(any())).thenReturn(clientAddress)

        val response = clientAddressService.create(createRequest)

        assertNotNull(response)
        assertEquals("user_123", response.clientId)
        assertEquals("Av. Corrientes 1234", response.address)
        verify(clientRepository).findById("user_123")
        verify(clientAddressRepository).save(any())
    }

    @Test
    fun `create should throw IllegalArgumentException when client not found`() {
        `when`(clientRepository.findById("user_not_found")).thenReturn(Optional.empty())

        val request = createRequest.copy(clientId = "user_not_found")
        val exception = assertThrows(IllegalArgumentException::class.java) {
            clientAddressService.create(request)
        }

        assertEquals("Client with id user_not_found not found", exception.message)
    }

    @Test
    fun `findByClientId should return list of ClientAddressResponse`() {
        `when`(clientAddressRepository.findByClientId("user_123")).thenReturn(listOf(clientAddress))

        val responses = clientAddressService.findByClientId("user_123")

        assertEquals(1, responses.size)
        assertEquals("Av. Corrientes 1234", responses[0].address)
        verify(clientAddressRepository).findByClientId("user_123")
    }

    @Test
    fun `findByClientId should return empty list when no addresses`() {
        `when`(clientAddressRepository.findByClientId("user_not_found")).thenReturn(emptyList())

        val responses = clientAddressService.findByClientId("user_not_found")

        assertEquals(0, responses.size)
        verify(clientAddressRepository).findByClientId("user_not_found")
    }

    @Test
    fun `findAll should return list of ClientAddressResponse`() {
        `when`(clientAddressRepository.findAll()).thenReturn(listOf(clientAddress))

        val responses = clientAddressService.findAll()

        assertEquals(1, responses.size)
        verify(clientAddressRepository).findAll()
    }

    @Test
    fun `findAll should return empty list when no addresses exist`() {
        `when`(clientAddressRepository.findAll()).thenReturn(emptyList())

        val responses = clientAddressService.findAll()

        assertEquals(0, responses.size)
        verify(clientAddressRepository).findAll()
    }

    @Test
    fun `update should return updated ClientAddressResponse when exists`() {
        val updatedAddress = clientAddress.copy(address = "Calle Falsa 123", city = "Rosario")
        `when`(clientAddressRepository.findById(1L)).thenReturn(Optional.of(clientAddress))
        `when`(clientAddressRepository.save(any())).thenReturn(updatedAddress)

        val response = clientAddressService.update(1L, updateRequest)

        assertNotNull(response)
        assertEquals("Calle Falsa 123", response.address)
        assertEquals("Rosario", response.city)
        verify(clientAddressRepository).findById(1L)
        verify(clientAddressRepository).save(any())
    }

    @Test
    fun `update should throw IllegalArgumentException when not found`() {
        `when`(clientAddressRepository.findById(999L)).thenReturn(Optional.empty())

        val exception = assertThrows(IllegalArgumentException::class.java) {
            clientAddressService.update(999L, updateRequest)
        }

        assertEquals("ClientAddress with id 999 not found", exception.message)
    }

    @Test
    fun `delete should remove client address when exists`() {
        `when`(clientAddressRepository.existsById(1L)).thenReturn(true)

        clientAddressService.delete(1L)

        verify(clientAddressRepository).existsById(1L)
        verify(clientAddressRepository).deleteById(1L)
    }

    @Test
    fun `delete should throw IllegalArgumentException when not found`() {
        `when`(clientAddressRepository.existsById(999L)).thenReturn(false)

        val exception = assertThrows(IllegalArgumentException::class.java) {
            clientAddressService.delete(999L)
        }

        assertEquals("ClientAddress with id 999 not found", exception.message)
        verify(clientAddressRepository).existsById(999L)
    }
}
