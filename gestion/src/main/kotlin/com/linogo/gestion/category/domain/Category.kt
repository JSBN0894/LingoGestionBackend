package com.linogo.gestion.category.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.linogo.gestion.product.domain.Product
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ForeignKey
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "categories")
data class Category(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(optional = true)
    @JoinColumn(name = "parentId", foreignKey = ForeignKey(name = "fk_category_parent"))
    val parent: Category? = null,

    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    @JsonIgnore
    val products: List<Product> = emptyList(),

    @Column(nullable = false)
    val name: String,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
