package com.linogo.gestion.security

import com.linogo.gestion.security.domain.User
import com.linogo.gestion.security.infrastructure.UserRepository
import com.linogo.gestion.security.service.CustomUserDetailsService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.security.core.userdetails.UsernameNotFoundException
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class CustomUserDetailsServiceTest {

    @Mock
    private lateinit var userRepository: UserRepository

    @InjectMocks
    private lateinit var userDetailsService: CustomUserDetailsService

    private lateinit var user: User

    @BeforeEach
    fun setUp() {
        user = User(
            username = "testuser",
            email = "test@example.com",
            password = "password123",
            fullName = "Test User",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }

    @Test
    fun `loadUserByUsername should return UserDetails when user exists`() {
        `when`(userRepository.findByUsername("testuser")).thenReturn(user)

        val userDetails = userDetailsService.loadUserByUsername("testuser")

        assertEquals("testuser", userDetails.username)
        assertEquals("password123", userDetails.password)
        verify(userRepository).findByUsername("testuser")
    }

    @Test
    fun `loadUserByUsername should throw UsernameNotFoundException when user not found`() {
        `when`(userRepository.findByUsername("unknown")).thenReturn(null)

        val exception = assertThrows(UsernameNotFoundException::class.java) {
            userDetailsService.loadUserByUsername("unknown")
        }

        assertTrue(exception.message!!.contains("unknown"))
        verify(userRepository).findByUsername("unknown")
    }

    @Test
    fun `findByUsername should return UserDetails when user exists`() {
        `when`(userRepository.findByUsername("testuser")).thenReturn(user)

        val userDetails = userDetailsService.findByUsername("testuser")

        assertEquals("testuser", userDetails.username)
        verify(userRepository).findByUsername("testuser")
    }

    @Test
    fun `existsByUsername should return true when user exists`() {
        `when`(userRepository.existsByUsername("testuser")).thenReturn(true)

        val exists = userDetailsService.existsByUsername("testuser")

        assertTrue(exists)
        verify(userRepository).existsByUsername("testuser")
    }

    @Test
    fun `existsByUsername should return false when user does not exist`() {
        `when`(userRepository.existsByUsername("unknown")).thenReturn(false)

        val exists = userDetailsService.existsByUsername("unknown")

        assertFalse(exists)
        verify(userRepository).existsByUsername("unknown")
    }

    @Test
    fun `existsByEmail should return true when email exists`() {
        `when`(userRepository.existsByEmail("test@example.com")).thenReturn(true)

        val exists = userDetailsService.existsByEmail("test@example.com")

        assertTrue(exists)
        verify(userRepository).existsByEmail("test@example.com")
    }

    @Test
    fun `existsByEmail should return false when email does not exist`() {
        `when`(userRepository.existsByEmail("unknown@example.com")).thenReturn(false)

        val exists = userDetailsService.existsByEmail("unknown@example.com")

        assertFalse(exists)
        verify(userRepository).existsByEmail("unknown@example.com")
    }
}
