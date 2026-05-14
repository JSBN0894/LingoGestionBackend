package com.linogo.gestion.customer.domain

data class Customer(
    val id: Long? = null,
    val name: String,
    val email: String,
    val phones: List<String> = emptyList()
)
