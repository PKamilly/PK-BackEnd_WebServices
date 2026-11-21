package br.pucpr.authserver.orders

import br.pucpr.authserver.users.User
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.time.LocalDateTime

enum class OrderStatus { OPEN, CLOSED }

@Entity
@Table(name = "PurchaseOrder")
class PurchaseOrder(
    @Id @GeneratedValue
    var id: Long? = null,

    @ManyToOne(optional = false)
    @JoinColumn(name = "idUser")
    var user: User,

    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: OrderStatus = OrderStatus.OPEN,

    @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL], orphanRemoval = true)
    var items: MutableList<OrderItem> = mutableListOf()
)
