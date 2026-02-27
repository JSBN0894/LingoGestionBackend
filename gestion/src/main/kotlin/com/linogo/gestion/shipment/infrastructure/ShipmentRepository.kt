package com.linogo.gestion.shipment.infrastructure

import com.linogo.gestion.shipment.domain.Shipment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ShipmentRepository : JpaRepository<Shipment, String>
