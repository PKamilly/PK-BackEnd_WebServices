package br.pucpr.authserver.orders

import br.pucpr.authserver.brands.Brand
import br.pucpr.authserver.products.InsufficientStockException
import br.pucpr.authserver.products.Product
import br.pucpr.authserver.products.ProductRepository
import br.pucpr.authserver.security.AuthenticatedUser
import br.pucpr.authserver.users.UserRepository
import br.pucpr.authserver.users.oneUser
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.util.Optional

class OrderServiceTest {
    private val repository = mockk<OrderRepository>()
    private val productRepository = mockk<ProductRepository>()
    private val userRepository = mockk<UserRepository>()
    private val service = OrderService(repository, productRepository, userRepository)

    private val owner = oneUser(id = 1)
    private val ownerAuth = AuthenticatedUser(1, "John Doe", emptySet())
    private val otherAuth = AuthenticatedUser(2, "Jane Doe", emptySet())
    private val adminAuth = AuthenticatedUser(99, "Admin", setOf("ADMIN"))

    private fun product(stock: Int = 10) = Product(
        id = 5,
        name = "Leash",
        price = BigDecimal("89.90"),
        stock = stock,
        brand = Brand(id = 1, name = "FCS")
    )

    private fun order(status: OrderStatus = OrderStatus.OPEN) =
        PurchaseOrder(id = 10, user = owner, status = status)

    private fun withItem(order: PurchaseOrder, product: Product, quantity: Int): OrderItem {
        val item = OrderItem(
            id = 100,
            order = order,
            product = product,
            quantity = quantity,
            unitPrice = product.price
        )
        order.items.add(item)
        return item
    }

    @BeforeEach
    fun setup() {
        clearAllMocks()
    }

    @Test
    fun `create should open an order for the authenticated user`() {
        every { userRepository.findById(1L) } returns Optional.of(owner)
        every { repository.save(any<PurchaseOrder>()) } answers { firstArg<PurchaseOrder>() }

        val result = service.create(ownerAuth)

        result.user shouldBe owner
        result.status shouldBe OrderStatus.OPEN
    }

    @Test
    fun `create should fail with unauthorized when the user no longer exists`() {
        every { userRepository.findById(1L) } returns Optional.empty()

        val error = shouldThrow<ResponseStatusException> { service.create(ownerAuth) }

        error.statusCode.value() shouldBe 401
    }

    @Test
    fun `findById should fail with forbidden for another user's order`() {
        every { repository.findById(10L) } returns Optional.of(order())

        val error = shouldThrow<ResponseStatusException> { service.findById(10, otherAuth) }

        error.statusCode.value() shouldBe 403
    }

    @Test
    fun `findById should let an admin read any order`() {
        val existing = order()
        every { repository.findById(10L) } returns Optional.of(existing)

        service.findById(10, adminAuth) shouldBe existing
    }

    @Test
    fun `findById should fail with not found when the order does not exist`() {
        every { repository.findById(10L) } returns Optional.empty()

        val error = shouldThrow<ResponseStatusException> { service.findById(10, ownerAuth) }

        error.statusCode.value() shouldBe 404
    }

    @Test
    fun `addItem should add a new product with the current price`() {
        val existing = order()
        every { repository.findById(10L) } returns Optional.of(existing)
        every { productRepository.findById(5L) } returns Optional.of(product())
        every { repository.save(existing) } returns existing

        val result = service.addItem(10, ownerAuth, 5, 2)

        result.items.size shouldBe 1
        result.items[0].quantity shouldBe 2
        result.items[0].unitPrice shouldBe BigDecimal("89.90")
    }

    @Test
    fun `addItem should sum the quantity when the product is already in the order`() {
        val existing = order()
        val leash = product()
        withItem(existing, leash, 3)
        every { repository.findById(10L) } returns Optional.of(existing)
        every { productRepository.findById(5L) } returns Optional.of(leash)
        every { repository.save(existing) } returns existing

        val result = service.addItem(10, ownerAuth, 5, 2)

        result.items.size shouldBe 1
        result.items[0].quantity shouldBe 5
    }

    @Test
    fun `addItem should fail with insufficient stock`() {
        val existing = order()
        every { repository.findById(10L) } returns Optional.of(existing)
        every { productRepository.findById(5L) } returns Optional.of(product(stock = 1))

        shouldThrow<InsufficientStockException> { service.addItem(10, ownerAuth, 5, 2) }
    }

    @Test
    fun `addItem should fail with conflict when the order is closed`() {
        every { repository.findById(10L) } returns Optional.of(order(OrderStatus.CLOSED))

        val error = shouldThrow<ResponseStatusException> { service.addItem(10, ownerAuth, 5, 1) }

        error.statusCode.value() shouldBe 409
    }

    @Test
    fun `addItem should fail with not found when the product does not exist`() {
        every { repository.findById(10L) } returns Optional.of(order())
        every { productRepository.findById(5L) } returns Optional.empty()

        val error = shouldThrow<ResponseStatusException> { service.addItem(10, ownerAuth, 5, 1) }

        error.statusCode.value() shouldBe 404
    }

    @Test
    fun `addItem should fail with forbidden for another user's order`() {
        every { repository.findById(10L) } returns Optional.of(order())

        val error = shouldThrow<ResponseStatusException> { service.addItem(10, otherAuth, 5, 1) }

        error.statusCode.value() shouldBe 403
    }

    @Test
    fun `removeItem should remove the item from the order`() {
        val existing = order()
        withItem(existing, product(), 2)
        every { repository.findById(10L) } returns Optional.of(existing)
        every { repository.save(existing) } returns existing

        service.removeItem(10, 100, ownerAuth).items.size shouldBe 0
    }

    @Test
    fun `removeItem should fail with not found for an unknown item`() {
        every { repository.findById(10L) } returns Optional.of(order())

        val error = shouldThrow<ResponseStatusException> { service.removeItem(10, 100, ownerAuth) }

        error.statusCode.value() shouldBe 404
    }

    @Test
    fun `close should decrease the stock and close the order`() {
        val existing = order()
        val leash = product(stock = 10)
        withItem(existing, leash, 3)
        every { repository.findById(10L) } returns Optional.of(existing)
        every { repository.save(existing) } returns existing

        val result = service.close(10, ownerAuth)

        result.status shouldBe OrderStatus.CLOSED
        leash.stock shouldBe 7
    }

    @Test
    fun `close should fail with conflict when the order has no items`() {
        every { repository.findById(10L) } returns Optional.of(order())

        val error = shouldThrow<ResponseStatusException> { service.close(10, ownerAuth) }

        error.statusCode.value() shouldBe 409
    }

    @Test
    fun `close should fail with insufficient stock`() {
        val existing = order()
        withItem(existing, product(stock = 1), 3)
        every { repository.findById(10L) } returns Optional.of(existing)

        shouldThrow<InsufficientStockException> { service.close(10, ownerAuth) }
    }

    @Test
    fun `delete should remove an open order`() {
        val existing = order()
        every { repository.findById(10L) } returns Optional.of(existing)
        every { repository.delete(existing) } just Runs

        service.delete(10, ownerAuth)

        verify(exactly = 1) { repository.delete(existing) }
    }

    @Test
    fun `delete should fail with conflict when the order is closed`() {
        every { repository.findById(10L) } returns Optional.of(order(OrderStatus.CLOSED))

        val error = shouldThrow<ResponseStatusException> { service.delete(10, ownerAuth) }

        error.statusCode.value() shouldBe 409
        verify(exactly = 0) { repository.delete(any<PurchaseOrder>()) }
    }

    @Test
    fun `search should apply the requested field, direction and page`() {
        val pageable = slot<Pageable>()
        every { repository.findAll(any<Specification<PurchaseOrder>>(), capture(pageable)) } returns
                PageImpl(listOf(order()))

        val result = service.search(ownerAuth, OrderStatus.OPEN, null, null, null, "status", "asc", 1, 20)

        result.content.size shouldBe 1
        pageable.captured.pageNumber shouldBe 1
        pageable.captured.pageSize shouldBe 20
        pageable.captured.sort.getOrderFor("status")!!.direction shouldBe Sort.Direction.ASC
    }

    @Test
    fun `search should sort by creation date descending by default`() {
        val pageable = slot<Pageable>()
        every { repository.findAll(any<Specification<PurchaseOrder>>(), capture(pageable)) } returns
                PageImpl(emptyList())

        service.search(adminAuth, null, null, null, null, null, null, 0, 10)

        pageable.captured.sort.getOrderFor("createdAt")!!.direction shouldBe Sort.Direction.DESC
    }

    @Test
    fun `search should fail with bad request for an unknown sort field`() {
        val error = shouldThrow<ResponseStatusException> {
            service.search(ownerAuth, null, null, null, null, "total", null, 0, 10)
        }

        error.statusCode.value() shouldBe 400
    }
}
