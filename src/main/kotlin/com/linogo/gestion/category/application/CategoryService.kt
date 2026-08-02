package com.linogo.gestion.category.application

import com.linogo.gestion.category.domain.Category
import com.linogo.gestion.category.infrastructure.CategoryRepository
import com.linogo.gestion.exception.AlreadyExistsException
import com.linogo.gestion.exception.NotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class CategoryService(
    private val categoryRepository: CategoryRepository
) {

    @Transactional
    fun create(request: CreateCategoryRequest): CategoryResponse {
        val parent = request.parentId?.let {
            categoryRepository.findById(it)
                .orElseThrow { NotFoundException("Category", it) }
        }

        val category = Category(
            name = request.name,
            parent = parent
        )

        val saved = categoryRepository.save(category)
        return saved.toResponse()
    }

    @Transactional(readOnly = true)
    fun findById(id: Long): CategoryResponse {
        val category = categoryRepository.findById(id)
            .orElseThrow { NotFoundException("Category", id) }
        return category.toResponse()
    }

    @Transactional(readOnly = true)
    fun findAll(): List<CategoryResponse> {
        return categoryRepository.findAll().map { it.toResponse() }
    }

    @Transactional
    fun update(id: Long, request: UpdateCategoryRequest): CategoryResponse {
        val category = categoryRepository.findById(id)
            .orElseThrow { NotFoundException("Category", id) }

        val parent = request.parentId?.let {
            categoryRepository.findById(it)
                .orElseThrow { NotFoundException("Category", it) }
        }

        val updated = category.copy(
            name = request.name,
            parent = parent,
            updatedAt = LocalDateTime.now()
        )

        val saved = categoryRepository.save(updated)
        return saved.toResponse()
    }

    @Transactional
    fun delete(id: Long) {
        if (!categoryRepository.existsById(id)) {
            throw NotFoundException("Category", id)
        }
        categoryRepository.deleteById(id)
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
