package com.linogo.gestion.clientaddress.application

import com.linogo.gestion.client.infrastructure.ClientRepository
import com.linogo.gestion.clientaddress.domain.ClientAddress
import com.linogo.gestion.clientaddress.infrastructure.ClientAddressRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class ClientAddressService(
    private val clientAddressRepository: ClientAddressRepository,
    private val clientRepository: ClientRepository
) {

    @Transactional
    fun create(request: CreateClientAddressRequest): ClientAddressResponse {
        val client = clientRepository.findById(request.clientId)
            .orElseThrow { IllegalArgumentException("Client with id ${request.clientId} not found") }

        val clientAddress = ClientAddress(
            client = client,
            address = request.address,
            city = request.city,
            updatedAt = LocalDateTime.now()
        )

        return clientAddressRepository.save(clientAddress).toResponse()
    }

    @Transactional(readOnly = true)
    fun findByClientId(clientId: String): List<ClientAddressResponse> {
        return clientAddressRepository.findByClientId(clientId).map { it.toResponse() }
    }

    @Transactional(readOnly = true)
    fun findAll(): List<ClientAddressResponse> {
        return clientAddressRepository.findAll().map { it.toResponse() }
    }

    @Transactional
    fun update(id: Long, request: UpdateClientAddressRequest): ClientAddressResponse {
        val clientAddress = clientAddressRepository.findById(id)
            .orElseThrow { IllegalArgumentException("ClientAddress with id $id not found") }

        val updated = clientAddress.copy(
            address = request.address,
            city = request.city,
            updatedAt = LocalDateTime.now()
        )

        return clientAddressRepository.save(updated).toResponse()
    }

    @Transactional
    fun delete(id: Long) {
        if (!clientAddressRepository.existsById(id)) {
            throw IllegalArgumentException("ClientAddress with id $id not found")
        }
        clientAddressRepository.deleteById(id)
    }

    private fun ClientAddress.toResponse(): ClientAddressResponse {
        return ClientAddressResponse(
            id = this.id!!,
            clientId = this.client.idUser,
            clientName = this.client.name,
            address = this.address,
            city = this.city,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }
}

data class CreateClientAddressRequest(
    val clientId: String,
    val address: String,
    val city: String
)

data class UpdateClientAddressRequest(
    val address: String,
    val city: String
)

data class ClientAddressResponse(
    val id: Long,
    val clientId: String,
    val clientName: String,
    val address: String,
    val city: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
