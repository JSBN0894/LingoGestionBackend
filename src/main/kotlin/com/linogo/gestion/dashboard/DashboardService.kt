package com.linogo.gestion.dashboard

import com.linogo.gestion.order.infrastructure.OrderRepository
import com.linogo.gestion.product.infrastructure.ProductRepository
import com.linogo.gestion.security.infrastructure.UserRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class DashboardService(
    private val orderRepository: OrderRepository,
    private val productRepository: ProductRepository,
    private val userRepository: UserRepository
) {

    fun getSalesSummary(from: LocalDateTime?, to: LocalDateTime?): SalesSummaryResponse {
        val start = from ?: LocalDateTime.now().minusDays(30)
        val end = to ?: LocalDateTime.now()

        val allOrders = orderRepository.findAll()
        val filteredOrders = allOrders.filter { it.createdAt in start..end }

        val totalOrders = filteredOrders.size
        val totalRevenue = filteredOrders.sumOf { it.orderPrice }
        val avgOrderValue = if (totalOrders > 0) totalRevenue / totalOrders else 0L

        val ordersByState = filteredOrders.groupBy { it.operationState.name }
            .map { (state, orders) -> StateCount(state, orders.size) }

        val totalProducts = productRepository.findAll().size
        val totalUsers = userRepository.findAll().size

        return SalesSummaryResponse(
            totalOrders = totalOrders,
            totalRevenue = totalRevenue,
            avgOrderValue = avgOrderValue,
            totalProducts = totalProducts,
            totalUsers = totalUsers,
            ordersByState = ordersByState
        )
    }
}

data class SalesSummaryResponse(
    val totalOrders: Int,
    val totalRevenue: Long,
    val avgOrderValue: Long,
    val totalProducts: Int,
    val totalUsers: Int,
    val ordersByState: List<StateCount>
)

data class StateCount(
    val state: String,
    val count: Int
)
