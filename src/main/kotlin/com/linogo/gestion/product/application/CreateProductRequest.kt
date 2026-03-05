package com.linogo.gestion.product.application

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

data class CreateProductRequest(
    @field:NotNull(message = "ID is required")
    val id: Long,

    @field:NotBlank(message = "Name is required")
    val name: String,

    @field:NotNull(message = "Price per unit is required")
    @field:Min(value = 0, message = "Price must be positive")
    val pricePerUnit: Long,

    @field:NotNull(message = "Stock is required")
    @field:Min(value = 0, message = "Stock must be non-negative")
    val stock: Int,

    @field:NotBlank(message = "Image URL is required")
    val imageUrl: String,

    @field:NotBlank(message = "Description is required")
    val description: String,

    val categoryId: Long? = null
)
