package com.linogo.gestion.state.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonManagedReference
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "states")
class State(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "parent_id", foreignKey = ForeignKey(name = "fk_state_parent"))
    @JsonIgnore
    var parent: State? = null,

    @OneToMany(mappedBy = "parent", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    @JsonIgnore
    var children: MutableList<State> = mutableListOf(),

    @Column(nullable = false, unique = true, length = 150)
    var name: String,

    @Column(nullable = true, columnDefinition = "TEXT")
    var description: String? = null,

    @Column(nullable = false)
    var priority: Int = 0,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as State
        return id != null && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()
}
