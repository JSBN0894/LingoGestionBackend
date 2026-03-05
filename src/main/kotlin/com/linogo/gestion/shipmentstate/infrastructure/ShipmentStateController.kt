package com.linogo.gestion.shipmentstate.infrastructure

import com.linogo.gestion.shipmentstate.application.CreateShipmentStateRequest
import com.linogo.gestion.shipmentstate.application.ShipmentStateService
import com.linogo.gestion.state.infrastructure.StateRepository
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
    private val service: ShipmentStateService,
    private val stateRepository: StateRepository
) {

    @PostMapping
    fun create(@Valid @RequestBody request: CreateShipmentStateRequest): ResponseEntity<Any> {
        val state = stateRepository.findById(request.stateId)
            .orElseThrow { IllegalArgumentException("State with id ${request.stateId} not found") }

        val requestWithState = request.copy().apply { setState(state) }
        val response = service.create(requestWithState)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @GetMapping
    fun getAll(): ResponseEntity<Any> {
        val responses = service.findAll()
        return ResponseEntity.ok(responses)
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<Void> {
        service.delete(id)
        return ResponseEntity.noContent().build()
    }
}
