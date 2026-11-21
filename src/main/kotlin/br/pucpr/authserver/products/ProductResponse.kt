package br.pucpr.authserver.products

import br.pucpr.authserver.brands.BrandResponse
import br.pucpr.authserver.categories.CategoryResponse
import java.math.BigDecimal

data class ProductResponse(
    val id: Long,
    val name: String,
    val price: BigDecimal,
    val stock: Int,
    val brand: BrandResponse,
    val categories: List<CategoryResponse>
) {
    constructor(product: Product) : this(
        id = product.id!!,
        name = product.name,
        price = product.price,
        stock = product.stock,
        brand = BrandResponse(product.brand),
        categories = product.categories
            .sortedBy { it.name }
            .map { CategoryResponse(it) }
    )
}
