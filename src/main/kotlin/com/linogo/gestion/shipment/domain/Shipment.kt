package com.linogo.gestion.shipment.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.linogo.gestion.carrier.domain.Carrier
import com.linogo.gestion.order.domain.Order
import com.linogo.gestion.shipmentstate.domain.ShipmentState
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "shipments")
data class Shipment(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @OneToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    @JoinColumn(name = "order_id", foreignKey = ForeignKey(name = "fk_shipment_order"), unique = true)
    val order: Order,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipping_state_id", foreignKey = ForeignKey(name = "fk_shipment_state"))
    val shippingState: ShipmentState,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carrier_id", foreignKey = ForeignKey(name = "fk_shipment_carrier"))
    val carrier: Carrier,

    @Column(nullable = false)
    val isCashOnDelivery: Boolean = true,

    @Column(nullable = true)
    val shippingCost: Long? = null,

    @Column(nullable = true)
    val estimateDeliveryDate: LocalDateTime? = null,

    @Column(nullable = true)
    val weight: Long? = null,

    @Column(nullable = true)
    val guideNumber: String? = null,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
