package com.linogo.gestion.product.infrastructure.persistence.entity

import com.linogo.gestion.category.domain.Category
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "products")
class ProductEntity(
    @Id
    val id: Long,

    @Column(nullable = false)
    val name: String,

    @Column(nullable = false)
    val pricePerUnit: Long,

    @Column(nullable = false)
    val stock: Int,

    @Column(nullable = false)
    val imageUrl: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    val description: String,

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "category_id", foreignKey = ForeignKey(name = "fk_product_category"))
    val category: Category? = null,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
