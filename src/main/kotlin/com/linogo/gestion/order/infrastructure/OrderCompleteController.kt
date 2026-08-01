package com.linogo.gestion.order.infrastructure

import com.linogo.gestion.order.application.CreateOrderCompleteRequest
import com.linogo.gestion.order.application.CreateOrderCompleteService
import com.linogo.gestion.security.config.RequiresPermission
import com.linogo.gestion.security.domain.Permission
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/orders/complete")
class OrderCompleteController(
    private val createOrderCompleteService: CreateOrderCompleteService
) {

    // Antes sin anotación de rol: cualquier usuario autenticado podía crear
    // órdenes. Se alinea con OrderController.create(), que ya exige VENTAS.
    @PostMapping
    @RequiresPermission(Permission.ORDERS_MANAGE)
    fun createOrder(@Valid @RequestBody request: CreateOrderCompleteRequest): ResponseEntity<Any> {
        val response = createOrderCompleteService.create(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }
}
