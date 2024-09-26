# 💸 Projeto Minhas Finanças - Backend (minhas-financas-api)

## 📝 Descrição

Minhas Finanças API é uma aplicação desenvolvida com Spring Boot projetada para simplificar o gerenciamento de finanças pessoais. Esta API permite aos usuários acompanhar suas finanças de maneira eficiente, oferecendo funcionalidades para o registro e a análise de ganhos e gastos.

🌐 [Projeto FrontEnd](https://dev.azure.com/muralisti/Programa%20de%20Est%C3%A1gio%20da%20Muralis/_git/pem-paulo-henrique-front?path=%2F&version=GBrelease&_a=contents)

## 🧪 Cobertura de Testes

- 53 testes funcionais 
- 52,75% de cobertura de testes no sistema

## 🚀 Tecnologias Utilizadas

- Java 1.8
- Spring Boot 2.1.8.RELEASE
- Spring Data JPA
- Spring Web
- Spring Security
- Spring Boot DevTools
- Spring Boot Test
- PostgreSQL
- H2 Database (para testes)
- Lombok 1.18.34
- JSON Web Token (JWT) 0.9.1
- OpenCSV 5.5.2

## 📄 Pré-requisitos

- JDK 8

## 🔧 Instalação

1. **Clone o repositório:**

   ```bash
   git clone https://muralisti@dev.azure.com/muralisti/Programa%20de%20Est%C3%A1gio%20da%20Muralis/_git/pem-paulo-henrique-back
   ```

2. **Navegue até o diretório do projeto:**

   ```bash
   cd pem-paulo-henrique-back
   ```

3. **Configure o banco de dados:**

   Edite o arquivo `src/main/resources/application.properties` com as configurações do seu banco de dados.

4. **Compile e execute o projeto:**

   ```bash
   mvn clean install
   mvn spring-boot:run
   ```


## 🚩 Endpoints

Abaixo está a descrição dos principais endpoints da API:

## Usuários

### **1. GET /api/usuarios**

- **Descrição:** Retorna uma lista de usuários.
- **Parâmetros de Consulta:**
  - `page` (opcional): Número da página.
  - `size` (opcional): Número de itens por página.
- **Resposta:**
  - **200 OK**
    ```json
    [
      {
        "id": 1,
        "nome": "João",
        "email": "joao@exemplo.com"
      },
      // ...
    ]
    ```

### **2. POST /api/usuarios**

- **Descrição:** Cria um novo usuário.
- **Corpo da Requisição:**
  ```json
  {
    "nome": "Maria",
    "email": "maria@exemplo.com"
  }
  ```
- **Resposta:**
  - **201 Created**
    ```json
    {
      "id": 2,
      "nome": "Maria",
      "email": "maria@exemplo.com"
    }
    ```

### **3. GET /api/usuarios/{id}**

- **Descrição:** Retorna um usuário específico pelo ID.
- **Parâmetros de Caminho:**
  - `id`: ID do usuário.
- **Resposta:**
  - **200 OK**
    ```json
    {
      "id": 1,
      "nome": "João",
      "email": "joao@exemplo.com"
    }
    ```
  - **404 Not Found** (se o usuário não for encontrado)

### **4. PUT /api/usuarios/{id}**

- **Descrição:** Atualiza um usuário existente.
- **Corpo da Requisição:**
  ```json
  {
    "nome": "João Atualizado",
    "email": "joaoatualizado@exemplo.com"
  }
  ```
- **Parâmetros de Caminho:**
  - `id`: ID do usuário.
- **Resposta:**
  - **200 OK**
    ```json
    {
      "id": 1,
      "nome": "João Atualizado",
      "email": "joaoatualizado@exemplo.com"
    }
    ```
  - **404 Not Found** (se o usuário não for encontrado)

### **5. DELETE /api/usuarios/{id}**

- **Descrição:** Remove um usuário pelo ID.
- **Parâmetros de Caminho:**
  - `id`: ID do usuário.
- **Resposta:**
  - **204 No Content**
  - **404 Not Found** (se o usuário não for encontrado)
  
---

## Lançamentos

### **1. GET /api/lancamentos**

- **Descrição:** Busca uma lista de lançamentos filtrados por critérios específicos.
- **Parâmetros de Consulta:**
  - `descricao` (opcional): Descrição do lançamento.
  - `mes` (opcional): Mês do lançamento.
  - `ano` (opcional): Ano do lançamento.
  - `categoriaId` (opcional): ID da categoria do lançamento.
  - `tipo` (opcional): Tipo do lançamento ("RECEITA" ou "DESPESA").
  - `usuario` (obrigatório): ID do usuário.
- **Resposta:**
  - **200 OK**
    ```json
    [
      {
        "id": 1,
        "descricao": "Venda",
        "mes": 9,
        "ano": 2024,
        "valor": 1500.00,
        "tipo": "RECEITA",
        "status": "EFETIVADO",
        "categoria": {
          "id": 1,
          "nome": "Vendas"
        }
      },
      // ...
    ]
    ```

### **2. GET /api/lancamentos/{id}**

- **Descrição:** Retorna os detalhes de um lançamento específico pelo ID.
- **Parâmetros de URL:**
  - `id`: ID do lançamento.
- **Resposta:**
  - **200 OK**
    ```json
    {
      "id": 1,
      "descricao": "Venda",
      "mes": 9,
      "ano": 2024,
      "valor": 1500.00,
      "tipo": "RECEITA",
      "status": "EFETIVADO",
      "categoria": {
        "id": 1,
        "nome": "Vendas"
      }
    }
    ```

### **3. POST /api/lancamentos**

- **Descrição:** Salva um novo lançamento.
- **Corpo da Requisição:** `LancamentoDTO`
- **Resposta:**
  - **201 Created**
    ```json
    {
      "id": 1,
      "descricao": "Venda",
      "mes": 9,
      "ano": 2024,
      "valor": 1500.00,
      "tipo": "RECEITA",
      "status": "PENDENTE",
      "categoria": {
        "id": 1,
        "nome": "Vendas"
      }
    }
    ```

### **4. PUT /api/lancamentos/{id}**

- **Descrição:** Atualiza um lançamento existente.
- **Parâmetros de URL:**
  - `id`: ID do lançamento.
- **Corpo da Requisição:** `LancamentoDTO`
- **Resposta:**
  - **200 OK**
    ```json
    {
      "id": 1,
      "descricao": "Venda Atualizada",
      "mes": 9,
      "ano": 2024,
      "valor": 2000.00,
      "tipo": "RECEITA",
      "status": "PENDENTE",
      "categoria": {
        "id": 1,
        "nome": "Vendas"
      }
    }
    ```

### **5. PUT /api/lancamentos/{id}/atualiza-status**

- **Descrição:** Atualiza o status de um lançamento.
- **Parâmetros de URL:**
  - `id`: ID do lançamento.
- **Corpo da Requisição:** `AtualizaStatusDTO`
- **Resposta:**
  - **200 OK**
    ```json
    {
      "id": 1,
      "status": "EFETIVADO"
    }
    ```

### **6. DELETE /api/lancamentos/{id}**

- **Descrição:** Deleta um lançamento específico pelo ID.
- **Parâmetros de URL:**
  - `id`: ID do lançamento.
- **Resposta:**
  - **204 No Content**

### **7. POST /api/lancamentos/{id}/importar**

- **Descrição:** Importa lançamentos a partir de um arquivo CSV.
- **Parâmetros de URL:**
  - `id`: ID do usuário.
- **Corpo da Requisição:** Arquivo CSV.
- **Resposta:**
  - **201 Created**
    ```json
    {
      "message": "Lançamentos importados com sucesso."
    }
    ```

### **8. GET /api/lancamentos/download**

- **Descrição:** Realiza o download dos lançamentos filtrados em formato JSON.
- **Parâmetros de Consulta:**
  - `descricao` (opcional): Descrição do lançamento.
  - `mes` (opcional): Mês do lançamento.
  - `ano` (opcional): Ano do lançamento.
  - `usuario` (obrigatório): ID do usuário.
- **Resposta:**
  - **200 OK**
    ```json
    [
      {
        "id": 1,
        "descricao": "Venda",
        "mes": 9,
        "ano": 2024,
        "valor": 1500.00,
        "tipo": "RECEITA",
        "status": "EFETIVADO",
        "categoria": {
          "id": 1,
          "nome": "Vendas"
        }
      },
      // ...
    ]
    ```

---

## Categorias

### **1. POST /api/categorias**

- **Descrição:** Salva uma nova categoria.
- **Corpo da Requisição:** `CategoriaDTO`
- **Resposta:**
  - **201 Created**
    ```json
    {
      "id": 1,
      "nome": "Vendas"
    }
    ```



