package com.linogo.gestion.product.application

import com.linogo.gestion.category.infrastructure.CategoryRepository
import com.linogo.gestion.product.domain.Product
import com.linogo.gestion.product.infrastructure.ProductRepository
import com.linogo.gestion.sync.application.SyncVersionService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProductService(
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository,
    private val syncVersionService: SyncVersionService
) {

    @Transactional
    fun create(request: CreateProductRequest): ProductResponse {
        val category = request.categoryId?.let { id ->
            categoryRepository.findById(id)
                .orElseThrow { IllegalArgumentException("Category with id $id not found") }
        }

        val product = Product(
            name = request.name,
            pricePerUnit = request.pricePerUnit,
            stock = request.stock,
            imageUrl = request.imageUrl,
            description = request.description,
            category = category
        )

        val saved = productRepository.save(product)
        syncVersionService.incrementVersion("Producto creado: ${request.name}")
        return saved.toResponse()
    }

    @Transactional(readOnly = true)
    fun findById(id: Long): ProductResponse {
        val product = productRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Product with id $id not found") }
        return product.toResponse()
    }

    @Transactional(readOnly = true)
    fun findAll(): List<ProductResponse> {
        return productRepository.findAll().map { it.toResponse() }
    }

    @Transactional(readOnly = true)
    fun findByCategoryId(categoryId: Long): List<ProductResponse> {
        return productRepository.findByCategoryId(categoryId).map { it.toResponse() }
    }

    @Transactional
    fun update(id: Long, request: UpdateProductRequest): ProductResponse {
        val product = productRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Product with id $id not found") }

        val category = request.categoryId?.let { catId ->
            categoryRepository.findById(catId)
                .orElseThrow { IllegalArgumentException("Category with id $catId not found") }
        }

        val updated = product.copy(
            name = request.name,
            pricePerUnit = request.pricePerUnit,
            stock = request.stock,
            imageUrl = request.imageUrl,
            description = request.description,
            category = category,
            updatedAt = java.time.LocalDateTime.now()
        )

        val saved = productRepository.save(updated)
        syncVersionService.incrementVersion("Producto actualizado: ${request.name}")
        return saved.toResponse()
    }

    @Transactional
    fun delete(id: Long) {
        val product = productRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Product with id $id not found") }
        val productName = product.name
        productRepository.deleteById(id)
        syncVersionService.incrementVersion("Producto eliminado: $productName")
    }

    private fun Product.toResponse(): ProductResponse {
        return ProductResponse(
            id = this.id!!,
            name = this.name,
            pricePerUnit = this.pricePerUnit,
            stock = this.stock,
            imageUrl = this.imageUrl,
            description = this.description,
            category = this.category?.let { CategoryResponse(it.id!!, it.name) },
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }
}
