package br.pucpr.authserver.products

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository

@Repository
interface ProductRepository : JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    fun existsByBrandId(brandId: Long): Boolean
    fun existsByCategoriesId(categoryId: Long): Boolean
}
