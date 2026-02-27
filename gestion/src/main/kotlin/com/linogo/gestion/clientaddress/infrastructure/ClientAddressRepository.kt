package com.linogo.gestion.clientaddress.infrastructure

import com.linogo.gestion.clientaddress.domain.ClientAddress
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ClientAddressRepository : JpaRepository<ClientAddress, Long> {
    fun findByClientId(clientId: String): List<ClientAddress>
}
