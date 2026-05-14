package com.linogo.gestion.sync

import com.linogo.gestion.security.infrastructure.JwtAuthenticationFilter
import com.linogo.gestion.security.infrastructure.JwtTokenProvider
import com.linogo.gestion.sync.application.SyncService
import com.linogo.gestion.sync.application.SyncVersionService
import com.linogo.gestion.sync.application.SyncValidateResponse
import com.linogo.gestion.sync.application.SyncCatalogResponse
import com.linogo.gestion.sync.application.SyncStatesResponse
import com.linogo.gestion.sync.infrastructure.SyncController
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(SyncController::class)
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
@org.springframework.test.context.ActiveProfiles("test")
class SyncControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var syncService: SyncService

    @MockBean
    private lateinit var syncVersionService: SyncVersionService

    @MockBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    @MockBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @Test
    fun `GET version should return current version`() {
        org.mockito.Mockito.`when`(syncService.getCurrentVersion()).thenReturn(
            com.linogo.gestion.sync.domain.SyncVersion(
                id = 1L, version = 5L, description = "Version 5",
                createdAt = java.time.LocalDateTime.now(), updatedAt = java.time.LocalDateTime.now()
            )
        )

        mockMvc.perform(get("/api/sync/version"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.currentVersion").value(5))
            .andExpect(jsonPath("$.description").value("Version 5"))
    }

    @Test
    fun `GET version should return defaults when no version exists`() {
        org.mockito.Mockito.`when`(syncService.getCurrentVersion()).thenReturn(null)

        mockMvc.perform(get("/api/sync/version"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.currentVersion").value(0))
            .andExpect(jsonPath("$.description").value("Sin descripción"))
    }

    @Test
    fun `GET catalog should return catalog`() {
        org.mockito.Mockito.`when`(syncService.getCatalog()).thenReturn(
            SyncCatalogResponse(version = 5L, products = emptyList(), categories = emptyList())
        )

        mockMvc.perform(get("/api/sync/catalog"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.version").value(5))
    }

    @Test
    fun `GET states should return states`() {
        org.mockito.Mockito.`when`(syncService.getStates()).thenReturn(
            SyncStatesResponse(version = 5L, states = emptyList(), shipmentStates = emptyList())
        )

        mockMvc.perform(get("/api/sync/states"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.version").value(5))
    }

    @Test
    fun `POST validate should return validation result`() {
        org.mockito.Mockito.`when`(syncService.validateSync(3L)).thenReturn(
            SyncValidateResponse(needsSync = true, currentVersion = 5L, description = "Version 5")
        )

        mockMvc.perform(post("/api/sync/validate")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"clientVersion": 3}"""))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.needsSync").value(true))
            .andExpect(jsonPath("$.currentVersion").value(5))
    }

    @Test
    fun `POST validate should return no sync needed when versions match`() {
        org.mockito.Mockito.`when`(syncService.validateSync(5L)).thenReturn(
            SyncValidateResponse(needsSync = false, currentVersion = 5L, description = "Version 5")
        )

        mockMvc.perform(post("/api/sync/validate")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"clientVersion": 5}"""))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.needsSync").value(false))
    }
}
