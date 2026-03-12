package com.linogo.gestion.sync.application

import com.linogo.gestion.category.domain.Category
import com.linogo.gestion.product.domain.Product
import com.linogo.gestion.sync.domain.SyncVersion
import com.linogo.gestion.sync.infrastructure.SyncVersionRepository
import com.linogo.gestion.state.infrastructure.StateRepository
import com.linogo.gestion.product.infrastructure.ProductRepository
import com.linogo.gestion.category.infrastructure.CategoryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SyncService(
    private val syncVersionRepository: SyncVersionRepository,
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository,
    private val stateRepository: StateRepository
) {

    fun getCurrentVersion(): SyncVersion? {
        return syncVersionRepository.findById(1L).orElse(null)
    }

    @Transactional(readOnly = true)
    fun getCatalog(): SyncCatalogResponse {
        val version = getCurrentVersion()?.version ?: 0L
        val products = productRepository.findAll()
        val categories = categoryRepository.findAll()

        return SyncCatalogResponse(
            version = version,
            products = products.map { it.toSyncResponse() },
            categories = categories.map { it.toSyncResponse() }
        )
    }

    @Transactional(readOnly = true)
    fun getStates(): SyncStatesResponse {
        val version = getCurrentVersion()?.version ?: 0L
        val states = stateRepository.findAll()

        return SyncStatesResponse(
            version = version,
            states = states.map { StateSyncResponse(it.id ?: 0, it.name, it.priority) }
        )
    }

    fun validateSync(clientVersion: Long): SyncValidateResponse {
        val currentVersion = getCurrentVersion()?.version ?: 0L
        return SyncValidateResponse(
            needsSync = clientVersion < currentVersion,
            currentVersion = currentVersion,
            description = getCurrentVersion()?.description ?: "Sin descripción"
        )
    }

    @Transactional
    fun updateVersion(version: Long, description: String): SyncVersion {
        val syncVersion = SyncVersion(
            id = 1L,
            version = version,
            description = description
        )
        return syncVersionRepository.save(syncVersion)
    }
}

private fun Product.toSyncResponse(): ProductSyncResponse {
    return ProductSyncResponse(
        id = this.id!!,
        name = this.name,
        pricePerUnit = this.pricePerUnit,
        stock = this.stock,
        imageUrl = this.imageUrl,
        description = this.description,
        categoryId = this.category?.id,
        categoryName = this.category?.name
    )
}

private fun Category.toSyncResponse(): CategorySyncResponse {
    return CategorySyncResponse(
        id = this.id!!,
        name = this.name,
        parentId = this.parent?.id
    )
}

// DTOs para sincronización
data class SyncCatalogResponse(
    val version: Long,
    val products: List<ProductSyncResponse>,
    val categories: List<CategorySyncResponse>
)

data class ProductSyncResponse(
    val id: Long,
    val name: String,
    val pricePerUnit: Long,
    val stock: Int,
    val imageUrl: String,
    val description: String,
    val categoryId: Long?,
    val categoryName: String?
)

data class CategorySyncResponse(
    val id: Long,
    val name: String,
    val parentId: Long?
)

data class SyncStatesResponse(
    val version: Long,
    val states: List<StateSyncResponse>
)

data class StateSyncResponse(
    val id: Long,
    val name: String,
    val priority: Int
)

data class SyncValidateRequest(
    val clientVersion: Long
)

data class SyncValidateResponse(
    val needsSync: Boolean,
    val currentVersion: Long,
    val description: String
)
