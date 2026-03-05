package com.linogo.gestion.shipmentstate.infrastructure

import com.linogo.gestion.shipmentstate.domain.ShipmentState
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ShipmentStateRepository : JpaRepository<ShipmentState, Long>
