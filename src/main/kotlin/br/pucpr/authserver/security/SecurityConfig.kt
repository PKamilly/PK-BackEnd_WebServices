package br.pucpr.authserver.security

import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
class SecurityConfig(private val jwt: JWT) {

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .headers { headers -> headers.frameOptions { it.sameOrigin() } }
            .exceptionHandling {
                it.authenticationEntryPoint(
                    AuthenticationEntryPoint { _, response, _ ->
                        writeError(response, HttpStatus.UNAUTHORIZED, "Authentication required")
                    }
                ).accessDeniedHandler(
                    AccessDeniedHandler { _, response, _ ->
                        writeError(response, HttpStatus.FORBIDDEN, "Access denied")
                    }
                )
            }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(
                        "/", "/health", "/error", "/swagger-ui/**", "/swagger-ui.html",
                        "/v3/api-docs/**", "/h2-console/**"
                    ).permitAll()
                    .requestMatchers(HttpMethod.POST, "/users", "/users/login").permitAll()
                    .requestMatchers(HttpMethod.GET, "/brands/**", "/categories/**", "/products/**").permitAll()
                    .requestMatchers("/orders/**").authenticated()
                    .requestMatchers(HttpMethod.DELETE, "/**").hasRole("ADMIN")
                    .requestMatchers(
                        HttpMethod.POST, "/brands/**", "/categories/**", "/products/**", "/roles/**"
                    ).hasRole("ADMIN")
                    .requestMatchers(
                        HttpMethod.PUT, "/brands/**", "/categories/**", "/products/**", "/users/*/roles/**"
                    ).hasRole("ADMIN")
                    .anyRequest().authenticated()
            }
            .addFilterBefore(JWTAuthFilter(jwt), UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    private fun writeError(response: HttpServletResponse, status: HttpStatus, detail: String) {
        response.status = status.value()
        response.contentType = "application/problem+json"
        response.characterEncoding = "UTF-8"
        response.writer.write(
            """{"type":"about:blank","title":"${status.reasonPhrase}","status":${status.value()},""" +
                    """"detail":"$detail","errors":{"detail":["$detail"]}}"""
        )
    }
}
