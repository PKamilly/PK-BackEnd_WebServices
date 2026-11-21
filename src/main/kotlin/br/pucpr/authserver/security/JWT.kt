package br.pucpr.authserver.security

import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date
import javax.crypto.SecretKey

@Component
class JWT(
    @Value("\${security.jwt.secret}") secret: String,
    @Value("\${security.jwt.expiration-minutes}") private val expirationMinutes: Long
) {
    private val key: SecretKey = Keys.hmacShaKeyFor(secret.toByteArray())

    fun createToken(user: AuthenticatedUser): String {
        val now = Instant.now()
        return Jwts.builder()
            .subject(user.id.toString())
            .claim("name", user.name)
            .claim("roles", user.roles.toList())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(expirationMinutes, ChronoUnit.MINUTES)))
            .signWith(key)
            .compact()
    }

    fun extract(token: String): AuthenticatedUser? =
        try {
            val claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .payload
            val roles = (claims["roles"] as? List<*>)
                ?.map { it.toString() }
                ?.toSet()
                ?: emptySet()
            AuthenticatedUser(
                id = claims.subject.toLong(),
                name = claims["name"]?.toString() ?: "",
                roles = roles
            )
        } catch (e: JwtException) {
            log.debug("Invalid JWT: {}", e.message)
            null
        } catch (e: IllegalArgumentException) {
            log.debug("Malformed JWT: {}", e.message)
            null
        }

    companion object {
        private val log = LoggerFactory.getLogger(JWT::class.java)
    }
}
