package com.linogo.gestion.security

import com.linogo.gestion.security.config.SecurityChecks
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.User

class SecurityChecksTest {

    private val securityChecks = SecurityChecks()

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `isAdmin should return true when user has ADMIN role`() {
        val auth = UsernamePasswordAuthenticationToken(
            User("admin", "password", listOf(SimpleGrantedAuthority("ROLE_ADMIN"))),
            null,
            listOf(SimpleGrantedAuthority("ROLE_ADMIN"))
        )
        SecurityContextHolder.getContext().authentication = auth

        assertTrue(securityChecks.isAdmin())
    }

    @Test
    fun `isAdmin should return false when user does not have ADMIN role`() {
        val auth = UsernamePasswordAuthenticationToken(
            User("user", "password", listOf(SimpleGrantedAuthority("ROLE_USER"))),
            null,
            listOf(SimpleGrantedAuthority("ROLE_USER"))
        )
        SecurityContextHolder.getContext().authentication = auth

        assertFalse(securityChecks.isAdmin())
    }

    @Test
    fun `isSeller should return true when user has SELLER role`() {
        val auth = UsernamePasswordAuthenticationToken(
            User("seller", "password", listOf(SimpleGrantedAuthority("ROLE_SELLER"))),
            null,
            listOf(SimpleGrantedAuthority("ROLE_SELLER"))
        )
        SecurityContextHolder.getContext().authentication = auth

        assertTrue(securityChecks.isSeller())
    }

    @Test
    fun `isUser should return true when user has USER role`() {
        val auth = UsernamePasswordAuthenticationToken(
            User("user", "password", listOf(SimpleGrantedAuthority("ROLE_USER"))),
            null,
            listOf(SimpleGrantedAuthority("ROLE_USER"))
        )
        SecurityContextHolder.getContext().authentication = auth

        assertTrue(securityChecks.isUser())
    }

    @Test
    fun `isAuthenticated should return true when user is authenticated`() {
        val auth = UsernamePasswordAuthenticationToken(
            User("user", "password", listOf(SimpleGrantedAuthority("ROLE_USER"))),
            null,
            listOf(SimpleGrantedAuthority("ROLE_USER"))
        )
        SecurityContextHolder.getContext().authentication = auth

        assertTrue(securityChecks.isAuthenticated())
    }

    @Test
    fun `isAuthenticated should return false when no authentication exists`() {
        SecurityContextHolder.clearContext()

        assertFalse(securityChecks.isAuthenticated())
    }

    @Test
    fun `isOwner should return true when username matches`() {
        val auth = UsernamePasswordAuthenticationToken(
            User("testuser", "password", listOf(SimpleGrantedAuthority("ROLE_USER"))),
            null,
            listOf(SimpleGrantedAuthority("ROLE_USER"))
        )
        SecurityContextHolder.getContext().authentication = auth

        assertTrue(securityChecks.isOwner("testuser"))
    }

    @Test
    fun `isOwner should return false when username does not match`() {
        val auth = UsernamePasswordAuthenticationToken(
            User("testuser", "password", listOf(SimpleGrantedAuthority("ROLE_USER"))),
            null,
            listOf(SimpleGrantedAuthority("ROLE_USER"))
        )
        SecurityContextHolder.getContext().authentication = auth

        assertFalse(securityChecks.isOwner("otheruser"))
    }

    @Test
    fun `isAdminOrOwner should return true for admin`() {
        val auth = UsernamePasswordAuthenticationToken(
            User("admin", "password", listOf(SimpleGrantedAuthority("ROLE_ADMIN"))),
            null,
            listOf(SimpleGrantedAuthority("ROLE_ADMIN"))
        )
        SecurityContextHolder.getContext().authentication = auth

        assertTrue(securityChecks.isAdminOrOwner("anyuser"))
    }

    @Test
    fun `isAdminOrOwner should return true for owner`() {
        val auth = UsernamePasswordAuthenticationToken(
            User("owner", "password", listOf(SimpleGrantedAuthority("ROLE_USER"))),
            null,
            listOf(SimpleGrantedAuthority("ROLE_USER"))
        )
        SecurityContextHolder.getContext().authentication = auth

        assertTrue(securityChecks.isAdminOrOwner("owner"))
    }

    @Test
    fun `isAdminOrOwner should return false for non-admin non-owner`() {
        val auth = UsernamePasswordAuthenticationToken(
            User("user", "password", listOf(SimpleGrantedAuthority("ROLE_USER"))),
            null,
            listOf(SimpleGrantedAuthority("ROLE_USER"))
        )
        SecurityContextHolder.getContext().authentication = auth

        assertFalse(securityChecks.isAdminOrOwner("otheruser"))
    }
}
