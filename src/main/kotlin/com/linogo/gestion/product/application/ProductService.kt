package com.linogo.gestion.product.application

import com.linogo.gestion.category.infrastructure.CategoryRepository
import com.linogo.gestion.product.domain.Product
import com.linogo.gestion.product.domain.ProductRepository
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
        val category = categoryRepository.findById(request.categoryId)
            .orElseThrow { IllegalArgumentException("Category with id ${request.categoryId} not found") }

        val product = Product(
            id = 0,
            name = request.name,
            pricePerUnit = request.pricePerUnit,
            stock = request.stock,
            imageUrl = request.imageUrl ?: "",
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
            ?: throw IllegalArgumentException("Product with id $id not found")
        return product.toResponse()
    }

    @Transactional(readOnly = true)
    fun findAll(page: Int = 0, size: Int = 50): List<ProductResponse> {
        return productRepository.findAll()
            .drop(page * size)
            .take(size)
            .map { it.toResponse() }
    }

    @Transactional(readOnly = true)
    fun findByCategoryId(categoryId: Long): List<ProductResponse> {
        return productRepository.findByCategoryId(categoryId).map { it.toResponse() }
    }

    @Transactional
    fun update(id: Long, request: UpdateProductRequest): ProductResponse {
        val product = productRepository.findById(id)
            ?: throw IllegalArgumentException("Product with id $id not found")

        val category = categoryRepository.findById(request.categoryId)
            .orElseThrow { IllegalArgumentException("Category with id ${request.categoryId} not found") }

        val updated = product.copy(
            name = request.name,
            pricePerUnit = request.pricePerUnit,
            stock = request.stock,
            imageUrl = request.imageUrl ?: product.imageUrl,
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
            ?: throw IllegalArgumentException("Product with id $id not found")
        val productName = product.name
        productRepository.deleteById(id)
        syncVersionService.incrementVersion("Producto eliminado: $productName")
    }

    private fun Product.toResponse(): ProductResponse {
        return ProductResponse(
            id = this.id,
            name = this.name,
            pricePerUnit = this.pricePerUnit,
            stock = this.stock,
            imageUrl = this.imageUrl,
            description = this.description,
            category = CategoryResponse(this.category.id!!, this.category.name),
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }
}
