package br.pucpr.authserver.orders

import br.pucpr.authserver.products.Product
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import java.math.BigDecimal

@Entity
class OrderItem(
    @Id @GeneratedValue
    var id: Long? = null,

    @ManyToOne(optional = false)
    @JoinColumn(name = "idOrder")
    var order: PurchaseOrder,

    @ManyToOne(optional = false)
    @JoinColumn(name = "idProduct")
    var product: Product,

    @Column(nullable = false)
    var quantity: Int,

    @Column(nullable = false, precision = 10, scale = 2)
    var unitPrice: BigDecimal
)
