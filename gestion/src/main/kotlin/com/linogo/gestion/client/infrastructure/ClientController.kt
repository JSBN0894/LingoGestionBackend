package com.linogo.gestion.client.infrastructure

import com.linogo.gestion.client.application.ClientService
import com.linogo.gestion.client.application.CreateClientRequest
import com.linogo.gestion.client.application.UpdateClientRequest
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
@RequestMapping("/api/clients")
class ClientController(
    private val clientService: ClientService
) {

    @PostMapping
    fun create(@Valid @RequestBody request: CreateClientRequest): ResponseEntity<Any> {
        val response = clientService.create(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @GetMapping("/{idUser}")
    fun getById(@PathVariable idUser: String): ResponseEntity<Any> {
        val response = clientService.findById(idUser)
        return ResponseEntity.ok(response)
    }

    @GetMapping
    fun getAll(): ResponseEntity<Any> {
        val responses = clientService.findAll()
        return ResponseEntity.ok(responses)
    }

    @PutMapping("/{idUser}")
    fun update(
        @PathVariable idUser: String,
        @Valid @RequestBody request: UpdateClientRequest
    ): ResponseEntity<Any> {
        val response = clientService.update(idUser, request)
        return ResponseEntity.ok(response)
    }

    @DeleteMapping("/{idUser}")
    fun delete(@PathVariable idUser: String): ResponseEntity<Void> {
        clientService.delete(idUser)
        return ResponseEntity.noContent().build()
    }
}
