package com.linogo.gestion.security.service

import com.linogo.gestion.security.infrastructure.UserRepository
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CustomUserDetailsService(
    private val userRepository: UserRepository
) : UserDetailsService {

    @Transactional(readOnly = true)
    override fun loadUserByUsername(username: String): UserDetails {
        val user = userRepository.findByUsername(username)
            ?: throw UsernameNotFoundException("Usuario no encontrado: $username")

        if (!user.enabled) {
            throw UsernameNotFoundException("Usuario deshabilitado: $username")
        }

        return user
    }

    @Transactional(readOnly = true)
    fun findByUsername(username: String): UserDetails {
        return loadUserByUsername(username)
    }

    @Transactional(readOnly = true)
    fun existsByUsername(username: String): Boolean {
        return userRepository.existsByUsername(username)
    }

    @Transactional(readOnly = true)
    fun existsByEmail(email: String): Boolean {
        return userRepository.existsByEmail(email)
    }
}
