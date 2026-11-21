package br.pucpr.authserver.products

import br.pucpr.authserver.brands.Brand
import br.pucpr.authserver.brands.BrandRepository
import br.pucpr.authserver.categories.Category
import br.pucpr.authserver.categories.CategoryRepository
import br.pucpr.authserver.orders.OrderItemRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
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

class ProductServiceTest {
    private val repository = mockk<ProductRepository>()
    private val brandRepository = mockk<BrandRepository>()
    private val categoryRepository = mockk<CategoryRepository>()
    private val orderItemRepository = mockk<OrderItemRepository>()
    private val service = ProductService(repository, brandRepository, categoryRepository, orderItemRepository)

    private val brand = Brand(id = 1, name = "Lost")
    private val category = Category(id = 2, name = "Pranchas")

    private fun product(id: Long? = 10, categories: MutableSet<Category> = mutableSetOf()) = Product(
        id = id,
        name = "Shortboard",
        price = BigDecimal("100.00"),
        stock = 5,
        brand = brand,
        categories = categories
    )

    private fun request(categoryIds: Set<Long>? = null) = ProductRequest(
        name = " Shortboard ",
        price = BigDecimal("100"),
        stock = 5,
        brandId = 1,
        categoryIds = categoryIds
    )

    @BeforeEach
    fun setup() {
        clearAllMocks()
    }

    @Test
    fun `search should apply the requested page, size, field and direction`() {
        val pageable = slot<Pageable>()
        val found = product()
        every { repository.findAll(any<Specification<Product>>(), capture(pageable)) } returns PageImpl(listOf(found))

        val result = service.search(null, null, null, null, null, null, null, "price", "desc", 2, 5)

        result.content shouldBe listOf(found)
        pageable.captured.pageNumber shouldBe 2
        pageable.captured.pageSize shouldBe 5
        pageable.captured.sort.getOrderFor("price")!!.direction shouldBe Sort.Direction.DESC
    }

    @Test
    fun `search should sort by name ascending by default`() {
        val pageable = slot<Pageable>()
        every { repository.findAll(any<Specification<Product>>(), capture(pageable)) } returns PageImpl(emptyList())

        service.search(null, null, null, null, null, null, null, null, null, 0, 10)

        pageable.captured.sort.getOrderFor("name")!!.direction shouldBe Sort.Direction.ASC
    }

    @Test
    fun `search should sort by the brand name when sortBy is brand`() {
        val pageable = slot<Pageable>()
        every { repository.findAll(any<Specification<Product>>(), capture(pageable)) } returns PageImpl(emptyList())

        service.search(null, null, null, null, null, null, null, "brand", "asc", 0, 10)

        pageable.captured.sort.getOrderFor("brand.name")!!.direction shouldBe Sort.Direction.ASC
    }

    @Test
    fun `search should fail with bad request for an unknown sort field`() {
        val error = shouldThrow<ResponseStatusException> {
            service.search(null, null, null, null, null, null, null, "color", null, 0, 10)
        }

        error.statusCode.value() shouldBe 400
    }

    @Test
    fun `search should fail with bad request when minPrice is greater than maxPrice`() {
        val error = shouldThrow<ResponseStatusException> {
            service.search(
                null, null, null, null, BigDecimal("500"), BigDecimal("100"), null, null, null, 0, 10
            )
        }

        error.statusCode.value() shouldBe 400
    }

    @Test
    fun `insert should create the product with the brand, categories and a two digit price`() {
        every { brandRepository.findById(1L) } returns Optional.of(brand)
        every { categoryRepository.findById(2L) } returns Optional.of(category)
        every { repository.save(any<Product>()) } answers { firstArg<Product>() }

        val result = service.insert(request(setOf(2)))

        result.name shouldBe "Shortboard"
        result.price shouldBe BigDecimal("100.00")
        result.brand shouldBe brand
        result.categories shouldBe mutableSetOf(category)
    }

    @Test
    fun `insert should fail with not found when the brand does not exist`() {
        every { brandRepository.findById(1L) } returns Optional.empty()

        val error = shouldThrow<ResponseStatusException> { service.insert(request()) }

        error.statusCode.value() shouldBe 404
    }

    @Test
    fun `insert should fail with not found when a category does not exist`() {
        every { brandRepository.findById(1L) } returns Optional.of(brand)
        every { categoryRepository.findById(2L) } returns Optional.empty()

        val error = shouldThrow<ResponseStatusException> { service.insert(request(setOf(2))) }

        error.statusCode.value() shouldBe 404
    }

    @Test
    fun `update should keep the categories when categoryIds is omitted`() {
        val existing = product(categories = mutableSetOf(category))
        every { repository.findById(10L) } returns Optional.of(existing)
        every { brandRepository.findById(1L) } returns Optional.of(brand)
        every { repository.save(existing) } returns existing

        val result = service.update(10, request(null))

        result.categories shouldBe mutableSetOf(category)
    }

    @Test
    fun `deleteById should fail with conflict when the product is part of an order`() {
        every { repository.findById(10L) } returns Optional.of(product())
        every { orderItemRepository.existsByProductId(10L) } returns true

        val error = shouldThrow<ResponseStatusException> { service.deleteById(10) }

        error.statusCode.value() shouldBe 409
        verify(exactly = 0) { repository.delete(any()) }
    }

    @Test
    fun `addCategory should associate the category with the product`() {
        val existing = product()
        every { repository.findById(10L) } returns Optional.of(existing)
        every { categoryRepository.findById(2L) } returns Optional.of(category)
        every { repository.save(existing) } returns existing

        service.addCategory(10, 2) shouldBe true

        existing.categories shouldBe mutableSetOf(category)
    }

    @Test
    fun `addCategory should return false when they are already associated`() {
        val existing = product(categories = mutableSetOf(category))
        every { repository.findById(10L) } returns Optional.of(existing)
        every { categoryRepository.findById(2L) } returns Optional.of(category)

        service.addCategory(10, 2) shouldBe false
    }

    @Test
    fun `addCategory should fail with not found when the category does not exist`() {
        every { repository.findById(10L) } returns Optional.of(product())
        every { categoryRepository.findById(2L) } returns Optional.empty()

        val error = shouldThrow<ResponseStatusException> { service.addCategory(10, 2) }

        error.statusCode.value() shouldBe 404
    }

    @Test
    fun `removeCategory should remove the association`() {
        val existing = product(categories = mutableSetOf(category))
        every { repository.findById(10L) } returns Optional.of(existing)
        every { repository.save(existing) } returns existing

        service.removeCategory(10, 2) shouldBe true

        existing.categories.size shouldBe 0
    }

    @Test
    fun `removeCategory should return false when they are not associated`() {
        every { repository.findById(10L) } returns Optional.of(product())

        service.removeCategory(10, 2) shouldBe false
    }
}
