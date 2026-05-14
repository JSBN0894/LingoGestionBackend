package com.linogo.gestion.product.infrastructure.persistence.mapper

import com.linogo.gestion.product.domain.Product
import com.linogo.gestion.product.infrastructure.persistence.entity.ProductEntity
import org.springframework.stereotype.Component

@Component
class ProductPersistenceMapper {
    fun toDomain(entity: ProductEntity): Product {
        return Product(
            id = entity.id,
            name = entity.name,
            pricePerUnit = entity.pricePerUnit,
            stock = entity.stock,
            imageUrl = entity.imageUrl,
            description = entity.description,
            category = entity.category,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    fun toEntity(domain: Product): ProductEntity {
        return ProductEntity(
            id = domain.id,
            name = domain.name,
            pricePerUnit = domain.pricePerUnit,
            stock = domain.stock,
            imageUrl = domain.imageUrl,
            description = domain.description,
            category = domain.category,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt
        )
    }
}
