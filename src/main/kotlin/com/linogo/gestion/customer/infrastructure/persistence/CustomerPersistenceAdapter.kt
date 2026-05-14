package com.linogo.gestion.customer.infrastructure.persistence

import com.linogo.gestion.customer.domain.Customer
import com.linogo.gestion.customer.domain.CustomerRepository
import org.springframework.stereotype.Component

@Component
class CustomerPersistenceAdapter(
    private val jpaCustomerRepository: JpaCustomerRepository
) : CustomerRepository {

    override fun save(customer: Customer): Customer {
        val entity = customer.toEntity()
        val savedEntity = jpaCustomerRepository.save(entity)
        return savedEntity.toDomain()
    }

    override fun findById(id: Long): Customer? {
        return jpaCustomerRepository.findById(id).map { it.toDomain() }.orElse(null)
    }

    override fun findAll(): List<Customer> {
        return jpaCustomerRepository.findAll().map { it.toDomain() }
    }

    override fun deleteById(id: Long) {
        jpaCustomerRepository.deleteById(id)
    }

    private fun Customer.toEntity() = CustomerEntity(
        id = id,
        name = name,
        email = email,
        phones = phones
    )

    private fun CustomerEntity.toDomain() = Customer(
        id = id,
        name = name,
        email = email,
        phones = phones
    )
}
