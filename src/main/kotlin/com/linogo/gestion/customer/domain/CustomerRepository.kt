package com.linogo.gestion.customer.domain

interface CustomerRepository {
    fun save(customer: Customer): Customer
    fun findById(id: Long): Customer?
    fun findAll(): List<Customer>
    fun deleteById(id: Long)
}
