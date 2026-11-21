package br.pucpr.authserver.orders

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

data class OrderItemRequest(
    @NotNull
    @Min(1)
    var productId: Long?,

    @NotNull
    @Min(1)
    var quantity: Int?
)
