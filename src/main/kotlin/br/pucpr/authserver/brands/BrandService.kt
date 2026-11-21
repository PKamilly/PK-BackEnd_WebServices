package br.pucpr.authserver.brands

import br.pucpr.authserver.products.ProductRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class BrandService(
    val repository: BrandRepository,
    val productRepository: ProductRepository
) {
    fun findAll(): List<Brand> = repository.findAll(Sort.by("name"))

    fun findByIdOrNull(id: Long): Brand? = repository.findByIdOrNull(id)

    fun insert(brand: Brand): Brand {
        if (repository.findByName(brand.name) != null) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Brand ${brand.name} already exists")
        }
        return repository
            .save(brand)
            .also { log.info("Brand {} created!", it.id) }
    }

    fun update(id: Long, name: String): Brand {
        val brand = repository.findByIdOrNull(id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Brand $id not found!")
        val sameName = repository.findByName(name)
        if (sameName != null && sameName.id != id) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Brand $name already exists")
        }
        brand.name = name
        return repository
            .save(brand)
            .also { log.info("Brand {} updated", id) }
    }

    fun deleteById(id: Long) {
        val brand = repository.findByIdOrNull(id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Brand $id not found!")
        if (productRepository.existsByBrandId(id)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Brand $id has products and cannot be deleted")
        }
        repository.delete(brand)
        log.warn("Brand {} deleted", id)
    }

    companion object {
        private val log = LoggerFactory.getLogger(BrandService::class.java)
    }
}
