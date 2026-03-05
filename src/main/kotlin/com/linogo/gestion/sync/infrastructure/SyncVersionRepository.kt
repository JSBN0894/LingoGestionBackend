package com.linogo.gestion.sync.infrastructure

import com.linogo.gestion.sync.domain.SyncVersion
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SyncVersionRepository : JpaRepository<SyncVersion, Long>
