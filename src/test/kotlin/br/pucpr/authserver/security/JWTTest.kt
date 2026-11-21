package br.pucpr.authserver.security

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class JWTTest {
    private val secret = "unit-test-secret-key-with-more-than-32-chars"
    private val jwt = JWT(secret, 60)
    private val user = AuthenticatedUser(5, "John Doe", setOf("ADMIN", "PREMIUM"))

    @Test
    fun `a created token should be read back with the same user`() {
        val token = jwt.createToken(user)

        jwt.extract(token) shouldBe user
    }

    @Test
    fun `a malformed token should be rejected`() {
        jwt.extract("not-a-jwt") shouldBe null
    }

    @Test
    fun `an expired token should be rejected`() {
        val expired = JWT(secret, -1).createToken(user)

        jwt.extract(expired) shouldBe null
    }

    @Test
    fun `a token signed with another secret should be rejected`() {
        val foreign = JWT("another-unit-test-secret-key-with-more-than-32", 60).createToken(user)

        jwt.extract(foreign) shouldBe null
    }
}
