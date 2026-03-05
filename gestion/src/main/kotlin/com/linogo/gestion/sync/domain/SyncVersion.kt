package com.linogo.gestion.sync.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "sync_version")
data class SyncVersion(
    @Id
    val id: Long = 1L,

    @Column(nullable = false)
    val version: Long,

    @Column(nullable = false)
    val description: String,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
