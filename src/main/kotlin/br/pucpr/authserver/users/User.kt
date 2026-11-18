package br.pucpr.authserver.users

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.ManyToMany
import jakarta.persistence.Table
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

@Entity
@Table(name = "tblUser")
open class User (
    @Id @GeneratedValue
    var id: Long? = null,

    @Column(nullable = false)
    var name: String = "",

    @Email
    @Column(unique = true, nullable = false)
    var email: String,

    @Size(min=8, max=50)
    var password: String,

    @ManyToMany
    val roles: Set<Role>
)