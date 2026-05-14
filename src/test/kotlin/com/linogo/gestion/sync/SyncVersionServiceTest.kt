package com.linogo.gestion.sync

import com.linogo.gestion.sync.application.SyncVersionService
import com.linogo.gestion.sync.domain.SyncVersion
import com.linogo.gestion.sync.infrastructure.SyncVersionRepository
import org.junit.jupiter.api.Assertions.assertEquals
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
class SyncVersionServiceTest {

    @Mock
    private lateinit var syncVersionRepository: SyncVersionRepository

    @InjectMocks
    private lateinit var syncVersionService: SyncVersionService

    private lateinit var syncVersion: SyncVersion

    @BeforeEach
    fun setUp() {
        syncVersion = SyncVersion(id = 1L, version = 5L, description = "Version 5",
            createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now())
    }

    @Test
    fun `incrementVersion should increment version when current version exists`() {
        `when`(syncVersionRepository.findById(1L)).thenReturn(Optional.of(syncVersion))
        `when`(syncVersionRepository.save(any())).thenReturn(
            syncVersion.copy(version = 6L)
        )

        val newVersion = syncVersionService.incrementVersion("Version 6")

        assertEquals(6L, newVersion)
        verify(syncVersionRepository).findById(1L)
        verify(syncVersionRepository).save(any())
    }

    @Test
    fun `incrementVersion should start from 0 when no current version exists`() {
        `when`(syncVersionRepository.findById(1L)).thenReturn(Optional.empty())
        `when`(syncVersionRepository.save(any())).thenReturn(
            SyncVersion(id = 1L, version = 1L, description = "Initial version")
        )

        val newVersion = syncVersionService.incrementVersion("Initial version")

        assertEquals(1L, newVersion)
        verify(syncVersionRepository).findById(1L)
        verify(syncVersionRepository).save(any())
    }

    @Test
    fun `getCurrentVersion should return version when exists`() {
        `when`(syncVersionRepository.findById(1L)).thenReturn(Optional.of(syncVersion))

        val version = syncVersionService.getCurrentVersion()

        assertEquals(5L, version)
        verify(syncVersionRepository).findById(1L)
    }

    @Test
    fun `getCurrentVersion should return 0 when no version exists`() {
        `when`(syncVersionRepository.findById(1L)).thenReturn(Optional.empty())

        val version = syncVersionService.getCurrentVersion()

        assertEquals(0L, version)
        verify(syncVersionRepository).findById(1L)
    }
}
