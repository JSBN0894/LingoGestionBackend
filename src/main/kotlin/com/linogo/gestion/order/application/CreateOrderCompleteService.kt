package com.linogo.gestion.order.application

import com.linogo.gestion.customer.domain.Customer
import com.linogo.gestion.customer.domain.CustomerRepository
import com.linogo.gestion.order.domain.Order
import com.linogo.gestion.order.infrastructure.OrderRepository
import com.linogo.gestion.orderproduct.domain.OrderProduct
import com.linogo.gestion.orderproduct.infrastructure.OrderProductRepository
import com.linogo.gestion.product.infrastructure.ProductRepository
import com.linogo.gestion.state.infrastructure.StateRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class CreateOrderCompleteService(
    private val orderRepository: OrderRepository,
    private val customerRepository: CustomerRepository,
    private val stateRepository: StateRepository,
    private val productRepository: ProductRepository,
    private val orderProductRepository: OrderProductRepository
) {

    @Transactional
    fun create(request: CreateOrderCompleteRequest): OrderCompleteResponse {
        // 1. Buscar o crear customer por cédula
        val existingCustomer = customerRepository.findByCedula(request.cedula)
        val customer = if (existingCustomer != null) {
            // Agregar teléfono si no existe
            val updatedPhones = if (existingCustomer.phones.none { it == request.phone }) {
                existingCustomer.phones + request.phone
            } else existingCustomer.phones

            // Agregar dirección si no existe (combinada address + city)
            val fullAddress = "${request.address}, ${request.city}"
            val updatedAddresses = if (existingCustomer.addresses.none { it == fullAddress }) {
                existingCustomer.addresses + fullAddress
            } else existingCustomer.addresses

            if (updatedPhones != existingCustomer.phones || updatedAddresses != existingCustomer.addresses) {
                customerRepository.save(existingCustomer.copy(phones = updatedPhones, addresses = updatedAddresses))
            } else existingCustomer
        } else {
            val fullAddress = "${request.address}, ${request.city}"
            customerRepository.save(
                Customer(
                    cedula = request.cedula,
                    name = request.name,
                    phones = listOf(request.phone),
                    addresses = listOf(fullAddress)
                )
            )
        }

        // 2. Buscar estado de operación por ID
        val operationState = stateRepository.findById(request.order.operationStateId)
            .orElseThrow { IllegalArgumentException("State with id ${request.order.operationStateId} not found") }

        // 3. Crear la orden (snapshot de customerId y customerName)
        val order = Order(
            customerId = customer.cedula,
            customerName = customer.name,
            operationState = operationState,
            orderPrice = request.order.orderPrice,
            orderAddress = request.address,
            orderPhone = request.phone,
            orderCity = request.city
        )
        val savedOrder = orderRepository.save(order)

        // 4. Crear productos de la orden
        val orderProducts = request.order.orderProducts.map { productData ->
            val product = productRepository.findById(productData.idProduct)
                .orElseThrow { IllegalArgumentException("Product with id ${productData.idProduct} not found") }

            OrderProduct(
                order = savedOrder,
                product = product,
                quantity = productData.quantity,
                price = product.pricePerUnit
            )
        }
        orderProductRepository.saveAll(orderProducts)

        return savedOrder.toResponse()
    }

    private fun Order.toResponse(): OrderCompleteResponse {
        return OrderCompleteResponse(
            id = this.id!!,
            customerId = this.customerId,
            customerName = this.customerName,
            operationStateId = this.operationState.id,
            operationStateName = this.operationState.name,
            orderPrice = this.orderPrice,
            orderAddress = this.orderAddress,
            orderPhone = this.orderPhone,
            orderCity = this.orderCity,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }
}

data class OrderCompleteResponse(
    val id: Long,
    val customerId: Long,
    val customerName: String,
    val operationStateId: Long,
    val operationStateName: String,
    val orderPrice: Long,
    val orderAddress: String,
    val orderPhone: String,
    val orderCity: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
