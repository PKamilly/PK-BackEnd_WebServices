package br.pucpr.authserver.users

import org.springframework.stereotype.Service

@Service
class UsersService(val repository: UsersRepository) {

    fun save(user: User) = repository.save(user)

    // Procupa o id pelo número, caso não ache, retorna NULLO
    fun getById(id: Long) = repository.findById(id)

    // Retorna imutável, não é possível alterar a lista
    fun findAll(): List<User> = repository.findAll()
}