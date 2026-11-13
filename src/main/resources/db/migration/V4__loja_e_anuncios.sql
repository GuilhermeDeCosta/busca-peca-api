-- V4: loja, anuncios de pecas e ligacoes da peca com o catalogo
-- Documentacao do Banco de Dados, Modulos 2 e 3 (ADR-007, ADR-008, ADR-010)

CREATE TABLE loja (
    id_loja                 BIGINT        NOT NULL AUTO_INCREMENT,
    id_usuario              BIGINT        NOT NULL,
    id_endereco             BIGINT        NOT NULL,
    nome_loja               VARCHAR(100)  NOT NULL,
    logo_url                VARCHAR(500),
    banner_url              VARCHAR(500),
    descricao               TEXT,
    avaliacao_media         DECIMAL(2,1)  NOT NULL DEFAULT 0,
    vendedor_verificado     BOOLEAN       NOT NULL DEFAULT FALSE,
    retirada_balcao         BOOLEAN       NOT NULL DEFAULT TRUE,
    entrega_rapida          BOOLEAN       NOT NULL DEFAULT FALSE,
    frete_gratis_raio_km    DECIMAL(5,1),
    aceita_faturamento_cnpj BOOLEAN       NOT NULL DEFAULT FALSE,
    ativa                   BOOLEAN       NOT NULL DEFAULT TRUE,
    atualizado_em           DATETIME      NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_loja PRIMARY KEY (id_loja),
    CONSTRAINT uk_loja_usuario UNIQUE (id_usuario),
    CONSTRAINT fk_loja_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario),
    CONSTRAINT fk_loja_endereco FOREIGN KEY (id_endereco) REFERENCES endereco (id_endereco),
    CONSTRAINT ck_loja_avaliacao CHECK (avaliacao_media BETWEEN 0 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE loja_metodo_pagamento (
    id_loja_metodo BIGINT      NOT NULL AUTO_INCREMENT,
    id_loja        BIGINT      NOT NULL,
    metodo         VARCHAR(20) NOT NULL,
    CONSTRAINT pk_loja_metodo_pagamento PRIMARY KEY (id_loja_metodo),
    CONSTRAINT uk_loja_metodo UNIQUE (id_loja, metodo),
    CONSTRAINT fk_loja_metodo_loja FOREIGN KEY (id_loja) REFERENCES loja (id_loja),
    CONSTRAINT ck_loja_metodo CHECK (metodo IN ('PIX', 'DINHEIRO', 'CARTAO_CREDITO', 'CARTAO_DEBITO',
        'BOLETO', 'FATURADO'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE peca (
    id_peca               BIGINT        NOT NULL AUTO_INCREMENT,
    id_loja               BIGINT        NOT NULL,
    id_categoria          INT           NOT NULL,
    codigo_oem            VARCHAR(50)   NOT NULL,
    codigo_ean            VARCHAR(14),
    codigo_sku            VARCHAR(50),
    titulo                VARCHAR(150)  NOT NULL,
    descricao             TEXT,
    preco                 DECIMAL(10,2) NOT NULL,
    quantidade_estoque    INT           NOT NULL DEFAULT 0,
    limite_alerta_estoque INT           NOT NULL DEFAULT 1,
    condicao              VARCHAR(20)   NOT NULL,
    origem                VARCHAR(20)   NOT NULL,
    pronta_entrega        BOOLEAN       NOT NULL DEFAULT TRUE,
    prazo_expedicao_dias  INT           NOT NULL DEFAULT 0,
    anuncio_ativo         BOOLEAN       NOT NULL DEFAULT TRUE,
    data_cadastro         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em         DATETIME      NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_peca PRIMARY KEY (id_peca),
    CONSTRAINT fk_peca_loja FOREIGN KEY (id_loja) REFERENCES loja (id_loja),
    CONSTRAINT fk_peca_categoria FOREIGN KEY (id_categoria) REFERENCES categoria (id_categoria),
    CONSTRAINT ck_peca_preco CHECK (preco > 0),
    CONSTRAINT ck_peca_estoque CHECK (quantidade_estoque >= 0),
    CONSTRAINT ck_peca_limite_alerta CHECK (limite_alerta_estoque >= 0),
    CONSTRAINT ck_peca_prazo CHECK (prazo_expedicao_dias >= 0),
    CONSTRAINT ck_peca_condicao CHECK (condicao IN ('NOVA', 'USADA', 'RECONDICIONADA')),
    CONSTRAINT ck_peca_origem CHECK (origem IN ('ORIGINAL', 'PARALELA'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE foto_peca (
    id_foto     BIGINT       NOT NULL AUTO_INCREMENT,
    id_peca     BIGINT       NOT NULL,
    url_imagem  VARCHAR(500) NOT NULL,
    ordem       TINYINT      NOT NULL,
    principal   BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_foto_peca PRIMARY KEY (id_foto),
    CONSTRAINT uk_foto_peca_ordem UNIQUE (id_peca, ordem),
    CONSTRAINT fk_foto_peca_peca FOREIGN KEY (id_peca) REFERENCES peca (id_peca) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE historico_preco (
    id_historico   BIGINT        NOT NULL AUTO_INCREMENT,
    id_peca        BIGINT        NOT NULL,
    preco_anterior DECIMAL(10,2) NOT NULL,
    preco_novo     DECIMAL(10,2) NOT NULL,
    data_alteracao DATETIME      NOT NULL,
    CONSTRAINT pk_historico_preco PRIMARY KEY (id_historico),
    CONSTRAINT fk_historico_preco_peca FOREIGN KEY (id_peca) REFERENCES peca (id_peca)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE promocao (
    id_promocao       BIGINT        NOT NULL AUTO_INCREMENT,
    id_peca           BIGINT        NOT NULL,
    preco_promocional DECIMAL(10,2) NOT NULL,
    data_inicio       DATETIME      NOT NULL,
    data_fim          DATETIME      NOT NULL,
    ativa             BOOLEAN       NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_promocao PRIMARY KEY (id_promocao),
    CONSTRAINT fk_promocao_peca FOREIGN KEY (id_peca) REFERENCES peca (id_peca),
    CONSTRAINT ck_promocao_preco CHECK (preco_promocional > 0),
    CONSTRAINT ck_promocao_datas CHECK (data_fim > data_inicio)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE metrica_anuncio (
    id_metrica      BIGINT NOT NULL AUTO_INCREMENT,
    id_peca         BIGINT NOT NULL,
    data_referencia DATE   NOT NULL,
    visualizacoes   INT    NOT NULL DEFAULT 0,
    acessos_detalhe INT    NOT NULL DEFAULT 0,
    interesses      INT    NOT NULL DEFAULT 0,
    CONSTRAINT pk_metrica_anuncio PRIMARY KEY (id_metrica),
    CONSTRAINT uk_metrica_peca_data UNIQUE (id_peca, data_referencia),
    CONSTRAINT fk_metrica_peca FOREIGN KEY (id_peca) REFERENCES peca (id_peca)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE compatibilidade (
    id_compatibilidade BIGINT NOT NULL AUTO_INCREMENT,
    id_peca            BIGINT NOT NULL,
    id_veiculo         BIGINT NOT NULL,
    CONSTRAINT pk_compatibilidade PRIMARY KEY (id_compatibilidade),
    CONSTRAINT uk_compatibilidade UNIQUE (id_peca, id_veiculo),
    CONSTRAINT fk_compatibilidade_peca FOREIGN KEY (id_peca)
        REFERENCES peca (id_peca) ON DELETE CASCADE,
    CONSTRAINT fk_compatibilidade_veiculo FOREIGN KEY (id_veiculo)
        REFERENCES veiculo (id_veiculo) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE peca_tag (
    id_peca BIGINT NOT NULL,
    id_tag  INT    NOT NULL,
    CONSTRAINT pk_peca_tag PRIMARY KEY (id_peca, id_tag),
    CONSTRAINT fk_peca_tag_peca FOREIGN KEY (id_peca) REFERENCES peca (id_peca) ON DELETE CASCADE,
    CONSTRAINT fk_peca_tag_tag FOREIGN KEY (id_tag) REFERENCES tag (id_tag) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE peca_equivalencia (
    id_equivalencia     BIGINT       NOT NULL AUTO_INCREMENT,
    id_peca             BIGINT       NOT NULL,
    id_peca_equivalente BIGINT       NOT NULL,
    tipo                VARCHAR(20)  NOT NULL,
    observacao          VARCHAR(255),
    CONSTRAINT pk_peca_equivalencia PRIMARY KEY (id_equivalencia),
    CONSTRAINT uk_peca_equivalencia UNIQUE (id_peca, id_peca_equivalente),
    CONSTRAINT fk_peca_equivalencia_peca FOREIGN KEY (id_peca) REFERENCES peca (id_peca),
    CONSTRAINT fk_peca_equivalencia_equivalente FOREIGN KEY (id_peca_equivalente)
        REFERENCES peca (id_peca),
    CONSTRAINT ck_peca_equivalencia_diferente CHECK (id_peca <> id_peca_equivalente),
    CONSTRAINT ck_peca_equivalencia_tipo CHECK (tipo IN ('EQUIVALENTE', 'SIMILAR'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
