package br.pucpr.authserver.brands

import jakarta.validation.constraints.NotBlank

data class BrandRequest(
    @NotBlank
    var name: String?
) {
    fun toBrand() = Brand(id = null, name = name!!.trim())
}
