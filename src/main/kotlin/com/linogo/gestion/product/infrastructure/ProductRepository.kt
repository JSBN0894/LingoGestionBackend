package com.linogo.gestion.product.infrastructure

import com.linogo.gestion.product.domain.Product
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ProductRepository : JpaRepository<Product, Long> {
    
    @EntityGraph(attributePaths = ["category"])
    fun findByCategoryId(categoryId: Long): List<Product>
    
    @EntityGraph(attributePaths = ["category"])
    override fun findAll(): List<Product>
    
    @EntityGraph(attributePaths = ["category"])
    override fun findById(id: Long): java.util.Optional<Product>
}
