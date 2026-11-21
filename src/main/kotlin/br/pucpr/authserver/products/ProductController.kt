package br.pucpr.authserver.products

import br.pucpr.authserver.common.PageResponse
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal

@RestController
@RequestMapping("/products")
@Validated
class ProductController(
    private val service: ProductService
) {
    @GetMapping
    fun list(
        @RequestParam name: String?,
        @RequestParam brandName: String?,
        @RequestParam brandId: Long?,
        @RequestParam categoryId: Long?,
        @RequestParam minPrice: BigDecimal?,
        @RequestParam maxPrice: BigDecimal?,
        @RequestParam inStock: Boolean?,
        @RequestParam sortBy: String?,
        @RequestParam sortDir: String?,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "10") @Min(1) @Max(100) size: Int
    ): ResponseEntity<PageResponse<ProductResponse>> =
        service.search(
            name, brandName, brandId, categoryId, minPrice, maxPrice, inStock, sortBy, sortDir, page, size
        )
            .let { PageResponse.of(it) { product -> ProductResponse(product) } }
            .let { ResponseEntity.ok(it) }

    @GetMapping("/{id}")
    fun getById(
        @Min(1) @PathVariable id: Long
    ): ResponseEntity<ProductResponse> =
        service.findByIdOrNull(id)
            ?.let { ProductResponse(it) }
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()

    @PostMapping
    fun insert(
        @Valid @RequestBody product: ProductRequest
    ): ResponseEntity<ProductResponse> =
        service.insert(product)
            .let { ProductResponse(it) }
            .let { ResponseEntity.status(HttpStatus.CREATED).body(it) }

    @PutMapping("/{id}")
    fun update(
        @Min(1) @PathVariable id: Long,
        @Valid @RequestBody product: ProductRequest
    ): ResponseEntity<ProductResponse> =
        service.update(id, product)
            .let { ProductResponse(it) }
            .let { ResponseEntity.ok(it) }

    @DeleteMapping("/{id}")
    fun deleteById(
        @Min(1) @PathVariable id: Long
    ): ResponseEntity<Void> =
        service.deleteById(id)
            .let { ResponseEntity.noContent().build() }

    @PutMapping("/{id}/categories/{categoryId}")
    fun addCategory(
        @Min(1) @PathVariable id: Long,
        @Min(1) @PathVariable categoryId: Long
    ): ResponseEntity<Void> =
        if (service.addCategory(id, categoryId)) ResponseEntity.ok().build()
        else ResponseEntity.noContent().build()

    @DeleteMapping("/{id}/categories/{categoryId}")
    fun removeCategory(
        @Min(1) @PathVariable id: Long,
        @Min(1) @PathVariable categoryId: Long
    ): ResponseEntity<Void> =
        if (service.removeCategory(id, categoryId)) ResponseEntity.ok().build()
        else ResponseEntity.noContent().build()
}
