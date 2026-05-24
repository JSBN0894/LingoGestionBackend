package com.linogo.gestion.config

import com.linogo.gestion.category.domain.Category
import com.linogo.gestion.category.infrastructure.CategoryRepository
import com.linogo.gestion.customer.domain.Customer
import com.linogo.gestion.customer.domain.CustomerRepository
import com.linogo.gestion.security.domain.Role
import com.linogo.gestion.security.domain.User
import com.linogo.gestion.security.infrastructure.UserRepository
import com.linogo.gestion.shipmentstate.domain.ShipmentState
import com.linogo.gestion.shipmentstate.infrastructure.ShipmentStateRepository
import com.linogo.gestion.state.domain.State
import com.linogo.gestion.state.infrastructure.StateRepository
import com.linogo.gestion.sync.domain.SyncVersion
import com.linogo.gestion.sync.infrastructure.SyncVersionRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.crypto.password.PasswordEncoder

@Configuration
class DataInitializer {

    private val log = LoggerFactory.getLogger(DataInitializer::class.java)

    @Bean
    @Profile("dev", "prod")
    fun seedData(
        userRepository: UserRepository,
        passwordEncoder: PasswordEncoder,
        stateRepository: StateRepository,
        shipmentStateRepository: ShipmentStateRepository,
        categoryRepository: CategoryRepository,
        syncVersionRepository: SyncVersionRepository,
        customerRepository: CustomerRepository
    ): CommandLineRunner {
        return CommandLineRunner {
            seedSyncVersion(syncVersionRepository)
            seedStates(stateRepository)
            seedShipmentStates(stateRepository, shipmentStateRepository)
            seedCategories(categoryRepository)
            seedUsers(userRepository, passwordEncoder)
            seedCustomers(customerRepository)
        }
    }

    private fun seedSyncVersion(repo: SyncVersionRepository) {
        if (!repo.existsById(1L)) {
            repo.save(SyncVersion(id = 1L, version = 1L, description = "Versión inicial"))
            log.info("Seed: sync_version creada")
        }
    }

    private fun seedStates(repo: StateRepository) {
        if (repo.count() > 0) return
        val states = listOf(
            State(id = 1, name = "PENDIENTE", type = "OPERATION", priority = 1),
            State(id = 2, name = "EN PROCESO", type = "OPERATION", priority = 2),
            State(id = 3, name = "ENVIADO", type = "OPERATION", priority = 3),
            State(id = 4, name = "DISPONIBLE", type = "OPERATION", priority = 4),
            State(id = 5, name = "FINALIZADO", type = "OPERATION", priority = 5),
            State(id = 6, name = "NOVEDAD", type = "OPERATION", priority = 6)
        )
        repo.saveAll(states)
        log.info("Seed: ${states.size} estados de operación creados")
    }

    private fun seedShipmentStates(stateRepo: StateRepository, repo: ShipmentStateRepository) {
        if (repo.count() > 0) return
        val states = stateRepo.findAll().associateBy { it.name }
        val shipmentStates = listOf(
            ShipmentState(id = 1, name = "Pendiente", state = states["PENDIENTE"]!!),
            ShipmentState(id = 2, name = "En tránsito", state = states["ENVIADO"]!!),
            ShipmentState(id = 3, name = "En oficina", state = states["DISPONIBLE"]!!),
            ShipmentState(id = 4, name = "Entregado", state = states["FINALIZADO"]!!),
            ShipmentState(id = 5, name = "Novedad", state = states["NOVEDAD"]!!)
        )
        repo.saveAll(shipmentStates)
        log.info("Seed: ${shipmentStates.size} estados de envío creados")
    }

    private fun seedCategories(repo: CategoryRepository) {
        if (repo.count() > 0) return
        val categories = listOf(
            Category(id = 1, name = "Moldes"),
            Category(id = 2, name = "Accesorios"),
            Category(id = 3, name = "Herramientas")
        )
        repo.saveAll(categories)
        log.info("Seed: ${categories.size} categorías creadas")
    }

    private fun seedCustomers(repo: CustomerRepository) {
        if (repo.findByCedula(123456789L) != null) return
        val customers = listOf(
            Customer(cedula = 123456789L, name = "Carlos García", phones = listOf("+57 300 123 4567"), addresses = listOf("Calle 10 #20-30, Bogotá")),
            Customer(cedula = 987654321L, name = "María López", phones = listOf("+57 311 987 6543"), addresses = listOf("Carrera 15 #45-67, Medellín"))
        )
        repo.saveAll(customers)
        log.info("Seed: ${customers.size} clientes creados")
    }

    private fun seedUsers(repo: UserRepository, encoder: PasswordEncoder) {
        if (repo.existsByUsername("admin")) return
        val users = listOf(
            User(
                id = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
                username = "admin",
                email = "admin@linogo.com",
                password = encoder.encode("Admin@123456"),
                fullName = "Administrador del Sistema",
                role = Role.ADMIN,
                _isEnabled = true
            ),
            User(
                id = "b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a22",
                username = "user",
                email = "user@linogo.com",
                password = encoder.encode("User@123456"),
                fullName = "Usuario Demo",
                role = Role.USER,
                _isEnabled = true
            )
        )
        repo.saveAll(users)
        log.info("Seed: ${users.size} usuarios creados (admin/Admin@123456, user/User@123456)")
    }
}
