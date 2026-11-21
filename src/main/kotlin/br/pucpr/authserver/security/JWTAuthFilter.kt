package br.pucpr.authserver.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

class JWTAuthFilter(private val jwt: JWT) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val header = request.getHeader(HttpHeaders.AUTHORIZATION)
        if (header != null && header.startsWith("Bearer ")) {
            jwt.extract(header.removePrefix("Bearer ").trim())?.let { user ->
                val authorities = user.roles.map { SimpleGrantedAuthority("ROLE_$it") }
                SecurityContextHolder.getContext().authentication =
                    UsernamePasswordAuthenticationToken(user, null, authorities)
            }
        }
        filterChain.doFilter(request, response)
    }
}
