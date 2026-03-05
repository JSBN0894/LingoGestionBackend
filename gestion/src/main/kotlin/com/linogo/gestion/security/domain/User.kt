package com.linogo.gestion.security.domain

import jakarta.persistence.*
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.time.LocalDateTime

@Entity
@Table(name = "users")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: String? = null,

    @Column(unique = true, nullable = false, length = 50)
    private val username: String, // Cambiado a private para evitar el getter automático de Kotlin

    @Column(unique = true, nullable = false, length = 100)
    val email: String,

    @Column(nullable = false)
    private val password: String, // Cambiado a private

    @Column(nullable = false)
    val fullName: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val role: Role = Role.USER,

    @Column(name = "is_enabled", nullable = false)
    private val isEnabled: Boolean = true, // Cambiado a private

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
) : UserDetails {

    // Implementación de UserDetails vinculada a las propiedades privadas
    override fun getUsername(): String = username

    override fun getPassword(): String = password

    override fun isEnabled(): Boolean = isEnabled

    override fun getAuthorities(): Collection<GrantedAuthority> =
        listOf(SimpleGrantedAuthority("ROLE_${role.name}"))

    override fun isAccountNonExpired(): Boolean = true
    override fun isAccountNonLocked(): Boolean = true
    override fun isCredentialsNonExpired(): Boolean = true
}

enum class Role {
    USER, ADMIN, SELLER
}