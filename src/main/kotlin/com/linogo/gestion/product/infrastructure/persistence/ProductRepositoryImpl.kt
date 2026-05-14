package com.linogo.gestion.product.infrastructure.persistence

import com.linogo.gestion.product.domain.Product
import com.linogo.gestion.product.domain.ProductRepository
import com.linogo.gestion.product.infrastructure.persistence.jpa.ProductJpaRepository
import com.linogo.gestion.product.infrastructure.persistence.mapper.ProductPersistenceMapper
import org.springframework.stereotype.Repository

@Repository
class ProductRepositoryImpl(
    private val jpaRepository: ProductJpaRepository,
    private val mapper: ProductPersistenceMapper
) : ProductRepository {

    override fun findById(id: Long): Product? {
        return jpaRepository.findById(id).map { mapper.toDomain(it) }.orElse(null)
    }

    override fun findAll(): List<Product> {
        return jpaRepository.findAll().map { mapper.toDomain(it) }
    }

    override fun findByCategoryId(categoryId: Long): List<Product> {
        return jpaRepository.findByCategoryId(categoryId).map { mapper.toDomain(it) }
    }

    override fun save(product: Product): Product {
        val entity = mapper.toEntity(product)
        val savedEntity = jpaRepository.save(entity)
        return mapper.toDomain(savedEntity)
    }

    override fun deleteById(id: Long) {
        jpaRepository.deleteById(id)
    }
}
