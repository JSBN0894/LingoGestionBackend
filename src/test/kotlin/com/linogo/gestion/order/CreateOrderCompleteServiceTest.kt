package com.linogo.gestion.order

import com.linogo.gestion.client.domain.Client
import com.linogo.gestion.client.infrastructure.ClientRepository
import com.linogo.gestion.clientaddress.domain.ClientAddress
import com.linogo.gestion.clientaddress.infrastructure.ClientAddressRepository
import com.linogo.gestion.clientphone.domain.ClientPhone
import com.linogo.gestion.clientphone.infrastructure.ClientPhoneRepository
import com.linogo.gestion.order.application.CreateOrderCompleteRequest
import com.linogo.gestion.order.application.CreateOrderCompleteService
import com.linogo.gestion.order.application.OrderData
import com.linogo.gestion.order.application.OrderProductData
import com.linogo.gestion.order.domain.Order
import com.linogo.gestion.order.infrastructure.OrderRepository
import com.linogo.gestion.orderproduct.infrastructure.OrderProductRepository
import com.linogo.gestion.product.infrastructure.persistence.entity.ProductEntity
import com.linogo.gestion.product.infrastructure.ProductRepository
import com.linogo.gestion.state.domain.State
import com.linogo.gestion.state.infrastructure.StateRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import java.time.LocalDateTime
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class CreateOrderCompleteServiceTest {

    @Mock
    private lateinit var orderRepository: OrderRepository

    @Mock
    private lateinit var clientRepository: ClientRepository

    @Mock
    private lateinit var clientAddressRepository: ClientAddressRepository

    @Mock
    private lateinit var clientPhoneRepository: ClientPhoneRepository

    @Mock
    private lateinit var stateRepository: StateRepository

    @Mock
    private lateinit var productRepository: ProductRepository

    @Mock
    private lateinit var orderProductRepository: OrderProductRepository

    @InjectMocks
    private lateinit var createOrderCompleteService: CreateOrderCompleteService

    private lateinit var existingClient: Client
    private lateinit var state: State
    private lateinit var product: ProductEntity
    private lateinit var request: CreateOrderCompleteRequest
    private lateinit var savedOrder: Order

    @BeforeEach
    fun setUp() {
        existingClient = Client(
            idUser = "user_123", name = "Juan Pérez",
            defaultPhone = "+54911223344", defaultCity = "Buenos Aires",
            defaultAddress = "Av. Corrientes 1234",
            createdAt = LocalDateTime.now().minusDays(1), updatedAt = LocalDateTime.now()
        )

        state = State(id = 1L, name = "Pendiente", priority = 1,
            createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now())

        product = ProductEntity(id = 1L, name = "Molde Silicona", pricePerUnit = 15000L, stock = 10,
            imageUrl = "img.jpg", description = "Molde de silicona",
            createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now())

        savedOrder = Order(id = 1L, client = existingClient, operationState = state,
            orderPrice = 30000L, orderAddress = "Av. Corrientes 1234",
            orderPhone = "+54911223344", orderCity = "Buenos Aires",
            createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now())

        request = CreateOrderCompleteRequest(
            name = "Juan Pérez",
            idUser = "user_123",
            address = "Av. Corrientes 1234",
            city = "Buenos Aires",
            phone = "+54911223344",
            order = OrderData(
                orderPrice = 30000L,
                operationState = "Pendiente",
                orderProducts = listOf(OrderProductData(idProduct = 1L, quantity = 2))
            )
        )
    }

    @Test
    fun `create should create complete order when client exists`() {
        val existingPhone = ClientPhone(client = existingClient, phone = "+54911223344")
        val existingAddress = ClientAddress(client = existingClient, address = "Av. Corrientes 1234", city = "Buenos Aires")

        `when`(clientRepository.findById("user_123")).thenReturn(Optional.of(existingClient))
        `when`(clientPhoneRepository.findByClientId("user_123")).thenReturn(listOf(existingPhone))
        `when`(clientAddressRepository.findByClientId("user_123")).thenReturn(listOf(existingAddress))
        `when`(stateRepository.findAll()).thenReturn(listOf(state))
        `when`(productRepository.findById(1L)).thenReturn(Optional.of(product))
        `when`(orderRepository.save(any())).thenReturn(savedOrder)

        val response = createOrderCompleteService.create(request)

        assertNotNull(response)
        assertEquals("user_123", response.clientId)
        assertEquals(30000L, response.orderPrice)
        verify(clientRepository).findById("user_123")
        verify(orderRepository).save(any())
        verify(orderProductRepository).saveAll(any<Iterable<com.linogo.gestion.orderproduct.domain.OrderProduct>>())
    }

    @Test
    fun `create should create new client when client does not exist`() {
        `when`(clientRepository.findById("user_123")).thenReturn(Optional.empty())
        `when`(clientRepository.save(any())).thenReturn(existingClient)
        `when`(clientPhoneRepository.findByClientId("user_123")).thenReturn(emptyList())
        `when`(clientAddressRepository.findByClientId("user_123")).thenReturn(emptyList())
        `when`(stateRepository.findAll()).thenReturn(listOf(state))
        `when`(productRepository.findById(1L)).thenReturn(Optional.of(product))
        `when`(orderRepository.save(any())).thenReturn(savedOrder)

        val response = createOrderCompleteService.create(request)

        assertNotNull(response)
        assertEquals("user_123", response.clientId)
        verify(clientRepository).findById("user_123")
        verify(clientRepository).save(any())
        verify(clientPhoneRepository).save(any())
        verify(clientAddressRepository).save(any())
    }

    @Test
    fun `create should add new phone when client exists but phone is different`() {
        `when`(clientRepository.findById("user_123")).thenReturn(Optional.of(existingClient))
        `when`(clientPhoneRepository.findByClientId("user_123")).thenReturn(emptyList())
        `when`(clientAddressRepository.findByClientId("user_123")).thenReturn(
            listOf(ClientAddress(client = existingClient, address = "Av. Corrientes 1234", city = "Buenos Aires"))
        )
        `when`(stateRepository.findAll()).thenReturn(listOf(state))
        `when`(productRepository.findById(1L)).thenReturn(Optional.of(product))
        `when`(orderRepository.save(any())).thenReturn(savedOrder)

        val response = createOrderCompleteService.create(request)

        assertNotNull(response)
        verify(clientPhoneRepository).save(any())
    }

    @Test
    fun `create should throw IllegalArgumentException when state not found`() {
        `when`(clientRepository.findById("user_123")).thenReturn(Optional.of(existingClient))
        `when`(clientPhoneRepository.findByClientId("user_123")).thenReturn(emptyList())
        `when`(clientAddressRepository.findByClientId("user_123")).thenReturn(emptyList())
        `when`(stateRepository.findAll()).thenReturn(emptyList())

        val exception = assertThrows(IllegalArgumentException::class.java) {
            createOrderCompleteService.create(request)
        }

        assertEquals("State 'Pendiente' not found", exception.message)
    }

    @Test
    fun `create should throw IllegalArgumentException when product not found`() {
        `when`(clientRepository.findById("user_123")).thenReturn(Optional.of(existingClient))
        `when`(clientPhoneRepository.findByClientId("user_123")).thenReturn(emptyList())
        `when`(clientAddressRepository.findByClientId("user_123")).thenReturn(emptyList())
        `when`(stateRepository.findAll()).thenReturn(listOf(state))
        `when`(orderRepository.save(any())).thenReturn(savedOrder)
        `when`(productRepository.findById(999L)).thenReturn(Optional.empty())

        val requestWithBadProduct = request.copy(
            order = request.order.copy(orderProducts = listOf(OrderProductData(idProduct = 999L, quantity = 1)))
        )

        val exception = assertThrows(IllegalArgumentException::class.java) {
            createOrderCompleteService.create(requestWithBadProduct)
        }

        assertEquals("Product with id 999 not found", exception.message)
    }
}
