package com.linogo.gestion

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootApplication
@EntityScan("com.linogo.gestion")
@EnableJpaRepositories("com.linogo.gestion")
class GestionApplication

fun main(args: Array<String>) {
	runApplication<GestionApplication>(*args)
}
