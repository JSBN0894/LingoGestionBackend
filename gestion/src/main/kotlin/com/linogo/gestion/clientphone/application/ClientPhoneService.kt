package com.linogo.gestion.clientphone.application

import com.linogo.gestion.client.infrastructure.ClientRepository
import com.linogo.gestion.clientphone.domain.ClientPhone
import com.linogo.gestion.clientphone.infrastructure.ClientPhoneRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class ClientPhoneService(
    private val clientPhoneRepository: ClientPhoneRepository,
    private val clientRepository: ClientRepository
) {

    @Transactional
    fun create(request: CreateClientPhoneRequest): ClientPhoneResponse {
        val client = clientRepository.findById(request.clientId)
            .orElseThrow { IllegalArgumentException("Client with id ${request.clientId} not found") }

        val clientPhone = ClientPhone(
            client = client,
            phone = request.phone,
            updatedAt = LocalDateTime.now()
        )

        return clientPhoneRepository.save(clientPhone).toResponse()
    }

    @Transactional(readOnly = true)
    fun findByClientId(clientId: String): List<ClientPhoneResponse> {
        return clientPhoneRepository.findByClientId(clientId).map { it.toResponse() }
    }

    @Transactional(readOnly = true)
    fun findAll(): List<ClientPhoneResponse> {
        return clientPhoneRepository.findAll().map { it.toResponse() }
    }

    @Transactional
    fun update(id: Long, request: UpdateClientPhoneRequest): ClientPhoneResponse {
        val clientPhone = clientPhoneRepository.findById(id)
            .orElseThrow { IllegalArgumentException("ClientPhone with id $id not found") }

        val updated = clientPhone.copy(
            phone = request.phone,
            updatedAt = LocalDateTime.now()
        )

        return clientPhoneRepository.save(updated).toResponse()
    }

    @Transactional
    fun delete(id: Long) {
        if (!clientPhoneRepository.existsById(id)) {
            throw IllegalArgumentException("ClientPhone with id $id not found")
        }
        clientPhoneRepository.deleteById(id)
    }

    private fun ClientPhone.toResponse(): ClientPhoneResponse {
        return ClientPhoneResponse(
            id = this.id,
            clientId = this.client.idUser,
            clientName = this.client.name,
            phone = this.phone,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }
}

data class CreateClientPhoneRequest(
    val clientId: String,
    val phone: String
)

data class UpdateClientPhoneRequest(
    val phone: String
)

data class ClientPhoneResponse(
    val id: Long,
    val clientId: String,
    val clientName: String,
    val phone: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
