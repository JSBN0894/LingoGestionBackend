package com.linogo.gestion.customer.domain

interface CustomerRepository {
    fun save(customer: Customer): Customer
    fun saveAll(customers: List<Customer>): List<Customer>
    fun findByCedula(cedula: Long): Customer?
    fun findAll(): List<Customer>
    fun deleteByCedula(cedula: Long)
    fun existsByCedula(cedula: Long): Boolean
}
