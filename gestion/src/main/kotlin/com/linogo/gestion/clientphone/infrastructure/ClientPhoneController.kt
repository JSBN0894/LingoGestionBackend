package com.linogo.gestion.clientphone.infrastructure

import com.linogo.gestion.clientphone.application.CreateClientPhoneRequest
import com.linogo.gestion.clientphone.application.ClientPhoneService
import com.linogo.gestion.clientphone.application.UpdateClientPhoneRequest
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
@RequestMapping("/api/client-phones")
class ClientPhoneController(
    private val clientPhoneService: ClientPhoneService
) {

    @PostMapping
    fun create(@Valid @RequestBody request: CreateClientPhoneRequest): ResponseEntity<Any> {
        val response = clientPhoneService.create(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @GetMapping("/client/{clientId}")
    fun getByClientId(@PathVariable clientId: String): ResponseEntity<Any> {
        val responses = clientPhoneService.findByClientId(clientId)
        return ResponseEntity.ok(responses)
    }

    @GetMapping
    fun getAll(): ResponseEntity<Any> {
        val responses = clientPhoneService.findAll()
        return ResponseEntity.ok(responses)
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateClientPhoneRequest
    ): ResponseEntity<Any> {
        val response = clientPhoneService.update(id, request)
        return ResponseEntity.ok(response)
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<Void> {
        clientPhoneService.delete(id)
        return ResponseEntity.noContent().build()
    }
}
