package com.example.project.module.domain

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

/**
 * Entity template for JPA entities.
 * 
 * Usage:
 * 1. Replace [ModuleName] with your module name (e.g., Product, Order, Client)
 * 2. Replace [TableName] with the database table name (snake_case)
 * 3. Add your domain fields
 * 4. Add domain methods if needed
 */
@Entity
@Table(name = "[table_name]")
@EntityListeners(AuditingEntityListener::class)
class [ModuleName](
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    // Example field - replace with your own
    @Column(nullable = false, unique = true, length = 100)
    val name: String,

    // Example nullable field
    @Column(nullable = true, length = 500)
    val description: String? = null,

    // Example enum field
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: [ModuleName]Status = [ModuleName]Status.ACTIVE,

    // Audit fields (auto-populated by JPA)
    @CreatedDate
    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @LastModifiedDate
    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
) {
    /**
     * Domain methods go here.
     * Keep business logic in the entity when possible.
     */
    
    fun activate() {
        // Example domain method
        // Add validation and state changes here
    }

    fun deactivate() {
        // Example domain method
    }

    // Override equals and hashCode for value comparison
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as [ModuleName]
        return id != null && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()

    override fun toString(): String = "[ModuleName](id=$id, name='$name')"
}

/**
 * Status enum for the entity.
 * Replace with your own statuses or remove if not needed.
 */
enum class [ModuleName]Status {
    ACTIVE,
    INACTIVE,
    ARCHIVED
}
