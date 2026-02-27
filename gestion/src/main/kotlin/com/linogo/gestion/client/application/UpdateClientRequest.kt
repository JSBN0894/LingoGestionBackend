package com.linogo.gestion.client.application

import jakarta.validation.constraints.NotBlank

data class UpdateClientRequest(
    @field:NotBlank(message = "Name is required")
    val name: String,

    val defaultPhone: String? = null,
    val defaultCity: String? = null,
    val defaultAddress: String? = null
)
