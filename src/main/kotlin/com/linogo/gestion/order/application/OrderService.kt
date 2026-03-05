package com.linogo.gestion.order.application

import com.linogo.gestion.client.infrastructure.ClientRepository
import com.linogo.gestion.order.domain.Order
import com.linogo.gestion.order.infrastructure.OrderRepository
import com.linogo.gestion.state.infrastructure.StateRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val clientRepository: ClientRepository,
    private val stateRepository: StateRepository
) {

    @Transactional
    fun create(request: CreateOrderRequest): OrderResponse {
        val client = clientRepository.findById(request.clientId)
            .orElseThrow { IllegalArgumentException("Client with id ${request.clientId} not found") }

        val operationState = stateRepository.findById(request.operationStateId)
            .orElseThrow { IllegalArgumentException("State with id ${request.operationStateId} not found") }

        val order = Order(
            client = client,
            operationState = operationState,
            orderPrice = request.orderPrice,
            orderAddress = request.orderAddress,
            orderPhone = request.orderPhone,
            orderCity = request.orderCity
        )

        return orderRepository.save(order).toResponse()
    }

    @Transactional(readOnly = true)
    fun findById(id: Long): OrderResponse {
        val order = orderRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Order with id $id not found") }
        return order.toResponse()
    }

    @Transactional(readOnly = true)
    fun findAll(): List<OrderResponse> {
        return orderRepository.findAll().map { it.toResponse() }
    }

    @Transactional
    fun update(id: Long, request: UpdateOrderRequest): OrderResponse {
        val order = orderRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Order with id $id not found") }

        val operationState = stateRepository.findById(request.operationStateId)
            .orElseThrow { IllegalArgumentException("State with id ${request.operationStateId} not found") }

        val updated = order.copy(
            operationState = operationState,
            orderPrice = request.orderPrice,
            orderAddress = request.orderAddress,
            orderPhone = request.orderPhone,
            orderCity = request.orderCity,
            updatedAt = LocalDateTime.now()
        )

        return orderRepository.save(updated).toResponse()
    }

    @Transactional
    fun delete(id: Long) {
        if (!orderRepository.existsById(id)) {
            throw IllegalArgumentException("Order with id $id not found")
        }
        orderRepository.deleteById(id)
    }

    private fun Order.toResponse(): OrderResponse {
        return OrderResponse(
            id = this.id!!,
            clientId = this.client.idUser,
            clientName = this.client.name,
            operationStateId = this.operationState.id,
            operationStateName = this.operationState.name,
            orderPrice = this.orderPrice,
            orderAddress = this.orderAddress,
            orderPhone = this.orderPhone,
            orderCity = this.orderCity,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }
}

data class CreateOrderRequest(
    val clientId: String,
    val operationStateId: Long,
    val orderPrice: Long,
    val orderAddress: String,
    val orderPhone: String,
    val orderCity: String
)

data class UpdateOrderRequest(
    val operationStateId: Long,
    val orderPrice: Long,
    val orderAddress: String,
    val orderPhone: String,
    val orderCity: String
)

data class OrderResponse(
    val id: Long,
    val clientId: String,
    val clientName: String,
    val operationStateId: Long,
    val operationStateName: String,
    val orderPrice: Long,
    val orderAddress: String,
    val orderPhone: String,
    val orderCity: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
