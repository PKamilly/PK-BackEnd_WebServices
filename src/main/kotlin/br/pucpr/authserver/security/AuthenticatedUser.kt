package br.pucpr.authserver.security

data class AuthenticatedUser(
    val id: Long,
    val name: String,
    val roles: Set<String>
) {
    val isAdmin: Boolean get() = "ADMIN" in roles
}
