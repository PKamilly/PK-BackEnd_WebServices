package br.pucpr.authserver.orders

import br.pucpr.authserver.common.PageResponse
import br.pucpr.authserver.security.AuthenticatedUser
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/orders")
@Validated
class OrderController(
    private val service: OrderService
) {
    @GetMapping
    fun list(
        @RequestParam status: OrderStatus?,
        @RequestParam userId: Long?,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate?,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate?,
        @RequestParam sortBy: String?,
        @RequestParam sortDir: String?,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "10") @Min(1) @Max(100) size: Int,
        @AuthenticationPrincipal auth: AuthenticatedUser
    ): ResponseEntity<PageResponse<OrderResponse>> =
        service.search(auth, status, userId, from, to, sortBy, sortDir, page, size)
            .let { PageResponse.of(it) { order -> OrderResponse(order) } }
            .let { ResponseEntity.ok(it) }

    @GetMapping("/{id}")
    fun getById(
        @Min(1) @PathVariable id: Long,
        @AuthenticationPrincipal auth: AuthenticatedUser
    ): ResponseEntity<OrderResponse> =
        service.findById(id, auth)
            .let { OrderResponse(it) }
            .let { ResponseEntity.ok(it) }

    @PostMapping
    fun insert(
        @AuthenticationPrincipal auth: AuthenticatedUser
    ): ResponseEntity<OrderResponse> =
        service.create(auth)
            .let { OrderResponse(it) }
            .let { ResponseEntity.status(HttpStatus.CREATED).body(it) }

    @PostMapping("/{id}/items")
    fun addItem(
        @Min(1) @PathVariable id: Long,
        @Valid @RequestBody item: OrderItemRequest,
        @AuthenticationPrincipal auth: AuthenticatedUser
    ): ResponseEntity<OrderResponse> =
        service.addItem(id, auth, item.productId!!, item.quantity!!)
            .let { OrderResponse(it) }
            .let { ResponseEntity.ok(it) }

    @DeleteMapping("/{id}/items/{itemId}")
    fun removeItem(
        @Min(1) @PathVariable id: Long,
        @Min(1) @PathVariable itemId: Long,
        @AuthenticationPrincipal auth: AuthenticatedUser
    ): ResponseEntity<OrderResponse> =
        service.removeItem(id, itemId, auth)
            .let { OrderResponse(it) }
            .let { ResponseEntity.ok(it) }

    @PostMapping("/{id}/close")
    fun close(
        @Min(1) @PathVariable id: Long,
        @AuthenticationPrincipal auth: AuthenticatedUser
    ): ResponseEntity<OrderResponse> =
        service.close(id, auth)
            .let { OrderResponse(it) }
            .let { ResponseEntity.ok(it) }

    @DeleteMapping("/{id}")
    fun delete(
        @Min(1) @PathVariable id: Long,
        @AuthenticationPrincipal auth: AuthenticatedUser
    ): ResponseEntity<Void> =
        service.delete(id, auth)
            .let { ResponseEntity.noContent().build() }
}
