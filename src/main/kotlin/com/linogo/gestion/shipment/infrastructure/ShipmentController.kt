package com.linogo.gestion.shipment.infrastructure

import com.linogo.gestion.security.config.LogisticaOnly
import com.linogo.gestion.shipment.application.AssignGuideRequest
import com.linogo.gestion.shipment.application.CreateShipmentRequest
import com.linogo.gestion.shipment.application.ShipmentService
import com.linogo.gestion.shipment.application.UpdateShipmentRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/shipments")
class ShipmentController(
    private val shipmentService: ShipmentService
) {

    @PostMapping
    fun create(@Valid @RequestBody request: CreateShipmentRequest): ResponseEntity<Any> {
        val response = shipmentService.create(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): ResponseEntity<Any> {
        val response = shipmentService.findById(id)
        return ResponseEntity.ok(response)
    }

    @GetMapping
    fun getAll(): ResponseEntity<Any> {
        val responses = shipmentService.findAll()
        return ResponseEntity.ok(responses)
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateShipmentRequest
    ): ResponseEntity<Any> {
        val response = shipmentService.update(id, request)
        return ResponseEntity.ok(response)
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<Void> {
        shipmentService.delete(id)
        return ResponseEntity.noContent().build()
    }

    @PatchMapping("/{id}/guide")
    @LogisticaOnly
    fun assignGuide(
        @PathVariable id: Long,
        @Valid @RequestBody request: AssignGuideRequest
    ): ResponseEntity<Any> {
        val response = shipmentService.assignGuide(id, request.guideNumber)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/{id}/tracking")
    fun getTracking(@PathVariable id: Long): ResponseEntity<Any> {
        val history = shipmentService.getTrackingHistory(id)
        return ResponseEntity.ok(history)
    }
}
