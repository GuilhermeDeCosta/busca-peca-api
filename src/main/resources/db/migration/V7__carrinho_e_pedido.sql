-- V7: carrinho, compra, pedidos por loja, itens, entrega e recibo
-- Documentacao do Banco de Dados, Modulo 4 (ADR-008, ADR-009)

CREATE TABLE carrinho (
    id_carrinho   BIGINT   NOT NULL AUTO_INCREMENT,
    id_usuario    BIGINT   NOT NULL,
    atualizado_em DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_carrinho PRIMARY KEY (id_carrinho),
    CONSTRAINT uk_carrinho_usuario UNIQUE (id_usuario),
    CONSTRAINT fk_carrinho_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE item_carrinho (
    id_item_carrinho BIGINT NOT NULL AUTO_INCREMENT,
    id_carrinho      BIGINT NOT NULL,
    id_peca          BIGINT NOT NULL,
    quantidade       INT    NOT NULL,
    CONSTRAINT pk_item_carrinho PRIMARY KEY (id_item_carrinho),
    CONSTRAINT uk_item_carrinho_peca UNIQUE (id_carrinho, id_peca),
    CONSTRAINT fk_item_carrinho_carrinho FOREIGN KEY (id_carrinho)
        REFERENCES carrinho (id_carrinho) ON DELETE CASCADE,
    CONSTRAINT fk_item_carrinho_peca FOREIGN KEY (id_peca) REFERENCES peca (id_peca),
    CONSTRAINT ck_item_carrinho_quantidade CHECK (quantidade > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE compra (
    id_compra           BIGINT        NOT NULL AUTO_INCREMENT,
    id_comprador        BIGINT        NOT NULL,
    id_endereco_entrega BIGINT,
    valor_total         DECIMAL(12,2) NOT NULL,
    data_compra         DATETIME      NOT NULL,
    CONSTRAINT pk_compra PRIMARY KEY (id_compra),
    CONSTRAINT fk_compra_comprador FOREIGN KEY (id_comprador) REFERENCES usuario (id_usuario),
    CONSTRAINT fk_compra_endereco FOREIGN KEY (id_endereco_entrega) REFERENCES endereco (id_endereco),
    CONSTRAINT ck_compra_valor CHECK (valor_total >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE pedido (
    id_pedido           BIGINT        NOT NULL AUTO_INCREMENT,
    id_compra           BIGINT        NOT NULL,
    id_loja             BIGINT        NOT NULL,
    valor_itens         DECIMAL(10,2) NOT NULL,
    valor_frete         DECIMAL(10,2) NOT NULL DEFAULT 0,
    modalidade_entrega  VARCHAR(20)   NOT NULL,
    status              VARCHAR(30)   NOT NULL DEFAULT 'AGUARDANDO_ACEITE',
    motivo_recusa       VARCHAR(255),
    motivo_cancelamento VARCHAR(255),
    codigo_retirada     VARCHAR(8)    NOT NULL,
    data_pedido         DATETIME      NOT NULL,
    atualizado_em       DATETIME      NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_pedido PRIMARY KEY (id_pedido),
    CONSTRAINT uk_pedido_codigo_retirada UNIQUE (codigo_retirada),
    CONSTRAINT fk_pedido_compra FOREIGN KEY (id_compra) REFERENCES compra (id_compra),
    CONSTRAINT fk_pedido_loja FOREIGN KEY (id_loja) REFERENCES loja (id_loja),
    CONSTRAINT ck_pedido_valores CHECK (valor_itens >= 0 AND valor_frete >= 0),
    CONSTRAINT ck_pedido_modalidade CHECK (modalidade_entrega IN ('RETIRADA', 'ENTREGA')),
    CONSTRAINT ck_pedido_status CHECK (status IN ('AGUARDANDO_ACEITE', 'ACEITO', 'RECUSADO',
        'PRONTO_RETIRADA', 'EM_ENTREGA', 'CONCLUIDO', 'CANCELADO'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX ix_pedido_loja_status ON pedido (id_loja, status);

CREATE TABLE item_pedido (
    id_item_pedido BIGINT        NOT NULL AUTO_INCREMENT,
    id_pedido      BIGINT        NOT NULL,
    id_peca        BIGINT        NOT NULL,
    quantidade     INT           NOT NULL,
    preco_unitario DECIMAL(10,2) NOT NULL,
    CONSTRAINT pk_item_pedido PRIMARY KEY (id_item_pedido),
    CONSTRAINT fk_item_pedido_pedido FOREIGN KEY (id_pedido) REFERENCES pedido (id_pedido),
    CONSTRAINT fk_item_pedido_peca FOREIGN KEY (id_peca) REFERENCES peca (id_peca),
    CONSTRAINT ck_item_pedido_quantidade CHECK (quantidade > 0),
    CONSTRAINT ck_item_pedido_preco CHECK (preco_unitario > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE entrega (
    id_entrega       BIGINT        NOT NULL AUTO_INCREMENT,
    id_pedido        BIGINT        NOT NULL,
    modalidade       VARCHAR(20)   NOT NULL,
    transportadora   VARCHAR(80),
    codigo_rastreio  VARCHAR(50),
    status_entrega   VARCHAR(20)   NOT NULL,
    latitude_atual   DECIMAL(10,7),
    longitude_atual  DECIMAL(10,7),
    previsao_entrega DATETIME,
    atualizada_em    DATETIME      NOT NULL,
    CONSTRAINT pk_entrega PRIMARY KEY (id_entrega),
    CONSTRAINT uk_entrega_pedido UNIQUE (id_pedido),
    CONSTRAINT fk_entrega_pedido FOREIGN KEY (id_pedido) REFERENCES pedido (id_pedido),
    CONSTRAINT ck_entrega_modalidade CHECK (modalidade IN ('PROPRIA', 'TRANSPORTADORA', 'MOTOBOY')),
    CONSTRAINT ck_entrega_status CHECK (status_entrega IN ('PREPARANDO', 'EM_TRANSITO', 'ENTREGUE', 'FALHOU'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE recibo (
    id_recibo    BIGINT       NOT NULL AUTO_INCREMENT,
    id_pedido    BIGINT       NOT NULL,
    numero       VARCHAR(20)  NOT NULL,
    url_pdf      VARCHAR(500) NOT NULL,
    data_emissao DATETIME     NOT NULL,
    CONSTRAINT pk_recibo PRIMARY KEY (id_recibo),
    CONSTRAINT uk_recibo_pedido UNIQUE (id_pedido),
    CONSTRAINT uk_recibo_numero UNIQUE (numero),
    CONSTRAINT fk_recibo_pedido FOREIGN KEY (id_pedido) REFERENCES pedido (id_pedido)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
