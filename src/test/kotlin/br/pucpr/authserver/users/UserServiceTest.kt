package br.pucpr.authserver.users

import br.pucpr.authserver.roles.Role
import br.pucpr.authserver.roles.RoleRepository
import br.pucpr.authserver.security.AuthenticatedUser
import br.pucpr.authserver.security.JWT
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.server.ResponseStatusException

class UserServiceTest {
    private val repository = mockk<UserRepository>()
    private val roleRepository = mockk<RoleRepository>()
    private val passwordEncoder = mockk<PasswordEncoder>()
    private val jwt = mockk<JWT>()

    private val service = UserService(repository, roleRepository, passwordEncoder, jwt)

    @BeforeEach
    fun setup() {
        clearAllMocks()
    }

    @AfterEach
    fun tearDown() {
        checkUnnecessaryStub(repository, roleRepository, passwordEncoder, jwt)
    }

    @Test
    fun `insert should encode the password, save and return the saved user`() {
        val user = oneUser(id = null)
        val saved = oneUser(id = 1)
        every { repository.findByEmail(user.email) } returns null
        every { passwordEncoder.encode(user.password) } returns "encoded"
        every { repository.save(user) } returns saved

        val result = service.insert(user)

        verify(exactly = 1) { repository.findByEmail(user.email) }
        user.password shouldBe "encoded"
        result shouldBe saved
    }

    @Test
    fun `insert should fail with conflict when the email already exists`() {
        val user = oneUser(id = null)
        every { repository.findByEmail(user.email) } returns oneUser(id = 2)

        val error = shouldThrow<ResponseStatusException> { service.insert(user) }

        error.statusCode.value() shouldBe 409
    }

    @Test
    fun `login should return the token and the user when the credentials are valid`() {
        val admin = Role(id = 1, name = "ADMIN", description = "Administrator")
        val user = oneUser(id = 7, password = "encoded", roles = mutableSetOf(admin))
        every { repository.findByEmail(user.email) } returns user
        every { passwordEncoder.matches("secret", "encoded") } returns true
        every { jwt.createToken(AuthenticatedUser(7, user.name, setOf("ADMIN"))) } returns "the-token"

        val result = service.login(user.email, "secret")

        result.token shouldBe "the-token"
        result.user.id shouldBe 7
        result.user.email shouldBe user.email
    }

    @Test
    fun `login should fail with unauthorized when the password is wrong`() {
        val user = oneUser(id = 7, password = "encoded")
        every { repository.findByEmail(user.email) } returns user
        every { passwordEncoder.matches("wrong", "encoded") } returns false

        val error = shouldThrow<ResponseStatusException> { service.login(user.email, "wrong") }

        error.statusCode.value() shouldBe 401
    }

    @Test
    fun `login should fail with unauthorized when the user does not exist`() {
        every { repository.findByEmail("nobody@email.com") } returns null

        val error = shouldThrow<ResponseStatusException> { service.login("nobody@email.com", "secret") }

        error.statusCode.value() shouldBe 401
    }
}
