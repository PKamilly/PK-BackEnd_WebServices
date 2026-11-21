package br.pucpr.authserver.products

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.math.BigDecimal

data class ProductRequest(
    @NotBlank
    var name: String?,

    @NotNull
    @Positive
    var price: BigDecimal?,

    @NotNull
    @Min(0)
    var stock: Int?,

    @NotNull
    @Min(1)
    var brandId: Long?,

    var categoryIds: Set<Long>? = null
)
