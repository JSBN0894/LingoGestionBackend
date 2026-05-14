package com.linogo.gestion.customer.infrastructure.persistence

import jakarta.persistence.*

@Entity
@Table(name = "customers")
class CustomerEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val name: String,

    @Column(nullable = false)
    val email: String,

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "customer_phones", joinColumns = [JoinColumn(name = "customer_id")])
    @Column(name = "phone")
    val phones: List<String> = emptyList()
)
