package com.linogo.gestion.clientphone.infrastructure

import com.linogo.gestion.clientphone.domain.ClientPhone
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ClientPhoneRepository : JpaRepository<ClientPhone, Long> {
    fun findByClientId(clientId: String): List<ClientPhone>
}
