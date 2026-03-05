package com.linogo.gestion.security.domain

import jakarta.persistence.*
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "users")
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: String,

    @Column(unique = true, nullable = false, length = 50)
    val username: String,

    @Column(unique = true, nullable = false, length = 100)
    val email: String,

    @Column(nullable = false)
    val password: String,

    @Column(nullable = false)
    val fullName: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val role: Role = Role.USER,

    @Column(nullable = false)
    val isEnabled: Boolean = true,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
) : UserDetails {

    override fun getAuthorities(): Collection<GrantedAuthority> =
        listOf(SimpleGrantedAuthority("ROLE_${role.name}"))

    override fun isAccountNonExpired(): Boolean = true
    override fun isAccountNonLocked(): Boolean = true
    override fun isCredentialsNonExpired(): Boolean = true
    override fun isEnabled(): Boolean = this.isEnabled
}

enum class Role {
    USER,       // Rol básico para clientes
    ADMIN,      // Administrador con acceso completo
    SELLER      // Vendedor con permisos limitados
}
