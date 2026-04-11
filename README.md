# Projeto Mensageria E-commerce

Sistema distribuído para processamento e consulta de pedidos de um e-commerce. A aplicação consome mensagens de uma fila no Google Cloud Pub/Sub através de um Worker em Java, processa e persiste os dados relacionalmente no Supabase (PostgreSQL), e disponibiliza essas informações através de uma API RESTful construída em Node.js com Express e Prisma.

## Arquitetura do Projeto

O projeto é dividido em dois microsserviços principais:

1. **Consumer (Java):** Escuta a assinatura do Google Cloud Pub/Sub, valida o payload (JSON), lida com regras de integridade (descarte de lixo e dados inválidos) e persiste os dados no banco usando JDBC puro com HikariCP para pool de conexões.
2. **API REST (Node.js/TypeScript):** Camada de leitura (Read) focada em entregar os dados dos pedidos formatados. Construída utilizando arquitetura MVC/Service, com filtros dinâmicos, paginação e cálculo de valores totais em tempo real utilizando Prisma ORM.

## Tecnologias Utilizadas

**Mensageria e Banco de Dados:**

- Google Cloud Pub/Sub
- Supabase (PostgreSQL) configurado com Session Pooler (IPv4 routing)

**Consumer (Worker):**

- Java 21
- Google Cloud Pub/Sub SDK
- HikariCP (Connection Pooling)
- Gson (Parse de JSON)
- JDBC (Persistência)

**API:**

- Node.js com TypeScript
- Express
- Prisma ORM (v6)
- Cors, Dotenv, TSX

## Estrutura de Diretórios

```text
/
├── api/                             # Backend Node.js (API REST)
│   ├── prisma/                      # Schema do Prisma
│   ├── src/
│   │   ├── controllers/             # Gerenciamento de requisições e respostas
│   │   ├── lib/                     # Configurações globais (Instância do Prisma)
│   │   ├── routes/                  # Definição dos endpoints
│   │   ├── services/                # Regras de negócio, cálculos e consultas
│   │   ├── app.ts                   # Configuração do Express e middlewares
│   │   └── server.ts                # Ponto de entrada e inicialização do servidor
│   └── package.json
└── consumer/gcp-pubsub-consumer/    # Worker Java
    ├── src/main/java/
    │   ├── domain/dto/              # Classes de transferência de dados (Records)
    │   ├── repository/              # Conexão de banco e transações SQL
    │   ├── service/                 # Regras de consumo do Pub/Sub
    │   └── org/example/Main.java    # Ponto de entrada
    └── pom.xml
```

## Como executar o Consumer (Java)

O consumer roda em segundo plano capturando as mensagens ativamente.

### Pré-requisitos

- Java 21+ e Maven instalados.
- Chave de serviço do GCP (arquivo `.json`) na raiz da pasta do consumer.

### Configuração de Variáveis de Ambiente

Configure as seguintes variáveis de ambiente no seu sistema ou na sua IDE (IntelliJ/Eclipse) antes de rodar o projeto:

- `GOOGLE_APPLICATION_CREDENTIALS`: Caminho absoluto para o arquivo `.json` com a chave do GCP.
- `SUPABASE_URL`: URL do banco de dados (Deve ser a URL do Session Pooler para evitar erro de rede IPv6, ex: `jdbc:postgresql://[host].pooler.supabase.com:6543/postgres`).
- `SUPABASE_USER`: Usuário do banco (ex: `postgres`).
- `SUPABASE_PASSWORD`: Senha do banco.

### Executando

Navegue até a pasta do consumer e execute a classe `Main.java` pela sua IDE, ou utilize o Maven:

```bash
cd consumer/gcp-pubsub-consumer
mvn clean install
mvn exec:java -Dexec.mainClass="org.example.Main"
```

## Como executar a API (Node.js)

A API fornece os dados para front-ends ou outros sistemas através de requisições HTTP.

### Pré-requisitos

- Node.js 18+ instalado.

### Passo a Passo

1. Navegue até o diretório da API:

```bash
cd api
```

2. Instale as dependências:

```bash
npm install
```

3. Crie um arquivo `.env` na raiz da pasta `api` e configure as URLs de conexão com o Supabase:

```text
# Conexão principal usando Pooler (Porta 6543)
DATABASE_URL="postgresql://USUARIO:SENHA@HOST.pooler.supabase.com:6543/postgres?pgbouncer=true"

# Conexão direta usada apenas pelo motor do Prisma para engenharia reversa (Porta 5432)
DIRECT_URL="postgresql://USUARIO:SENHA@HOST.pooler.supabase.com:5432/postgres"
```

4. Gere o cliente do Prisma para obter a tipagem do banco:

```bash
npx prisma generate
```

5. Inicie o servidor em modo de desenvolvimento (auto-reload ativado via tsx):

```bash
npm run dev
```

A API estará rodando em `http://localhost:3000`.

## Endpoints da API

A API segue os princípios RESTful. Todos os retornos de pedidos obedecem o contrato de payload original da mensageria, com adição de cálculos de valor dinâmicos.

### 1. Consultar Pedidos (Com Filtros e Paginação)

**Rota:** `GET /orders`

**Query Parameters Suportados:**

- `page` (Opcional): Número da página (Padrão: 1).
- `limit` (Opcional): Quantidade de registros por página (Padrão: 10).
- `order` (Opcional): Ordenação por data de criação (`asc` ou `desc`. Padrão: `desc`).
- `codigoCliente` (Opcional): Filtra pedidos pertencentes a um ID de cliente específico.
- `codigoProduto` (Opcional): Filtra pedidos que contenham um ID de produto específico.
- `status` (Opcional): Filtra pelo status do pedido (ex: `created`, `paid`, `shipped`).

**Exemplo de Requisição:**

```text
GET /orders?page=1&limit=5&status=paid&codigoCliente=49494
```

**Retorno Esperado:**

```json
{
  "data": [
    {
      "uuid": "uuid-do-pedido",
      "created_at": "2025-10-23T10:00:00Z",
      "channel": "WEB",
      "total": 250.00,
      "status": "paid",
      "customer": { ... },
      "seller": { ... },
      "items": [
        {
          "id": 1,
          "product_id": 99,
          "product_name": "Produto Exemplo",
          "unit_price": 125.00,
          "quantity": 2,
          "total": 250.00,
          "category": { ... }
        }
      ],
      "shipment": { ... },
      "payment": { ... },
      "metadata": { ... }
    }
  ],
  "page": 1,
  "limit": 5,
  "total": 1,
  "totalPages": 1
}
```

### 2. Consultar Pedido Específico por UUID

**Rota:** `GET /orders/:uuid`

**Exemplo de Requisição:**

```text
GET /orders/fca312f2-3ab4-4ea0-8fb1-b6431e169ad5
```

Retorna o objeto do pedido formatado (ou erro 404 caso não encontrado).

### 3. Health Check

**Rota:** `GET /health`
Usado para verificar a disponibilidade da API.

## Decisões Arquiteturais e Desafios Resolvidos

- **Idempotência e Deadlocks (Java):** A inserção no banco foi segmentada em duas fases. A inserção de dados de dicionário (Cliente, Seller, Categorias) ocorre com AutoCommit para evitar _table locks_. O processamento do Pedido e seus dependentes ocorre em uma transação estrita. Caso ocorra erro nos itens do pedido, aplica-se o Rollback do pedido, mantendo a integridade do banco.
- **Connection Pooling:** O uso de HikariCP no Java reduz a latência e o consumo de rede, mantendo instâncias de conexão TCP reaproveitáveis.
- **Proteção contra Lixo de Fila (Dead Letter Logic):** O Consumer verifica se as mensagens contêm sintaxe JSON falha ou padrões de UUID inválidos (ex: _ORD-2025_), disparando o _acknowledge_ (ACK) imediato para retirar a mensagem falsa da fila e não travar a aplicação.
- **Prisma Versionamento:** A API faz uso da versão 6 do Prisma para assegurar estabilidade nas consultas relacionais sem forçar arquitetura baseada em edge-adapters (exigidos na versão 7), garantindo máxima compatibilidade em ambiente Node padrão.
