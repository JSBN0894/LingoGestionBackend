package com.linogo.gestion.state.infrastructure

import com.linogo.gestion.state.application.StateService
import com.linogo.gestion.state.application.CreateStateRequest
import com.linogo.gestion.state.application.UpdateStateRequest
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/states")
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Estados", description = "Gestión de estados anidados para órdenes y envíos")
class StateController(
    private val stateService: StateService
) {

    @PostMapping
    fun create(@Valid @RequestBody request: CreateStateRequest): ResponseEntity<Any> {
        val response = stateService.create(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): ResponseEntity<Any> {
        val response = stateService.findById(id)
        return ResponseEntity.ok(response)
    }

    @GetMapping
    fun getAll(
        @RequestParam(required = false) parentId: Long?
    ): ResponseEntity<Any> {
        val responses = if (parentId != null) {
            stateService.findByParentId(parentId)
        } else {
            stateService.findAll()
        }
        return ResponseEntity.ok(responses)
    }

    @GetMapping("/root")
    fun getRootStates(): ResponseEntity<Any> {
        val responses = stateService.findRootStates()
        return ResponseEntity.ok(responses)
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateStateRequest
    ): ResponseEntity<Any> {
        val response = stateService.update(id, request)
        return ResponseEntity.ok(response)
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<Void> {
        stateService.delete(id)
        return ResponseEntity.noContent().build()
    }
}
