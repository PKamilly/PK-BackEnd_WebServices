package br.pucpr.authserver

import br.pucpr.authserver.brands.Brand
import br.pucpr.authserver.brands.BrandRepository
import br.pucpr.authserver.categories.Category
import br.pucpr.authserver.categories.CategoryRepository
import br.pucpr.authserver.products.Product
import br.pucpr.authserver.products.ProductRepository
import br.pucpr.authserver.roles.Role
import br.pucpr.authserver.roles.RoleRepository
import br.pucpr.authserver.users.User
import br.pucpr.authserver.users.UserRepository
import jakarta.transaction.Transactional
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class BootStrapper(
    val roleRepository: RoleRepository,
    val userRepository: UserRepository,
    val brandRepository: BrandRepository,
    val categoryRepository: CategoryRepository,
    val productRepository: ProductRepository,
    val passwordEncoder: PasswordEncoder
) : ApplicationListener<ContextRefreshedEvent> {

    @Transactional
    override fun onApplicationEvent(event: ContextRefreshedEvent) {
        val adminRole = roleRepository.findByName("ADMIN") ?: roleRepository
            .save(Role(name = "ADMIN", description = "System Administrator"))
        roleRepository.findByName("PREMIUM") ?: roleRepository.save(
            Role(
                name = "PREMIUM",
                description = "Premium User"
            )
        )
        if (userRepository.findByRole("ADMIN").isEmpty()) {
            val admin = User(
                name = "Administrator",
                password = passwordEncoder.encode("admin")!!,
                email = "admin@authserver.com"
            )
            admin.roles.add(adminRole)
            userRepository.save(admin)
        }
        if (userRepository.findByEmail("cliente@surfshop.com") == null) {
            userRepository.save(
                User(
                    name = "Cliente Surf",
                    password = passwordEncoder.encode("cliente123")!!,
                    email = "cliente@surfshop.com"
                )
            )
        }
        seedCatalog()
    }

    private fun seedCatalog() {
        if (brandRepository.count() > 0) return

        val ripCurl = brandRepository.save(Brand(name = "Rip Curl"))
        val quiksilver = brandRepository.save(Brand(name = "Quiksilver"))
        val billabong = brandRepository.save(Brand(name = "Billabong"))
        val fcs = brandRepository.save(Brand(name = "FCS"))
        val lost = brandRepository.save(Brand(name = "Lost"))
        val sexWax = brandRepository.save(Brand(name = "Sex Wax"))

        val boards = categoryRepository.save(Category(name = "Pranchas"))
        val accessories = categoryRepository.save(Category(name = "Acessórios"))
        val clothing = categoryRepository.save(Category(name = "Roupas"))
        val care = categoryRepository.save(Category(name = "Cuidados"))

        fun product(name: String, price: String, stock: Int, brand: Brand, vararg categories: Category) =
            productRepository.save(
                Product(
                    name = name,
                    price = BigDecimal(price),
                    stock = stock,
                    brand = brand,
                    categories = categories.toMutableSet()
                )
            )

        product("Shortboard Driver 6'0", "2890.00", 5, lost, boards)
        product("Longboard Classic 9'0", "3450.00", 3, billabong, boards)
        product("Leash Essential 6'", "89.90", 40, fcs, accessories)
        product("Quilhas FCS II Performer", "540.00", 15, fcs, accessories, boards)
        product("Wetsuit Flash Bomb 3/2", "1299.90", 12, ripCurl, clothing)
        product("Boardshort Highline 19", "249.90", 30, quiksilver, clothing)
        product("Parafina Cold Water", "19.90", 100, sexWax, care, accessories)
    }
}
