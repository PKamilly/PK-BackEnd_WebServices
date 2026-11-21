package br.pucpr.authserver.products

import br.pucpr.authserver.brands.Brand
import br.pucpr.authserver.brands.BrandRepository
import br.pucpr.authserver.categories.Category
import br.pucpr.authserver.categories.CategoryRepository
import br.pucpr.authserver.orders.OrderItemRepository
import br.pucpr.authserver.users.SortDir
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
import java.math.BigDecimal
import java.math.RoundingMode

@Service
class ProductService(
    val repository: ProductRepository,
    val brandRepository: BrandRepository,
    val categoryRepository: CategoryRepository,
    val orderItemRepository: OrderItemRepository
) {
    fun search(
        name: String?,
        brandName: String?,
        brandId: Long?,
        categoryId: Long?,
        minPrice: BigDecimal?,
        maxPrice: BigDecimal?,
        inStock: Boolean?,
        sortBy: String?,
        sortDir: String?,
        page: Int,
        size: Int
    ): Page<Product> {
        val property = SORTABLE_FIELDS[sortBy ?: "name"]
            ?: throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid sortBy! Use one of: ${SORTABLE_FIELDS.keys.joinToString()}"
            )
        if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "minPrice cannot be greater than maxPrice")
        }
        val direction =
            if (SortDir.findOrThrow(sortDir ?: "ASC") == SortDir.ASC) Sort.Direction.ASC
            else Sort.Direction.DESC

        val sort = Sort.by(direction, property).let {
            if (property == "id") it else it.and(Sort.by(Sort.Direction.ASC, "id"))
        }

        val spec = Specification<Product> { root, _, cb ->
            val predicates = mutableListOf<Predicate>()
            if (!name.isNullOrBlank()) {
                predicates += cb.like(cb.lower(root.get<String>("name")), "%${name.trim().lowercase()}%")
            }
            if (!brandName.isNullOrBlank()) {
                predicates += cb.like(
                    cb.lower(root.get<Brand>("brand").get<String>("name")),
                    "%${brandName.trim().lowercase()}%"
                )
            }
            if (brandId != null) {
                predicates += cb.equal(root.get<Brand>("brand").get<Long>("id"), brandId)
            }
            if (categoryId != null) {
                predicates += cb.equal(root.join<Product, Category>("categories").get<Long>("id"), categoryId)
            }
            if (minPrice != null) {
                predicates += cb.greaterThanOrEqualTo(root.get<BigDecimal>("price"), minPrice)
            }
            if (maxPrice != null) {
                predicates += cb.lessThanOrEqualTo(root.get<BigDecimal>("price"), maxPrice)
            }
            if (inStock == true) {
                predicates += cb.greaterThan(root.get<Int>("stock"), 0)
            }
            cb.and(*predicates.toTypedArray())
        }
        return repository.findAll(spec, PageRequest.of(page, size, sort))
    }

    fun findByIdOrNull(id: Long): Product? = repository.findByIdOrNull(id)

    @Transactional
    fun insert(request: ProductRequest): Product {
        val product = Product(
            id = null,
            name = request.name!!.trim(),
            price = request.price!!.setScale(2, RoundingMode.HALF_UP),
            stock = request.stock!!,
            brand = findBrand(request.brandId!!),
            categories = findCategories(request.categoryIds)
        )
        return repository
            .save(product)
            .also { log.info("Product {} created!", it.id) }
    }

    @Transactional
    fun update(id: Long, request: ProductRequest): Product {
        val product = findOrThrow(id)
        product.name = request.name!!.trim()
        product.price = request.price!!.setScale(2, RoundingMode.HALF_UP)
        product.stock = request.stock!!
        product.brand = findBrand(request.brandId!!)
        request.categoryIds?.let {
            product.categories.clear()
            product.categories.addAll(findCategories(it))
        }
        return repository
            .save(product)
            .also { log.info("Product {} updated", id) }
    }

    @Transactional
    fun deleteById(id: Long) {
        val product = findOrThrow(id)
        if (orderItemRepository.existsByProductId(id)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Product $id is part of orders and cannot be deleted")
        }
        repository.delete(product)
        log.warn("Product {} deleted", id)
    }

    @Transactional
    fun addCategory(id: Long, categoryId: Long): Boolean {
        val product = findOrThrow(id)
        val category = categoryRepository.findByIdOrNull(categoryId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Category $categoryId not found!")
        if (product.categories.any { it.id == categoryId }) return false
        product.categories.add(category)
        repository.save(product)
        log.info("Category {} added to product {}", categoryId, id)
        return true
    }

    @Transactional
    fun removeCategory(id: Long, categoryId: Long): Boolean {
        val product = findOrThrow(id)
        val removed = product.categories.removeAll { it.id == categoryId }
        if (removed) {
            repository.save(product)
            log.info("Category {} removed from product {}", categoryId, id)
        }
        return removed
    }

    private fun findOrThrow(id: Long): Product =
        repository.findByIdOrNull(id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Product $id not found!")

    private fun findBrand(id: Long): Brand =
        brandRepository.findByIdOrNull(id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Brand $id not found!")

    private fun findCategories(ids: Set<Long>?): MutableSet<Category> =
        ids.orEmpty()
            .map {
                categoryRepository.findByIdOrNull(it)
                    ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Category $it not found!")
            }
            .toMutableSet()

    companion object {
        private val log = LoggerFactory.getLogger(ProductService::class.java)
        private val SORTABLE_FIELDS = mapOf(
            "id" to "id",
            "name" to "name",
            "price" to "price",
            "stock" to "stock",
            "brand" to "brand.name"
        )
    }
}
