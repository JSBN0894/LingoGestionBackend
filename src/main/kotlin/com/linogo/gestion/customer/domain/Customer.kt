package com.linogo.gestion.customer.domain

data class Customer(
    val cedula: Long,
    val name: String,
    val phones: List<String> = emptyList(),
    val addresses: List<String> = emptyList()
)
