package com.linogo.gestion.product.domain

import com.linogo.gestion.category.domain.Category
import java.time.LocalDateTime

data class Product(
    val id: Long,
    val name: String,
    val pricePerUnit: Long,
    val stock: Int,
    val imageUrl: String,
    val description: String,
    val category: Category? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
