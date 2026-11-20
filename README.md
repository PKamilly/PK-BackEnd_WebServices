# 🚀 PK-BackEnd Web Services

> Repositório desenvolvido para o trabalho prático de Web Services / Desenvolvimento Back-End (**Opção 1 – Estendendo os serviços**).
> API de uma **loja de surf**, construída sobre o servidor `authserver` visto em aula.

## 👥 Integrantes
* Kamilly Mattana

---

## 📌 Sobre o Projeto
O `authserver` da disciplina (usuários, papéis e login) foi estendido com o catálogo de uma loja de surf: marcas, categorias e produtos. O projeto contempla:

* **Modelagem de Dados:** entidades JPA com relacionamentos One-to-Many e Many-to-Many.
* **CRUD Completo:** criação, leitura, atualização e exclusão de marcas, categorias e produtos.
* **Segurança e Autenticação:** login em `POST /users/login` que devolve um token JWT; ações destrutivas e administrativas exigem o papel `ADMIN`; senhas armazenadas com BCrypt.
* **Filtros e Ordenação:** `GET /products` aceita filtros e ordenação por parâmetros de query.
* **Boas Práticas:** tratamento global de exceções (`ProblemDetail`), logs (SLF4J) nos serviços e validação de dados (Bean Validation).

---

## 🛠️ Tecnologias Utilizadas
* **Linguagem:** Kotlin 2.2 (JVM 21)
* **Framework:** Spring Boot 4.0 (Web, Data JPA, Validation, Security)
* **Banco de Dados:** H2 (em memória)
* **Segurança:** Spring Security & JWT (jjwt 0.13)
* **Documentação:** springdoc-openapi (Swagger UI)
* **Testes:** Kotest e MockK
* **Ferramentas:** Gradle, Git & GitHub

---

## 📂 Modelagem do Domínio (Classes e Relacionamentos)

**Classes do servidor da disciplina:** `User` e `Role` (relação `Many-to-Many` entre elas).

**Classes novas do trabalho:**
1. **Brand (Marca):** fabricante do produto (Rip Curl, FCS, Lost...).
2. **Category (Categoria):** classificação dos produtos (Pranchas, Acessórios, Roupas...).
3. **Product (Produto):** item principal da loja, com nome, preço e estoque.

**Relacionamentos:**
* `Brand` → `Product`: **One-to-Many** (uma marca tem vários produtos).
* `Product` ↔ `Category`: **Many-to-Many** (tabela `ProductCategory`).

---

## 🔗 Endpoints

| Método | Rota | Acesso | Descrição |
|--------|------|--------|-----------|
| POST | `/users` | Público | Cadastro de usuário |
| POST | `/users/login` | Público | Login, devolve o token JWT |
| GET | `/users`, `/users/{id}` | Logado | Consulta de usuários |
| PATCH | `/users/{id}` | Próprio usuário ou ADMIN | Altera o nome |
| PUT | `/users/{id}/roles/{name}` | ADMIN | Concede um papel ao usuário |
| DELETE | `/users/{id}` | ADMIN | Remove usuário |
| GET | `/brands`, `/brands/{id}` | Público | Consulta de marcas |
| POST / PUT / DELETE | `/brands`, `/brands/{id}` | ADMIN | Cria, altera e remove marca |
| GET | `/categories`, `/categories/{id}` | Público | Consulta de categorias |
| POST / PUT / DELETE | `/categories`, `/categories/{id}` | ADMIN | Cria, altera e remove categoria |
| GET | `/products`, `/products/{id}` | Público | Consulta de produtos |
| POST / PUT / DELETE | `/products`, `/products/{id}` | ADMIN | Cria, altera e remove produto |

### 🔎 Consulta com filtro e ordenação

```
GET /api/products?name=prancha&brandId=2&categoryId=1&minPrice=100&maxPrice=3000&inStock=true&sortBy=price&sortDir=desc
```

| Parâmetro | Descrição |
|-----------|-----------|
| `name` | Trecho do nome (sem diferenciar maiúsculas) |
| `brandId` | Filtra pela marca |
| `categoryId` | Filtra pela categoria |
| `minPrice` / `maxPrice` | Faixa de preço |
| `inStock` | `true` para listar só produtos com estoque |
| `sortBy` | `name` (padrão), `price` ou `stock` |
| `sortDir` | `ASC` (padrão) ou `DESC` |

Todos são opcionais; só os informados entram na consulta.

---

## ✅ Boas Práticas Adotadas
* **Segurança:** filtro JWT, rotas protegidas por papel e respostas 401/403 padronizadas.
* **Exceções:** `ResponseStatusException` (404, 409, 403...) e `GlobalExceptionHandler` padronizando o formato dos erros, incluindo violações de integridade do banco (409).
* **Logs:** SLF4J em `UserService`, `BrandService`, `CategoryService` e `ProductService` (criação, alteração, remoção e falhas de login). Os arquivos ficam em `./logs`.
* **Validação:** `@Valid` nos corpos das requisições e `@Min` nos parâmetros de rota.

---

## ⚙️ Como Executar o Projeto

### Pré-requisitos
* JDK 21
* Gradle (o projeto já inclui o wrapper `./gradlew`)

### Passo a Passo

1. **Clone o repositório:**
   ```bash
   git clone https://github.com/PKamilly/PK-BackEnd_WebServices.git
   cd PK-BackEnd_WebServices
   ```

2. **Execute a aplicação:**
   ```bash
   ./gradlew bootRun
   ```

3. **Acesse:**
   * Swagger UI (com botão **Authorize** para informar o token): http://localhost:8080/api/
   * Console do H2: http://localhost:8080/api/h2-console (`jdbc:h2:mem:db`, usuário `sa`, senha `sa`)
   * Chamadas prontas para testar: arquivo `requests.http`

4. **Rode os testes (opcional):**
   ```bash
   ./gradlew test
   ```

### 🔑 Usuários criados na inicialização

| Papel | E-mail | Senha |
|-------|--------|-------|
| ADMIN | admin@authserver.com | admin |
| Cliente | cliente@surfshop.com | cliente123 |

O catálogo também é populado com marcas, categorias e produtos de exemplo.

Para usar uma chave própria no JWT, defina a variável de ambiente `JWT_SECRET` (mínimo de 32 caracteres).

### Como autenticar
1. Faça `POST /api/users/login` com `{ "email": "...", "password": "..." }`.
2. Envie o token recebido no header `Authorization: Bearer <token>` nas rotas protegidas.
