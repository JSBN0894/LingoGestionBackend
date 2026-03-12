package com.example.project.module.application

import com.example.project.module.domain.[ModuleName]
import com.example.project.module.infrastructure.[ModuleName]Repository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.assertj.core.api.Assertions.*
import java.util.*

/**
 * Unit test template for Service classes.
 * 
 * Testing approach:
 * - Mock external dependencies (repositories, external services)
 * - Test business logic in isolation
 * - Use descriptive test names with backticks
 * - Follow Arrange-Act-Assert pattern
 */
@ExtendWith(MockitoExtension::class)
class [ModuleName]ServiceTest {

    @Mock
    private lateinit var [moduleName]Repository: [ModuleName]Repository

    @InjectMocks
    private lateinit var [moduleName]Service: [ModuleName]Service

    // Test fixtures
    private lateinit var testEntity: [ModuleName]
    private lateinit var testRequest: Create[ModuleName]Request

    @BeforeEach
    fun setUp() {
        // Initialize test data
        testEntity = [ModuleName](
            id = 1L,
            name = "Test [ModuleName]",
            description = "Test description"
        )

        testRequest = Create[ModuleName]Request(
            name = "Test [ModuleName]",
            description = "Test description"
        )
    }

    @Test
    fun `create should return response when data is valid`() {
        // Arrange
        given([moduleName]Repository.existsByName(testRequest.name)).willReturn(false)
        given([moduleName]Repository.save(any())).willReturn(testEntity)

        // Act
        val response = [moduleName]Service.create(testRequest)

        // Assert
        assertThat(response.id).isEqualTo(testEntity.id)
        assertThat(response.name).isEqualTo(testEntity.name)
        
        // Verify interactions
        verify([moduleName]Repository).existsByName(testRequest.name)
        verify([moduleName]Repository).save(any())
    }

    @Test
    fun `create should throw exception when name already exists`() {
        // Arrange
        given([moduleName]Repository.existsByName(testRequest.name)).willReturn(true)

        // Act & Assert
        assertThatThrownBy { [moduleName]Service.create(testRequest) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("ya existe")
        
        verify([moduleName]Repository, never()).save(any())
    }

    @Test
    fun `findById should return response when entity exists`() {
        // Arrange
        val testId = 1L
        given([moduleName]Repository.findById(testId)).willReturn(Optional.of(testEntity))

        // Act
        val response = [moduleName]Service.findById(testId)

        // Assert
        assertThat(response.id).isEqualTo(testEntity.id)
        assertThat(response.name).isEqualTo(testEntity.name)
    }

    @Test
    fun `findById should throw exception when entity not found`() {
        // Arrange
        val testId = 999L
        given([moduleName]Repository.findById(testId)).willReturn(Optional.empty())

        // Act & Assert
        assertThatThrownBy { [moduleName]Service.findById(testId) }
            .isInstanceOf([ModuleName]NotFoundException::class.java)
            .hasMessageContaining("no encontrado")
    }

    @Test
    fun `findAll should return list of responses`() {
        // Arrange
        val entities = listOf(testEntity)
        given([moduleName]Repository.findAll()).willReturn(entities)

        // Act
        val responses = [moduleName]Service.findAll()

        // Assert
        assertThat(responses).hasSize(1)
        assertThat(responses[0].id).isEqualTo(testEntity.id)
    }

    @Test
    fun `update should return updated response when entity exists`() {
        // Arrange
        val testId = 1L
        val updateRequest = Update[ModuleName]Request(
            name = "Updated name",
            description = "Updated description"
        )
        val updatedEntity = testEntity.copy(name = updateRequest.name, description = updateRequest.description)
        
        given([moduleName]Repository.findById(testId)).willReturn(Optional.of(testEntity))
        given([moduleName]Repository.save(any())).willReturn(updatedEntity)

        // Act
        val response = [moduleName]Service.update(testId, updateRequest)

        // Assert
        assertThat(response.name).isEqualTo(updateRequest.name)
        verify([moduleName]Repository).save(testEntity)
    }

    @Test
    fun `delete should remove entity when exists`() {
        // Arrange
        val testId = 1L
        given([moduleName]Repository.findById(testId)).willReturn(Optional.of(testEntity))

        // Act
        [moduleName]Service.delete(testId)

        // Assert
        verify([moduleName]Repository).delete(testEntity)
    }

    @Test
    fun `delete should throw exception when entity not found`() {
        // Arrange
        val testId = 999L
        given([moduleName]Repository.findById(testId)).willReturn(Optional.empty())

        // Act & Assert
        assertThatThrownBy { [moduleName]Service.delete(testId) }
            .isInstanceOf([ModuleName]NotFoundException::class.java)
    }
}

// Placeholder classes for template compilation
// Remove these when using the template
data class Create[ModuleName]Request(val name: String, val description: String?)
data class Update[ModuleName]Request(val name: String, val description: String?)
class [ModuleName]NotFoundException(message: String) : RuntimeException(message)
class [ModuleName]Service(private val repository: [ModuleName]Repository) {
    fun create(request: Create[ModuleName]Request) = TODO()
    fun findById(id: Long) = TODO()
    fun findAll() = TODO()
    fun update(id: Long, request: Update[ModuleName]Request) = TODO()
    fun delete(id: Long) = TODO()
}
interface [ModuleName]Repository
