package com.linogo.gestion.shipment.application

import com.linogo.gestion.order.infrastructure.OrderRepository
import com.linogo.gestion.shipment.domain.Shipment
import com.linogo.gestion.shipment.infrastructure.ShipmentRepository
import com.linogo.gestion.shipmentstate.infrastructure.ShipmentStateRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class ShipmentService(
    private val shipmentRepository: ShipmentRepository,
    private val orderRepository: OrderRepository,
    private val shipmentStateRepository: ShipmentStateRepository
) {

    @Transactional
    fun create(request: CreateShipmentRequest): ShipmentResponse {
        val order = orderRepository.findById(request.orderId)
            .orElseThrow { IllegalArgumentException("Order with id ${request.orderId} not found") }

        val shipmentState = shipmentStateRepository.findById(request.shippingStateId)
            .orElseThrow { IllegalArgumentException("ShipmentState with id ${request.shippingStateId} not found") }

        val shipment = Shipment(
            trackingNumber = request.trackingNumber,
            order = order,
            shippingState = shipmentState,
            carrier = request.carrier,
            isCashOnDelivery = request.isCashOnDelivery,
            shippingCost = request.shippingCost,
            estimateDeliveryDate = request.estimateDeliveryDate,
            weight = request.weight,
            updatedAt = LocalDateTime.now()
        )

        return shipmentRepository.save(shipment).toResponse()
    }

    @Transactional(readOnly = true)
    fun findByTrackingNumber(trackingNumber: String): ShipmentResponse {
        val shipment = shipmentRepository.findById(trackingNumber)
            .orElseThrow { IllegalArgumentException("Shipment with trackingNumber $trackingNumber not found") }
        return shipment.toResponse()
    }

    @Transactional(readOnly = true)
    fun findAll(): List<ShipmentResponse> {
        return shipmentRepository.findAll().map { it.toResponse() }
    }

    @Transactional
    fun update(trackingNumber: String, request: UpdateShipmentRequest): ShipmentResponse {
        val shipment = shipmentRepository.findById(trackingNumber)
            .orElseThrow { IllegalArgumentException("Shipment with trackingNumber $trackingNumber not found") }

        val shipmentState = shipmentStateRepository.findById(request.shippingStateId)
            .orElseThrow { IllegalArgumentException("ShipmentState with id ${request.shippingStateId} not found") }

        val updated = shipment.copy(
            shippingState = shipmentState,
            carrier = request.carrier,
            isCashOnDelivery = request.isCashOnDelivery,
            shippingCost = request.shippingCost,
            estimateDeliveryDate = request.estimateDeliveryDate,
            weight = request.weight,
            updatedAt = LocalDateTime.now()
        )

        return shipmentRepository.save(updated).toResponse()
    }

    @Transactional
    fun delete(trackingNumber: String) {
        if (!shipmentRepository.existsById(trackingNumber)) {
            throw IllegalArgumentException("Shipment with trackingNumber $trackingNumber not found")
        }
        shipmentRepository.deleteById(trackingNumber)
    }

    private fun Shipment.toResponse(): ShipmentResponse {
        return ShipmentResponse(
            trackingNumber = this.trackingNumber,
            orderId = this.order.id,
            carrier = this.carrier,
            isCashOnDelivery = this.isCashOnDelivery,
            shippingStateId = this.shippingState.id,
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
    val trackingNumber: String,
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
    val trackingNumber: String,
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
