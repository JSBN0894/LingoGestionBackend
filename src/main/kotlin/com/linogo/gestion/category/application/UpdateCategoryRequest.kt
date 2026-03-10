package com.linogo.gestion.category.application

import jakarta.validation.constraints.NotBlank

data class UpdateCategoryRequest(
    @field:NotBlank(message = "Name is required")
    val name: String,

    val parentId: Long? = null
)
