package com.linogo.gestion.category

import com.fasterxml.jackson.databind.ObjectMapper
import com.linogo.gestion.category.application.CategoryService
import com.linogo.gestion.category.application.CreateCategoryRequest
import com.linogo.gestion.category.application.UpdateCategoryRequest
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

@WebMvcTest(com.linogo.gestion.category.infrastructure.CategoryController::class)
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
@org.springframework.test.context.ActiveProfiles("test")
class CategoryControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var categoryService: CategoryService

    @MockBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    @MockBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `should create category when request is valid`() {
        val request = CreateCategoryRequest(name = "Moldes", parentId = null)

        mockMvc.perform(post("/api/categories")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated)
    }

    @Test
    fun `should return category when exists`() {
        mockMvc.perform(get("/api/categories/1"))
            .andExpect(status().isOk)
    }

    @Test
    fun `should return all categories`() {
        org.mockito.Mockito.`when`(categoryService.findAll()).thenReturn(emptyList())

        mockMvc.perform(get("/api/categories"))
            .andExpect(status().isOk)
    }

    @Test
    fun `should update category when request is valid`() {
        val request = UpdateCategoryRequest(name = "Moldes Pro", parentId = null)

        mockMvc.perform(put("/api/categories/1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk)
    }

    @Test
    fun `should delete category when exists`() {
        mockMvc.perform(delete("/api/categories/1"))
            .andExpect(status().isNoContent)
    }
}
