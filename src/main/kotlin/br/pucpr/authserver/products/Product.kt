package br.pucpr.authserver.products

import br.pucpr.authserver.brands.Brand
import br.pucpr.authserver.categories.Category
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import java.math.BigDecimal
import java.util.Objects

@Entity
class Product(
    @Id @GeneratedValue
    var id: Long? = null,

    @Column(nullable = false)
    var name: String,

    @Column(nullable = false, precision = 10, scale = 2)
    var price: BigDecimal,

    @Column(nullable = false)
    var stock: Int = 0,

    @ManyToOne(optional = false)
    @JoinColumn(name = "idBrand")
    var brand: Brand,

    @ManyToMany
    @JoinTable(
        name = "ProductCategory",
        joinColumns = [JoinColumn(name = "idProduct")],
        inverseJoinColumns = [JoinColumn(name = "idCategory")],
    )
    var categories: MutableSet<Category> = mutableSetOf()
) {
    final override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (this === other) return true
        if (this::class !== other::class) return false
        return (other as Product).id == id
    }

    final override fun hashCode(): Int = Objects.hash(id)
}
