package com.linogo.gestion.security.domain

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "refresh_tokens")
data class RefreshToken(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: String,

    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    val user: User,

    @Column(nullable = false, unique = true)
    val token: String,

    @Column(nullable = false)
    val expiryDate: Instant,

    @Column(nullable = false)
    val isRevoked: Boolean = false,

    @Column(nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
)
