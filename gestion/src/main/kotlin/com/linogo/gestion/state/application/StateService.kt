package com.linogo.gestion.state.application

import com.linogo.gestion.state.domain.State
import com.linogo.gestion.state.infrastructure.StateRepository
import jakarta.validation.constraints.NotBlank
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class StateService(
    private val stateRepository: StateRepository
) {

    @Transactional
    fun create(request: CreateStateRequest): StateResponse {
        val parent = request.parentId?.let { id ->
            stateRepository.findById(id)
                .orElseThrow { IllegalArgumentException("Estado padre con id $id no encontrado") }
        }

        val state = State(
            parent = parent,
            name = request.name,
            description = request.description,
            priority = request.priority
        )
        val saved = stateRepository.save(state)
        return saved.toResponse()
    }

    @Transactional(readOnly = true)
    fun findById(id: Long): StateResponse {
        val state = stateRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Estado con id $id no encontrado") }
        return state.toResponse()
    }

    @Transactional(readOnly = true)
    fun findAll(): List<StateResponse> {
        return stateRepository.findAll().map { it.toResponse() }
    }

    @Transactional(readOnly = true)
    fun findByParentId(parentId: Long?): List<StateResponse> {
        return stateRepository.findByParentId(parentId).map { it.toResponse() }
    }

    @Transactional(readOnly = true)
    fun findRootStates(): List<StateResponse> {
        return stateRepository.findByParentId(null).map { it.toResponse() }
    }

    @Transactional
    fun update(id: Long, request: UpdateStateRequest): StateResponse {
        val state = stateRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Estado con id $id no encontrado") }

        val parent = request.parentId?.let { pId ->
            stateRepository.findById(pId)
                .orElseThrow { IllegalArgumentException("Estado padre con id $pId no encontrado") }
        }

        state.apply {
            this.parent = parent
            this.name = request.name
            this.description = request.description
            this.priority = request.priority
            this.updatedAt = LocalDateTime.now()
        }

        val saved = stateRepository.save(state)
        return saved.toResponse()
    }

    @Transactional
    fun delete(id: Long) {
        if (!stateRepository.existsById(id)) {
            throw IllegalArgumentException("Estado con id $id no encontrado")
        }
        stateRepository.deleteById(id)
    }

    private fun State.toResponse(): StateResponse {
        return StateResponse(
            id = this.id!!,
            parentId = this.parent?.id,
            name = this.name,
            description = this.description,
            priority = this.priority,
            hasChildren = this.children.isNotEmpty(),
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }
}

data class CreateStateRequest(
    @field:NotBlank(message = "El nombre es obligatorio")
    val name: String,
    val description: String? = null,
    val parentId: Long? = null,
    val priority: Int = 0
)

data class UpdateStateRequest(
    @field:NotBlank(message = "El nombre es obligatorio")
    val name: String,
    val description: String? = null,
    val parentId: Long? = null,
    val priority: Int = 0
)

data class StateResponse(
    val id: Long,
    val parentId: Long?,
    val name: String,
    val description: String?,
    val priority: Int,
    val hasChildren: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
