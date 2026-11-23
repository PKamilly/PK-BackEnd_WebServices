# 🚀 PK-BackEnd Web Services

> Repositório desenvolvido para o trabalho prático de Web Services / Desenvolvimento Back-End.
> API de uma **loja de surf**, construída sobre o servidor `authserver` visto em aula.

## 👥 Integrantes
* Kamilly Mattana

---

## 📌 Sobre o Projeto
O `authserver` da disciplina (usuários, papéis e login) foi estendido com uma loja de surf: marcas, categorias, produtos e pedidos. O projeto contempla:

* **Modelagem de Dados:** entidades JPA com relacionamentos One-to-Many, Many-to-Many e Many-to-Many com tabela no meio.
* **CRUD Completo:** marcas, categorias e produtos; pedidos com criação, consulta, itens e remoção.
* **Associações:** endpoints para ligar produtos a categorias e produtos a pedidos.
* **Segurança e Autenticação:** login em `POST /users/login` que devolve um token JWT; ações destrutivas e administrativas exigem o papel `ADMIN`; senhas armazenadas com BCrypt.
* **Filtros, Ordenação e Paginação:** `GET /products` e `GET /orders` aceitam filtros, escolha de campo e direção da ordenação e paginação por parâmetros de query.
* **Boas Práticas:** tratamento global de exceções (`ProblemDetail`), logs (SLF4J) nos serviços, validação de dados e testes unitários.

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
3. **Product (Produto):** item da loja, com nome, preço e estoque.
4. **PurchaseOrder (Pedido):** compra feita por um usuário, com status `OPEN` ou `CLOSED`.
5. **OrderItem (Item do Pedido):** produto, quantidade e preço no momento da compra.

**Relacionamentos:**

| Relação | Entidades |
|---------|-----------|
| One-to-Many | `Brand` → `Product` e `User` → `PurchaseOrder` |
| Many-to-Many | `Product` ↔ `Category` (tabela `ProductCategory`) |
| Many-to-Many com tabela no meio | `PurchaseOrder` ↔ `Product` via `OrderItem` (guarda quantidade e preço) |

`PurchaseOrder` tem esse nome porque `ORDER` é palavra reservada do SQL.

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
| GET | `/products`, `/products/{id}` | Público | Consulta de produtos (com filtros) |
| POST / PUT / DELETE | `/products`, `/products/{id}` | ADMIN | Cria, altera e remove produto |
| PUT | `/products/{id}/categories/{categoryId}` | ADMIN | Associa categoria ao produto |
| DELETE | `/products/{id}/categories/{categoryId}` | ADMIN | Remove a associação |
| GET | `/orders` | Logado | Lista pedidos com filtros (cliente vê só os seus; ADMIN vê todos) |
| GET | `/orders/{id}` | Dono do pedido ou ADMIN | Consulta um pedido |
| POST | `/orders` | Logado | Cria um pedido aberto |
| POST | `/orders/{id}/items` | Dono do pedido ou ADMIN | Adiciona produto (`productId`, `quantity`) |
| DELETE | `/orders/{id}/items/{itemId}` | Dono do pedido ou ADMIN | Remove produto do pedido |
| POST | `/orders/{id}/close` | Dono do pedido ou ADMIN | Fecha o pedido e dá baixa no estoque |
| DELETE | `/orders/{id}` | Dono do pedido ou ADMIN | Remove um pedido aberto |

### 🔎 Filtro, ordenação e paginação de produtos

```
GET /api/products?name=prancha&brandName=lost&minPrice=100&maxPrice=3000&inStock=true&sortBy=price&sortDir=desc&page=0&size=10
```

| Parâmetro | Descrição |
|-----------|-----------|
| `name` | Trecho do nome do produto (sem diferenciar maiúsculas) |
| `brandName` | Trecho do nome da marca |
| `brandId` | Filtra pela marca |
| `categoryId` | Filtra pela categoria |
| `minPrice` / `maxPrice` | Faixa de preço |
| `inStock` | `true` para listar só produtos com estoque |
| `sortBy` | Campo da ordenação: `name` (padrão), `price`, `stock`, `brand` ou `id` |
| `sortDir` | `ASC` (padrão) ou `DESC` |
| `page` / `size` | Página (começa em 0, padrão 0) e tamanho (1 a 100, padrão 10) |

### 🔎 Filtro, ordenação e paginação de pedidos

```
GET /api/orders?status=OPEN&from=2026-11-01&to=2026-11-30&sortBy=createdAt&sortDir=desc&page=0&size=10
```

| Parâmetro | Descrição |
|-----------|-----------|
| `status` | `OPEN` ou `CLOSED` |
| `userId` | Filtra pelo cliente (só tem efeito para ADMIN) |
| `from` / `to` | Período de criação, no formato `AAAA-MM-DD` |
| `sortBy` | `createdAt` (padrão), `status` ou `id` |
| `sortDir` | `DESC` (padrão) ou `ASC` |
| `page` / `size` | Paginação, igual à de produtos |

Todos os parâmetros são opcionais; só os informados entram na consulta. As duas listagens devolvem `content`, `page`, `size`, `totalElements` e `totalPages`.

---

## ✅ Boas Práticas Adotadas
* **Segurança:** filtro JWT, rotas protegidas por papel, regra de dono nos pedidos e respostas 401/403 padronizadas.
* **Exceções:** `ResponseStatusException` (404, 409, 403...), `InsufficientStockException` (estoque insuficiente, 409) e `GlobalExceptionHandler` padronizando o formato dos erros, incluindo violações de integridade do banco (409).
* **Logs:** SLF4J em `UserService`, `BrandService`, `CategoryService`, `ProductService` e `OrderService` (criação, alteração, remoção e falhas de login). Os arquivos ficam em `./logs`.
* **Validação:** `@Valid` nos corpos das requisições e `@Min`/`@Max` nos parâmetros de rota e de paginação.
* **Testes unitários:** Kotest e MockK para `UserService` (inclui login), `JWT`, `BrandService`, `CategoryService`, `ProductService` e `OrderService`.

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

4. **Rode os testes:**
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

---

## 📋 Requisitos do Trabalho

| Requisito | Situação |
|-----------|----------|
| Pelo menos 4 classes novas com pelo menos 2 tipos de relação | 5 classes novas, com 3 tipos de relação |
| CRUD completo de pelo menos 2 classes, com autenticação nas ações destrutivas | 3 classes (marca, categoria e produto); `DELETE` exige ADMIN |
| Endpoints para associar as classes entre si | Produto ↔ categoria e pedido ↔ produto (itens) |
| Endpoint de consulta com filtro e ordenação por query params | `GET /products` e `GET /orders`, com campo, direção e paginação |
| Logs, exceções e segurança em pelo menos 2 serviços | Em 5 serviços |
| Testes unitários | 6 classes de teste |
