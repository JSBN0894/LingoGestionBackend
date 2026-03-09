package com.linogo.gestion.client.application

import com.linogo.gestion.client.domain.Client
import com.linogo.gestion.client.infrastructure.ClientRepository
import com.linogo.gestion.exception.AlreadyExistsException
import com.linogo.gestion.exception.NotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ClientService(
    private val clientRepository: ClientRepository
) {

    @Transactional
    fun create(request: CreateClientRequest): ClientResponse {
        if (clientRepository.existsById(request.idUser)) {
            throw AlreadyExistsException("Client", "idUser", request.idUser)
        }

        val client = Client(
            idUser = request.idUser,
            name = request.name,
            defaultPhone = request.defaultPhone,
            defaultCity = request.defaultCity,
            defaultAddress = request.defaultAddress
        )

        val saved = clientRepository.save(client)
        return saved.toResponse()
    }

    @Transactional(readOnly = true)
    fun findById(idUser: String): ClientResponse {
        val client = clientRepository.findById(idUser)
            .orElseThrow { NotFoundException("Client", idUser) }
        return client.toResponse()
    }

    @Transactional(readOnly = true)
    fun findAll(): List<ClientResponse> {
        return clientRepository.findAll().map { it.toResponse() }
    }

    @Transactional
    fun update(idUser: String, request: UpdateClientRequest): ClientResponse {
        val client = clientRepository.findById(idUser)
            .orElseThrow { NotFoundException("Client", idUser) }

        val updated = client.copy(
            name = request.name,
            defaultPhone = request.defaultPhone,
            defaultCity = request.defaultCity,
            defaultAddress = request.defaultAddress,
            updatedAt = java.time.LocalDateTime.now()
        )

        val saved = clientRepository.save(updated)
        return saved.toResponse()
    }

    @Transactional
    fun delete(idUser: String) {
        if (!clientRepository.existsById(idUser)) {
            throw NotFoundException("Client", idUser)
        }
        clientRepository.deleteById(idUser)
    }

    private fun Client.toResponse(): ClientResponse {
        return ClientResponse(
            idUser = this.idUser,
            name = this.name,
            defaultPhone = this.defaultPhone,
            defaultCity = this.defaultCity,
            defaultAddress = this.defaultAddress,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }
}
