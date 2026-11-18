package br.pucpr.authserver.users

import br.pucpr.authserver.users.requests.UserRequest
import br.pucpr.authserver.users.responses.UserResponse
import jakarta.transaction.Transactional
import jakarta.validation.Valid
import jakarta.websocket.server.PathParam
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/users") // Indica a raiz deste controlador. Todas as requisições dentro deste controlador vão ser dentro do /users
class UsersController(val service: UsersService) {

    // URL para testar o CRUD de usuários: http://localhost:8080/api/swagger-ui.html
    // URL do console do Banco de Dados: http://localhost:8080/api/h2-console
    @GetMapping // Lista os usuários existentes
    fun listUsers() = service.findAll().map { it.toResponse() }

    @Transactional
    @PostMapping // Cria um usuário novo
    fun createUser(@RequestBody req: UserRequest): ResponseEntity<UserResponse>{
        val user = User(name = req.name!!, email=  req.email!!, password =  req.password!!)
        val saved = service.save(user).toResponse()
        return ResponseEntity.status(HttpStatus.CREATED).body(saved)
    }

    @GetMapping("/{id}") // Lista o usuário pelo seu ID
    fun getUser(@PathVariable("id") id: Long) =
        service.getById(id).orElse(null)
            ?.let { ResponseEntity.ok(it.toResponse()) }
            ?: ResponseEntity.notFound().build()

    private fun User.toResponse() = UserResponse(id!!, name, email)
}