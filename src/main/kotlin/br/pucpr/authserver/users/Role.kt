package br.pucpr.authserver.users

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.ManyToMany

@Entity
open class Role (
    @Id @GeneratedValue
    val id: Long?,

    @Column(unique = true, nullable = false)
    val name: String,

    @ManyToMany
    val users: Set<User>
)