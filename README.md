# 🚀 PK-BackEnd Web Services

> Repositório desenvolvido para o trabalho prático de Back-End: Web Services.

## 👥 Integrantes
* Kamilly Mattana

---

## 🎥 Apresentação em Vídeo
O vídeo explicativo detalhando a arquitetura, as escolhas de implementação e a demonstração dos endpoints está disponível no YouTube:
👉 [Assistir ao vídeo no YouTube](LINK_DO_VIDEO_AQUI)

---

## 📌 Sobre o Projeto
Este projeto implementa uma API RESTful robusta, contemplando os requisitos de:
* **Modelagem de Dados:** Criação de entidades relacionais (One-to-Many, Many-to-Many, etc.).
* **CRUD Completo:** Operações de Criação, Leitura, Atualização e Exclusão para as principais classes do domínio.
* **Segurança e Autenticação:** Proteção de rotas sensíveis/destrutivas utilizando autenticação baseada em token (JWT).
* **Filtros e Ordenação:** Consultas avançadas utilizando parâmetros de query (`Query Parameters`) para paginação, filtragem e ordenação.
* **Boas Práticas:** Tratamento global de exceções, logs estruturados e validação de dados.

---

## 🛠️ Tecnologias Utilizadas
* **Linguagem:** Java 17+ / [Outra tecnologia se aplicável]
* **Framework:** Spring Boot / [Outro framework se aplicável]
* **Banco de Dados:** H2 / PostgreSQL / MySQL
* **Segurança:** Spring Security & JWT (JSON Web Token)
* **Ferramentas:** Maven / Gradle, Git & GitHub

---

## 📂 Modelagem do Domínio (Classes e Relacionamentos)
O sistema é composto pelas seguintes entidades principais:
1. **[Classe 1, ex: Usuario]**: Gerenciamento de usuários do sistema.
2. **[Classe 2, ex: Categoria]**: Classificação dos itens.
3. **[Classe 3, ex: Produto]**: Item principal gerenciado pela API (possui relação `Many-to-One` com Categoria).
4. **[Classe 4, ex: Pedido]**: Relacionamento `Many-to-Many` com Produtos.

---

## ⚙️ Como Executar o Projeto

### Pré-requisitos
Certifique-se de ter instalado em sua máquina:
* JDK 17 ou superior
* Maven ou Gradle

### Passo a Passo

1. **Clone o repositório:**
   ```bash
   git clone https://github.com/PKamilly/PK-BackEnd_WebServices.git
   cd PK-BackEnd_WebServices
