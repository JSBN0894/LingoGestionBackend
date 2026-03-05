package com.linogo.gestion.clientaddress.infrastructure

import com.linogo.gestion.clientaddress.application.ClientAddressService
import com.linogo.gestion.clientaddress.application.CreateClientAddressRequest
import com.linogo.gestion.clientaddress.application.UpdateClientAddressRequest
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
@RequestMapping("/api/client-addresses")
class ClientAddressController(
    private val clientAddressService: ClientAddressService
) {

    @PostMapping
    fun create(@Valid @RequestBody request: CreateClientAddressRequest): ResponseEntity<Any> {
        val response = clientAddressService.create(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @GetMapping("/client/{clientId}")
    fun getByClientId(@PathVariable clientId: String): ResponseEntity<Any> {
        val responses = clientAddressService.findByClientId(clientId)
        return ResponseEntity.ok(responses)
    }

    @GetMapping
    fun getAll(): ResponseEntity<Any> {
        val responses = clientAddressService.findAll()
        return ResponseEntity.ok(responses)
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateClientAddressRequest
    ): ResponseEntity<Any> {
        val response = clientAddressService.update(id, request)
        return ResponseEntity.ok(response)
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<Void> {
        clientAddressService.delete(id)
        return ResponseEntity.noContent().build()
    }
}
