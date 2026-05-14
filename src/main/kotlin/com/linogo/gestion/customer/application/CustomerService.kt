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
    fun getCustomer(cedula: Long): Customer? {
        return customerRepository.findByCedula(cedula)
    }

    @Transactional(readOnly = true)
    fun getAllCustomers(): List<Customer> {
        return customerRepository.findAll()
    }

    @Transactional
    fun updateCustomer(cedula: Long, customer: Customer): Customer {
        if (!customerRepository.existsByCedula(cedula)) {
            throw IllegalArgumentException("Customer with cedula $cedula not found")
        }
        return customerRepository.save(customer.copy(cedula = cedula))
    }

    @Transactional
    fun deleteCustomer(cedula: Long) {
        customerRepository.deleteByCedula(cedula)
    }
}
