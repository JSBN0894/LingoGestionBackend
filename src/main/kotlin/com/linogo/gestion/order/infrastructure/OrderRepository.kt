package com.linogo.gestion.order.infrastructure

import com.linogo.gestion.order.domain.Order
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface OrderRepository : JpaRepository<Order, Long>
