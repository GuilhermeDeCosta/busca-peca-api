# Busca Peça – Back-end

API REST do **Busca Peça**, marketplace de autopeças. É uma aplicação única em Java com Spring Boot, organizada como **monolito modular** (ADR-003), e é a única porta de acesso ao banco MySQL (ADR-001, ADR-004). O front-end fica em um repositório separado (ADR-015).

> **Estado atual:** banco com as migrações V1 a V13, tratador de erros, paginação, Swagger, segurança (cadastro, login com JWT em cookie, logout, `me`, CSRF, CORS, recuperação de senha), perfil e endereços, catálogo (categorias, veículos, tags), loja, peças (cadastro, edição, detalhe, compatibilidade e histórico de preço) e busca. As rotas de usuário, catálogo e peças estão em [O que já funciona](#o-que-já-funciona). Carrinho, pedidos, comunicação, pós-venda e notificações ainda têm só as pastas, com um arquivo `.gitkeep`; apague-o quando a pasta receber o primeiro arquivo.

## Sumário

1. [Tecnologias](#tecnologias)
2. [O que instalar](#o-que-instalar)
3. [Clonar o repositório](#clonar-o-repositório)
4. [Rodar o projeto pela primeira vez](#rodar-o-projeto-pela-primeira-vez)
5. [Como trabalhar com branches](#como-trabalhar-com-branches)
6. [Estrutura de pastas](#estrutura-de-pastas)
7. [Banco de dados e migrações](#banco-de-dados-e-migrações)
8. [Perfis e variáveis de ambiente](#perfis-e-variáveis-de-ambiente)
9. [Padrões da API](#padrões-da-api)
10. [Ordem de implementação](#ordem-de-implementação)
11. [O que já funciona](#o-que-já-funciona)
11. [Problemas comuns](#problemas-comuns)

---

## Tecnologias

| Item | Escolha |
|---|---|
| Linguagem | Java 21 (LTS) |
| Framework | Spring Boot 4.1.2 |
| Build | Maven (pelo Maven Wrapper `mvnw`, não precisa instalar) |
| Banco | MySQL 8 (InnoDB, `utf8mb4_0900_ai_ci`) |
| IDE | IntelliJ IDEA |
| Ambiente local | MySQL em Docker Compose + aplicação pelo IntelliJ em `localhost:8080` |
| Pacote base | `br.com.buscapeca.buscapecaapi` |

Dependências: Spring Web, Spring Data JPA, MySQL Driver, Flyway Migration (com `flyway-mysql`), Spring Security, OAuth2 Resource Server, Validation, Java Mail Sender, Spring Boot Actuator, Spring Boot DevTools e Lombok.

A adicionar depois: springdoc-openapi (Swagger), MapStruct, Resilience4j e Testcontainers.

---

## O que instalar

Instale tudo antes de clonar. Depois de cada instalação, feche e abra o terminal e rode o comando de verificação.

| Ferramenta | Para que serve | Onde baixar | Como verificar |
|---|---|---|---|
| **Git** | Versionar o código e trabalhar com branches | https://git-scm.com/downloads | `git --version` |
| **JDK 21** | Compilar e rodar o Java | https://adoptium.net (Eclipse Temurin 21 LTS) | `java -version` (deve mostrar `21`) |
| **Docker Desktop** | Rodar o MySQL sem instalá-lo na máquina | https://www.docker.com/products/docker-desktop | `docker --version` e `docker compose version` |
| **IntelliJ IDEA** | Editar e rodar o projeto (Community ou Ultimate) | https://www.jetbrains.com/idea/download | Abrir o programa |

Opcional, mas útil:

- **Cliente de banco** para ver as tabelas: MySQL Workbench, DBeaver ou o painel *Database* do IntelliJ Ultimate.
- **Cliente HTTP** para testar a API: Postman, Insomnia ou o *HTTP Client* do IntelliJ.

### Observações por sistema

- **Windows:** no instalador do Temurin, marque *Set JAVA_HOME* e *Add to PATH*. O Docker Desktop pede o WSL 2; aceite e reinicie o computador se ele pedir.
- **macOS:** escolha o instalador certo do Docker Desktop (Apple Silicon ou Intel).
- **Linux:** instale o Docker Engine e o plugin `docker compose` pelo gerenciador da sua distribuição e adicione seu usuário ao grupo `docker`.

Não é preciso instalar MySQL nem Maven: o MySQL roda no Docker e o Maven vem junto no projeto pelo `mvnw`.

### Configurar o Git (só na primeira vez)

```bash
git config --global user.name "Seu Nome"
git config --global user.email "seu-email@exemplo.com"
git config --global init.defaultBranch main
```

Use o mesmo e-mail da sua conta do GitHub, para os commits aparecerem no seu perfil.

---

## Clonar o repositório

1. No GitHub, abra o repositório, clique em **Code** e copie a URL (HTTPS).
2. No terminal, vá até a pasta onde guarda seus projetos e rode:

   ```bash
   git clone <url-do-repositorio>
   cd buscapeca-backend
   ```

3. Confira que está na branch `main` e atualizado:

   ```bash
   git status
   ```

Pelo IntelliJ também funciona: na tela inicial, *Get from VCS* → cole a URL → *Clone*.

> Se o GitHub pedir senha no `git push`, use um **Personal Access Token** (GitHub → *Settings → Developer settings → Personal access tokens*) no lugar da senha, ou faça login pelo IntelliJ em *Settings → Version Control → GitHub*.

---

## Rodar o projeto pela primeira vez

1. **Criar o arquivo de variáveis.** Copie o modelo `.env.exemplo` para `.env`:

   ```bash
   # Linux e macOS
   cp .env.exemplo .env
   ```
   ```powershell
   # Windows (PowerShell)
   Copy-Item .env.exemplo .env
   ```

   O `.env` tem senhas e **nunca** vai para o GitHub (já está no `.gitignore`).

2. **Subir o MySQL.** Com o Docker Desktop aberto:

   ```bash
   docker compose up -d
   ```

   Isso cria o schema `buscapeca` com `utf8mb4_0900_ai_ci` na porta 3306. Para conferir se está rodando: `docker compose ps`.

3. **Abrir no IntelliJ.** *File → Open* → pasta `buscapeca-backend`. O IntelliJ lê o `pom.xml` e baixa as dependências (a primeira vez demora alguns minutos).

4. **Conferir o JDK.** *File → Project Structure → Project* → SDK = **21**.

5. **Ativar o Lombok.** *Settings → Build, Execution, Deployment → Compiler → Annotation Processors* → marque *Enable annotation processing*. Se o IntelliJ sugerir instalar o plugin do Lombok, aceite.

6. **Rodar a aplicação.** Execute a classe principal (botão ▶ ao lado do `main`). O perfil `dev` já é o padrão; para deixá-lo explícito, use *Run → Edit Configurations → Active profiles* = `dev`.

   Pelo terminal também funciona:

   ```bash
   # Linux e macOS
   ./mvnw spring-boot:run
   ```
   ```powershell
   # Windows
   .\mvnw.cmd spring-boot:run
   ```

7. **Testar.** Acesse `http://localhost:8080/api/actuator/health`. A resposta deve ser `{"status":"UP"}`. O Swagger fica em `http://localhost:8080/api/swagger-ui.html`.

> **Sem Docker?** Também funciona com o MySQL 8 instalado direto no Windows. Crie o banco e o usuário uma vez no *MySQL Command Line Client*:
> ```sql
> CREATE DATABASE IF NOT EXISTS buscapeca CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
> CREATE USER IF NOT EXISTS 'buscapeca'@'localhost' IDENTIFIED BY 'buscapeca';
> GRANT ALL PRIVILEGES ON buscapeca.* TO 'buscapeca'@'localhost';
> ```

Para desligar o banco ao fim do dia: `docker compose down` (os dados continuam salvos). Para apagar o banco e começar do zero: `docker compose down -v`.

---

## Como trabalhar com branches

A branch `main` tem sempre código que compila e roda. **Ninguém faz commit direto na `main`.** Cada tarefa ganha sua própria branch, e o código entra na `main` por Pull Request (PR) revisado por outra pessoa do grupo.

### Nome das branches

Formato: `tipo/codigo-do-caso-de-uso-descricao-curta`, tudo em minúsculas, com hífen e sem acento.

| Tipo | Quando usar | Exemplo |
|---|---|---|
| `feature/` | Funcionalidade nova | `feature/bp1-41-login` |
| `fix/` | Correção de erro | `fix/bp1-22-estoque-carrinho` |
| `migration/` | Nova migração Flyway | `migration/v3-catalogo` |
| `docs/` | Só documentação | `docs/readme-inicial` |
| `refactor/` | Reorganizar código sem mudar o comportamento | `refactor/tratador-erros` |
| `chore/` | Configuração, dependências, build | `chore/adicionar-swagger` |

### Passo a passo de uma tarefa

1. **Atualize a `main`** antes de começar:

   ```bash
   git checkout main
   git pull origin main
   ```

2. **Crie a branch** a partir dela:

   ```bash
   git checkout -b feature/bp1-41-login
   ```

3. **Trabalhe e faça commits pequenos**, cada um com uma mudança só:

   ```bash
   git status                      # ver o que mudou
   git add .                       # preparar os arquivos
   git commit -m "feat(usuario): cria endpoint de login"
   ```

4. **Envie a branch** para o GitHub (o `-u` só é preciso no primeiro envio):

   ```bash
   git push -u origin feature/bp1-41-login
   ```

5. **Abra o Pull Request** no GitHub: *Compare & pull request* → base `main` → descreva o que foi feito e o caso de uso (ex.: BP1-41) → peça revisão a alguém do grupo.

6. **Depois do merge**, volte para a `main`, atualize e apague a branch local:

   ```bash
   git checkout main
   git pull origin main
   git branch -d feature/bp1-41-login
   ```

### Manter a branch atualizada

Se a `main` recebeu mudanças enquanto você trabalhava, traga-as para a sua branch antes de abrir o PR:

```bash
git checkout main
git pull origin main
git checkout feature/bp1-41-login
git merge main
```

Se houver conflito, o Git marca os arquivos. Resolva no IntelliJ (*Git → Resolve Conflicts*), depois rode `git add .` e `git commit`.

### Mensagens de commit

Formato: `tipo(modulo): o que foi feito`, no presente.

```
feat(peca): adiciona cadastro de peça
fix(carrinho): impede quantidade maior que o estoque
migration(catalogo): cria V3 com categoria, veiculo e tag
docs: atualiza README com passo a passo de branches
```

### Antes de abrir o PR, confira

- [ ] A aplicação sobe sem erro e `/api/actuator/health` responde `UP`.
- [ ] Os testes passam (`./mvnw test` ou `.\mvnw.cmd test`).
- [ ] Nenhum `.env`, senha ou chave foi commitado.
- [ ] Migrações novas seguem o número combinado no grupo e nenhuma migração antiga foi editada.
- [ ] A API devolve DTOs, nunca entidades.

---

## Estrutura de pastas

Os pacotes são organizados **por domínio**, não por tipo técnico.

```
buscapeca-backend/
├── pom.xml                  dependências e build
├── mvnw, mvnw.cmd           Maven Wrapper (Linux/macOS e Windows)
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

---

## Banco de dados e migrações

O esquema `buscapeca` é criado **somente** pelas migrações Flyway em `src/main/resources/db/migration`. O Hibernate está em `ddl-auto: validate`: ele confere se as entidades batem com as tabelas, mas nunca cria nem altera nada.

- Nome do arquivo: `V<número>__<descricao>.sql` (dois sublinhados), ex.: `V3__criar_tabelas_catalogo.sql`.
- Uma migração que já rodou **nunca** é editada; para corrigir, crie outra.
- Combinem no grupo o número antes de abrir a branch, para duas pessoas não criarem a mesma versão.
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
| V10 | endereco.identificacao; usuario.razao_social e inscricao_estadual (BP1-134) |
| V11 | loja: `telefone`, `horario_retirada`, `motoboy_proprio`, `entrega_combinar`, `pedido_minimo_frete_gratis` (BP1-219) |
| V12 | modelo de peça do front: `status` (RASCUNHO, ATIVO, PAUSADO, EXCLUIDO) no lugar de `anuncio_ativo`, origem NACIONAL ou IMPORTADA, `marca_peca`, `caracteristicas`; categoria, OEM e preço opcionais no rascunho (BP1-220) |
| V13 | historico_preco: `preco_anterior` opcional (a linha da publicação não tem preço anterior) e índice por peça e data (BP1-53) |

As tabelas `diagrama_tecnico` e `hotspot` (BP1-12, fase 2) entram na próxima versão livre quando forem priorizadas.

---

## Perfis e variáveis de ambiente

| Perfil | Uso |
|---|---|
| `dev` (padrão) | Banco do Docker Compose, SQL no log, front em `http://localhost:5500`, cookie sem `Secure` |
| `prod` | Tudo lido de variáveis de ambiente, SQL desligado, cookie com `Secure` |

Variáveis (lista completa em `.env.exemplo`): `DB_URL`, `DB_USUARIO`, `DB_SENHA`, `JWT_SEGREDO`, `JWT_EXPIRACAO_MINUTOS`, `FRONT_ORIGEM`, `COOKIE_DOMINIO`, `EMAIL_HOST`, `EMAIL_USUARIO`, `EMAIL_SENHA`, `CODIGO_PECA_API_URL`, `CODIGO_PECA_API_CHAVE`, `UPLOAD_DIRETORIO`.

Quando alguém adicionar uma variável nova, ela deve entrar também no `.env.exemplo` (sem o valor real) e ser avisada no grupo.

---

## Padrões da API

- Rotas começam com `/api`, substantivos no plural, JSON em camelCase.
- Listagens paginadas com `pagina` e `tamanho` (padrão 20, máximo 100).
- Erros: 400 dados inválidos, 401 não logado, 403 sem permissão, 404 não existe, 409 conflito de regra, 500 erro inesperado.
- A lista completa de endpoints está na *Documentação do Back-end v2* e, para as rotas já prontas, no Swagger em `http://localhost:8080/api/swagger-ui.html`.

---

## Ordem de implementação

1. Base: perfis, `compartilhado`, tratador de erros, Swagger.
2. Usuários e segurança (V1, V2, V9): cadastro, login com cookie, logout, `me`, CSRF, CORS.
3. Catálogo (V3).
4. Minha Loja e peças (V4).
5. Autopreenchimento (integração `codigopeca`).
6. Busca (V5): código, texto, filtros, distância, favoritos.
7. Carrinho e pedido (V7).
8. Comunicação e pós-venda (V8).
9. Administração.

---

## O que já funciona

Todas as rotas começam com `http://localhost:8080/api`.

| Método | Rota | Acesso | O que faz |
|---|---|---|---|
| GET | `/auth/csrf` | Público | Devolve `{ headerName, token }`. Mande o `token` no cabeçalho `X-XSRF-TOKEN` em todo POST, PUT, PATCH e DELETE |
| POST | `/auth/login` | Público | `{ email, senha, manterConectado }`. Grava o JWT no cookie HttpOnly `BP_SESSAO` |
| POST | `/auth/logout` | Público | Apaga o cookie |
| GET | `/auth/me` | Logado | Nome, papéis, `temLoja`, `administrador`, `aceiteTermosPendente` |
| POST | `/auth/recuperar-senha` | Público | `{ email }`. 404 se o e-mail não tem conta. No perfil `dev` o e-mail sai no console |
| POST | `/auth/redefinir-senha` | Público | `{ token, novaSenha, confirmacaoSenha }` |
| POST | `/usuarios` | Público | Cadastro (papel COMPRADOR). Aceita camelCase e snake_case; não exige CSRF |
| GET | `/usuarios/me` | Logado | Perfil |
| PUT ou PATCH | `/usuarios/me` | Logado | `{ nomeExibicao, telefone, descricao }`. Campo ausente não muda |
| PUT | `/usuarios/me/senha` | Logado | `{ senhaAtual, novaSenha }` |
| POST | `/usuarios/me/termos/aceite` | Logado | Aceita a versão vigente dos termos |
| GET | `/termos/vigentes` | Público | Termos de Uso e Política de Privacidade vigentes |
| GET | `/categorias` e `/categorias/{id}` | Público | Árvore `{ idCategoria, nome, subcategorias }` |
| GET | `/veiculos/marcas` | Público | Lista de marcas |
| GET | `/veiculos?marca=&modelo=&ano=` | Público | Veículos (lista simples, filtros opcionais) |
| GET | `/tags` | Público | Tags dos filtros |
| POST | `/pecas` | Vendedor | Cadastra a peça como rascunho (só o título é obrigatório) ou publicada (exige categoria, OEM e preço). 201 com `Location` |
| PUT | `/pecas/{id}` | Vendedor (dono) | Edita a peça inteira. Publicar ou mudar o preço grava o histórico de preço |
| GET | `/pecas/{id}` | Público | Detalhe com as aplicações. Rascunho e pausado só para o dono |
| PUT | `/pecas/{id}/compatibilidades` | Vendedor (dono) | Substitui as aplicações (marca, modelo, ano inicial e final, versão) |
| GET | `/pecas/{id}/historico-preco` | Vendedor (dono) | `[{ data, precoAnterior, precoNovo }]`, da mudança mais recente para a publicação (`precoAnterior` nulo). Lista vazia se nunca foi publicada |

**Usuário administrador no perfil `dev`:** `admin@buscapeca.com.br` / `Admin@123`. Em produção, o e-mail e a senha vêm de `ADMIN_EMAIL` e `ADMIN_SENHA`, e nenhuma senha fica no repositório.

**Testar pelo Swagger:** chame `GET /auth/csrf` uma vez (o Swagger reenvia o token sozinho), depois `POST /auth/login`, e as rotas de "Logado" passam a funcionar.

**Dados de teste:** no perfil `dev`, a pasta `db/dev` carrega veículos e tags de exemplo (`V1000__dados_de_teste.sql`). Ela nunca roda em produção.

**Testes unitários:** não precisam de banco (usam Mockito). Ex.: `.\mvnw.cmd test "-Dtest=HistoricoPrecoServiceTest,PecaServiceTest"` no PowerShell. Rodar `test` sem `-Dtest` inclui o `contextLoads`, que precisa do MySQL ligado.

**Front-end:** abra o front em `http://localhost:5500` (não em `127.0.0.1:5500`), senão o navegador trata como outro site e não envia o cookie de login.

---

## Problemas comuns

| Sintoma | Causa provável | Solução |
|---|---|---|
| `java -version` mostra outra versão | Outro JDK no PATH | Ajuste o `JAVA_HOME` para o JDK 21 e reabra o terminal |
| `Communications link failure` ao iniciar | MySQL não está rodando | Abra o Docker Desktop e rode `docker compose up -d` |
| Porta 3306 já em uso | MySQL instalado na máquina | Pare o serviço MySQL local ou mude a porta no `docker-compose.yml` e no `.env` |
| Porta 8080 já em uso | Outra aplicação rodando | Feche a outra instância ou mude `server.port` no `application-dev.yml` |
| Getters e setters "não existem" | Lombok desativado | Ative *Annotation Processors* (passo 5) |
| `Validate failed: Migration checksum mismatch` | Uma migração já executada foi editada | Desfaça a edição e crie uma migração nova; em último caso, `docker compose down -v` apaga o banco local |
| `Schema-validation: missing table` | Entidade criada sem a migração | Crie a migração da tabela antes de rodar |
| `git push` recusado | Branch desatualizada | `git pull` na sua branch e envie de novo |

---

## Documentação de referência

- Busca Peça – Documentação do Back-end v2
- Busca Peça – Documentação do Banco de Dados (MySQL)
- ADRs do Busca Peça