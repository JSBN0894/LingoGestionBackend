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

    @GetMapping("/{cedula}")
    fun get(@PathVariable cedula: Long): Customer? {
        return customerService.getCustomer(cedula)
    }

    @GetMapping
    fun getAll(): List<Customer> {
        return customerService.getAllCustomers()
    }

    @PutMapping("/{cedula}")
    fun update(@PathVariable cedula: Long, @RequestBody customer: Customer): Customer {
        return customerService.updateCustomer(cedula, customer)
    }

    @DeleteMapping("/{cedula}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable cedula: Long) {
        customerService.deleteCustomer(cedula)
    }
}
