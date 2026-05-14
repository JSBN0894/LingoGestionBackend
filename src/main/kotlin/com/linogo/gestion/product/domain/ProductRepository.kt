package com.linogo.gestion.product.domain

interface ProductRepository {
    fun findById(id: Long): Product?
    fun findAll(): List<Product>
    fun findByCategoryId(categoryId: Long): List<Product>
    fun save(product: Product): Product
    fun deleteById(id: Long)
}
