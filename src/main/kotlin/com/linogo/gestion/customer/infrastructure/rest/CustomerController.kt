package com.linogo.gestion.customer.infrastructure.rest

import com.linogo.gestion.customer.application.CustomerService
import com.linogo.gestion.customer.domain.Customer
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/customers")
class CustomerController(private val customerService: CustomerService) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody customer: Customer): Customer {
        return customerService.createCustomer(customer)
    }

    @GetMapping("/{id}")
    fun get(@PathVariable id: Long): Customer? {
        return customerService.getCustomer(id)
    }

    @GetMapping
    fun getAll(): List<Customer> {
        return customerService.getAllCustomers()
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long) {
        customerService.deleteCustomer(id)
    }
}
