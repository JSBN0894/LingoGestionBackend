package com.linogo.gestion.security.infrastructure

import com.linogo.gestion.security.domain.Role
import com.linogo.gestion.security.domain.User
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.crypto.password.PasswordEncoder

/**
 * Configuración para crear el usuario administrador inicial.
 * 
 * Se ejecuta solo en perfil 'dev' al iniciar la aplicación.
 * 
 * Credenciales por defecto:
 *   Username: admin
 *   Password: Admin@123456
 * 
 * IMPORTANTE: Cambiar la contraseña en producción!
 */
@Configuration
class AdminInitializer {

    private val logger = LoggerFactory.getLogger(AdminInitializer::class.java)

    @Bean
    @Profile("dev")  // Solo en desarrollo
    fun createAdminUser(
        userRepository: UserRepository,
        passwordEncoder: PasswordEncoder
    ): CommandLineRunner {
        return CommandLineRunner {
            // Verificar si ya existe el admin
            if (!userRepository.existsByUsername("admin")) {
                val admin = User(
                    username = "admin",
                    email = "admin@linogo.local",
                    password = passwordEncoder.encode("Admin@123456"),
                    fullName = "Administrador del Sistema",
                    role = Role.ADMIN,
                    enabled = true
                )

                userRepository.save(admin)
                logger.info("========================================")
                logger.info("USUARIO ADMINISTRADOR CREADO")
                logger.info("Username: admin")
                logger.info("Password: Admin@123456")
                logger.info("========================================")
                logger.info("IMPORTANTE: Cambiar la contraseña después del primer login!")
            }
        }
    }
}
