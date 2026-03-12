package com.linogo.gestion.shipment.application

import com.linogo.gestion.order.infrastructure.OrderRepository
import com.linogo.gestion.shipment.domain.Shipment
import com.linogo.gestion.shipment.infrastructure.ShipmentRepository
import com.linogo.gestion.state.infrastructure.StateRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class ShipmentService(
    private val shipmentRepository: ShipmentRepository,
    private val orderRepository: OrderRepository,
    private val stateRepository: StateRepository
) {

    @Transactional
    fun create(request: CreateShipmentRequest): ShipmentResponse {
        val order = orderRepository.findById(request.orderId)
            .orElseThrow { IllegalArgumentException("Pedido con id ${request.orderId} no encontrado") }

        val shipmentState = stateRepository.findById(request.shippingStateId)
            .orElseThrow { IllegalArgumentException("Estado con id ${request.shippingStateId} no encontrado") }

        val shipment = Shipment(
            order = order,
            shippingState = shipmentState,
            carrier = request.carrier,
            isCashOnDelivery = request.isCashOnDelivery,
            shippingCost = request.shippingCost,
            estimateDeliveryDate = request.estimateDeliveryDate,
            weight = request.weight
        )

        return shipmentRepository.save(shipment).toResponse()
    }

    @Transactional(readOnly = true)
    fun findById(id: Long): ShipmentResponse {
        val shipment = shipmentRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Envío con id $id no encontrado") }
        return shipment.toResponse()
    }

    @Transactional(readOnly = true)
    fun findAll(): List<ShipmentResponse> {
        return shipmentRepository.findAll().map { it.toResponse() }
    }

    @Transactional
    fun update(id: Long, request: UpdateShipmentRequest): ShipmentResponse {
        val shipment = shipmentRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Envío con id $id no encontrado") }

        val shipmentState = stateRepository.findById(request.shippingStateId)
            .orElseThrow { IllegalArgumentException("Estado con id ${request.shippingStateId} no encontrado") }

        shipment.apply {
            this.shippingState = shipmentState
            this.carrier = request.carrier
            this.isCashOnDelivery = request.isCashOnDelivery
            this.shippingCost = request.shippingCost
            this.estimateDeliveryDate = request.estimateDeliveryDate
            this.weight = request.weight
            this.updatedAt = LocalDateTime.now()
        }

        return shipmentRepository.save(shipment).toResponse()
    }

    @Transactional
    fun delete(id: Long) {
        if (!shipmentRepository.existsById(id)) {
            throw IllegalArgumentException("Envío con id $id no encontrado")
        }
        shipmentRepository.deleteById(id)
    }

    private fun Shipment.toResponse(): ShipmentResponse {
        return ShipmentResponse(
            id = this.id!!,
            orderId = this.order?.id ?: 0,
            carrier = this.carrier,
            isCashOnDelivery = this.isCashOnDelivery,
            shippingStateId = this.shippingState.id ?: 0,
            shippingStateName = this.shippingState.name,
            shippingCost = this.shippingCost,
            estimateDeliveryDate = this.estimateDeliveryDate,
            weight = this.weight,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }
}

data class CreateShipmentRequest(
    val orderId: Long,
    val carrier: String = "Inter rapidisimo",
    val isCashOnDelivery: Boolean = true,
    val shippingStateId: Long,
    val shippingCost: Long? = null,
    val estimateDeliveryDate: LocalDateTime? = null,
    val weight: Long? = null
)

data class UpdateShipmentRequest(
    val shippingStateId: Long,
    val carrier: String = "Inter rapidisimo",
    val isCashOnDelivery: Boolean = true,
    val shippingCost: Long? = null,
    val estimateDeliveryDate: LocalDateTime? = null,
    val weight: Long? = null
)

data class ShipmentResponse(
    val id: Long,
    val orderId: Long,
    val carrier: String,
    val isCashOnDelivery: Boolean,
    val shippingStateId: Long,
    val shippingStateName: String,
    val shippingCost: Long?,
    val estimateDeliveryDate: LocalDateTime?,
    val weight: Long?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
