# 💸 Projeto Minhas Finanças - Backend (minhas-financas-api)

## 📝 Descrição

Minhas Finanças API é uma aplicação desenvolvida com Spring Boot projetada para simplificar o gerenciamento de finanças pessoais. Esta API permite aos usuários acompanhar suas finanças de maneira eficiente, oferecendo funcionalidades para o registro e a análise de ganhos e gastos.

🌐 [Projeto FrontEnd](https://dev.azure.com/muralisti/Programa%20de%20Est%C3%A1gio%20da%20Muralis/_git/pem-paulo-henrique-front?path=%2F&version=GBdevelop&_a=contents)

## 🧪 Cobertura de Testes

- 22 testes

## 🚀 Tecnologias Utilizadas

- Java 1.8
- Spring Boot 2.1.8.RELEASE
- Lombok 1.18.34
- JSON Webtoken 0.9.1
- Postgresql 9.4

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
