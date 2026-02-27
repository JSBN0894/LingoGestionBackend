package com.linogo.gestion.product.application

import java.time.LocalDateTime

data class ProductResponse(
    val id: Long,
    val name: String,
    val pricePerUnit: Long,
    val stock: Int,
    val imageUrl: String,
    val description: String,
    val categoryId: Long?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
