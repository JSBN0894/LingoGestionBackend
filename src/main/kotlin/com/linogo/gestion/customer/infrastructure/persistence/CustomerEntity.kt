package com.linogo.gestion.customer.infrastructure.persistence

import jakarta.persistence.*

@Entity
@Table(name = "customers")
class CustomerEntity(
    @Id
    val cedula: Long,

    @Column(nullable = false)
    val name: String,

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "customer_phones", joinColumns = [JoinColumn(name = "customer_cedula")])
    @Column(name = "phone")
    val phones: List<String> = emptyList(),

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "customer_addresses", joinColumns = [JoinColumn(name = "customer_cedula")])
    @Column(name = "address")
    val addresses: List<String> = emptyList()
)
