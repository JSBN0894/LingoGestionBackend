package com.linogo.gestion.client.application

import java.time.LocalDateTime

data class ClientResponse(
    val idUser: String,
    val name: String,
    val defaultPhone: String?,
    val defaultCity: String?,
    val defaultAddress: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
