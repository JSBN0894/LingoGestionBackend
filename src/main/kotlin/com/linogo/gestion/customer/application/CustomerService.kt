package com.linogo.gestion.customer.application

import com.linogo.gestion.customer.domain.Customer
import com.linogo.gestion.customer.domain.CustomerRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CustomerService(private val customerRepository: CustomerRepository) {

    @Transactional
    fun createCustomer(customer: Customer): Customer {
        return customerRepository.save(customer)
    }

    @Transactional(readOnly = true)
    fun getCustomer(id: Long): Customer? {
        return customerRepository.findById(id)
    }

    @Transactional(readOnly = true)
    fun getAllCustomers(): List<Customer> {
        return customerRepository.findAll()
    }

    @Transactional
    fun deleteCustomer(id: Long) {
        customerRepository.deleteById(id)
    }
}
