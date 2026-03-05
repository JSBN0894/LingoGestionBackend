package com.linogo.gestion.security.infrastructure

import com.linogo.gestion.security.domain.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface RefreshTokenRepository : JpaRepository<RefreshToken, String> {
    
    fun findByToken(token: String): RefreshToken?
    
    fun findByUserId(userId: String): RefreshToken?
    
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.user.id = :userId")
    fun deleteByUserId(userId: String)
    
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiryDate < CURRENT_TIMESTAMP")
    fun deleteExpiredTokens()
}
