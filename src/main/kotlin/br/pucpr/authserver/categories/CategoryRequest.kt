package br.pucpr.authserver.categories

import jakarta.validation.constraints.NotBlank

data class CategoryRequest(
    @NotBlank
    var name: String?
) {
    fun toCategory() = Category(id = null, name = name!!.trim())
}
