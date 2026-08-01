package com.linogo.gestion.shipmentstate.infrastructure

import com.linogo.gestion.security.config.Authenticated
import com.linogo.gestion.security.config.RequiresPermission
import com.linogo.gestion.security.domain.Permission
import com.linogo.gestion.shipmentstate.application.CreateShipmentStateRequest
import com.linogo.gestion.shipmentstate.application.ShipmentStateResponse
import com.linogo.gestion.shipmentstate.application.ShipmentStateService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/shipment-states")
class ShipmentStateController(
    private val service: ShipmentStateService
) {

    // La búsqueda/validación del State vive en el service, no aquí — antes
    // el controller inyectaba StateRepository directamente y mutaba el
    // request, mezclando acceso a datos con la capa HTTP.
    @PostMapping
    @RequiresPermission(Permission.SHIPMENT_STATES_MANAGE)
    fun create(@Valid @RequestBody request: CreateShipmentStateRequest): ResponseEntity<ShipmentStateResponse> {
        val response = service.create(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @GetMapping
    @Authenticated
    fun getAll(): ResponseEntity<List<ShipmentStateResponse>> {
        val responses = service.findAll()
        return ResponseEntity.ok(responses)
    }

    @DeleteMapping("/{id}")
    @RequiresPermission(Permission.SHIPMENT_STATES_MANAGE)
    fun delete(@PathVariable id: Long): ResponseEntity<Void> {
        service.delete(id)
        return ResponseEntity.noContent().build()
    }
}
