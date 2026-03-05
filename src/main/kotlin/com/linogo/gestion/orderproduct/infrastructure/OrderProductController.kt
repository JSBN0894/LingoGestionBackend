package com.linogo.gestion.orderproduct.infrastructure

import com.linogo.gestion.orderproduct.application.CreateOrderProductRequest
import com.linogo.gestion.orderproduct.application.OrderProductService
import com.linogo.gestion.orderproduct.application.UpdateOrderProductRequest
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
@RequestMapping("/api/order-products")
class OrderProductController(
    private val orderProductService: OrderProductService
) {

    @PostMapping
    fun create(@Valid @RequestBody request: CreateOrderProductRequest): ResponseEntity<Any> {
        val response = orderProductService.create(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @GetMapping("/order/{orderId}")
    fun getByOrderId(@PathVariable orderId: Long): ResponseEntity<Any> {
        val responses = orderProductService.findByOrderId(orderId)
        return ResponseEntity.ok(responses)
    }

    @GetMapping
    fun getAll(): ResponseEntity<Any> {
        val responses = orderProductService.findAll()
        return ResponseEntity.ok(responses)
    }

    @PutMapping("/order/{orderId}/product/{productId}")
    fun update(
        @PathVariable orderId: Long,
        @PathVariable productId: Long,
        @Valid @RequestBody request: UpdateOrderProductRequest
    ): ResponseEntity<Any> {
        val response = orderProductService.update(orderId, productId, request)
        return ResponseEntity.ok(response)
    }

    @DeleteMapping("/order/{orderId}/product/{productId}")
    fun delete(
        @PathVariable orderId: Long,
        @PathVariable productId: Long
    ): ResponseEntity<Void> {
        orderProductService.delete(orderId, productId)
        return ResponseEntity.noContent().build()
    }
}
