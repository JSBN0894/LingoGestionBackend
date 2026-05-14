package com.linogo.gestion.security

import com.linogo.gestion.exception.AlreadyExistsException
import com.linogo.gestion.exception.BusinessException
import com.linogo.gestion.exception.NotFoundException
import com.linogo.gestion.exception.ValidationException
import com.linogo.gestion.security.infrastructure.GlobalExceptionHandler
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.core.MethodParameter
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException

class GlobalExceptionHandlerTest {

    private val handler = GlobalExceptionHandler()

    @Test
    fun `handleValidationExceptions should return 400 with field errors`() {
        val mockParameter = org.mockito.Mockito.mock(MethodParameter::class.java)
        val bindingResult = BeanPropertyBindingResult(Any(), "test")
        bindingResult.addError(FieldError("test", "username", "El username es requerido"))
        val ex = MethodArgumentNotValidException(mockParameter, bindingResult)

        val response = handler.handleValidationExceptions(ex)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        val body = response.body
        assertEquals(400, body?.status)
        assertEquals("Bad Request", body?.error)
    }

    @Test
    fun `handleBadCredentialsException should return 401`() {
        val ex = BadCredentialsException("Credenciales inválidas")

        val response = handler.handleBadCredentialsException(ex)

        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        assertEquals("Credenciales inválidas", response.body?.message)
    }

    @Test
    fun `handleNotFoundException should return 404`() {
        val ex = NotFoundException("Product", 999L)

        val response = handler.handleNotFoundException(ex)

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        assertEquals("Product con id '999' no encontrado", response.body?.message)
    }

    @Test
    fun `handleAlreadyExistsException should return 409`() {
        val ex = AlreadyExistsException("User", "username", "test")

        val response = handler.handleAlreadyExistsException(ex)

        assertEquals(HttpStatus.CONFLICT, response.statusCode)
        assertEquals("User con username 'test' ya existe", response.body?.message)
    }

    @Test
    fun `handleValidationException should return 400`() {
        val ex = ValidationException("Datos inválidos")

        val response = handler.handleValidationException(ex)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("Datos inválidos", response.body?.message)
    }

    @Test
    fun `handleBusinessException should return 400`() {
        val ex = BusinessException("Error de negocio")

        val response = handler.handleBusinessException(ex)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("Error de negocio", response.body?.message)
    }
}
