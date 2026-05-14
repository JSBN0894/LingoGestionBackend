package com.linogo.gestion.product.infrastructure

import com.linogo.gestion.product.infrastructure.persistence.entity.ProductEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ProductRepository : JpaRepository<ProductEntity, Long>
