package com.linogo.gestion.sync.infrastructure

import com.linogo.gestion.sync.application.*
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/sync")
@Tag(name = "Sincronización", description = "Endpoints para sincronización de la app móvil")
class SyncController(
    private val syncService: SyncService,
    private val syncVersionService: SyncVersionService
) {

    @GetMapping("/version")
    fun getVersion(): ResponseEntity<SyncValidateResponse> {
        val currentVersion = syncService.getCurrentVersion()?.version ?: 0L
        val description = syncService.getCurrentVersion()?.description ?: "Sin descripción"
        return ResponseEntity.ok(
            SyncValidateResponse(
                needsSync = false,
                currentVersion = currentVersion,
                description = description
            )
        )
    }

    @GetMapping("/catalog")
    fun getCatalog(): ResponseEntity<SyncCatalogResponse> {
        return ResponseEntity.ok(syncService.getCatalog())
    }

    @GetMapping("/states")
    fun getStates(): ResponseEntity<SyncStatesResponse> {
        return ResponseEntity.ok(syncService.getStates())
    }

    @PostMapping("/validate")
    fun validateSync(@RequestBody request: SyncValidateRequest): ResponseEntity<SyncValidateResponse> {
        return ResponseEntity.ok(syncService.validateSync(request.clientVersion))
    }

    @PostMapping("/version/increment")
    fun incrementVersion(@RequestParam description: String): ResponseEntity<Map<String, Any>> {
        val newVersion = syncVersionService.incrementVersion(description)
        return ResponseEntity.ok(mapOf(
            "version" to newVersion,
            "description" to description
        ))
    }
}
