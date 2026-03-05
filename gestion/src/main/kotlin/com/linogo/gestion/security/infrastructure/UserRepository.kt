package com.linogo.gestion.security.infrastructure

import com.linogo.gestion.security.domain.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserRepository : JpaRepository<User, String> {
    
    fun findByUsername(username: String): User?
    
    fun findByEmail(email: String): User?
    
    fun existsByUsername(username: String): Boolean
    
    fun existsByEmail(email: String): Boolean

    @Query("SELECT u FROM User u WHERE u.username = :username")
    fun findByUsernameWithAuthorities(username: String): User?
}
