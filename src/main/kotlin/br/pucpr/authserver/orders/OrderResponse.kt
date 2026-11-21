package br.pucpr.authserver.orders

import java.math.BigDecimal
import java.time.LocalDateTime

data class OrderItemResponse(
    val id: Long,
    val productId: Long,
    val productName: String,
    val quantity: Int,
    val unitPrice: BigDecimal,
    val subtotal: BigDecimal
) {
    constructor(item: OrderItem) : this(
        id = item.id!!,
        productId = item.product.id!!,
        productName = item.product.name,
        quantity = item.quantity,
        unitPrice = item.unitPrice,
        subtotal = item.unitPrice.multiply(BigDecimal(item.quantity))
    )
}

data class OrderResponse(
    val id: Long,
    val userId: Long,
    val userName: String,
    val createdAt: LocalDateTime,
    val status: OrderStatus,
    val items: List<OrderItemResponse>,
    val total: BigDecimal
) {
    constructor(order: PurchaseOrder) : this(
        id = order.id!!,
        userId = order.user.id!!,
        userName = order.user.name,
        createdAt = order.createdAt,
        status = order.status,
        items = order.items.map { OrderItemResponse(it) },
        total = order.items.fold(BigDecimal.ZERO) { total, item ->
            total.add(item.unitPrice.multiply(BigDecimal(item.quantity)))
        }
    )
}
