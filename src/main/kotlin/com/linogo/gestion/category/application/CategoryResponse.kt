package com.linogo.gestion.category.application

import java.time.LocalDateTime

data class CategoryResponse(
    val id: Long,
    val name: String,
    val parentId: Long?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
