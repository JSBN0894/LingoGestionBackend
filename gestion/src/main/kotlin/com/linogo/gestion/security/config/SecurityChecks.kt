package com.linogo.gestion.security.config

import com.linogo.gestion.security.domain.Role
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.User as SpringUser
import org.springframework.stereotype.Component

/**
 * Componente para verificaciones de seguridad en expresiones SpEL
 * 
 * Uso en @PreAuthorize:
 * @PreAuthorize("@securityChecks.isAdmin()")
 * @PreAuthorize("@securityChecks.isOwner(#userId)")
 */
@Component
class SecurityChecks {

    fun isAdmin(): Boolean {
        return hasRole(Role.ADMIN.name)
    }

    fun isSeller(): Boolean {
        return hasRole(Role.SELLER.name)
    }

    fun isUser(): Boolean {
        return hasRole(Role.USER.name)
    }

    fun hasRole(role: String): Boolean {
        val authentication = SecurityContextHolder.getContext().authentication
        return authentication?.authorities?.any { it.authority == "ROLE_$role" } ?: false
    }

    fun isAuthenticated(): Boolean {
        val authentication = SecurityContextHolder.getContext().authentication
        return authentication?.isAuthenticated == true
    }

    fun getCurrentUserId(): String? {
        val authentication = SecurityContextHolder.getContext().authentication
        return (authentication?.principal as? SpringUser)?.username
    }

    fun isOwner(userId: String): Boolean {
        return getCurrentUserId() == userId
    }

    fun isAdminOrOwner(userId: String): Boolean {
        return isAdmin() || isOwner(userId)
    }
}
