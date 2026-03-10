package com.linogo.gestion.category.application

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class CreateCategoryRequest(
    @field:NotNull(message = "ID is required")
    val id: Long,

    @field:NotBlank(message = "Name is required")
    val name: String,

    val parentId: Long? = null
)
