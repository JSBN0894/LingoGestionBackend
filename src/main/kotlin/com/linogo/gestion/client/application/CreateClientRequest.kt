package com.linogo.gestion.client.application

import jakarta.validation.constraints.NotBlank

data class CreateClientRequest(
    @field:NotBlank(message = "Name is required")
    val name: String,

    @field:NotBlank(message = "idUser is required")
    val idUser: String,

    val defaultPhone: String? = null,
    val defaultCity: String? = null,
    val defaultAddress: String? = null
)
