package com.linogo.gestion.client.infrastructure

import com.linogo.gestion.client.domain.Client
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ClientRepository : JpaRepository<Client, String>
