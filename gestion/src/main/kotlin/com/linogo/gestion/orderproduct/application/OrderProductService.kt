package com.linogo.gestion.orderproduct.application

import com.linogo.gestion.order.infrastructure.OrderRepository
import com.linogo.gestion.orderproduct.domain.OrderProduct
import com.linogo.gestion.orderproduct.domain.OrderProductId
import com.linogo.gestion.orderproduct.infrastructure.OrderProductRepository
import com.linogo.gestion.product.infrastructure.ProductRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class OrderProductService(
    private val orderProductRepository: OrderProductRepository,
    private val orderRepository: OrderRepository,
    private val productRepository: ProductRepository
) {

    @Transactional
    fun create(request: CreateOrderProductRequest): OrderProductResponse {
        val order = orderRepository.findById(request.orderId)
            .orElseThrow { IllegalArgumentException("Order with id ${request.orderId} not found") }

        val product = productRepository.findById(request.productId)
            .orElseThrow { IllegalArgumentException("Product with id ${request.productId} not found") }

        val orderProduct = OrderProduct(
            order = order,
            product = product,
            quantity = request.quantity,
            price = request.price,
            updatedAt = LocalDateTime.now()
        )

        return orderProductRepository.save(orderProduct).toResponse()
    }

    @Transactional(readOnly = true)
    fun findByOrderId(orderId: Long): List<OrderProductResponse> {
        return orderProductRepository.findByOrderId(orderId).map { it.toResponse() }
    }

    @Transactional(readOnly = true)
    fun findAll(): List<OrderProductResponse> {
        return orderProductRepository.findAll().map { it.toResponse() }
    }

    @Transactional
    fun update(orderId: Long, productId: Long, request: UpdateOrderProductRequest): OrderProductResponse {
        val orderProduct = orderProductRepository.findById(OrderProductId(orderId, productId))
            .orElseThrow { IllegalArgumentException("OrderProduct not found for order $orderId and product $productId") }

        val updated = orderProduct.copy(
            quantity = request.quantity,
            price = request.price,
            updatedAt = LocalDateTime.now()
        )

        return orderProductRepository.save(updated).toResponse()
    }

    @Transactional
    fun delete(orderId: Long, productId: Long) {
        val id = OrderProductId(orderId, productId)
        if (!orderProductRepository.existsById(id)) {
            throw IllegalArgumentException("OrderProduct not found for order $orderId and product $productId")
        }
        orderProductRepository.deleteById(id)
    }

    private fun OrderProduct.toResponse(): OrderProductResponse {
        return OrderProductResponse(
            orderId = this.order.id!!,
            productId = this.product.id!!,
            productName = this.product.name,
            quantity = this.quantity,
            price = this.price,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }
}

data class CreateOrderProductRequest(
    val orderId: Long,
    val productId: Long,
    val quantity: Int,
    val price: Long
)

data class UpdateOrderProductRequest(
    val quantity: Int,
    val price: Long
)

data class OrderProductResponse(
    val orderId: Long,
    val productId: Long,
    val productName: String,
    val quantity: Int,
    val price: Long,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
