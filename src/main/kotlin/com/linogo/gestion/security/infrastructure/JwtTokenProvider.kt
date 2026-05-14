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
    
    init {
        // Validar que el secreto JWT esté configurado y sea lo suficientemente seguro
        if (jwtSecret.isBlank()) {
            throw IllegalStateException("JWT_SECRET no está configurado. Debes establecer la variable de entorno JWT_SECRET con un valor seguro (mínimo 64 caracteres base64). Genera uno con: openssl rand -base64 64")
        }
        
        // Validar longitud mínima del secreto (256 bits = 32 bytes = ~43 caracteres base64)
        val decodedSecret = Decoders.BASE64.decode(jwtSecret)
        if (decodedSecret.size < 32) {
            throw IllegalStateException("El JWT_SECRET es demasiado corto. Debe tener al menos 256 bits (32 bytes). Genera uno seguro con: openssl rand -base64 64")
        }
        
        logger.info("JWT configurado correctamente con secreto de ${decodedSecret.size * 8} bits")
    }
    
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

    fun getRefreshTokenExpirationMs(): Long = refreshTokenExpiration
}

enum class TokenType {
    ACCESS,
    REFRESH
}
