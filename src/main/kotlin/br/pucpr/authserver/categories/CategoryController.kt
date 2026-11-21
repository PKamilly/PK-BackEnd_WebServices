package br.pucpr.authserver.categories

import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/categories")
@Validated
class CategoryController(
    private val service: CategoryService
) {
    @GetMapping
    fun list(): ResponseEntity<List<CategoryResponse>> =
        service.findAll()
            .map { CategoryResponse(it) }
            .let { ResponseEntity.ok(it) }

    @GetMapping("/{id}")
    fun getById(
        @Min(1) @PathVariable id: Long
    ): ResponseEntity<CategoryResponse> =
        service.findByIdOrNull(id)
            ?.let { CategoryResponse(it) }
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()

    @PostMapping
    fun insert(
        @Valid @RequestBody category: CategoryRequest
    ): ResponseEntity<CategoryResponse> =
        service.insert(category.toCategory())
            .let { CategoryResponse(it) }
            .let { ResponseEntity.status(HttpStatus.CREATED).body(it) }

    @PutMapping("/{id}")
    fun update(
        @Min(1) @PathVariable id: Long,
        @Valid @RequestBody category: CategoryRequest
    ): ResponseEntity<CategoryResponse> =
        service.update(id, category.name!!.trim())
            .let { CategoryResponse(it) }
            .let { ResponseEntity.ok(it) }

    @DeleteMapping("/{id}")
    fun deleteById(
        @Min(1) @PathVariable id: Long
    ): ResponseEntity<Void> =
        service.deleteById(id)
            .let { ResponseEntity.noContent().build() }
}
