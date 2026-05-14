package com.linogo.gestion.order.application

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class CreateOrderCompleteRequest(
    @field:NotBlank(message = "Name is required")
    val name: String,

    @field:NotNull(message = "Cedula is required")
    val cedula: Long,

    @field:NotBlank(message = "Address is required")
    val address: String,

    @field:NotBlank(message = "City is required")
    val city: String,

    @field:NotBlank(message = "Phone is required")
    val phone: String,

    @field:NotNull(message = "Order is required")
    val order: OrderData
)

data class OrderData(
    @field:NotNull(message = "Order price is required")
    val orderPrice: Long,

    @field:NotNull(message = "Operation state is required")
    val operationStateId: Long,

    @field:NotNull(message = "Order products are required")
    @field:Valid
    val orderProducts: List<OrderProductData>
)

data class OrderProductData(
    @field:NotNull(message = "Product ID is required")
    val idProduct: Long,

    @field:NotNull(message = "Quantity is required")
    val quantity: Int
)
