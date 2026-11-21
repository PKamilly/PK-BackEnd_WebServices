package br.pucpr.authserver.brands

data class BrandResponse(
    val id: Long,
    val name: String
) {
    constructor(brand: Brand) : this(
        id = brand.id!!,
        name = brand.name
    )
}
