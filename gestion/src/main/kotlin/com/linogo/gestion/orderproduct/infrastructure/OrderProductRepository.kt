package com.linogo.gestion.orderproduct.infrastructure

import com.linogo.gestion.orderproduct.domain.OrderProduct
import com.linogo.gestion.orderproduct.domain.OrderProductId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface OrderProductRepository : JpaRepository<OrderProduct, OrderProductId> {
    fun findByOrderId(orderId: Long): List<OrderProduct>
}
