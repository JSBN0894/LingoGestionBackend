package com.linogo.gestion.product.application

import java.time.LocalDateTime

data class ProductResponse(
    val id: Long,
    val name: String,
    val pricePerUnit: Long,
    val stock: Int,
    val imageUrl: String,
    val description: String,
    val category: CategoryResponse,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

data class CategoryResponse(
    val id: Long,
    val name: String
)
