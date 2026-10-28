# Busca Peça – Back-end

API REST do **Busca Peça**, marketplace de autopeças. Aplicação única em Java com Spring Boot, organizada como **monolito modular** (ADR-003). É a única porta de acesso ao banco MySQL (ADR-001, ADR-004). O front-end fica em um repositório separado (ADR-015).

> **Estado atual:** apenas a estrutura do projeto. As pastas de cada módulo e camada já existem, mas ainda não há controllers, serviços, entidades nem migrações. As pastas vazias têm um arquivo `.gitkeep` só para o Git conseguir versioná-las; ele pode ser apagado quando a pasta receber o primeiro arquivo.

## Tecnologias

| Item | Escolha |
|---|---|
| Linguagem | Java 21 (LTS) |
| Framework | Spring Boot 4.1.2 |
| Build | Maven |
| Banco | MySQL 8 (InnoDB, `utf8mb4_0900_ai_ci`) |
| IDE | IntelliJ IDEA |
| Ambiente local | MySQL em Docker Compose + aplicação pelo IntelliJ em `localhost:8080` |
| Pacote base | `br.com.buscapeca.buscapecaapi` |

Dependências: Spring Web, Spring Data JPA, MySQL Driver, Flyway Migration (com `flyway-mysql`), Spring Security, OAuth2 Resource Server, Validation, Java Mail Sender, Spring Boot Actuator, Spring Boot DevTools e Lombok.

A adicionar depois: springdoc-openapi (Swagger), MapStruct, Resilience4j e Testcontainers.

## Pré-requisitos

- JDK 21
- IntelliJ IDEA
- Docker Desktop (para o MySQL local)
- Git

## Como iniciar o projeto

1. **Clonar o repositório**
   ```bash
   git clone <url-do-repositorio>
   cd buscapeca-backend
   ```
2. **Criar o arquivo de variáveis**: copie `.env.exemplo` para `.env` (no PowerShell: `Copy-Item .env.exemplo .env`). O `.env` não vai para o GitHub.
3. **Subir o MySQL**
   ```bash
   docker compose up -d
   ```
   Cria o schema `buscapeca` com `utf8mb4_0900_ai_ci` na porta 3306.
4. **Abrir no IntelliJ**: *File → Open* → pasta do projeto. O IntelliJ lê o `pom.xml` e baixa as dependências. Confira em *File → Project Structure → Project* que o SDK é o 21.
5. **Ativar o Lombok**: *Settings → Build, Execution, Deployment → Compiler → Annotation Processors* → *Enable annotation processing*.
6. **Rodar**: execute a classe principal. O perfil `dev` já é o padrão (para deixar explícito: *Run → Edit Configurations → Active profiles* = `dev`).
7. **Testar**: acesse `http://localhost:8080/actuator/health`. Deve responder `{"status":"UP"}`.

> Enquanto a configuração de segurança não for criada, o Spring Security protege todas as rotas com um usuário gerado automaticamente (a senha aparece no console). Isso é esperado nesta fase.

## Estrutura de pastas

Os pacotes são organizados **por domínio**, não por tipo técnico.

```
buscapeca-backend/
├── pom.xml                  dependências e build
├── docker-compose.yml       MySQL local
├── .env.exemplo             modelo das variáveis (o .env real fica fora do Git)
├── .gitignore
├── README.md
└── src/
    ├── main/
    │   ├── java/br/com/buscapeca/buscapecaapi/
    │   │   ├── (classe principal)
    │   │   ├── config/                  CORS, OpenAPI, Jackson, armazenamento de imagens
    │   │   ├── seguranca/               Spring Security, JWT, cookie, CSRF
    │   │   ├── compartilhado/           exceções, respostas de erro, paginação, utilitários
    │   │   ├── usuario/                 conta, papéis, endereços, tokens
    │   │   ├── loja/                    Minha Loja, métodos de pagamento aceitos
    │   │   ├── peca/                    anúncios, fotos, estoque, preços, promoções
    │   │   ├── catalogo/                categorias, veículos, compatibilidade, tags
    │   │   ├── busca/                   busca, buscas salvas, favoritos, avisos
    │   │   ├── carrinho/                carrinho e itens
    │   │   ├── pedido/                  compra, pedidos por loja, entrega, recibo
    │   │   ├── comunicacao/             conversas e mensagens (chat)
    │   │   ├── posvenda/                avaliações, problemas, devoluções, disputas
    │   │   ├── notificacao/             notificações no sistema e por e-mail
    │   │   ├── administracao/           termos de uso, permissões, log de auditoria
    │   │   └── integracao/              adaptadores de serviços externos (ADR-012)
    │   │       ├── codigopeca/          base externa de códigos (autopreenchimento)
    │   │       ├── frete/               cálculo de frete
    │   │       ├── geolocalizacao/      CEP para latitude e longitude
    │   │       ├── email/               envio de e-mails
    │   │       └── armazenamento/       upload e leitura de imagens
    │   └── resources/
    │       ├── application.yml          configuração comum
    │       ├── application-dev.yml      desenvolvimento local
    │       ├── application-prod.yml     produção
    │       ├── db/migration/            migrações Flyway
    │       └── templates/email/         modelos dos e-mails
    └── test/java/br/com/buscapeca/buscapecaapi/   mesma estrutura do main
```

### Camadas dentro de cada módulo

| Pasta | Responsabilidade | O que **não** fica aqui |
|---|---|---|
| `controller` | Recebe a requisição HTTP, valida o DTO, chama o serviço e devolve o status | Regras de negócio, acesso ao banco |
| `service` | Regras de negócio, verificação de dono, transações | Detalhes de HTTP |
| `repository` | Consultas pelo Spring Data JPA | Regras de negócio |
| `entity` | Classes mapeadas para as tabelas do DER | Dados que só a tela usa |
| `dto` | Formato do que entra (`Request`) e sai (`Response`) | Senha, hash ou dados internos |
| `mapper` | Conversão entre entidade e DTO | Lógica de negócio |
| `enums` | Valores fixos (condição, status, tipo) | — |

Regras gerais: toda regra de negócio fica no `service`; a API nunca devolve entidades, só DTOs; cada módulo é dono das suas tabelas e outro módulo usa o **serviço** do dono, nunca o repositório; pagamento, comissão e cupom são fase 2 (ADR-008).

### Módulos, tabelas e rotas

| Módulo | Tabelas | Rota base |
|---|---|---|
| usuario | usuario, papel, usuario_papel, endereco, token_verificacao, aceite_termo | `/api/usuarios`, `/api/auth` |
| loja | loja, loja_metodo_pagamento | `/api/lojas` |
| peca | peca, foto_peca, historico_preco, promocao, metrica_anuncio | `/api/pecas` |
| catalogo | categoria, veiculo, compatibilidade, tag, peca_tag, peca_equivalencia, diagrama_tecnico, hotspot | `/api/categorias`, `/api/veiculos` |
| busca | busca_salva, favorito, aviso_disponibilidade | `/api/busca`, `/api/favoritos` |
| carrinho | carrinho, item_carrinho | `/api/carrinho` |
| pedido | compra, pedido, item_pedido, entrega, recibo | `/api/compras`, `/api/pedidos` |
| comunicacao | conversa, mensagem | `/api/conversas` |
| posvenda | avaliacao, problema_pedido, anexo_problema, devolucao, disputa | `/api/avaliacoes`, `/api/problemas` |
| notificacao | notificacao | `/api/notificacoes` |
| administracao | termo_uso, permissao, papel_permissao, log_auditoria | `/api/admin` |

## Banco de dados e migrações

O esquema `buscapeca` é criado **somente** pelas migrações Flyway em `src/main/resources/db/migration`. O Hibernate está em `ddl-auto: validate`.

- Nome do arquivo: `V<número>__<descricao>.sql` (dois sublinhados), ex.: `V3__criar_tabelas_catalogo.sql`.
- Uma migração que já rodou **nunca** é editada; para corrigir, crie outra.
- Combinem no grupo o número antes de abrir a branch.
- Tabelas e colunas em minúsculas, singular, com sublinhado; PK `id_<tabela>`; dinheiro em `DECIMAL`, nunca `FLOAT`.

| Versão | Conteúdo |
|---|---|
| V1 | usuario, papel, permissao, usuario_papel, papel_permissao, endereco, token_verificacao |
| V2 | termo_uso, aceite_termo, log_auditoria, notificacao |
| V3 | categoria, veiculo, tag |
| V4 | loja, loja_metodo_pagamento, peca, foto_peca, historico_preco, promocao, metrica_anuncio, compatibilidade, peca_tag, peca_equivalencia |
| V5 | índices de busca (FULLTEXT; OEM, EAN, SKU) |
| V6 | busca_salva, favorito, aviso_disponibilidade |
| V7 | carrinho, item_carrinho, compra, pedido, item_pedido, entrega, recibo |
| V8 | conversa, mensagem, avaliacao, problema_pedido, anexo_problema, devolucao, disputa |
| V9 | dados iniciais: papéis, permissões, categorias, primeiro termo de uso |
| V10 | diagrama_tecnico, hotspot (quando BP1-12 for priorizado) |

## Perfis e variáveis de ambiente

| Perfil | Uso |
|---|---|
| `dev` (padrão) | Banco do Docker Compose, SQL no log, front em `http://localhost:5500`, cookie sem `Secure` |
| `prod` | Tudo lido de variáveis de ambiente, SQL desligado, cookie com `Secure` |

Variáveis (lista completa em `.env.exemplo`): `DB_URL`, `DB_USUARIO`, `DB_SENHA`, `JWT_SEGREDO`, `JWT_EXPIRACAO_MINUTOS`, `FRONT_ORIGEM`, `COOKIE_DOMINIO`, `EMAIL_HOST`, `EMAIL_USUARIO`, `EMAIL_SENHA`, `CODIGO_PECA_API_URL`, `CODIGO_PECA_API_CHAVE`, `UPLOAD_DIRETORIO`.

## Padrões da API

- Rotas começam com `/api`, substantivos no plural, JSON em camelCase.
- Listagens paginadas com `pagina` e `tamanho` (padrão 20, máximo 100).
- Erros: 400 dados inválidos, 401 não logado, 403 sem permissão, 404 não existe, 409 conflito de regra, 500 erro inesperado.

## Ordem de implementação

1. Base: Docker Compose, perfis, `compartilhado`, tratador de erros, Swagger.
2. Usuários e segurança (V1, V2, V9): cadastro, login com cookie, logout, `me`, CSRF, CORS.
3. Catálogo (V3).
4. Minha Loja e peças (V4).
5. Autopreenchimento (integração `codigopeca`).
6. Busca (V5): código, texto, filtros, distância, favoritos.
7. Carrinho e pedido (V7).
8. Comunicação e pós-venda (V8).
9. Administração.

## Documentação de referência

- Busca Peça – Documentação do Back-end
- Busca Peça – Documentação do Banco de Dados (MySQL)
- ADRs do Busca Peça