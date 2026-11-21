package br.pucpr.authserver.brands

import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/brands")
@Validated
class BrandController(
    private val service: BrandService
) {
    @GetMapping
    fun list(): ResponseEntity<List<BrandResponse>> =
        service.findAll()
            .map { BrandResponse(it) }
            .let { ResponseEntity.ok(it) }

    @GetMapping("/{id}")
    fun getById(
        @Min(1) @PathVariable id: Long
    ): ResponseEntity<BrandResponse> =
        service.findByIdOrNull(id)
            ?.let { BrandResponse(it) }
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()

    @PostMapping
    fun insert(
        @Valid @RequestBody brand: BrandRequest
    ): ResponseEntity<BrandResponse> =
        service.insert(brand.toBrand())
            .let { BrandResponse(it) }
            .let { ResponseEntity.status(HttpStatus.CREATED).body(it) }

    @PutMapping("/{id}")
    fun update(
        @Min(1) @PathVariable id: Long,
        @Valid @RequestBody brand: BrandRequest
    ): ResponseEntity<BrandResponse> =
        service.update(id, brand.name!!.trim())
            .let { BrandResponse(it) }
            .let { ResponseEntity.ok(it) }

    @DeleteMapping("/{id}")
    fun deleteById(
        @Min(1) @PathVariable id: Long
    ): ResponseEntity<Void> =
        service.deleteById(id)
            .let { ResponseEntity.noContent().build() }
}
