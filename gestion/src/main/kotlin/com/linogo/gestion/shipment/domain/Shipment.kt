package com.linogo.gestion.shipment.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.linogo.gestion.order.domain.Order
import com.linogo.gestion.shipmentstate.domain.ShipmentState
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ForeignKey
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "shipments")
data class Shipment(
    @Id
    val trackingNumber: String,

    @OneToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    @JoinColumn(name = "order_id", foreignKey = ForeignKey(name = "fk_shipment_order"))
    val order: Order,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipping_state_id", foreignKey = ForeignKey(name = "fk_shipment_state"))
    val shippingState: ShipmentState,

    @Column(nullable = false)
    val carrier: String = "Inter rapidisimo",

    @Column(nullable = false)
    val isCashOnDelivery: Boolean = true,

    @Column(nullable = true)
    val shippingCost: Long? = null,

    @Column(nullable = true)
    val estimateDeliveryDate: LocalDateTime? = null,

    @Column(nullable = true)
    val weight: Long? = null,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
