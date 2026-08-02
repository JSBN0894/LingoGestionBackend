package com.linogo.gestion.shipment.infrastructure

import com.linogo.gestion.security.config.Authenticated
import com.linogo.gestion.security.config.RequiresPermission
import com.linogo.gestion.security.domain.Permission
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
    @RequiresPermission(Permission.SHIPMENTS_MANAGE)
    fun create(@Valid @RequestBody request: CreateShipmentRequest): ResponseEntity<Any> {
        val response = shipmentService.create(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @GetMapping("/{id}")
    @Authenticated
    fun getById(@PathVariable id: Long): ResponseEntity<Any> {
        val response = shipmentService.findById(id)
        return ResponseEntity.ok(response)
    }

    @GetMapping
    @Authenticated
    fun getAll(): ResponseEntity<Any> {
        val responses = shipmentService.findAll()
        return ResponseEntity.ok(responses)
    }

    @PutMapping("/{id}")
    @RequiresPermission(Permission.SHIPMENTS_MANAGE)
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateShipmentRequest
    ): ResponseEntity<Any> {
        val response = shipmentService.update(id, request)
        return ResponseEntity.ok(response)
    }

    @DeleteMapping("/{id}")
    @RequiresPermission(Permission.SHIPMENTS_DELETE)
    fun delete(@PathVariable id: Long): ResponseEntity<Void> {
        shipmentService.delete(id)
        return ResponseEntity.noContent().build()
    }

    @PatchMapping("/{id}/guide")
    @RequiresPermission(Permission.SHIPMENTS_MANAGE)
    fun assignGuide(
        @PathVariable id: Long,
        @Valid @RequestBody request: AssignGuideRequest
    ): ResponseEntity<Any> {
        val response = shipmentService.assignGuide(id, request.guideNumber)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/{id}/tracking")
    @Authenticated
    fun getTracking(@PathVariable id: Long): ResponseEntity<Any> {
        val history = shipmentService.getTrackingHistory(id)
        return ResponseEntity.ok(history)
    }
}
