package br.pucpr.authserver.categories

import br.pucpr.authserver.products.ProductRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.server.ResponseStatusException
import java.util.Optional

class CategoryServiceTest {
    private val repository = mockk<CategoryRepository>()
    private val productRepository = mockk<ProductRepository>()
    private val service = CategoryService(repository, productRepository)

    @BeforeEach
    fun setup() {
        clearAllMocks()
    }

    @Test
    fun `insert should save a new category`() {
        val category = Category(name = "Pranchas")
        val saved = Category(id = 1, name = "Pranchas")
        every { repository.findByName("Pranchas") } returns null
        every { repository.save(category) } returns saved

        service.insert(category) shouldBe saved
    }

    @Test
    fun `insert should fail with conflict when the name already exists`() {
        every { repository.findByName("Pranchas") } returns Category(id = 1, name = "Pranchas")

        val error = shouldThrow<ResponseStatusException> { service.insert(Category(name = "Pranchas")) }

        error.statusCode.value() shouldBe 409
    }

    @Test
    fun `deleteById should fail with conflict when the category has products`() {
        val category = Category(id = 1, name = "Pranchas")
        every { repository.findById(1L) } returns Optional.of(category)
        every { productRepository.existsByCategoriesId(1L) } returns true

        val error = shouldThrow<ResponseStatusException> { service.deleteById(1) }

        error.statusCode.value() shouldBe 409
        verify(exactly = 0) { repository.delete(any()) }
    }

    @Test
    fun `deleteById should fail with not found when the category does not exist`() {
        every { repository.findById(9L) } returns Optional.empty()

        val error = shouldThrow<ResponseStatusException> { service.deleteById(9) }

        error.statusCode.value() shouldBe 404
    }
}
