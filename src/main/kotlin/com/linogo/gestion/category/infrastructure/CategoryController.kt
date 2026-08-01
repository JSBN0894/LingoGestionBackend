package com.linogo.gestion.category.infrastructure

import com.linogo.gestion.category.application.CategoryResponse
import com.linogo.gestion.category.application.CategoryService
import com.linogo.gestion.category.application.CreateCategoryRequest
import com.linogo.gestion.category.application.UpdateCategoryRequest
import com.linogo.gestion.security.config.Authenticated
import com.linogo.gestion.security.config.RequiresPermission
import com.linogo.gestion.security.domain.Permission
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/categories")
class CategoryController(
    private val categoryService: CategoryService
) {

    @PostMapping
    @RequiresPermission(Permission.CATEGORIES_MANAGE)
    fun create(@Valid @RequestBody request: CreateCategoryRequest): ResponseEntity<CategoryResponse> {
        val response = categoryService.create(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @GetMapping("/{id}")
    @Authenticated
    fun getById(@PathVariable id: Long): ResponseEntity<CategoryResponse> {
        val response = categoryService.findById(id)
        return ResponseEntity.ok(response)
    }

    @GetMapping
    @Authenticated
    fun getAll(): ResponseEntity<List<CategoryResponse>> {
        val responses = categoryService.findAll()
        return ResponseEntity.ok(responses)
    }

    @PutMapping("/{id}")
    @RequiresPermission(Permission.CATEGORIES_MANAGE)
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateCategoryRequest
    ): ResponseEntity<CategoryResponse> {
        val response = categoryService.update(id, request)
        return ResponseEntity.ok(response)
    }

    @DeleteMapping("/{id}")
    @RequiresPermission(Permission.CATEGORIES_MANAGE)
    fun delete(@PathVariable id: Long): ResponseEntity<Void> {
        categoryService.delete(id)
        return ResponseEntity.noContent().build()
    }
}
