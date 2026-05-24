package com.linogo.gestion.product.infrastructure

import com.linogo.gestion.product.application.ProductService
import com.linogo.gestion.product.application.CreateProductRequest
import com.linogo.gestion.product.application.UpdateProductRequest
import com.linogo.gestion.security.config.Authenticated
import com.linogo.gestion.security.config.ProduccionOnly
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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/products")
class ProductController(
    private val productService: ProductService
) {

    @PostMapping
    @ProduccionOnly
    fun create(@Valid @RequestBody request: CreateProductRequest): ResponseEntity<Any> {
        val response = productService.create(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @GetMapping("/{id}")
    @Authenticated
    fun getById(@PathVariable id: Long): ResponseEntity<Any> {
        val response = productService.findById(id)
        return ResponseEntity.ok(response)
    }

    @GetMapping
    @Authenticated
    fun getAll(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int
    ): ResponseEntity<Any> {
        val responses = productService.findAll(page, size)
        return ResponseEntity.ok(responses)
    }

    @GetMapping("/category/{categoryId}")
    @Authenticated
    fun getByCategoryId(@PathVariable categoryId: Long): ResponseEntity<Any> {
        val responses = productService.findByCategoryId(categoryId)
        return ResponseEntity.ok(responses)
    }

    @PutMapping("/{id}")
    @ProduccionOnly
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateProductRequest
    ): ResponseEntity<Any> {
        val response = productService.update(id, request)
        return ResponseEntity.ok(response)
    }

    @DeleteMapping("/{id}")
    @ProduccionOnly
    fun delete(@PathVariable id: Long): ResponseEntity<Void> {
        productService.delete(id)
        return ResponseEntity.noContent().build()
    }
}
