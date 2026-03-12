package com.linogo.gestion.shipment.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.linogo.gestion.order.domain.Order
import com.linogo.gestion.state.domain.State
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "shipments")
class Shipment(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @OneToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    @JoinColumn(name = "order_id", foreignKey = ForeignKey(name = "fk_shipment_order"), unique = true)
    var order: Order? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipping_state_id", foreignKey = ForeignKey(name = "fk_shipment_state"))
    var shippingState: State,

    @Column(nullable = false)
    var carrier: String = "Inter rapidisimo",

    @Column(nullable = false)
    var isCashOnDelivery: Boolean = true,

    @Column(nullable = true)
    var shippingCost: Long? = null,

    @Column(nullable = true)
    var estimateDeliveryDate: LocalDateTime? = null,

    @Column(nullable = true)
    var weight: Long? = null,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Shipment
        return id != null && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()
}
