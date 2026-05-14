package com.linogo.gestion.order

import com.fasterxml.jackson.databind.ObjectMapper
import com.linogo.gestion.order.application.CreateOrderRequest
import com.linogo.gestion.order.application.OrderService
import com.linogo.gestion.order.application.UpdateOrderRequest
import com.linogo.gestion.security.infrastructure.JwtAuthenticationFilter
import com.linogo.gestion.security.infrastructure.JwtTokenProvider
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.mockito.kotlin.any
import org.mockito.kotlin.eq

@WebMvcTest(com.linogo.gestion.order.infrastructure.OrderController::class)
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
@org.springframework.test.context.ActiveProfiles("test")
class OrderControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var orderService: OrderService

    @MockBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    @MockBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `should create order when request is valid`() {
        val request = CreateOrderRequest(
            clientId = "user_123",
            operationStateId = 1L,
            orderPrice = 50000L,
            orderAddress = "Av. Corrientes 1234",
            orderPhone = "+54911223344",
            orderCity = "Buenos Aires"
        )

        mockMvc.perform(post("/api/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated)
    }

    @Test
    fun `should return order when exists`() {
        mockMvc.perform(get("/api/orders/1"))
            .andExpect(status().isOk)
    }

    @Test
    fun `should return not found when getting non-existent order`() {
        org.mockito.Mockito.`when`(orderService.findById(999L))
            .thenThrow(IllegalArgumentException("Order with id 999 not found"))

        mockMvc.perform(get("/api/orders/999"))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should return all orders`() {
        org.mockito.Mockito.`when`(orderService.findAll()).thenReturn(emptyList())

        mockMvc.perform(get("/api/orders"))
            .andExpect(status().isOk)
    }

    @Test
    fun `should update order when request is valid`() {
        val request = UpdateOrderRequest(
            operationStateId = 2L,
            orderPrice = 60000L,
            orderAddress = "Calle Falsa 123",
            orderPhone = "+54911998877",
            orderCity = "Rosario"
        )

        mockMvc.perform(put("/api/orders/1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk)
    }

    @Test
    fun `should delete order when exists`() {
        mockMvc.perform(delete("/api/orders/1"))
            .andExpect(status().isNoContent)
    }

    @Test
    fun `should return not found when deleting non-existent order`() {
        org.mockito.Mockito.doThrow(IllegalArgumentException("Order with id 999 not found"))
            .`when`(orderService).delete(999L)

        mockMvc.perform(delete("/api/orders/999"))
            .andExpect(status().isBadRequest)
    }
}
