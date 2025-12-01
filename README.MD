# Laboratório DAINF - Backend

Repositório do servidor backend da aplicação de gerenciamento de laboratórios do Departamento Acadêmico de Informática (DAINF) da UTFPR - Campus Pato Branco.

Este sistema fornece uma API RESTful construída com Spring Boot, responsável por gerenciar laboratórios, usuários e reservas.

---

## ⚙️ Tecnologias Utilizadas

- **Java 17**
- **Spring Boot**
    - Spring Web
    - Spring Data JPA
    - Spring Security
- **Banco de Dados**: PostgreSQL 
- **Maven** ou **Gradle**
- **Lombok**
- **Swagger/OpenAPI** (documentação da API, se disponível)
- **Flyway**: Migração do banco de dados.

---

## 📦 Como Executar o Projeto

### 1. Pré-requisitos

- Java 17+
- Maven ou Gradle
- IDE (IntelliJ, Eclipse, VS Code)
- Criar um banco de dados no PostgresSQL

### 2. Clone o repositório

```bash
git clone https://github.com/utfprapps-pb/laboratorio-dainf-server.git
cd laboratorio-dainf-server
```

### 3. Configure o ambiente

Crie um arquivo chamado **.env** em src/main/resources/, com as configurações básicas, conforme o arquivo **.env.example**. 
Nesse arquivo devem ser informadas os dados da conexão com o banco de dados conforme os dados do SGBD utilizado.

Nesse arquivo também deve ser configurado os dados do Minio para teste de upload de arquivos, 
o arquivo **docker-compose-minio.yml** possui um exemplo de criação de uma instância minio em um contêiner Docker. 

Também é necessário criar uma conta de email para testes, os dados da conta de email também devem ser informados no arquivo 
**.env**, o GMail permite configuração de conta de email para envio por aplicações externas no endereço: [https://myaccount.google.com/apppasswords](https://myaccount.google.com/apppasswords). 

```properties
SERVER_PORT=8099
SPRING_PROFILES_ACTIVE=dev

DATABASE_URL=jdbc:postgresql://localhost:5432/dainf_labs
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=postgres

GOOGLE_CLIENT_ID=123456
GOOGLE_CLIENT_SECRET=123456

UTFPR_TOKEN_SECRET=Utfpr@D4!NF

# Email Configuration (usando Spring Boot padrão spring.mail.*)
UTFPR_EMAIL_USERNAME=email@utfpr.edu.br
UTFPR_EMAIL_PASSWORD=123456
UTFPR_EMAIL_HOST=smtp.utfpr.edu.br
#UTFPR_EMAIL_HOST=200.19.73.102
UTFPR_EMAIL_PORT=587

UTFPR_FRONT_URL=http://localhost:4200/#

MINIO_ENDPOINT=http://127.0.0.1:9000
MINIO_PORT=9000
MINIO_ACCESS_KEY=minio@admin
MINIO_SECRET_KEY=minio@admin
MINIO_SECURE=false
MINIO_BUCKET_NAME=dainf-labs
```


### 4. Execute a aplicação

Iniciar a aplicação pelo arquivo **ServerApplication** se utilizando uma IDE como uma aplicação Spring Boot. ou com o Maven executando:

```bash
./mvnw spring-boot:run
```

### 📁 Estrutura do Projeto

```bash
br.com.utfpr.gerenciamento.server
src/
├── main/
│   ├── br/com/utfpr/gerenciamento/server
│   │   ├── component/
│   │   ├── config/
│   │   ├── controller/
│   │   ├── dto/
│   │   ├── enumeration/
│   │   ├── error/
│   │   ├── mienio/
│   │   ├── model/
│   │   ├── repository/
│   │   ├── security/
│   │   ├── service/
│   │   ├── util/
│   │   └── ServerApplication.java
│   └── resources/
│       ├── db/
│       ├── templates/
│       ├── .env
│       └── application.properties (-dev, -prod, -test)
└── test/
```

---

## Matriz de Permissões por Perfil de Usuário

O sistema possui 4 perfis de usuário com diferentes níveis de acesso:

| Perfil | Descrição |
|--------|-----------|
| ADMINISTRADOR | Acesso total ao sistema |
| LABORATORISTA | Técnico de laboratório com acesso total às operações CRUD |
| PROFESSOR | Professor com acesso restrito (leitura + exportação + criar reserva) |
| ALUNO | Aluno com acesso restrito (leitura + exportação + criar reserva) |

### Permissões por Módulo

#### Reserva

| Operação | ADMINISTRADOR | LABORATORISTA | PROFESSOR | ALUNO |
|----------|---------------|---------------|-----------|-------|
| Visualizar (GET) | Sim | Sim | Sim | Sim |
| Criar (POST) | Sim | Sim | Sim | Sim |
| Editar (PUT) | Sim | Sim | Não | Não |
| Excluir (DELETE) | Sim | Sim | Não | Não |

#### Solicitação de Compra

| Operação | ADMINISTRADOR | LABORATORISTA | PROFESSOR | ALUNO |
|----------|---------------|---------------|-----------|-------|
| Visualizar (GET) | Sim | Sim | Sim | Sim |
| Criar (POST) | Sim | Sim | Não | Não |
| Editar (PUT) | Sim | Sim | Não | Não |
| Excluir (DELETE) | Sim | Sim | Não | Não |

#### Saída, Compra, Entrada, Fornecedor, Grupo

| Operação | ADMINISTRADOR | LABORATORISTA | PROFESSOR | ALUNO |
|----------|---------------|---------------|-----------|-------|
| Visualizar (GET) | Sim | Sim | Não | Não |
| Criar (POST) | Sim | Sim | Não | Não |
| Editar (PUT) | Sim | Sim | Não | Não |
| Excluir (DELETE) | Sim | Sim | Não | Não |

#### Empréstimo

| Operação | ADMINISTRADOR | LABORATORISTA | PROFESSOR | ALUNO |
|----------|---------------|---------------|-----------|-------|
| Visualizar (GET) | Sim | Sim | Sim (próprio) | Sim (próprio) |
| Criar (POST) | Sim | Sim | Não | Não |
| Editar (PUT) | Sim | Sim | Não | Não |
| Excluir (DELETE) | Sim | Sim | Não | Não |

#### Item

| Operação | ADMINISTRADOR | LABORATORISTA | PROFESSOR | ALUNO |
|----------|---------------|---------------|-----------|-------|
| Visualizar (GET) | Sim | Sim | Sim | Sim |
| Criar (POST) | Sim | Sim | Não | Não |
| Editar (PUT) | Sim | Sim | Sim | Sim |
| Excluir (DELETE) | Sim | Sim | Não | Não |

#### Usuário

| Operação | ADMINISTRADOR | LABORATORISTA | PROFESSOR | ALUNO |
|----------|---------------|---------------|-----------|-------|
| Visualizar (GET) | Sim | Não | Não | Não |
| Criar (POST) | Sim | Não | Não | Não |
| Editar (PUT) | Sim | Não | Não | Não |
| Excluir (DELETE) | Sim | Não | Não | Não |

#### Nada Consta

| Operação | ADMINISTRADOR | LABORATORISTA | PROFESSOR | ALUNO |
|----------|---------------|---------------|-----------|-------|
| Visualizar (GET) | Sim | Sim | Não | Não |
| Solicitar (POST) | Sim | Sim | Não | Não |
| Verificar Pendências (PUT) | Sim | Sim | Não | Não |
| Invalidar (PUT) | Sim | Sim | Não | Não |
| Reenviar (POST) | Sim | Sim | Não | Não |
| Gerar PDF (GET) | Sim | Sim | Não | Não |

### Observações

- Professores e alunos podem visualizar apenas suas próprias reservas e empréstimos
- A exportação de dados (Excel/CSV) está disponível para todos os perfis autenticados
- Tentativas de acesso não autorizado retornam HTTP 403 (Forbidden)

---

### Licença
Projeto desenvolvido com fins acadêmicos no âmbito da UTFPR - Campus Pato Branco.
Licenciado sob os termos da licença MIT.

---

## <img src="https://sistemas2.utfpr.edu.br/assets/favicon.svg" alt="UTFPR Logo" width="20" /> Desenvolvido por alunos e professores da UTFPR-PB.

#### A aplicação faz parte do projeto **Bowser: desenvolvimento de softwares de código aberto para a comunidade**.

 - Professor: Vinicius Pegorini
 - Aluno: Gustavo Henrique Lopes Spachuk Zaffani (TCC - Análise e Desenvolvimento de Sistemas)