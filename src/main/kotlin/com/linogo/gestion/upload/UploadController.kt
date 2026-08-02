package com.linogo.gestion.upload

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Instant
import java.util.*

@RestController
@RequestMapping("/api/upload")
class UploadController(
    @Value("\${app.upload.dir:uploads}") private val uploadDir: String
) {

    @PostMapping
    fun upload(@RequestParam("file") file: MultipartFile): ResponseEntity<Map<String, String>> {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(mapOf("error" to "No file provided"))
        }

        val originalName = file.originalFilename ?: "unknown"
        val extension = originalName.substringAfterLast('.', "jpg")
        val safeName = "${Instant.now().toEpochMilli()}_${UUID.randomUUID().toString().take(8)}.$extension"
        val uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize()
        val targetPath = uploadPath.resolve(safeName)

        Files.createDirectories(uploadPath)
        file.transferTo(targetPath.toFile())

        val url = "/uploads/$safeName"
        return ResponseEntity.status(HttpStatus.CREATED).body(mapOf("url" to url))
    }
}
