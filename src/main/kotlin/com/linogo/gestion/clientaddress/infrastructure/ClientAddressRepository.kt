package com.linogo.gestion.clientaddress.infrastructure

import com.linogo.gestion.clientaddress.domain.ClientAddress
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ClientAddressRepository : JpaRepository<ClientAddress, Long> {
    @Query("SELECT ca FROM ClientAddress ca WHERE ca.client.idUser = :clientId")
    fun findByClientId(@Param("clientId") clientId: String): List<ClientAddress>
}
