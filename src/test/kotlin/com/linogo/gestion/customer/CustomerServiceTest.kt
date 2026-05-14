package com.linogo.gestion.customer

import com.linogo.gestion.customer.application.CustomerService
import com.linogo.gestion.customer.domain.Customer
import com.linogo.gestion.customer.domain.CustomerRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any

@ExtendWith(MockitoExtension::class)
class CustomerServiceTest {

    @Mock
    private lateinit var customerRepository: CustomerRepository

    @InjectMocks
    private lateinit var customerService: CustomerService

    private lateinit var customer: Customer

    @BeforeEach
    fun setUp() {
        customer = Customer(name = "Juan Pérez", email = "juan@example.com", phones = listOf("+54911223344"))
    }

    @Test
    fun `createCustomer should save and return customer`() {
        val savedCustomer = customer.copy(id = 1L)
        `when`(customerRepository.save(any())).thenReturn(savedCustomer)

        val result = customerService.createCustomer(customer)

        assertNotNull(result)
        assertEquals(1L, result.id)
        assertEquals("Juan Pérez", result.name)
        verify(customerRepository).save(any())
    }

    @Test
    fun `getCustomer should return customer when exists`() {
        val saved = customer.copy(id = 1L)
        `when`(customerRepository.findById(1L)).thenReturn(saved)

        val result = customerService.getCustomer(1L)

        assertNotNull(result)
        assertEquals(1L, result!!.id)
        verify(customerRepository).findById(1L)
    }

    @Test
    fun `getCustomer should return null when not found`() {
        `when`(customerRepository.findById(999L)).thenReturn(null)

        val result = customerService.getCustomer(999L)

        assertNull(result)
        verify(customerRepository).findById(999L)
    }

    @Test
    fun `getAllCustomers should return list of customers`() {
        val customers = listOf(customer.copy(id = 1L), customer.copy(id = 2L, name = "María"))
        `when`(customerRepository.findAll()).thenReturn(customers)

        val result = customerService.getAllCustomers()

        assertEquals(2, result.size)
        verify(customerRepository).findAll()
    }

    @Test
    fun `getAllCustomers should return empty list when none exist`() {
        `when`(customerRepository.findAll()).thenReturn(emptyList())

        val result = customerService.getAllCustomers()

        assertEquals(0, result.size)
        verify(customerRepository).findAll()
    }

    @Test
    fun `deleteCustomer should remove customer`() {
        customerService.deleteCustomer(1L)

        verify(customerRepository).deleteById(1L)
    }
}
