package com.linogo.gestion.product.application

import com.linogo.gestion.product.domain.Product
import com.linogo.gestion.product.infrastructure.ProductRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProductService(
    private val productRepository: ProductRepository
) {

    @Transactional
    fun create(request: CreateProductRequest): ProductResponse {
        val product = Product(
            id = request.id,
            name = request.name,
            pricePerUnit = request.pricePerUnit,
            stock = request.stock,
            imageUrl = request.imageUrl,
            description = request.description,
            categoryId = request.categoryId
        )

        val saved = productRepository.save(product)
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

    @Transactional
    fun update(id: Long, request: UpdateProductRequest): ProductResponse {
        val product = productRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Product with id $id not found") }

        val updated = product.copy(
            name = request.name,
            pricePerUnit = request.pricePerUnit,
            stock = request.stock,
            imageUrl = request.imageUrl,
            description = request.description,
            categoryId = request.categoryId,
            updatedAt = java.time.LocalDateTime.now()
        )

        val saved = productRepository.save(updated)
        return saved.toResponse()
    }

    @Transactional
    fun delete(id: Long) {
        if (!productRepository.existsById(id)) {
            throw IllegalArgumentException("Product with id $id not found")
        }
        productRepository.deleteById(id)
    }

    private fun Product.toResponse(): ProductResponse {
        return ProductResponse(
            id = this.id,
            name = this.name,
            pricePerUnit = this.pricePerUnit,
            stock = this.stock,
            imageUrl = this.imageUrl,
            description = this.description,
            categoryId = this.categoryId,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }
}
