package com.linogo.gestion.sync.application

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service

@Service
class SyncVersionService(
    private val syncVersionRepository: com.linogo.gestion.sync.infrastructure.SyncVersionRepository
) {

    @Transactional
    fun incrementVersion(description: String): Long {
        val currentVersion = syncVersionRepository.findById(1L).orElse(null)
        val newVersion = (currentVersion?.version ?: 0L) + 1
        
        val syncVersion = currentVersion?.copy(
            version = newVersion,
            description = description
        ) ?: com.linogo.gestion.sync.domain.SyncVersion(
            id = 1L,
            version = newVersion,
            description = description
        )
        
        syncVersionRepository.save(syncVersion)
        return newVersion
    }

    fun getCurrentVersion(): Long {
        return syncVersionRepository.findById(1L).orElse(0L).version
    }
}
