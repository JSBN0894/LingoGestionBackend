package com.linogo.gestion.shipmenttracking.infrastructure

import com.linogo.gestion.shipmenttracking.domain.ShipmentTrackingHistory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ShipmentTrackingRepository : JpaRepository<ShipmentTrackingHistory, Long> {
    fun findByShipmentIdOrderByChangedAtAsc(shipmentId: Long): List<ShipmentTrackingHistory>
}
