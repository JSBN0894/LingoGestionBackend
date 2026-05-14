package com.linogo.gestion.customer.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface JpaCustomerRepository : JpaRepository<CustomerEntity, Long>
