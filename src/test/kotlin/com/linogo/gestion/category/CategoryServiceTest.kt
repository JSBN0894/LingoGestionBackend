package com.linogo.gestion.category

import com.linogo.gestion.category.application.CategoryService
import com.linogo.gestion.category.application.CreateCategoryRequest
import com.linogo.gestion.category.application.UpdateCategoryRequest
import com.linogo.gestion.category.domain.Category
import com.linogo.gestion.category.infrastructure.CategoryRepository
import com.linogo.gestion.exception.AlreadyExistsException
import com.linogo.gestion.exception.NotFoundException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDateTime
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class CategoryServiceTest {

    @Mock
    private lateinit var categoryRepository: CategoryRepository

    @InjectMocks
    private lateinit var categoryService: CategoryService

    private lateinit var parentCategory: Category
    private lateinit var childCategory: Category
    private lateinit var createRequest: CreateCategoryRequest

    @BeforeEach
    fun setUp() {
        parentCategory = Category(
            id = 1L,
            name = "Moldes",
            parent = null,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        childCategory = Category(
            id = 2L,
            name = "Silicona",
            parent = parentCategory,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        createRequest = CreateCategoryRequest(
            id = 3L,
            name = "Metal",
            parentId = 1L
        )
    }

    @Test
    fun `create should save category when data is valid`() {
        // Given
        val savedCategory = Category(id = 3L, name = "Metal", parent = parentCategory)
        `when`(categoryRepository.existsById(3L)).thenReturn(false)
        `when`(categoryRepository.findById(1L)).thenReturn(Optional.of(parentCategory))
        `when`(categoryRepository.save(org.mockito.ArgumentMatchers.any())).thenReturn(savedCategory)

        // When
        val response = categoryService.create(createRequest)

        // Then
        assertNotNull(response)
        assertEquals(3L, response.id)
        assertEquals("Metal", response.name)
        assertEquals(1L, response.parentId)
        verify(categoryRepository).save(org.mockito.ArgumentMatchers.any())
    }

    @Test
    fun `create should throw AlreadyExistsException when ID already exists`() {
        // Given
        `when`(categoryRepository.existsById(createRequest.id)).thenReturn(true)

        // When & Then
        assertThrows(AlreadyExistsException::class.java) {
            categoryService.create(createRequest)
        }
    }

    @Test
    fun `findById should return response when exists`() {
        // Given
        `when`(categoryRepository.findById(2L)).thenReturn(Optional.of(childCategory))

        // When
        val response = categoryService.findById(2L)

        // Then
        assertEquals("Silicona", response.name)
        assertEquals(1L, response.parentId)
    }

    @Test
    fun `update should change name and parent`() {
        // Given
        val updateRequest = UpdateCategoryRequest(name = "Silicona Pro", parentId = null)
        val updatedCategory = childCategory.copy(name = "Silicona Pro", parent = null)
        
        `when`(categoryRepository.findById(2L)).thenReturn(Optional.of(childCategory))
        `when`(categoryRepository.save(org.mockito.ArgumentMatchers.any())).thenReturn(updatedCategory)

        // When
        val response = categoryService.update(2L, updateRequest)

        // Then
        assertEquals("Silicona Pro", response.name)
        assertEquals(null, response.parentId)
    }
}
