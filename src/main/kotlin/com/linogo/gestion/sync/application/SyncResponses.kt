package com.linogo.gestion.sync.application

data class SyncVersionResponse(
    val version: Long,
    val description: String,
    val updatedAt: java.time.LocalDateTime
)

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
    val categoryId: Long,
    val categoryName: String
)

data class CategorySyncResponse(
    val id: Long,
    val name: String,
    val parentId: Long?
)

data class SyncStatesResponse(
    val version: Long,
    val states: List<StateSyncResponse>,
    val shipmentStates: List<ShipmentStateSyncResponse>
)

data class StateSyncResponse(
    val id: Long,
    val name: String,
    val priority: Int
)

data class ShipmentStateSyncResponse(
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
