package com.linogo.gestion.clientphone.infrastructure

import com.linogo.gestion.clientphone.domain.ClientPhone
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ClientPhoneRepository : JpaRepository<ClientPhone, Long> {
    @Query("SELECT cp FROM ClientPhone cp WHERE cp.client.idUser = :clientId")
    fun findByClientId(@Param("clientId") clientId: String): List<ClientPhone>
}
