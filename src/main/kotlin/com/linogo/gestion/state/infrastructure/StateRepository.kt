package com.linogo.gestion.state.infrastructure

import com.linogo.gestion.state.domain.State
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface StateRepository : JpaRepository<State, Long>
