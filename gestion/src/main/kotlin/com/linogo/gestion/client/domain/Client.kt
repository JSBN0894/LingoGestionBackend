package com.linogo.gestion.client.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "client")
data class Client(
    @Id
    val idUser: String,

    @Column(unique = true, nullable = true)
    val idNumber: String? = null,  // Número de identificación nacional (RUT, DNI, etc.)

    @Column(nullable = false)
    val name: String,

    @Column(nullable = true)
    val defaultPhone: String? = null,

    @Column(nullable = true)
    val defaultCity: String? = null,

    @Column(nullable = true)
    val defaultAddress: String? = null,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
