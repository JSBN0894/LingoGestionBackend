package com.linogo.gestion.shipmentstate.application

import com.linogo.gestion.shipmentstate.domain.ShipmentState
import com.linogo.gestion.shipmentstate.infrastructure.ShipmentStateRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class ShipmentStateService(
    private val repository: ShipmentStateRepository
) {

    @Transactional
    fun create(request: CreateShipmentStateRequest): ShipmentStateResponse {
        val shipmentState = ShipmentState(
            id = request.id,
            state = request.state,
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
            id = this.id,
            stateId = this.state.id,
            stateName = this.state.name,
            name = this.name,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }
}

data class CreateShipmentStateRequest(
    val id: Long,
    val stateId: Long,
    val name: String
) {
    lateinit var state: com.linogo.gestion.state.domain.State
        private set

    fun setState(state: com.linogo.gestion.state.domain.State) {
        this.state = state
    }
}

data class ShipmentStateResponse(
    val id: Long,
    val stateId: Long,
    val stateName: String,
    val name: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
