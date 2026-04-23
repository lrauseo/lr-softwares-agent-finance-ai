# Roadmap do projeto `finance_ai_agent`

## Objetivo do MVP

Criar um agente de IA consultor financeiro pessoal que:

- receba receitas e despesas do usuário
- categorize os lançamentos
- gere resumo financeiro mensal
- identifique alertas financeiros
- responda perguntas com base nos dados reais do usuário
- futuramente evolua para RAG, histórico, memória e recomendações melhores

---

## 1. Estado atual do projeto

### O que já existe

- `Category` e `Transaction` como entidades JPA
- controllers para:
  - categorias
  - transações
  - resumo mensal
- serviços para:
  - salvar categoria
  - salvar transação
  - listar transações
  - resumir mês
- início da camada de análise financeira:
  - `FinancialAnalysisService`
  - `FinancialAnalysisServiceImpl`
- início da camada de IA:
  - `FinancialAdvisorChatService`
- documentação OpenAPI

### O que está faltando ou incompleto

- `ChatController` está vazio
- não há endpoint funcional de chat
- não há persistência de conversas
- não há memória do usuário
- não há RAG
- não há configuração visível de Spring AI / modelo / propriedades
- não há dockerização visível no material enviado
- não há migrations visíveis
- não há tratamento global de exceções
- não há autenticação
- não há testes

### Problemas técnicos que precisam ser corrigidos cedo

- `SummaryService` calcula receita/despesa pelo **sinal do valor**, mas a entidade já tem `type`
- `CreateTransactionRequest` aceita `type`, mas o `amount` é sempre positivo por validação. Isso conflita com a lógica atual do resumo
- `TransactionService.salvar()` salva a transação mesmo se a categoria não existir
- `TransactionResponse` retorna a entidade `Category` inteira. Para API isso é ruim
- `updated_ai` parece erro de nome. Provavelmente deveria ser `updated_at`
- `ChatQuestionRequest` usa `io.micrometer.common.lang.NonNull`, enquanto o resto usa `jakarta.validation` / `spring.lang.NonNull`
- há import estranho em `FinancialAdvisorChatService`: `org.mapstruct.control.NoComplexMapping` sem uso
- `CategoryService` injeta bem, mas ainda não impõe validações de negócio como duplicidade por usuário + tipo + nome

---

## 2. Visão de arquitetura do MVP

### Camadas recomendadas

- **controller**: recebe request e devolve response
- **service**: regra de negócio
- **repository**: acesso a banco
- **mapper**: conversão entre request/entity/response
- **analysis**: regras financeiras determinísticas
- **ai**: orquestra LLM com contexto controlado

### Fluxo principal do MVP

1. usuário cadastra categorias
2. usuário cadastra receitas e despesas
3. sistema gera resumo mensal
4. sistema detecta alertas
5. usuário faz pergunta no chat
6. backend monta contexto com resumo + alertas + transações relevantes
7. LLM responde sem inventar dados

---

## 3. Sprint 0 - Colocar a casa em ordem

### Objetivo

Padronizar a base antes de crescer.

### O que implementar

1. corrigir nomenclaturas e inconsistências
2. definir padrão de pacotes
3. revisar DTOs de entrada e saída
4. garantir que receita/despesa seja determinada por `TransactionType`, não por sinal do valor
5. adicionar validações mínimas de negócio

### Tarefas

- renomear colunas:
  - `updated_ai` -> `updated_at`
- revisar `TransactionResponse`
  - não devolver `Category` inteiro
  - devolver algo como:
    - `categoryId`
    - `categoryName`
- revisar `CreateTransactionRequest`
  - manter `amount` sempre positivo
  - usar `type` para definir se é receita ou despesa
- ajustar `SummaryService`
  - somar receitas por `type == INCOME`
  - somar despesas por `type == EXPENSE`
  - `balance = income - expense`
- ajustar categorização no resumo
  - categorias devem consolidar despesas positivas, não depender de valor negativo
- validar categoria ao salvar transação
  - se não existir: lançar exceção 404 ou 400
- impedir categoria duplicada por usuário/tipo/nome
- padronizar validações com `jakarta.validation`
- remover imports mortos

### Entregáveis

- CRUD de categorias e transações consistente
- resumo mensal correto
- projeto com naming limpo

### Critério de pronto

- consigo cadastrar categoria
- consigo cadastrar transação
- consigo listar transações
- consigo consultar resumo mensal com valores corretos

### O que você aprende nesta sprint

- desenho de DTO
- separação entre entidade e response
- regra de negócio no service
- validação transacional básica

---

## 4. Sprint 1 - Fechar o núcleo financeiro

### Objetivo

Ter um backend funcional de finanças pessoais sem IA ainda.

### O que implementar

1. completar regras financeiras básicas
2. melhorar endpoints
3. preparar dados confiáveis para IA

### Tarefas

- criar endpoint de detalhe de transação por id
- criar endpoint de exclusão/edição de transação
- criar endpoint de exclusão/edição de categoria
- impedir remover categoria em uso sem regra clara
- criar consultas úteis:
  - total por categoria no mês
  - total recorrente do mês
  - top despesas do mês
  - saldo acumulado por mês
- melhorar repository com queries específicas em vez de depender só de stream em memória

### Sugestão de endpoints

- `POST /api/categories`
- `GET /api/categories?userId=...`
- `PUT /api/categories/{id}`
- `DELETE /api/categories/{id}`
- `POST /api/transactions`
- `GET /api/transactions?userId=...&startDate=...&endDate=...`
- `GET /api/transactions/{id}`
- `PUT /api/transactions/{id}`
- `DELETE /api/transactions/{id}`
- `GET /api/summary/monthly?userId=...&monthDate=2026-04`

### Entregáveis

- API financeira sólida
- base pronta para análises

### Critério de pronto

- o sistema funciona como um mini gerenciador financeiro sem IA

### O que você aprende nesta sprint

- design REST
- queries JPA úteis para domínio real
- tradeoff entre stream na service e agregação no banco

---

## 5. Sprint 2 - Motor de análise financeira determinística

### Objetivo

Gerar inteligência de negócio sem depender da LLM.

### O que implementar

Aproveitar o que já começou em `FinancialAnalysisServiceImpl` e fechar isso direito.

### Alertas iniciais

- despesas maiores que receitas
- saldo apertado
- categoria consumindo percentual alto da renda
- crescimento forte de gasto por categoria
- peso alto de despesas recorrentes

### Melhorias necessárias

- garantir que os percentuais usem receita e despesa corretas
- separar regras configuráveis em constantes centralizadas ou properties
- criar resposta de análise estruturada

### Novo DTO sugerido

- `FinancialDiagnosisResponse`
  - `monthlySummary`
  - `alerts`
  - `highlights`
  - `recommendations`

### Tarefas

- criar endpoint:
  - `GET /api/analysis/monthly?userId=...&monthDate=2026-04`
- padronizar severidade
- adicionar mensagem orientativa por alerta
- ordenar alertas por severidade
- escrever testes unitários das regras

### Entregáveis

- diagnóstico financeiro explicável
- regras auditáveis e previsíveis

### Critério de pronto

- o backend consegue dizer, sem LLM, por que a situação do usuário está ruim ou boa

### O que você aprende nesta sprint

- engine de regras
- análise de domínio
- como preparar contexto confiável para IA

---

## 6. Sprint 3 - Primeiro chat com IA de verdade

### Objetivo

Fazer o agente responder perguntas usando dados reais do usuário.

### O que implementar

Completar a camada de chat já iniciada.

### Tarefas

- implementar `ChatController`
- criar endpoint:
  - `POST /api/chat/question`
- injetar `FinancialAdvisorChatService`
- montar contexto com:
  - resumo do mês atual
  - alertas
  - transações recentes
  - categorias com maior gasto
- reforçar prompt do sistema

### Estrutura recomendada do fluxo

1. recebe `userId` + `question`
2. busca dados financeiros do usuário
3. transforma em contexto enxuto
4. envia para LLM
5. devolve resposta textual

### Cuidados

- não mandar dados demais para o prompt
- não mandar entidade JPA crua
- não confiar na LLM para calcular números
- a LLM explica, não calcula a verdade final

### DTO sugerido de resposta

- `ChatAnswerResponse`
  - `answer`
  - `summaryReference`
  - `alerts`
  - `generatedAt`

### Exemplo de perguntas que devem funcionar

- "onde estou gastando mais?"
- "meu mês está saudável?"
- "o que posso cortar para economizar?"
- "minhas despesas recorrentes estão altas?"

### Entregáveis

- chat funcional com base nos dados do mês

### Critério de pronto

- perguntar no endpoint e receber uma resposta coerente baseada em dados reais

### O que você aprende nesta sprint

- integração Spring AI
- prompt estruturado com contexto controlado
- responsabilidade entre regra de negócio e LLM

---

## 7. Sprint 4 - Persistência de conversas e memória curta

### Objetivo

Parar de tratar cada pergunta como isolada.

### O que implementar

Criar histórico de conversas por usuário.

### Entidades sugeridas

- `ChatSession`
  - `id`
  - `userId`
  - `title`
  - `createdAt`
  - `updatedAt`
- `ChatMessage`
  - `id`
  - `sessionId`
  - `role` (`USER`, `ASSISTANT`, `SYSTEM`)
  - `content`
  - `createdAt`

### Tarefas

- criar tabelas e migrations
- criar endpoints:
  - `POST /api/chat/sessions`
  - `GET /api/chat/sessions?userId=...`
  - `GET /api/chat/sessions/{id}/messages`
  - `POST /api/chat/sessions/{id}/messages`
- enviar últimas N mensagens no contexto do chat

### Entregáveis

- histórico simples de conversa

### Critério de pronto

- o assistente consegue responder mantendo contexto recente

### O que você aprende nesta sprint

- memória conversacional básica
- modelagem de chat backend

---

## 8. Sprint 5 - RAG interno do domínio financeiro

### Objetivo

Adicionar base de conhecimento para melhorar as respostas.

### Possíveis fontes para o RAG

- conteúdo educativo sobre finanças pessoais
- regras de orçamento
- glossário de categorias
- explicações sobre reserva de emergência, endividamento, juros, cartão, etc.
- regras próprias do app

### Arquitetura sugerida

- documentos fonte
- chunking
- embeddings
- base vetorial
- recuperação por pergunta
- envio dos trechos recuperados ao prompt

### Tarefas

- criar ingestão de documentos
- definir metadata dos chunks
  - `source`
  - `theme`
  - `audience`
  - `language`
- integrar busca vetorial antes da chamada ao LLM
- montar prompt com:
  - dados do usuário
  - alertas
  - trechos recuperados

### Entregáveis

- respostas mais educativas e contextualizadas

### Critério de pronto

- o assistente responde usando dados do usuário + conhecimento documental

### O que você aprende nesta sprint

- RAG na prática
- embeddings
- busca vetorial
- grounding

---

## 9. Sprint 6 - Segurança, identidade e multiusuário real

### Objetivo

Parar de depender de `userId` aberto em query param como se o mundo fosse um jardim zen.

### O que implementar

- autenticação JWT
- extrair usuário do token
- remover `userId` do request sempre que possível
- autorização por dono do registro

### Tarefas

- integrar Spring Security
- definir strategy de auth
  - local simples no MVP ou Keycloak depois
- ajustar controllers e services para usar usuário autenticado
- auditar acesso a categorias/transações/chat

### Entregáveis

- backend minimamente seguro

### Critério de pronto

- um usuário não consegue ver dados do outro

### O que você aprende nesta sprint

- segurança em API Spring
- ownership de dados

---

## 10. Sprint 7 - Qualidade, observabilidade e docker

### Objetivo

Deixar o projeto executável de forma previsível.

### O que implementar

- Dockerfile
- docker-compose
- banco local
- variáveis de ambiente
- logs úteis
- health check
- testes básicos

### Tarefas

- criar `Dockerfile`
- criar `docker-compose.yml` com:
  - app
  - postgres
- criar `application.yml`
- criar `application-local.yml`
- criar `application-docker.yml`
- adicionar actuator
- criar endpoint de health
- testes:
  - service unit test
  - repository test
  - controller integration test

### Entregáveis

- projeto sobe com um comando
- ambiente previsível para desenvolvimento

### Critério de pronto

- `docker compose up` sobe tudo e Swagger abre

### O que você aprende nesta sprint

- empacotamento real de backend
- configuração por ambiente
- testabilidade

---

## 11. Sprint 8 - Evoluções pós-MVP

### Itens futuros

- importação de CSV/OFX
- dashboards
- metas financeiras
- orçamento mensal por categoria
- previsão de fluxo de caixa
- notificação proativa
- classificação automática de gastos por IA
- MCP para integrações externas
- recomendação de ações com explicação e score de confiança

---

## 12. Ordem recomendada de execução

1. Sprint 0
2. Sprint 1
3. Sprint 2
4. Sprint 3
5. Sprint 4
6. Sprint 7
7. Sprint 6
8. Sprint 5
9. Sprint 8

### Por que essa ordem?

- primeiro confiabilidade de domínio
- depois análise
- depois IA
- depois memória
- depois empacotamento
- depois segurança mais completa
- depois RAG

---

## 13. Próxima ação prática

### Primeiro bloco de implementação comigo

Começar agora pela **Sprint 0** com estes passos:

1. revisar DTOs
2. corrigir cálculo do resumo
3. corrigir validação de categoria na transação
4. melhorar responses
5. criar exceções de negócio

### Resultado esperado logo no começo

Quando a Sprint 0 terminar, você terá uma base consistente para continuar sem retrabalho.

---

## 14. Regra de trabalho daqui pra frente

Para não virar bagunça de novo, cada sprint deve seguir este formato:

- objetivo
- classes a criar/alterar
- ordem dos commits
- endpoint/teste esperado
- o que validar manualmente no Swagger
- o que você aprende naquele passo

---

## 15. Como vamos conduzir juntos

Em cada etapa eu posso te passar exatamente:

- quais arquivos criar
- quais arquivos alterar
- código sugerido por classe
- sequência de implementação
- payloads para teste no Swagger/Postman
- checklist de pronto

Isso evita o caos artesanal que costuma acontecer quando a IA despeja arquitetura, IA, RAG, Docker e promessas metafísicas tudo na mesma resposta.
