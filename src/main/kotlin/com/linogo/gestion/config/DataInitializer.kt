package com.linogo.gestion.config

import com.linogo.gestion.carrier.domain.Carrier
import com.linogo.gestion.carrier.infrastructure.CarrierRepository
import com.linogo.gestion.category.domain.Category
import com.linogo.gestion.category.infrastructure.CategoryRepository
import com.linogo.gestion.customer.domain.Customer
import com.linogo.gestion.customer.domain.CustomerRepository
import com.linogo.gestion.security.domain.Permission
import com.linogo.gestion.security.domain.Role
import com.linogo.gestion.security.domain.User
import com.linogo.gestion.security.infrastructure.RoleRepository
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

    // Dataset completo de prueba: solo para desarrollo local. Nunca debe
    // correr en produccion (crea clientes y usuarios ficticios).
    @Bean
    @Profile("dev")
    fun seedDevData(
        userRepository: UserRepository,
        roleRepository: RoleRepository,
        passwordEncoder: PasswordEncoder,
        stateRepository: StateRepository,
        shipmentStateRepository: ShipmentStateRepository,
        categoryRepository: CategoryRepository,
        syncVersionRepository: SyncVersionRepository,
        customerRepository: CustomerRepository,
        carrierRepository: CarrierRepository
    ): CommandLineRunner {
        return CommandLineRunner {
            seedSyncVersion(syncVersionRepository)
            seedStates(stateRepository)
            seedShipmentStates(stateRepository, shipmentStateRepository)
            seedCategories(categoryRepository)
            val adminRole = seedAdminRole(roleRepository)
            val ventasRole = seedRole(roleRepository, "Ventas", setOf(
                Permission.CUSTOMERS_MANAGE, Permission.ORDERS_MANAGE, Permission.ORDERS_SHIP, Permission.DASHBOARD_VIEW
            ))
            seedRole(roleRepository, "Producción", setOf(Permission.PRODUCTS_MANAGE))
            seedRole(roleRepository, "Logística", setOf(
                Permission.CARRIERS_MANAGE, Permission.SHIPMENTS_MANAGE, Permission.ORDERS_SHIP
            ))
            seedUsers(userRepository, passwordEncoder, adminRole, ventasRole)
            seedCustomers(customerRepository)
            seedCarriers(carrierRepository)
        }
    }

    // Produccion arranca en blanco: el unico dato creado es el admin inicial,
    // con contrasena temporal que se debe cambiar en el primer ingreso.
    @Bean
    @Profile("prod")
    fun seedProdAdmin(
        userRepository: UserRepository,
        roleRepository: RoleRepository,
        passwordEncoder: PasswordEncoder
    ): CommandLineRunner {
        return CommandLineRunner {
            if (!userRepository.existsByUsername("admin")) {
                val adminRole = seedAdminRole(roleRepository)
                val admin = User(
                    username = "admin",
                    email = "admin@linogo.com",
                    password = passwordEncoder.encode("123456"),
                    fullName = "Administrador del Sistema",
                    roles = mutableSetOf(adminRole),
                    _isEnabled = true
                )
                userRepository.save(admin)
                log.warn("Seed: usuario admin inicial creado con contraseña temporal '123456' — cámbiala de inmediato desde la plataforma.")
            }
        }
    }

    // "Buscar o crear": una vez creado, el rol admin es propiedad de la UI de
    // Roles y Permisos — nunca se resincroniza en cada arranque, para no
    // pisar una edición manual posterior.
    private fun seedAdminRole(repo: RoleRepository): Role =
        repo.findByNameIgnoreCase("Administrador") ?: repo.save(
            Role(
                name = "Administrador",
                description = "Acceso total al sistema",
                permissions = Permission.entries.map { it.code }.toMutableSet()
            )
        )

    private fun seedRole(repo: RoleRepository, name: String, permissions: Set<Permission>): Role =
        repo.findByNameIgnoreCase(name) ?: repo.save(
            Role(name = name, permissions = permissions.map { it.code }.toMutableSet())
        )

    private fun seedSyncVersion(repo: SyncVersionRepository) {
        if (!repo.existsById(1L)) {
            repo.save(SyncVersion(id = 1L, version = 1L, description = "Versión inicial"))
            log.info("Seed: sync_version creada")
        }
    }

    private fun seedStates(repo: StateRepository) {
        if (repo.count() > 0) return
        val states = listOf(
            State(name = "PENDIENTE", type = "OPERATION", priority = 1),
            State(name = "EN PROCESO", type = "OPERATION", priority = 2),
            State(name = "ENVIADO", type = "OPERATION", priority = 3),
            State(name = "DISPONIBLE", type = "OPERATION", priority = 4),
            State(name = "FINALIZADO", type = "OPERATION", priority = 5),
            State(name = "NOVEDAD", type = "OPERATION", priority = 6)
        )
        repo.saveAll(states)
        log.info("Seed: ${states.size} estados de operación creados")
    }

    private fun seedShipmentStates(stateRepo: StateRepository, repo: ShipmentStateRepository) {
        if (repo.count() > 0) return
        val states = stateRepo.findAll().associateBy { it.name }
        val shipmentStates = listOf(
            ShipmentState(name = "Pendiente", state = states["PENDIENTE"]!!),
            ShipmentState(name = "En tránsito", state = states["ENVIADO"]!!),
            ShipmentState(name = "En oficina", state = states["DISPONIBLE"]!!),
            ShipmentState(name = "Entregado", state = states["FINALIZADO"]!!),
            ShipmentState(name = "Novedad", state = states["NOVEDAD"]!!)
        )
        repo.saveAll(shipmentStates)
        log.info("Seed: ${shipmentStates.size} estados de envío creados")
    }

    private fun seedCategories(repo: CategoryRepository) {
        if (repo.count() > 0) return
        val categories = listOf(
            Category(name = "Moldes"),
            Category(name = "Accesorios"),
            Category(name = "Herramientas")
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

    private fun seedCarriers(repo: CarrierRepository) {
        // No usamos "if (repo.count() > 0) return" como en los otros seeds:
        // la migracion V4 ya inserta "Inter rapidisimo" (la necesita para
        // migrar envios existentes), asi que el conteo nunca seria 0 aqui.
        // Se verifica cada transportadora por separado para no bloquear la
        // siembra del resto solo porque una ya exista.
        val carriers = listOf(
            Carrier(name = "Inter rapidisimo", contactPhone = "+57 1 800 000 0001"),
            Carrier(name = "Coordinadora", contactPhone = "+57 1 800 000 0002"),
            Carrier(name = "Servientrega", contactPhone = "+57 1 800 000 0003"),
            Carrier(name = "TCC", contactPhone = "+57 1 800 000 0004")
        )
        val toCreate = carriers.filter { repo.findByNameIgnoreCase(it.name) == null }
        if (toCreate.isEmpty()) return
        repo.saveAll(toCreate)
        log.info("Seed: ${toCreate.size} transportadoras creadas")
    }

    private fun seedUsers(repo: UserRepository, encoder: PasswordEncoder, adminRole: Role, ventasRole: Role) {
        if (!repo.existsByUsername("admin")) {
            val admin = User(
                username = "admin",
                email = "admin@linogo.com",
                password = encoder.encode("Admin@123456"),
                fullName = "Administrador del Sistema",
                roles = mutableSetOf(adminRole),
                _isEnabled = true
            )
            repo.save(admin)
        }

        if (!repo.existsByUsername("user")) {
            val demoUser = User(
                username = "user",
                email = "user@linogo.com",
                password = encoder.encode("User@123456"),
                fullName = "Usuario Demo",
                roles = mutableSetOf(ventasRole),
                _isEnabled = true
            )
            repo.save(demoUser)
        }

        log.info("Seed: usuarios creados (admin/Admin@123456, user/User@123456)")
    }
}
