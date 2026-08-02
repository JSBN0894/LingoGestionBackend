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
    private var password: String, // Cambiado a private

    @Column(nullable = false)
    val fullName: String,

    // EAGER es obligatorio: CustomUserDetailsService carga el usuario dentro
    // de una transacción que termina antes de que JwtAuthenticationFilter/
    // CookieJwtFilter lean getAuthorities() fuera de sesión de Hibernate.
    // Con LAZY, cada request autenticado lanzaría LazyInitializationException.
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = [JoinColumn(name = "user_id")],
        inverseJoinColumns = [JoinColumn(name = "role_id")]
    )
    var roles: MutableSet<Role> = mutableSetOf(),

    @Column(name = "is_enabled", nullable = false)
    private var _isEnabled: Boolean = true,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) : UserDetails {

    // Implementación de UserDetails vinculada a las propiedades privadas
    override fun getUsername(): String = username

    override fun getPassword(): String = password

    override fun isEnabled(): Boolean = _isEnabled

    fun toggleEnabled(): Boolean {
        _isEnabled = !_isEnabled
        updatedAt = LocalDateTime.now()
        return _isEnabled
    }

    fun changePassword(encodedPassword: String) {
        password = encodedPassword
        updatedAt = LocalDateTime.now()
    }

    override fun getAuthorities(): Collection<GrantedAuthority> =
        roles.flatMap { it.permissions }
            .toSet()
            .map { SimpleGrantedAuthority("PERM_$it") }

    override fun isAccountNonExpired(): Boolean = true
    override fun isAccountNonLocked(): Boolean = true
    override fun isCredentialsNonExpired(): Boolean = true
}