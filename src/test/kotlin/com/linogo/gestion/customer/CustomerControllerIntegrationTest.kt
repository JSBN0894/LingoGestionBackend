package com.linogo.gestion.customer

import com.fasterxml.jackson.databind.ObjectMapper
import com.linogo.gestion.customer.application.CustomerService
import com.linogo.gestion.customer.domain.Customer
import com.linogo.gestion.security.infrastructure.JwtAuthenticationFilter
import com.linogo.gestion.security.infrastructure.JwtTokenProvider
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest(com.linogo.gestion.customer.infrastructure.rest.CustomerController::class)
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
@org.springframework.test.context.ActiveProfiles("test")
class CustomerControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var customerService: CustomerService

    @MockBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    @MockBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `should create customer when request is valid`() {
        val request = Customer(cedula = 123456789L, name = "Juan Pérez")

        mockMvc.perform(post("/api/customers")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated)
    }

    @Test
    fun `should return customer when exists`() {
        `when`(customerService.getCustomer(123456789L))
            .thenReturn(Customer(cedula = 123456789L, name = "Juan"))

        mockMvc.perform(get("/api/customers/123456789"))
            .andExpect(status().isOk)
    }

    @Test
    fun `should return null when customer not found`() {
        `when`(customerService.getCustomer(999L)).thenReturn(null)

        mockMvc.perform(get("/api/customers/999"))
            .andExpect(status().isOk)
    }

    @Test
    fun `should return all customers`() {
        `when`(customerService.getAllCustomers()).thenReturn(emptyList())

        mockMvc.perform(get("/api/customers"))
            .andExpect(status().isOk)
    }

    @Test
    fun `should delete customer when exists`() {
        mockMvc.perform(delete("/api/customers/123456789"))
            .andExpect(status().isNoContent)
    }
}
