package com.linogo.gestion.customer.infrastructure.rest

import com.linogo.gestion.customer.application.CustomerService
import com.linogo.gestion.customer.domain.Customer
import com.linogo.gestion.security.config.Authenticated
import com.linogo.gestion.security.config.RequiresPermission
import com.linogo.gestion.security.domain.Permission
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/customers")
class CustomerController(private val customerService: CustomerService) {

    @PostMapping
    @RequiresPermission(Permission.CUSTOMERS_MANAGE)
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody customer: Customer): Customer {
        return customerService.createCustomer(customer)
    }

    @GetMapping("/{cedula}")
    @Authenticated
    fun get(@PathVariable cedula: Long): Customer? {
        return customerService.getCustomer(cedula)
    }

    @GetMapping
    @Authenticated
    fun getAll(): List<Customer> {
        return customerService.getAllCustomers()
    }

    @PutMapping("/{cedula}")
    @RequiresPermission(Permission.CUSTOMERS_MANAGE)
    fun update(@PathVariable cedula: Long, @RequestBody customer: Customer): Customer {
        return customerService.updateCustomer(cedula, customer)
    }

    @DeleteMapping("/{cedula}")
    @RequiresPermission(Permission.CUSTOMERS_DELETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable cedula: Long) {
        customerService.deleteCustomer(cedula)
    }
}
