package br.pucpr.authserver.orders

import br.pucpr.authserver.products.InsufficientStockException
import br.pucpr.authserver.products.ProductRepository
import br.pucpr.authserver.security.AuthenticatedUser
import br.pucpr.authserver.users.SortDir
import br.pucpr.authserver.users.User
import br.pucpr.authserver.users.UserRepository
import jakarta.persistence.criteria.Predicate
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate
import java.time.LocalDateTime

@Service
@Transactional
class OrderService(
    val repository: OrderRepository,
    val productRepository: ProductRepository,
    val userRepository: UserRepository
) {
    fun search(
        requester: AuthenticatedUser,
        status: OrderStatus?,
        userId: Long?,
        from: LocalDate?,
        to: LocalDate?,
        sortBy: String?,
        sortDir: String?,
        page: Int,
        size: Int
    ): Page<PurchaseOrder> {
        val property = SORTABLE_FIELDS[sortBy ?: "createdAt"]
            ?: throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid sortBy! Use one of: ${SORTABLE_FIELDS.keys.joinToString()}"
            )
        if (from != null && to != null && from.isAfter(to)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "from cannot be after to")
        }
        val direction =
            if (SortDir.findOrThrow(sortDir ?: "DESC") == SortDir.ASC) Sort.Direction.ASC
            else Sort.Direction.DESC
        val sort = Sort.by(direction, property).let {
            if (property == "id") it else it.and(Sort.by(Sort.Direction.DESC, "id"))
        }
        val ownerId = if (requester.isAdmin) userId else requester.id

        val spec = Specification<PurchaseOrder> { root, _, cb ->
            val predicates = mutableListOf<Predicate>()
            if (ownerId != null) {
                predicates += cb.equal(root.get<User>("user").get<Long>("id"), ownerId)
            }
            if (status != null) {
                predicates += cb.equal(root.get<OrderStatus>("status"), status)
            }
            if (from != null) {
                predicates += cb.greaterThanOrEqualTo(root.get<LocalDateTime>("createdAt"), from.atStartOfDay())
            }
            if (to != null) {
                predicates += cb.lessThan(root.get<LocalDateTime>("createdAt"), to.plusDays(1).atStartOfDay())
            }
            cb.and(*predicates.toTypedArray())
        }
        return repository.findAll(spec, PageRequest.of(page, size, sort))
    }

    fun findById(id: Long, requester: AuthenticatedUser): PurchaseOrder =
        findAuthorized(id, requester)

    fun create(requester: AuthenticatedUser): PurchaseOrder {
        val user = userRepository.findByIdOrNull(requester.id)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "User ${requester.id} not found!")
        return repository
            .save(PurchaseOrder(user = user))
            .also { log.info("Order {} created by user {}", it.id, user.id) }
    }

    fun addItem(orderId: Long, requester: AuthenticatedUser, productId: Long, quantity: Int): PurchaseOrder {
        val order = findAuthorized(orderId, requester)
        requireOpen(order)
        val product = productRepository.findByIdOrNull(productId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Product $productId not found!")

        val existing = order.items.find { it.product.id == product.id }
        val newQuantity = (existing?.quantity ?: 0) + quantity
        if (newQuantity > product.stock) {
            throw InsufficientStockException(product.name, product.stock)
        }

        if (existing != null) {
            existing.quantity = newQuantity
        } else {
            order.items.add(
                OrderItem(order = order, product = product, quantity = quantity, unitPrice = product.price)
            )
        }
        return repository
            .save(order)
            .also { log.info("Product {} (x{}) added to order {}", productId, quantity, orderId) }
    }

    fun removeItem(orderId: Long, itemId: Long, requester: AuthenticatedUser): PurchaseOrder {
        val order = findAuthorized(orderId, requester)
        requireOpen(order)
        if (!order.items.removeAll { it.id == itemId }) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Item $itemId not found in order $orderId")
        }
        return repository
            .save(order)
            .also { log.info("Item {} removed from order {}", itemId, orderId) }
    }

    fun close(orderId: Long, requester: AuthenticatedUser): PurchaseOrder {
        val order = findAuthorized(orderId, requester)
        requireOpen(order)
        if (order.items.isEmpty()) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Order $orderId has no items")
        }
        order.items.forEach { item ->
            val product = item.product
            if (item.quantity > product.stock) {
                throw InsufficientStockException(product.name, product.stock)
            }
            product.stock -= item.quantity
        }
        order.status = OrderStatus.CLOSED
        return repository
            .save(order)
            .also { log.info("Order {} closed", orderId) }
    }

    fun delete(orderId: Long, requester: AuthenticatedUser) {
        val order = findAuthorized(orderId, requester)
        requireOpen(order)
        repository.delete(order)
        log.warn("Order {} deleted by user {}", orderId, requester.id)
    }

    private fun findAuthorized(id: Long, requester: AuthenticatedUser): PurchaseOrder {
        val order = repository.findByIdOrNull(id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Order $id not found!")
        if (!requester.isAdmin && order.user.id != requester.id) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Order $id belongs to another user")
        }
        return order
    }

    private fun requireOpen(order: PurchaseOrder) {
        if (order.status != OrderStatus.OPEN) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Order ${order.id} is already closed")
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(OrderService::class.java)
        private val SORTABLE_FIELDS = mapOf(
            "id" to "id",
            "createdAt" to "createdAt",
            "status" to "status"
        )
    }
}
