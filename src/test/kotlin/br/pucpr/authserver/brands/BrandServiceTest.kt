package br.pucpr.authserver.brands

import br.pucpr.authserver.products.ProductRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.server.ResponseStatusException
import java.util.Optional

class BrandServiceTest {
    private val repository = mockk<BrandRepository>()
    private val productRepository = mockk<ProductRepository>()
    private val service = BrandService(repository, productRepository)

    @BeforeEach
    fun setup() {
        clearAllMocks()
    }

    @Test
    fun `insert should save a new brand`() {
        val brand = Brand(name = "Rip Curl")
        val saved = Brand(id = 1, name = "Rip Curl")
        every { repository.findByName("Rip Curl") } returns null
        every { repository.save(brand) } returns saved

        service.insert(brand) shouldBe saved
    }

    @Test
    fun `insert should fail with conflict when the name already exists`() {
        every { repository.findByName("Rip Curl") } returns Brand(id = 1, name = "Rip Curl")

        val error = shouldThrow<ResponseStatusException> { service.insert(Brand(name = "Rip Curl")) }

        error.statusCode.value() shouldBe 409
    }

    @Test
    fun `update should change the name`() {
        val brand = Brand(id = 1, name = "Old")
        every { repository.findById(1L) } returns Optional.of(brand)
        every { repository.findByName("New") } returns null
        every { repository.save(brand) } returns brand

        service.update(1, "New").name shouldBe "New"
    }

    @Test
    fun `update should fail with not found when the brand does not exist`() {
        every { repository.findById(1L) } returns Optional.empty()

        val error = shouldThrow<ResponseStatusException> { service.update(1, "New") }

        error.statusCode.value() shouldBe 404
    }

    @Test
    fun `update should fail with conflict when another brand has the name`() {
        val brand = Brand(id = 1, name = "Old")
        every { repository.findById(1L) } returns Optional.of(brand)
        every { repository.findByName("Taken") } returns Brand(id = 2, name = "Taken")

        val error = shouldThrow<ResponseStatusException> { service.update(1, "Taken") }

        error.statusCode.value() shouldBe 409
    }

    @Test
    fun `deleteById should remove a brand without products`() {
        val brand = Brand(id = 1, name = "Rip Curl")
        every { repository.findById(1L) } returns Optional.of(brand)
        every { productRepository.existsByBrandId(1L) } returns false
        every { repository.delete(brand) } just Runs

        service.deleteById(1)

        verify(exactly = 1) { repository.delete(brand) }
    }

    @Test
    fun `deleteById should fail with conflict when the brand has products`() {
        val brand = Brand(id = 1, name = "Rip Curl")
        every { repository.findById(1L) } returns Optional.of(brand)
        every { productRepository.existsByBrandId(1L) } returns true

        val error = shouldThrow<ResponseStatusException> { service.deleteById(1) }

        error.statusCode.value() shouldBe 409
        verify(exactly = 0) { repository.delete(any()) }
    }
}
