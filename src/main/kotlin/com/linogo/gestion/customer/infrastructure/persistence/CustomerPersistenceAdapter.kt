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

    override fun saveAll(customers: List<Customer>): List<Customer> {
        return jpaCustomerRepository.saveAll(customers.map { it.toEntity() }).map { it.toDomain() }
    }

    override fun findByCedula(cedula: Long): Customer? {
        return jpaCustomerRepository.findById(cedula).map { it.toDomain() }.orElse(null)
    }

    override fun findAll(): List<Customer> {
        return jpaCustomerRepository.findAll().map { it.toDomain() }
    }

    override fun deleteByCedula(cedula: Long) {
        jpaCustomerRepository.deleteById(cedula)
    }

    override fun existsByCedula(cedula: Long): Boolean {
        return jpaCustomerRepository.existsById(cedula)
    }

    private fun Customer.toEntity() = CustomerEntity(
        cedula = cedula,
        name = name,
        phones = phones,
        addresses = addresses
    )

    private fun CustomerEntity.toDomain() = Customer(
        cedula = cedula,
        name = name,
        phones = phones,
        addresses = addresses
    )
}
