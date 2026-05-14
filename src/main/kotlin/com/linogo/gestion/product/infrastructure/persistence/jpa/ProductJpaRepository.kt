package com.linogo.gestion.product.infrastructure.persistence.jpa

import com.linogo.gestion.product.infrastructure.persistence.entity.ProductEntity
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface ProductJpaRepository : JpaRepository<ProductEntity, Long> {
    
    @EntityGraph(attributePaths = ["category"])
    fun findByCategoryId(categoryId: Long): List<ProductEntity>
    
    @EntityGraph(attributePaths = ["category"])
    override fun findAll(): List<ProductEntity>
    
    @EntityGraph(attributePaths = ["category"])
    override fun findById(id: Long): Optional<ProductEntity>
}
