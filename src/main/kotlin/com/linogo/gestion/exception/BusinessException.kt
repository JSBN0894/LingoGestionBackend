package com.linogo.gestion.exception

/**
 * Excepción base para errores de negocio.
 * Se usa para validar reglas de negocio y condiciones esperadas.
 */
open class BusinessException(
    message: String
) : RuntimeException(message)

/**
 * Excepción para cuando un recurso no existe.
 */
class NotFoundException(
    resource: String,
    id: Any
) : BusinessException("$resource con id '$id' no encontrado")

/**
 * Excepción para cuando ya existe un recurso.
 */
class AlreadyExistsException(
    resource: String,
    identifier: String,
    value: Any
) : BusinessException("$resource con $identifier '$value' ya existe")

/**
 * Excepción para validaciones de negocio fallidas.
 */
class ValidationException(
    message: String
) : BusinessException(message)
