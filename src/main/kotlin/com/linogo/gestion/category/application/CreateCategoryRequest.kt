package com.linogo.gestion.category.application

import jakarta.validation.constraints.NotBlank

data class CreateCategoryRequest(
    @field:NotBlank(message = "Name is required")
    val name: String,

    val parentId: Long? = null
)
