package com.linogo.gestion.product

import com.linogo.gestion.category.domain.Category
import com.linogo.gestion.category.infrastructure.CategoryRepository
import com.linogo.gestion.product.application.CreateProductRequest
import com.linogo.gestion.product.application.ProductService
import com.linogo.gestion.product.application.UpdateProductRequest
import com.linogo.gestion.product.domain.Product
import com.linogo.gestion.product.domain.ProductRepository
import com.linogo.gestion.sync.application.SyncVersionService
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
class ProductServiceTest {

    @Mock
    private lateinit var productRepository: ProductRepository

    @Mock
    private lateinit var categoryRepository: CategoryRepository

    @Mock
    private lateinit var syncVersionService: SyncVersionService

    @InjectMocks
    private lateinit var productService: ProductService

    private lateinit var existingProduct: Product
    private lateinit var category: Category
    private lateinit var createRequest: CreateProductRequest
    private lateinit var updateRequest: UpdateProductRequest

    @BeforeEach
    fun setUp() {
        category = Category(id = 1L, name = "Moldes")

        existingProduct = Product(
            id = 1L,
            name = "Molde Silicona",
            pricePerUnit = 15000L,
            stock = 10,
            imageUrl = "https://example.com/image.jpg",
            description = "Molde de silicona para velas",
            category = category,
            createdAt = LocalDateTime.now().minusDays(1),
            updatedAt = LocalDateTime.now()
        )

        createRequest = CreateProductRequest(
            name = "Molde Metal",
            pricePerUnit = 25000L,
            stock = 5,
            imageUrl = "https://example.com/metal.jpg",
            description = "Molde de metal para velas",
            categoryId = 1L
        )

        updateRequest = UpdateProductRequest(
            name = "Molde Silicona Pro",
            pricePerUnit = 18000L,
            stock = 20,
            imageUrl = "https://example.com/pro.jpg",
            description = "Molde de silicona profesional",
            categoryId = 1L
        )
    }

    @Test
    fun `create should save product when data is valid with category`() {
        val savedProduct = existingProduct.copy(id = 2L, name = "Molde Metal")
        `when`(categoryRepository.findById(1L)).thenReturn(Optional.of(category))
        `when`(productRepository.save(any())).thenReturn(savedProduct)

        val response = productService.create(createRequest)

        assertNotNull(response)
        assertEquals(2L, response.id)
        assertEquals("Molde Metal", response.name)
        assertEquals(1L, response.category?.id)
        verify(categoryRepository).findById(1L)
        verify(productRepository).save(any())
    }

    @Test
    fun `create should throw IllegalArgumentException when category not found`() {
        `when`(categoryRepository.findById(999L)).thenReturn(Optional.empty())

        val request = createRequest.copy(categoryId = 999L)
        val exception = assertThrows(IllegalArgumentException::class.java) {
            productService.create(request)
        }

        assertEquals("Category with id 999 not found", exception.message)
    }

    @Test
    fun `findById should return ProductResponse when product exists`() {
        `when`(productRepository.findById(1L)).thenReturn(existingProduct)

        val response = productService.findById(1L)

        assertNotNull(response)
        assertEquals(1L, response.id)
        assertEquals("Molde Silicona", response.name)
        verify(productRepository).findById(1L)
    }

    @Test
    fun `findById should throw IllegalArgumentException when product does not exist`() {
        `when`(productRepository.findById(999L)).thenReturn(null)

        val exception = assertThrows(IllegalArgumentException::class.java) {
            productService.findById(999L)
        }

        assertEquals("Product with id 999 not found", exception.message)
        verify(productRepository).findById(999L)
    }

    @Test
    fun `findAll should return list of ProductResponse`() {
        `when`(productRepository.findAll()).thenReturn(listOf(existingProduct))

        val responses = productService.findAll()

        assertEquals(1, responses.size)
        assertEquals(1L, responses[0].id)
        verify(productRepository).findAll()
    }

    @Test
    fun `findAll should return empty list when no products exist`() {
        `when`(productRepository.findAll()).thenReturn(emptyList())

        val responses = productService.findAll()

        assertEquals(0, responses.size)
        verify(productRepository).findAll()
    }

    @Test
    fun `findByCategoryId should return list of ProductResponse`() {
        `when`(productRepository.findByCategoryId(1L)).thenReturn(listOf(existingProduct))

        val responses = productService.findByCategoryId(1L)

        assertEquals(1, responses.size)
        assertEquals(1L, responses[0].id)
        verify(productRepository).findByCategoryId(1L)
    }

    @Test
    fun `findByCategoryId should return empty list when no products for category`() {
        `when`(productRepository.findByCategoryId(999L)).thenReturn(emptyList())

        val responses = productService.findByCategoryId(999L)

        assertEquals(0, responses.size)
        verify(productRepository).findByCategoryId(999L)
    }

    @Test
    fun `update should return updated ProductResponse when product exists`() {
        val updatedProduct = existingProduct.copy(
            name = updateRequest.name,
            pricePerUnit = updateRequest.pricePerUnit,
            stock = updateRequest.stock,
            imageUrl = updateRequest.imageUrl ?: existingProduct.imageUrl,
            description = updateRequest.description
        )
        `when`(productRepository.findById(1L)).thenReturn(existingProduct)
        `when`(categoryRepository.findById(1L)).thenReturn(Optional.of(category))
        `when`(productRepository.save(any())).thenReturn(updatedProduct)

        val response = productService.update(1L, updateRequest)

        assertNotNull(response)
        assertEquals("Molde Silicona Pro", response.name)
        assertEquals(18000L, response.pricePerUnit)
        verify(productRepository).findById(1L)
        verify(categoryRepository).findById(1L)
        verify(productRepository).save(any())
    }

    @Test
    fun `update should throw IllegalArgumentException when product does not exist`() {
        `when`(productRepository.findById(999L)).thenReturn(null)

        val exception = assertThrows(IllegalArgumentException::class.java) {
            productService.update(999L, updateRequest)
        }

        assertEquals("Product with id 999 not found", exception.message)
        verify(productRepository).findById(999L)
    }

    @Test
    fun `update should throw IllegalArgumentException when category not found`() {
        `when`(productRepository.findById(1L)).thenReturn(existingProduct)
        `when`(categoryRepository.findById(999L)).thenReturn(Optional.empty())

        val request = updateRequest.copy(categoryId = 999L)
        val exception = assertThrows(IllegalArgumentException::class.java) {
            productService.update(1L, request)
        }

        assertEquals("Category with id 999 not found", exception.message)
    }

    @Test
    fun `delete should remove product when product exists`() {
        `when`(productRepository.findById(1L)).thenReturn(existingProduct)

        productService.delete(1L)

        verify(productRepository).findById(1L)
        verify(productRepository).deleteById(1L)
    }

    @Test
    fun `delete should throw IllegalArgumentException when product does not exist`() {
        `when`(productRepository.findById(999L)).thenReturn(null)

        val exception = assertThrows(IllegalArgumentException::class.java) {
            productService.delete(999L)
        }

        assertEquals("Product with id 999 not found", exception.message)
        verify(productRepository).findById(999L)
    }
}
