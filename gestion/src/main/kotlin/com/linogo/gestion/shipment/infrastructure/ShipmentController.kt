package com.linogo.gestion.shipment.infrastructure

import com.linogo.gestion.shipment.application.CreateShipmentRequest
import com.linogo.gestion.shipment.application.ShipmentService
import com.linogo.gestion.shipment.application.UpdateShipmentRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

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

    @GetMapping("/{trackingNumber}")
    fun getByTrackingNumber(@PathVariable trackingNumber: String): ResponseEntity<Any> {
        val response = shipmentService.findByTrackingNumber(trackingNumber)
        return ResponseEntity.ok(response)
    }

    @GetMapping
    fun getAll(): ResponseEntity<Any> {
        val responses = shipmentService.findAll()
        return ResponseEntity.ok(responses)
    }

    @PutMapping("/{trackingNumber}")
    fun update(
        @PathVariable trackingNumber: String,
        @Valid @RequestBody request: UpdateShipmentRequest
    ): ResponseEntity<Any> {
        val response = shipmentService.update(trackingNumber, request)
        return ResponseEntity.ok(response)
    }

    @DeleteMapping("/{trackingNumber}")
    fun delete(@PathVariable trackingNumber: String): ResponseEntity<Void> {
        shipmentService.delete(trackingNumber)
        return ResponseEntity.noContent().build()
    }
}
