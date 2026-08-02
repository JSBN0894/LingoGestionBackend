package com.linogo.gestion.shipmentstate.application

import com.linogo.gestion.exception.NotFoundException
import com.linogo.gestion.shipmentstate.domain.ShipmentState
import com.linogo.gestion.shipmentstate.infrastructure.ShipmentStateRepository
import com.linogo.gestion.state.infrastructure.StateRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class ShipmentStateService(
    private val repository: ShipmentStateRepository,
    private val stateRepository: StateRepository
) {

    @Transactional
    fun create(request: CreateShipmentStateRequest): ShipmentStateResponse {
        val state = stateRepository.findById(request.stateId)
            .orElseThrow { NotFoundException("State", request.stateId) }

        val shipmentState = ShipmentState(
            state = state,
            name = request.name,
            updatedAt = LocalDateTime.now()
        )
        return repository.save(shipmentState).toResponse()
    }

    @Transactional(readOnly = true)
    fun findAll(): List<ShipmentStateResponse> {
        return repository.findAll().map { it.toResponse() }
    }

    @Transactional
    fun delete(id: Long) {
        if (!repository.existsById(id)) {
            throw IllegalArgumentException("ShipmentState with id $id not found")
        }
        repository.deleteById(id)
    }

    private fun ShipmentState.toResponse(): ShipmentStateResponse {
        return ShipmentStateResponse(
            id = this.id!!,
            stateId = this.state.id!!,
            stateName = this.state.name,
            name = this.name,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }
}

data class CreateShipmentStateRequest(
    val stateId: Long,
    val name: String
)

data class ShipmentStateResponse(
    val id: Long,
    val stateId: Long,
    val stateName: String,
    val name: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
