package com.linogo.gestion.security.infrastructure

import com.linogo.gestion.security.domain.User
import io.jsonwebtoken.*
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import java.security.spec.KeySpec
import javax.crypto.SecretKey
import java.util.*

@Component
class JwtTokenProvider(
    @Value("\${app.jwt.secret}")
    private val jwtSecret: String,

    @Value("\${app.jwt.access-token-expiration}")
    private val accessTokenExpiration: Long,

    @Value("\${app.jwt.refresh-token-expiration}")
    private val refreshTokenExpiration: Long
) {

    private val logger = LoggerFactory.getLogger(JwtTokenProvider::class.java)
    private val signingKey: SecretKey by lazy { Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret)) as SecretKey }

    fun generateAccessToken(user: User): String {
        return generateToken(user, accessTokenExpiration, TokenType.ACCESS)
    }

    fun generateRefreshToken(user: User): String {
        return generateToken(user, refreshTokenExpiration, TokenType.REFRESH)
    }

    private fun generateToken(user: UserDetails, expirationMs: Long, tokenType: TokenType): String {
        val now = Date()
        val expiryDate = Date(now.time + expirationMs)

        return Jwts.builder()
            .setSubject(user.username)
            .claim("userId", (user as User).id)
            .claim("role", user.authorities.first().authority)
            .claim("type", tokenType.name)
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(signingKey, SignatureAlgorithm.HS256)
            .compact()
    }

    fun getUsernameFromToken(token: String): String {
        return getClaimsFromToken(token).subject
    }

    fun getUserIdFromToken(token: String): String {
        return getClaimsFromToken(token).get("userId", String::class.java)
    }

    fun getTokenType(token: String): TokenType? {
        val type = getClaimsFromToken(token).get("type", String::class.java)
        return try {
            TokenType.valueOf(type)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    fun isTokenValid(token: String): Boolean {
        return try {
            Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
            true
        } catch (e: SecurityException) {
            logger.warn("Token inválido: ${e.message}")
            false
        } catch (e: MalformedJwtException) {
            logger.warn("Token mal formado: ${e.message}")
            false
        } catch (e: ExpiredJwtException) {
            logger.warn("Token expirado: ${e.message}")
            false
        } catch (e: UnsupportedJwtException) {
            logger.warn("Token no soportado: ${e.message}")
            false
        } catch (e: IllegalArgumentException) {
            logger.warn("Token vacío o nulo: ${e.message}")
            false
        }
    }

    fun isTokenExpired(token: String): Boolean {
        return try {
            val claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .payload
            claims.expiration.before(Date())
        } catch (e: ExpiredJwtException) {
            true
        }
    }

    private fun getClaimsFromToken(token: String): Claims {
        return Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .payload
    }

    fun getAccessTokenExpirationMs(): Long = accessTokenExpiration
}

enum class TokenType {
    ACCESS,
    REFRESH
}
