package com.linogo.gestion.security.config

import org.springframework.security.access.prepost.PreAuthorize

/**
 * Marca endpoints de solo-lectura accesibles a cualquier usuario logueado,
 * sin importar sus permisos. El control de acceso granular vive en
 * [RequiresPermission] + [com.linogo.gestion.security.service.PermissionAspect].
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize("isAuthenticated()")
annotation class Authenticated
