package com.linogo.gestion.sync

import com.linogo.gestion.category.domain.Category
import com.linogo.gestion.category.infrastructure.CategoryRepository
import com.linogo.gestion.product.infrastructure.ProductRepository
import com.linogo.gestion.product.infrastructure.persistence.entity.ProductEntity
import com.linogo.gestion.shipmentstate.domain.ShipmentState
import com.linogo.gestion.shipmentstate.infrastructure.ShipmentStateRepository
import com.linogo.gestion.state.domain.State
import com.linogo.gestion.state.infrastructure.StateRepository
import com.linogo.gestion.sync.application.SyncService
import com.linogo.gestion.sync.domain.SyncVersion
import com.linogo.gestion.sync.infrastructure.SyncVersionRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import java.time.LocalDateTime
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class SyncServiceTest {

    @Mock
    private lateinit var syncVersionRepository: SyncVersionRepository

    @Mock
    private lateinit var productRepository: ProductRepository

    @Mock
    private lateinit var categoryRepository: CategoryRepository

    @Mock
    private lateinit var stateRepository: StateRepository

    @Mock
    private lateinit var shipmentStateRepository: ShipmentStateRepository

    @InjectMocks
    private lateinit var syncService: SyncService

    private lateinit var syncVersion: SyncVersion
    private lateinit var category: Category
    private lateinit var productEntity: ProductEntity
    private lateinit var state: State
    private lateinit var shipmentState: ShipmentState

    @BeforeEach
    fun setUp() {
        syncVersion = SyncVersion(id = 1L, version = 5L, description = "Version 5",
            createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now())

        category = Category(id = 1L, name = "Moldes")

        productEntity = ProductEntity(id = 1L, name = "Molde Silicona", pricePerUnit = 15000L,
            stock = 10, imageUrl = "img.jpg", description = "Molde de silicona",
            category = category, createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now())

        state = State(id = 1L, name = "Pendiente", priority = 1,
            createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now())

        shipmentState = ShipmentState(id = 1L, name = "En preparación", state = state,
            createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now())
    }

    @Test
    fun `getCurrentVersion should return SyncVersion when exists`() {
        `when`(syncVersionRepository.findById(1L)).thenReturn(Optional.of(syncVersion))

        val result = syncService.getCurrentVersion()

        assertNotNull(result)
        assertEquals(5L, result?.version)
        assertEquals("Version 5", result?.description)
        verify(syncVersionRepository).findById(1L)
    }

    @Test
    fun `getCurrentVersion should return null when no version exists`() {
        `when`(syncVersionRepository.findById(1L)).thenReturn(Optional.empty())

        val result = syncService.getCurrentVersion()

        assertNull(result)
        verify(syncVersionRepository).findById(1L)
    }

    @Test
    fun `getCatalog should return catalog with products and categories`() {
        `when`(syncVersionRepository.findById(1L)).thenReturn(Optional.of(syncVersion))
        `when`(productRepository.findAll()).thenReturn(listOf(productEntity))
        `when`(categoryRepository.findAll()).thenReturn(listOf(category))

        val response = syncService.getCatalog()

        assertNotNull(response)
        assertEquals(5L, response.version)
        assertEquals(1, response.products.size)
        assertEquals(1, response.categories.size)
        assertEquals("Molde Silicona", response.products[0].name)
        assertEquals("Moldes", response.categories[0].name)
        verify(productRepository).findAll()
        verify(categoryRepository).findAll()
    }

    @Test
    fun `getCatalog should return empty lists when no data exists`() {
        `when`(syncVersionRepository.findById(1L)).thenReturn(Optional.of(syncVersion))
        `when`(productRepository.findAll()).thenReturn(emptyList())
        `when`(categoryRepository.findAll()).thenReturn(emptyList())

        val response = syncService.getCatalog()

        assertEquals(0, response.products.size)
        assertEquals(0, response.categories.size)
    }

    @Test
    fun `getCatalog should return version 0 when no sync version exists`() {
        `when`(syncVersionRepository.findById(1L)).thenReturn(Optional.empty())
        `when`(productRepository.findAll()).thenReturn(emptyList())
        `when`(categoryRepository.findAll()).thenReturn(emptyList())

        val response = syncService.getCatalog()

        assertEquals(0L, response.version)
    }

    @Test
    fun `getStates should return states and shipment states`() {
        `when`(syncVersionRepository.findById(1L)).thenReturn(Optional.of(syncVersion))
        `when`(stateRepository.findAll()).thenReturn(listOf(state))
        `when`(shipmentStateRepository.findAll()).thenReturn(listOf(shipmentState))

        val response = syncService.getStates()

        assertNotNull(response)
        assertEquals(5L, response.version)
        assertEquals(1, response.states.size)
        assertEquals(1, response.shipmentStates.size)
        assertEquals("Pendiente", response.states[0].name)
        assertEquals("En preparación", response.shipmentStates[0].name)
        verify(stateRepository).findAll()
        verify(shipmentStateRepository).findAll()
    }

    @Test
    fun `getStates should return empty lists when no states exist`() {
        `when`(syncVersionRepository.findById(1L)).thenReturn(Optional.of(syncVersion))
        `when`(stateRepository.findAll()).thenReturn(emptyList())
        `when`(shipmentStateRepository.findAll()).thenReturn(emptyList())

        val response = syncService.getStates()

        assertEquals(0, response.states.size)
        assertEquals(0, response.shipmentStates.size)
    }

    @Test
    fun `validateSync should return needsSync true when client version is behind`() {
        `when`(syncVersionRepository.findById(1L)).thenReturn(Optional.of(syncVersion))

        val response = syncService.validateSync(3L)

        assertEquals(true, response.needsSync)
        assertEquals(5L, response.currentVersion)
    }

    @Test
    fun `validateSync should return needsSync false when client version is current`() {
        `when`(syncVersionRepository.findById(1L)).thenReturn(Optional.of(syncVersion))

        val response = syncService.validateSync(5L)

        assertEquals(false, response.needsSync)
        assertEquals(5L, response.currentVersion)
    }

    @Test
    fun `validateSync should return needsSync false when client version is ahead`() {
        `when`(syncVersionRepository.findById(1L)).thenReturn(Optional.of(syncVersion))

        val response = syncService.validateSync(10L)

        assertEquals(false, response.needsSync)
        assertEquals(5L, response.currentVersion)
    }

    @Test
    fun `validateSync should return version 0 and default description when no version exists`() {
        `when`(syncVersionRepository.findById(1L)).thenReturn(Optional.empty())

        val response = syncService.validateSync(0L)

        assertEquals(false, response.needsSync)
        assertEquals(0L, response.currentVersion)
        assertEquals("Sin descripción", response.description)
    }

    @Test
    fun `updateVersion should save and return new SyncVersion`() {
        `when`(syncVersionRepository.save(any())).thenReturn(syncVersion)

        val result = syncService.updateVersion(5L, "Version 5")

        assertNotNull(result)
        assertEquals(5L, result.version)
        assertEquals("Version 5", result.description)
        verify(syncVersionRepository).save(any())
    }
}
