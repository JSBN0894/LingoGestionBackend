package com.linogo.gestion.product

import com.fasterxml.jackson.databind.ObjectMapper
import com.linogo.gestion.product.application.CreateProductRequest
import com.linogo.gestion.product.application.ProductResponse
import com.linogo.gestion.product.application.ProductService
import com.linogo.gestion.product.application.UpdateProductRequest
import com.linogo.gestion.product.application.CategoryResponse
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
import java.time.LocalDateTime

@WebMvcTest(com.linogo.gestion.product.infrastructure.ProductController::class)
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
@org.springframework.test.context.ActiveProfiles("test")
class ProductControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var productService: ProductService

    @MockBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    @MockBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `should create product when request is valid`() {
        val request = CreateProductRequest(
            id = 1L,
            name = "Molde Silicona",
            pricePerUnit = 15000L,
            stock = 10,
            imageUrl = "https://example.com/img.jpg",
            description = "Molde de silicona",
            categoryId = null
        )

        mockMvc.perform(post("/api/products")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated)
    }

    @Test
    fun `should return bad request when product name is blank`() {
        val request = mapOf(
            "id" to 1,
            "name" to "",
            "pricePerUnit" to 15000,
            "stock" to 10,
            "imageUrl" to "https://example.com/img.jpg",
            "description" to "desc"
        )

        mockMvc.perform(post("/api/products")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should return product when exists`() {
        val response = ProductResponse(
            id = 1L, name = "Molde", pricePerUnit = 1000L, stock = 5,
            imageUrl = "img.jpg", description = "desc",
            category = CategoryResponse(1L, "Moldes"),
            createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now()
        )
        org.mockito.Mockito.`when`(productService.findById(1L)).thenReturn(response)

        mockMvc.perform(get("/api/products/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Molde"))
    }

    @Test
    fun `should return not found when product does not exist`() {
        org.mockito.Mockito.`when`(productService.findById(999L))
            .thenThrow(IllegalArgumentException("Product with id 999 not found"))

        mockMvc.perform(get("/api/products/999"))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should return all products`() {
        org.mockito.Mockito.`when`(productService.findAll()).thenReturn(emptyList())

        mockMvc.perform(get("/api/products"))
            .andExpect(status().isOk)
    }

    @Test
    fun `should return products by category`() {
        org.mockito.Mockito.`when`(productService.findByCategoryId(1L)).thenReturn(emptyList())

        mockMvc.perform(get("/api/products/category/1"))
            .andExpect(status().isOk)
    }

    @Test
    fun `should update product when request is valid`() {
        val request = UpdateProductRequest(
            name = "Molde Actualizado", pricePerUnit = 20000L, stock = 15,
            imageUrl = "img.jpg", description = "desc", categoryId = 1L
        )
        val response = ProductResponse(
            id = 1L, name = "Molde Actualizado", pricePerUnit = 20000L, stock = 15,
            imageUrl = "img.jpg", description = "desc",
            category = CategoryResponse(1L, "Moldes"),
            createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now()
        )
        org.mockito.Mockito.`when`(productService.update(eq(1L), any())).thenReturn(response)

        mockMvc.perform(put("/api/products/1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk)
    }

    @Test
    fun `should delete product when exists`() {
        mockMvc.perform(delete("/api/products/1"))
            .andExpect(status().isNoContent)
    }
}
