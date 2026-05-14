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
        customer = Customer(cedula = 123456789L, name = "Juan Pérez", phones = listOf("+54911223344"))
    }

    @Test
    fun `createCustomer should save and return customer`() {
        `when`(customerRepository.save(any())).thenReturn(customer)

        val result = customerService.createCustomer(customer)

        assertNotNull(result)
        assertEquals(123456789L, result.cedula)
        assertEquals("Juan Pérez", result.name)
        verify(customerRepository).save(any())
    }

    @Test
    fun `getCustomer should return customer when exists`() {
        `when`(customerRepository.findByCedula(123456789L)).thenReturn(customer)

        val result = customerService.getCustomer(123456789L)

        assertNotNull(result)
        assertEquals(123456789L, result!!.cedula)
        verify(customerRepository).findByCedula(123456789L)
    }

    @Test
    fun `getCustomer should return null when not found`() {
        `when`(customerRepository.findByCedula(999L)).thenReturn(null)

        val result = customerService.getCustomer(999L)

        assertNull(result)
        verify(customerRepository).findByCedula(999L)
    }

    @Test
    fun `getAllCustomers should return list of customers`() {
        val customers = listOf(customer, Customer(cedula = 987654321L, name = "María"))
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
        customerService.deleteCustomer(123456789L)

        verify(customerRepository).deleteByCedula(123456789L)
    }
}
