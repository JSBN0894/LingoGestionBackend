package com.linogo.gestion.order.infrastructure

import com.linogo.gestion.order.application.CreateOrderRequest
import com.linogo.gestion.order.application.OrderService
import com.linogo.gestion.order.application.UpdateOrderRequest
import com.linogo.gestion.security.config.AdminOnly
import com.linogo.gestion.security.config.Authenticated
import com.linogo.gestion.security.config.VentasOnly
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
@RequestMapping("/api/orders")
class OrderController(
    private val orderService: OrderService
) {

    @PostMapping
    @VentasOnly
    fun create(@Valid @RequestBody request: CreateOrderRequest): ResponseEntity<Any> {
        val response = orderService.create(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @GetMapping("/{id}")
    @Authenticated
    fun getById(@PathVariable id: Long): ResponseEntity<Any> {
        val response = orderService.findById(id)
        return ResponseEntity.ok(response)
    }

    @GetMapping
    @Authenticated
    fun getAll(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int
    ): ResponseEntity<Any> {
        val responses = orderService.findAll(page, size)
        return ResponseEntity.ok(responses)
    }

    @PutMapping("/{id}")
    @VentasOnly
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateOrderRequest
    ): ResponseEntity<Any> {
        val response = orderService.update(id, request)
        return ResponseEntity.ok(response)
    }

    @DeleteMapping("/{id}")
    @AdminOnly
    fun delete(@PathVariable id: Long): ResponseEntity<Void> {
        orderService.delete(id)
        return ResponseEntity.noContent().build()
    }
}
