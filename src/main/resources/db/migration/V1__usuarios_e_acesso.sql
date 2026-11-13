-- V1: usuarios, papeis, permissoes, enderecos e tokens
-- Documentacao do Banco de Dados, Modulo 1 (ADR-004, ADR-006, ADR-007)

CREATE TABLE usuario (
    id_usuario            BIGINT       NOT NULL AUTO_INCREMENT,
    nome                  VARCHAR(150) NOT NULL,
    nome_exibicao         VARCHAR(80),
    email                 VARCHAR(150) NOT NULL,
    senha_hash            VARCHAR(100) NOT NULL,
    tipo_pessoa           VARCHAR(2)   NOT NULL,
    cnpj_cpf              VARCHAR(14),
    telefone              VARCHAR(20),
    foto_url              VARCHAR(500),
    descricao             VARCHAR(500),
    identidade_verificada BOOLEAN      NOT NULL DEFAULT FALSE,
    status                VARCHAR(20)  NOT NULL DEFAULT 'ATIVO',
    data_cadastro         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em         DATETIME     NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_usuario PRIMARY KEY (id_usuario),
    CONSTRAINT uk_usuario_email UNIQUE (email),
    CONSTRAINT uk_usuario_cnpj_cpf UNIQUE (cnpj_cpf),
    CONSTRAINT ck_usuario_tipo_pessoa CHECK (tipo_pessoa IN ('PF', 'PJ')),
    CONSTRAINT ck_usuario_status CHECK (status IN ('ATIVO', 'INATIVO', 'BLOQUEADO'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE papel (
    id_papel  INT          NOT NULL AUTO_INCREMENT,
    nome      VARCHAR(30)  NOT NULL,
    descricao VARCHAR(200),
    interno   BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_papel PRIMARY KEY (id_papel),
    CONSTRAINT uk_papel_nome UNIQUE (nome)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE permissao (
    id_permissao INT          NOT NULL AUTO_INCREMENT,
    codigo       VARCHAR(50)  NOT NULL,
    descricao    VARCHAR(200),
    CONSTRAINT pk_permissao PRIMARY KEY (id_permissao),
    CONSTRAINT uk_permissao_codigo UNIQUE (codigo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE usuario_papel (
    id_usuario      BIGINT   NOT NULL,
    id_papel        INT      NOT NULL,
    data_atribuicao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_usuario_papel PRIMARY KEY (id_usuario, id_papel),
    CONSTRAINT fk_usuario_papel_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuario (id_usuario) ON DELETE CASCADE,
    CONSTRAINT fk_usuario_papel_papel FOREIGN KEY (id_papel)
        REFERENCES papel (id_papel) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE papel_permissao (
    id_papel     INT NOT NULL,
    id_permissao INT NOT NULL,
    CONSTRAINT pk_papel_permissao PRIMARY KEY (id_papel, id_permissao),
    CONSTRAINT fk_papel_permissao_papel FOREIGN KEY (id_papel)
        REFERENCES papel (id_papel) ON DELETE CASCADE,
    CONSTRAINT fk_papel_permissao_permissao FOREIGN KEY (id_permissao)
        REFERENCES permissao (id_permissao) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE endereco (
    id_endereco BIGINT        NOT NULL AUTO_INCREMENT,
    id_usuario  BIGINT        NOT NULL,
    logradouro  VARCHAR(150)  NOT NULL,
    numero      VARCHAR(10)   NOT NULL,
    complemento VARCHAR(80),
    bairro      VARCHAR(80)   NOT NULL,
    cidade      VARCHAR(80)   NOT NULL,
    estado      CHAR(2)       NOT NULL,
    cep         CHAR(8)       NOT NULL,
    latitude    DECIMAL(10,7),
    longitude   DECIMAL(10,7),
    principal   BOOLEAN       NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_endereco PRIMARY KEY (id_endereco),
    CONSTRAINT fk_endereco_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuario (id_usuario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE token_verificacao (
    id_token       BIGINT       NOT NULL AUTO_INCREMENT,
    id_usuario     BIGINT       NOT NULL,
    tipo           VARCHAR(30)  NOT NULL,
    token_hash     VARCHAR(100) NOT NULL,
    data_expiracao DATETIME     NOT NULL,
    usado          BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_token_verificacao PRIMARY KEY (id_token),
    CONSTRAINT uk_token_verificacao_hash UNIQUE (token_hash),
    CONSTRAINT fk_token_verificacao_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuario (id_usuario),
    CONSTRAINT ck_token_verificacao_tipo
        CHECK (tipo IN ('RECUPERAR_SENHA', 'CONFIRMAR_EMAIL', 'VERIFICAR_IDENTIDADE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
