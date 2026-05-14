package com.linogo.gestion.order.domain

import com.linogo.gestion.state.domain.State
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "orders")
data class Order(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    val customerId: Long,

    @Column(nullable = false)
    val customerName: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operation_state_id", foreignKey = ForeignKey(name = "fk_order_operation_state"))
    val operationState: State,

    @Column(nullable = false)
    val orderPrice: Long,

    @Column(nullable = false)
    val orderAddress: String,

    @Column(nullable = false)
    val orderPhone: String,

    @Column(nullable = false)
    val orderCity: String,

    @Column(nullable = true)
    val observation: String? = null,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
