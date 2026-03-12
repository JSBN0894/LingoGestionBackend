package com.linogo.gestion.category.application

import com.linogo.gestion.category.domain.Category
import com.linogo.gestion.category.infrastructure.CategoryRepository
import com.linogo.gestion.sync.application.SyncVersionService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class CategoryService(
    private val categoryRepository: CategoryRepository,
    private val syncVersionService: SyncVersionService
) {

    @Transactional
    fun create(request: CreateCategoryRequest): CategoryResponse {
        val parent = request.parentId?.let { id ->
            categoryRepository.findById(id)
                .orElseThrow { IllegalArgumentException("Category with id $id not found") }
        }

        val category = Category(
            id = null, // Auto-generado
            name = request.name,
            parent = parent
        )

        val saved = categoryRepository.save(category)
        syncVersionService.incrementVersion("Categoría creada: ${request.name}")
        return saved.toResponse()
    }

    @Transactional(readOnly = true)
    fun findById(id: Long): CategoryResponse {
        val category = categoryRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Category with id $id not found") }
        return category.toResponse()
    }

    @Transactional(readOnly = true)
    fun findAll(): List<CategoryResponse> {
        return categoryRepository.findAll().map { it.toResponse() }
    }

    @Transactional
    fun update(id: Long, request: UpdateCategoryRequest): CategoryResponse {
        val category = categoryRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Category with id $id not found") }

        val parent = request.parentId?.let { catId ->
            categoryRepository.findById(catId)
                .orElseThrow { IllegalArgumentException("Category with id $catId not found") }
        }

        val updated = category.copy(
            name = request.name,
            parent = parent,
            updatedAt = LocalDateTime.now()
        )

        val saved = categoryRepository.save(updated)
        syncVersionService.incrementVersion("Categoría actualizada: ${request.name}")
        return saved.toResponse()
    }

    @Transactional
    fun delete(id: Long) {
        val category = categoryRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Category with id $id not found") }
        val categoryName = category.name
        categoryRepository.deleteById(id)
        syncVersionService.incrementVersion("Categoría eliminada: $categoryName")
    }

    private fun Category.toResponse(): CategoryResponse {
        return CategoryResponse(
            id = this.id!!,
            name = this.name,
            parentId = this.parent?.id,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }
}

data class CreateCategoryRequest(
    @field:jakarta.validation.constraints.NotBlank(message = "Name is required")
    val name: String,
    val parentId: Long? = null
)

data class UpdateCategoryRequest(
    @field:jakarta.validation.constraints.NotBlank(message = "Name is required")
    val name: String,
    val parentId: Long? = null
)

data class CategoryResponse(
    val id: Long,
    val name: String,
    val parentId: Long?,
    val createdAt: java.time.LocalDateTime,
    val updatedAt: java.time.LocalDateTime
)
