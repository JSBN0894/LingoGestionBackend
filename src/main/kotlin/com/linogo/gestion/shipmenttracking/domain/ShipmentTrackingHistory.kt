package com.linogo.gestion.shipmenttracking.domain

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "shipment_tracking_history")
data class ShipmentTrackingHistory(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val shipmentId: Long,

    @Column(nullable = true)
    val statusFrom: String? = null,

    @Column(nullable = false)
    val statusTo: String,

    @Column(nullable = true)
    val changedBy: String? = null,

    @Column(nullable = true, columnDefinition = "TEXT")
    val notes: String? = null,

    @Column(nullable = false, updatable = false)
    val changedAt: LocalDateTime = LocalDateTime.now()
)
