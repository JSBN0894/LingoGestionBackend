package com.linogo.gestion.sync.application

import com.linogo.gestion.category.domain.Category
import com.linogo.gestion.product.domain.Product
import com.linogo.gestion.sync.domain.SyncVersion
import com.linogo.gestion.sync.infrastructure.SyncVersionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SyncService(
    private val syncVersionRepository: SyncVersionRepository,
    private val productRepository: com.linogo.gestion.product.infrastructure.ProductRepository,
    private val categoryRepository: com.linogo.gestion.category.infrastructure.CategoryRepository,
    private val stateRepository: com.linogo.gestion.state.infrastructure.StateRepository,
    private val shipmentStateRepository: com.linogo.gestion.shipmentstate.infrastructure.ShipmentStateRepository
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
        val shipmentStates = shipmentStateRepository.findAll()

        return SyncStatesResponse(
            version = version,
            states = states.map { StateSyncResponse(it.id, it.name, it.priority) },
            shipmentStates = shipmentStates.map { ShipmentStateSyncResponse(it.id, it.name, it.state.priority) }
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
        id = this.id,
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
        id = this.id,
        name = this.name,
        parentId = this.parent?.id
    )
}
