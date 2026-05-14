package com.linogo.gestion.security.config

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity

/**
 * Anotaciones para control de acceso basado en roles (RBAC)
 * 
 * Uso en controllers o servicios:
 * 
 * @PreAuthorize("@securityChecks.isAdmin()")
 * @PreAuthorize("@securityChecks.hasRole('ADMIN')")
 * @PreAuthorize("@securityChecks.isOwner(#userId)")
 */

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize("hasRole('ADMIN')")
annotation class AdminOnly

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
annotation class AdminOrSeller

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
annotation class AuthenticatedUser

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize("isAuthenticated()")
annotation class Authenticated

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize("hasAnyRole('ADMIN', 'VENTAS')")
annotation class VentasOnly

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize("hasAnyRole('ADMIN', 'PRODUCCION')")
annotation class ProduccionOnly

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize("hasAnyRole('ADMIN', 'LOGISTICA')")
annotation class LogisticaOnly
