package com.linogo.gestion.orderproduct.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.linogo.gestion.order.domain.Order
import com.linogo.gestion.product.domain.Product
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ForeignKey
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.io.Serializable
import java.time.LocalDateTime

@Entity
@Table(name = "order_product")
@IdClass(OrderProductId::class)
data class OrderProduct(
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    @JoinColumn(name = "order_id", foreignKey = ForeignKey(name = "fk_order_product_order"))
    val order: Order,

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    @JoinColumn(name = "product_id", foreignKey = ForeignKey(name = "fk_order_product_product"))
    val product: Product,

    @Column(nullable = false)
    val quantity: Int,

    @Column(nullable = false)
    val price: Long,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

data class OrderProductId(
    val order: Long,
    val product: Long
) : Serializable
