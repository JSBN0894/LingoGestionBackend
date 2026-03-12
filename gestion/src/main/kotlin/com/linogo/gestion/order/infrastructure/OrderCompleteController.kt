package com.linogo.gestion.order.infrastructure

import com.linogo.gestion.order.application.CreateOrderCompleteRequest
import com.linogo.gestion.order.application.CreateOrderCompleteService
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/orders/complete")
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Órdenes Completas", description = "Gestión de órdenes completas")
class OrderCompleteController(
    private val createOrderCompleteService: CreateOrderCompleteService
) {

    @PostMapping
    fun createOrder(@Valid @RequestBody request: CreateOrderCompleteRequest): ResponseEntity<Any> {
        val response = createOrderCompleteService.create(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }
}
