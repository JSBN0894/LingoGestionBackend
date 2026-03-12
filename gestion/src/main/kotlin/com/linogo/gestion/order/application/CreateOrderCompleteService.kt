package com.linogo.gestion.order.application

import com.linogo.gestion.client.domain.Client
import com.linogo.gestion.client.infrastructure.ClientRepository
import com.linogo.gestion.clientaddress.domain.ClientAddress
import com.linogo.gestion.clientaddress.infrastructure.ClientAddressRepository
import com.linogo.gestion.clientphone.domain.ClientPhone
import com.linogo.gestion.clientphone.infrastructure.ClientPhoneRepository
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
    private val clientRepository: ClientRepository,
    private val clientAddressRepository: ClientAddressRepository,
    private val clientPhoneRepository: ClientPhoneRepository,
    private val stateRepository: StateRepository,
    private val productRepository: ProductRepository,
    private val orderProductRepository: OrderProductRepository
) {

    @Transactional
    fun create(request: CreateOrderCompleteRequest): OrderCompleteResponse {
        // 1. Buscar o crear cliente por número de identificación
        var client = clientRepository.findByIdNumber(request.clientIdNumber)
        
        if (client == null) {
            // Cliente nuevo: crear con idUser = clientIdNumber (UUID generado si es necesario)
            val newClient = Client(
                idUser = java.util.UUID.randomUUID().toString(),
                idNumber = request.clientIdNumber,
                name = request.clientName,
                defaultPhone = request.phone,
                defaultCity = request.city,
                defaultAddress = request.address
            )
            client = clientRepository.save(newClient)
        } else {
            // Cliente existente: actualizar datos si cambiaron
            val updatedClient = client.copy(
                name = request.clientName,
                defaultPhone = request.phone,
                defaultCity = request.city,
                defaultAddress = request.address,
                updatedAt = LocalDateTime.now()
            )
            client = clientRepository.save(updatedClient)
        }

        // 2. Agregar teléfono si no existe
        val existingPhones = clientPhoneRepository.findByClientId(client.idUser)
        if (existingPhones.none { it.phone == request.phone }) {
            val clientPhone = ClientPhone(
                client = client,
                phone = request.phone
            )
            clientPhoneRepository.save(clientPhone)
        }

        // 3. Agregar dirección si no existe
        val existingAddresses = clientAddressRepository.findByClientId(client.idUser)
        if (existingAddresses.none { it.address == request.address && it.city == request.city }) {
            val clientAddress = ClientAddress(
                client = client,
                address = request.address,
                city = request.city
            )
            clientAddressRepository.save(clientAddress)
        }

        // 4. Buscar estado de operación por nombre (usar "Pendiente" por defecto)
        val operationState = stateRepository.findAll().find { it.name.equals("Pendiente", ignoreCase = true) }
            ?: stateRepository.findAll().firstOrNull()
            ?: throw IllegalArgumentException("No se encontraron estados de operación en la base de datos")

        // 5. Crear la orden
        val order = Order(
            client = client,
            operationState = operationState,
            orderPrice = request.order.orderPrice,
            orderAddress = request.address,
            orderPhone = request.phone,
            orderCity = request.city
        )
        val savedOrder = orderRepository.save(order)

        // 6. Crear productos de la orden
        val orderProducts = request.order.orderProducts.map { productData ->
            val product = productRepository.findById(productData.idProduct)
                .orElseThrow { IllegalArgumentException("Producto con id ${productData.idProduct} no encontrado") }

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
            clientId = this.client.idUser,
            clientName = this.client.name,
            operationStateId = this.operationState.id!!,
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
    val clientId: String,
    val clientName: String,
    val operationStateId: Long,
    val operationStateName: String,
    val orderPrice: Long,
    val orderAddress: String,
    val orderPhone: String,
    val orderCity: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
