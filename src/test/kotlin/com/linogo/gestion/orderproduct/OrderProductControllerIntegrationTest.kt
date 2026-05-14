package com.linogo.gestion.orderproduct

import com.fasterxml.jackson.databind.ObjectMapper
import com.linogo.gestion.orderproduct.application.CreateOrderProductRequest
import com.linogo.gestion.orderproduct.application.OrderProductService
import com.linogo.gestion.orderproduct.application.UpdateOrderProductRequest
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

@WebMvcTest(com.linogo.gestion.orderproduct.infrastructure.OrderProductController::class)
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
@org.springframework.test.context.ActiveProfiles("test")
class OrderProductControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var orderProductService: OrderProductService

    @MockBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    @MockBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `should create order product when request is valid`() {
        val request = CreateOrderProductRequest(orderId = 1L, productId = 1L, quantity = 2, price = 20000L)

        mockMvc.perform(post("/api/order-products")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated)
    }

    @Test
    fun `should return order products by order id`() {
        org.mockito.Mockito.`when`(orderProductService.findByOrderId(1L)).thenReturn(emptyList())

        mockMvc.perform(get("/api/order-products/order/1"))
            .andExpect(status().isOk)
    }

    @Test
    fun `should return all order products`() {
        org.mockito.Mockito.`when`(orderProductService.findAll()).thenReturn(emptyList())

        mockMvc.perform(get("/api/order-products"))
            .andExpect(status().isOk)
    }

    @Test
    fun `should update order product when request is valid`() {
        val request = UpdateOrderProductRequest(quantity = 3, price = 30000L)

        mockMvc.perform(put("/api/order-products/order/1/product/1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk)
    }

    @Test
    fun `should delete order product when exists`() {
        mockMvc.perform(delete("/api/order-products/order/1/product/1"))
            .andExpect(status().isNoContent)
    }
}
