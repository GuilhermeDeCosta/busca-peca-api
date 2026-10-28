<#
.SYNOPSIS
    Configura o projeto buscapeca-backend criado pelo IntelliJ (Spring Initializr, Maven).

.DESCRIPTION
    Rode este script UMA VEZ, dentro da pasta do projeto (onde fica o pom.xml).
    Ele:
      1. Confere se as dependências da documentação estão no pom.xml (e adiciona o flyway-mysql se faltar)
      2. Cria toda a estrutura de pastas por domínio (controller, service, repository...)
      3. Cria application.yml, application-dev.yml, application-prod.yml
      4. Cria docker-compose.yml, .env.exemplo, README.md e completa o .gitignore
      5. (Opcional) Faz o commit e publica no GitHub, se você passar -RepoUrl

    Não cria nenhum código Java.

.EXAMPLE
    .\configurar-backend.ps1
    Só configura o projeto.

.EXAMPLE
    .\configurar-backend.ps1 -RepoUrl https://github.com/SEU_USUARIO/buscapeca-backend.git
    Configura e publica no GitHub.

.EXAMPLE
    .\configurar-backend.ps1 -Forcar
    Sobrescreve os arquivos de configuração que já existirem.
#>
param(
    [string]$RepoUrl = "",
    [switch]$Forcar
)

$ErrorActionPreference = 'Stop'
$raiz = (Get-Location).Path
$utf8SemBom = New-Object System.Text.UTF8Encoding($false)

function Titulo($texto) {
    Write-Host ""
    Write-Host "== $texto ==" -ForegroundColor Cyan
}

function Salvar-Arquivo([string]$caminho, [string]$conteudo) {
    if ((Test-Path $caminho) -and -not $Forcar) {
        Write-Host "  ja existe, mantido: $caminho" -ForegroundColor DarkYellow
        return
    }
    $pasta = Split-Path $caminho -Parent
    if ($pasta -and -not (Test-Path $pasta)) {
        New-Item -ItemType Directory -Path $pasta -Force | Out-Null
    }
    $conteudo = $conteudo -replace "`r`n", "`n"
    [System.IO.File]::WriteAllText($caminho, $conteudo, $utf8SemBom)
    Write-Host "  criado: $caminho" -ForegroundColor Green
}

# ---------------------------------------------------------------------------
# 0. Verificações
# ---------------------------------------------------------------------------
Titulo "Verificando o projeto"

$pomPath = Join-Path $raiz 'pom.xml'
if (-not (Test-Path $pomPath)) {
    throw "pom.xml nao encontrado. Rode o script dentro da pasta do projeto (a mesma do pom.xml)."
}

$javaMain = Join-Path $raiz 'src\main\java'
$classePrincipal = Get-ChildItem -Path $javaMain -Recurse -Filter '*Application.java' |
    Where-Object { (Get-Content $_.FullName -Raw) -match '@SpringBootApplication' } |
    Select-Object -First 1

if (-not $classePrincipal) {
    throw "Nao encontrei a classe com @SpringBootApplication em src\main\java. O projeto foi criado pelo Spring Initializr?"
}

$pastaBase = $classePrincipal.DirectoryName
$caminhoPacote = $pastaBase.Substring($javaMain.Length).TrimStart('\', '/')
$pacote = $caminhoPacote -replace '[\\/]', '.'
$pastaTesteBase = Join-Path $raiz "src\test\java\$caminhoPacote"
$pastaResources = Join-Path $raiz 'src\main\resources'

Write-Host "  Classe principal: $($classePrincipal.Name)"
Write-Host "  Pacote base:      $pacote"
if ($pacote -ne 'br.com.buscapeca') {
    Write-Host "  AVISO: a documentacao define o pacote base 'br.com.buscapeca'." -ForegroundColor Yellow
    Write-Host "         O script vai usar o pacote detectado ($pacote)." -ForegroundColor Yellow
    Write-Host "         Para corrigir: no IntelliJ, clique com o botao direito no pacote > Refactor > Rename." -ForegroundColor Yellow
}

# ---------------------------------------------------------------------------
# 1. Dependências do pom.xml
# ---------------------------------------------------------------------------
Titulo "Conferindo dependencias do pom.xml"

$pom = [System.IO.File]::ReadAllText($pomPath)

$versaoBoot = 'desconhecida'
$m = [regex]::Match($pom, '<artifactId>spring-boot-starter-parent</artifactId>\s*<version>([^<]+)</version>')
if ($m.Success) { $versaoBoot = $m.Groups[1].Value }
$versaoJava = '21'
$m = [regex]::Match($pom, '<java.version>([^<]+)</java.version>')
if ($m.Success) { $versaoJava = $m.Groups[1].Value }

Write-Host "  Spring Boot: $versaoBoot | Java: $versaoJava"
if ($versaoJava -ne '21') {
    Write-Host "  AVISO: a documentacao pede Java 21." -ForegroundColor Yellow
}

$esperadas = [ordered]@{
    'Spring Web'             = 'spring-boot-starter-web(mvc)?</artifactId>'
    'Spring Data JPA'        = 'spring-boot-starter-data-jpa</artifactId>'
    'MySQL Driver'           = 'mysql-connector-j</artifactId>'
    'Flyway Migration'       = '(spring-boot-starter-flyway|flyway-core)</artifactId>'
    'Spring Security'        = 'spring-boot-starter-security</artifactId>'
    'OAuth2 Resource Server' = 'oauth2-resource-server</artifactId>'
    'Validation'             = 'spring-boot-starter-validation</artifactId>'
    'Java Mail Sender'       = 'spring-boot-starter-mail</artifactId>'
    'Spring Boot Actuator'   = 'spring-boot-starter-actuator</artifactId>'
    'Spring Boot DevTools'   = 'spring-boot-devtools</artifactId>'
    'Lombok'                 = '<artifactId>lombok</artifactId>'
}

$faltando = @()
foreach ($nome in $esperadas.Keys) {
    if ($pom -match $esperadas[$nome]) {
        Write-Host "  ok    $nome" -ForegroundColor Green
    } else {
        Write-Host "  FALTA $nome" -ForegroundColor Red
        $faltando += $nome
    }
}

if ($pom -notmatch '<artifactId>flyway-mysql</artifactId>') {
    $trecho = "<dependency>`n`t`t`t<groupId>org.flywaydb</groupId>`n`t`t`t<artifactId>flyway-mysql</artifactId>`n`t`t</dependency>`n`t"
    $indice = $pom.IndexOf('</dependencies>')
    if ($indice -ge 0) {
        $pom = $pom.Insert($indice, $trecho)
        [System.IO.File]::WriteAllText($pomPath, $pom, $utf8SemBom)
        Write-Host "  adicionado: flyway-mysql (modulo MySQL do Flyway)" -ForegroundColor Green
    }
} else {
    Write-Host "  ok    flyway-mysql" -ForegroundColor Green
}

# ---------------------------------------------------------------------------
# 2. Estrutura de pastas
# ---------------------------------------------------------------------------
Titulo "Criando estrutura de pastas"

$transversais = @('config', 'seguranca', 'compartilhado')
$modulos = @('usuario', 'loja', 'peca', 'catalogo', 'busca', 'carrinho', 'pedido',
             'comunicacao', 'posvenda', 'notificacao', 'administracao')
$camadas = @('controller', 'service', 'repository', 'entity', 'dto', 'mapper', 'enums')
$integracoes = @('codigopeca', 'frete', 'geolocalizacao', 'email', 'armazenamento')

$pastas = @()
foreach ($base in @($pastaBase, $pastaTesteBase)) {
    foreach ($t in $transversais) { $pastas += Join-Path $base $t }
    foreach ($mod in $modulos) {
        foreach ($c in $camadas) { $pastas += Join-Path $base "$mod\$c" }
    }
    foreach ($i in $integracoes) { $pastas += Join-Path $base "integracao\$i" }
}
$pastas += Join-Path $pastaResources 'db\migration'
$pastas += Join-Path $pastaResources 'templates\email'

$criadas = 0
foreach ($p in $pastas) {
    if (-not (Test-Path $p)) {
        New-Item -ItemType Directory -Path $p -Force | Out-Null
        $criadas++
    }
}
Write-Host "  $criadas pastas criadas" -ForegroundColor Green

# .gitkeep nas pastas vazias (o Git nao versiona pasta vazia)
$gitkeeps = 0
foreach ($p in $pastas) {
    if (@(Get-ChildItem -Path $p -Force).Count -eq 0) {
        New-Item -ItemType File -Path (Join-Path $p '.gitkeep') | Out-Null
        $gitkeeps++
    }
}
Write-Host "  $gitkeeps arquivos .gitkeep criados" -ForegroundColor Green

# ---------------------------------------------------------------------------
# 3. Configuração (application*.yml)
# ---------------------------------------------------------------------------
Titulo "Criando arquivos de configuracao"

$appProperties = Join-Path $pastaResources 'application.properties'
if (Test-Path $appProperties) {
    Remove-Item $appProperties
    Write-Host "  removido: application.properties (substituido pelos .yml)" -ForegroundColor DarkYellow
}

$helpMd = Join-Path $raiz 'HELP.md'
if (Test-Path $helpMd) {
    Remove-Item $helpMd
    Write-Host "  removido: HELP.md (substituido pelo README.md)" -ForegroundColor DarkYellow
}

Salvar-Arquivo (Join-Path $pastaResources 'application.yml') @'
# Configuracao comum aos perfis dev e prod
spring:
  application:
    name: buscapeca-backend
  profiles:
    default: dev
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: validate          # o esquema e criado so pelo Flyway
    properties:
      hibernate.jdbc.time_zone: America/Sao_Paulo
  flyway:
    enabled: true
    locations: classpath:db/migration
  servlet:
    multipart:
      max-file-size: 5MB
      max-request-size: 20MB
  jackson:
    time-zone: America/Sao_Paulo

management:
  endpoints:
    web:
      exposure:
        include: health
'@

Salvar-Arquivo (Join-Path $pastaResources 'application-dev.yml') @'
# Perfil de desenvolvimento local (MySQL do docker-compose.yml)
spring:
  datasource:
    url: ${DB_URL:jdbc:mysql://localhost:3306/buscapeca?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Sao_Paulo}
    username: ${DB_USUARIO:buscapeca}
    password: ${DB_SENHA:buscapeca}
  jpa:
    show-sql: true
    properties:
      hibernate.format_sql: true

app:
  front-origem: ${FRONT_ORIGEM:http://localhost:5500}
  cookie-secure: false
  upload-diretorio: ${UPLOAD_DIRETORIO:./uploads}
'@

Salvar-Arquivo (Join-Path $pastaResources 'application-prod.yml') @'
# Perfil de producao: tudo vem de variaveis de ambiente
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USUARIO}
    password: ${DB_SENHA}
  jpa:
    show-sql: false
  mail:
    host: ${EMAIL_HOST}
    username: ${EMAIL_USUARIO}
    password: ${EMAIL_SENHA}

app:
  front-origem: ${FRONT_ORIGEM}
  cookie-secure: true
  cookie-dominio: ${COOKIE_DOMINIO}
  upload-diretorio: ${UPLOAD_DIRETORIO}
'@

# ---------------------------------------------------------------------------
# 4. Arquivos da raiz
# ---------------------------------------------------------------------------
Titulo "Criando arquivos da raiz"

Salvar-Arquivo (Join-Path $raiz 'docker-compose.yml') @'
services:
  mysql:
    image: mysql:8.0
    container_name: buscapeca-mysql
    restart: unless-stopped
    environment:
      MYSQL_DATABASE: buscapeca
      MYSQL_USER: ${DB_USUARIO:-buscapeca}
      MYSQL_PASSWORD: ${DB_SENHA:-buscapeca}
      MYSQL_ROOT_PASSWORD: ${DB_ROOT_SENHA:-root}
    command:
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_0900_ai_ci
    ports:
      - "3306:3306"
    volumes:
      - buscapeca-mysql-dados:/var/lib/mysql

volumes:
  buscapeca-mysql-dados:
'@

Salvar-Arquivo (Join-Path $raiz '.env.exemplo') @'
# Copie para .env e preencha. O arquivo .env NUNCA vai para o GitHub.

# Banco
DB_URL=jdbc:mysql://localhost:3306/buscapeca?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Sao_Paulo
DB_USUARIO=buscapeca
DB_SENHA=buscapeca
DB_ROOT_SENHA=root

# JWT (ADR-006)
JWT_SEGREDO=troque-por-uma-chave-longa-e-aleatoria
JWT_EXPIRACAO_MINUTOS=120

# Front e cookie
FRONT_ORIGEM=http://localhost:5500
COOKIE_DOMINIO=

# E-mail
EMAIL_HOST=
EMAIL_USUARIO=
EMAIL_SENHA=

# Base externa de codigos de peca (ADR-002)
CODIGO_PECA_API_URL=
CODIGO_PECA_API_CHAVE=

# Imagens (ADR-010)
UPLOAD_DIRETORIO=./uploads
'@

# .gitignore: completa o que o IntelliJ gerou
$gitignorePath = Join-Path $raiz '.gitignore'
$linhasGitignore = @()
if (Test-Path $gitignorePath) { $linhasGitignore = Get-Content $gitignorePath }
$necessarias = @('target/', '.idea/', '*.iml', '.env', 'uploads/', '*.log', '.DS_Store', 'Thumbs.db')
$novas = $necessarias | Where-Object { $linhasGitignore -notcontains $_ }
if ($novas) {
    $texto = ""
    if (Test-Path $gitignorePath) { $texto = [System.IO.File]::ReadAllText($gitignorePath).TrimEnd() + "`n`n" }
    $texto += "### Busca Peca ###`n" + ($novas -join "`n") + "`n"
    [System.IO.File]::WriteAllText($gitignorePath, $texto, $utf8SemBom)
    Write-Host "  .gitignore atualizado: $($novas -join ', ')" -ForegroundColor Green
} else {
    Write-Host "  .gitignore ja estava completo" -ForegroundColor DarkYellow
}

# ---------------------------------------------------------------------------
# 5. README.md
# ---------------------------------------------------------------------------
$readme = @'
# Busca Peça – Back-end

API REST do **Busca Peça**, marketplace de autopeças. Aplicação única em Java com Spring Boot, organizada como **monolito modular** (ADR-003). É a única porta de acesso ao banco MySQL (ADR-001, ADR-004). O front-end fica em um repositório separado (ADR-015).

> **Estado atual:** apenas a estrutura do projeto. As pastas de cada módulo e camada já existem, mas ainda não há controllers, serviços, entidades nem migrações. As pastas vazias têm um arquivo `.gitkeep` só para o Git conseguir versioná-las; ele pode ser apagado quando a pasta receber o primeiro arquivo.

## Tecnologias

| Item | Escolha |
|---|---|
| Linguagem | Java {{JAVA}} (LTS) |
| Framework | Spring Boot {{BOOT}} |
| Build | Maven |
| Banco | MySQL 8 (InnoDB, `utf8mb4_0900_ai_ci`) |
| IDE | IntelliJ IDEA |
| Ambiente local | MySQL em Docker Compose + aplicação pelo IntelliJ em `localhost:8080` |
| Pacote base | `{{PACOTE}}` |

Dependências: Spring Web, Spring Data JPA, MySQL Driver, Flyway Migration (com `flyway-mysql`), Spring Security, OAuth2 Resource Server, Validation, Java Mail Sender, Spring Boot Actuator, Spring Boot DevTools e Lombok.

A adicionar depois: springdoc-openapi (Swagger), MapStruct, Resilience4j e Testcontainers.

## Pré-requisitos

- JDK {{JAVA}}
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
4. **Abrir no IntelliJ**: *File → Open* → pasta do projeto. O IntelliJ lê o `pom.xml` e baixa as dependências. Confira em *File → Project Structure → Project* que o SDK é o {{JAVA}}.
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
    │   ├── java/{{PASTA}}/
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
    └── test/java/{{PASTA}}/   mesma estrutura do main
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
'@

$readme = $readme.Replace('{{BOOT}}', $versaoBoot).Replace('{{JAVA}}', $versaoJava).Replace('{{PACOTE}}', $pacote).Replace('{{PASTA}}', ($caminhoPacote -replace '\\', '/'))
Salvar-Arquivo (Join-Path $raiz 'README.md') $readme

# ---------------------------------------------------------------------------
# 6. Git e GitHub (opcional)
# ---------------------------------------------------------------------------
if ($RepoUrl) {
    Titulo "Publicando no GitHub"

    if (-not (Get-Command git -ErrorAction SilentlyContinue)) {
        throw "Git nao encontrado. Instale em https://git-scm.com e rode o script de novo."
    }
    $email = git config user.email
    if (-not $email) {
        Write-Host "  Configure seu usuario do Git antes e rode de novo:" -ForegroundColor Red
        Write-Host '    git config --global user.name "Seu Nome"'
        Write-Host '    git config --global user.email "seu@email.com"'
        exit 1
    }

    if (-not (Test-Path (Join-Path $raiz '.git'))) { git init | Out-Null }
    git add .
    git commit -m "Estrutura inicial do back-end (Spring Boot $versaoBoot, Maven)" | Out-Null
    git branch -M main

    $remotos = @(git remote)
    if ($remotos -contains 'origin') { git remote set-url origin $RepoUrl }
    else { git remote add origin $RepoUrl }

    # Se o repositorio do GitHub ja tem arquivos (ex.: README criado pelo site), junta os historicos
    $remotoMain = git ls-remote --heads origin main
    if ($remotoMain) {
        Write-Host "  O repositorio remoto ja tem commits; juntando os historicos (vale a versao local em conflitos)..."
        git pull origin main --allow-unrelated-histories --no-edit -X ours
    }

    git push -u origin main
    if ($LASTEXITCODE -eq 0) {
        Write-Host "  Publicado em $RepoUrl" -ForegroundColor Green
    } else {
        Write-Host "  O push falhou. Veja a mensagem do Git acima." -ForegroundColor Red
    }
}

# ---------------------------------------------------------------------------
# Resumo
# ---------------------------------------------------------------------------
Titulo "Pronto"
if ($faltando.Count -gt 0) {
    Write-Host "  Dependencias faltando no pom.xml: $($faltando -join ', ')" -ForegroundColor Red
    Write-Host "  Adicione pelo IntelliJ (pom.xml > Alt+Insert > Add dependency) ou recrie o projeto marcando-as." -ForegroundColor Red
}
Write-Host "  Proximos passos:"
Write-Host "    1. No IntelliJ: clique em 'Reload All Maven Projects' (icone do Maven, a direita)"
Write-Host "    2. Copy-Item .env.exemplo .env"
Write-Host "    3. docker compose up -d"
Write-Host "    4. Rode a classe principal e abra http://localhost:8080/actuator/health"
if (-not $RepoUrl) {
    Write-Host ""
    Write-Host "  Para publicar no GitHub, rode de novo com:"
    Write-Host "    .\configurar-backend.ps1 -RepoUrl https://github.com/SEU_USUARIO/buscapeca-backend.git"
}
