-- V8: conversas, mensagens, avaliacoes, problemas, devolucoes e disputas
-- Documentacao do Banco de Dados, Modulo 5

CREATE TABLE conversa (
    id_conversa  BIGINT   NOT NULL AUTO_INCREMENT,
    id_comprador BIGINT   NOT NULL,
    id_loja      BIGINT   NOT NULL,
    id_peca      BIGINT,
    id_pedido    BIGINT,
    data_criacao DATETIME NOT NULL,
    CONSTRAINT pk_conversa PRIMARY KEY (id_conversa),
    CONSTRAINT fk_conversa_comprador FOREIGN KEY (id_comprador) REFERENCES usuario (id_usuario),
    CONSTRAINT fk_conversa_loja FOREIGN KEY (id_loja) REFERENCES loja (id_loja),
    CONSTRAINT fk_conversa_peca FOREIGN KEY (id_peca) REFERENCES peca (id_peca),
    CONSTRAINT fk_conversa_pedido FOREIGN KEY (id_pedido) REFERENCES pedido (id_pedido)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE mensagem (
    id_mensagem  BIGINT        NOT NULL AUTO_INCREMENT,
    id_conversa  BIGINT        NOT NULL,
    id_remetente BIGINT        NOT NULL,
    conteudo     VARCHAR(2000) NOT NULL,
    data_envio   DATETIME      NOT NULL,
    lida         BOOLEAN       NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_mensagem PRIMARY KEY (id_mensagem),
    CONSTRAINT fk_mensagem_conversa FOREIGN KEY (id_conversa) REFERENCES conversa (id_conversa),
    CONSTRAINT fk_mensagem_remetente FOREIGN KEY (id_remetente) REFERENCES usuario (id_usuario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX ix_mensagem_conversa_data ON mensagem (id_conversa, data_envio);

CREATE TABLE avaliacao (
    id_avaliacao         BIGINT        NOT NULL AUTO_INCREMENT,
    id_pedido            BIGINT        NOT NULL,
    id_usuario_avaliador BIGINT        NOT NULL,
    id_loja              BIGINT        NOT NULL,
    nota                 TINYINT       NOT NULL,
    comentario           VARCHAR(1000),
    data_avaliacao       DATETIME      NOT NULL,
    CONSTRAINT pk_avaliacao PRIMARY KEY (id_avaliacao),
    CONSTRAINT uk_avaliacao_pedido UNIQUE (id_pedido),
    CONSTRAINT fk_avaliacao_pedido FOREIGN KEY (id_pedido) REFERENCES pedido (id_pedido),
    CONSTRAINT fk_avaliacao_usuario FOREIGN KEY (id_usuario_avaliador) REFERENCES usuario (id_usuario),
    CONSTRAINT fk_avaliacao_loja FOREIGN KEY (id_loja) REFERENCES loja (id_loja),
    CONSTRAINT ck_avaliacao_nota CHECK (nota BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE problema_pedido (
    id_problema    BIGINT      NOT NULL AUTO_INCREMENT,
    id_pedido      BIGINT      NOT NULL,
    id_item_pedido BIGINT,
    tipo_problema  VARCHAR(30) NOT NULL,
    descricao      TEXT        NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'ABERTO',
    data_registro  DATETIME    NOT NULL,
    CONSTRAINT pk_problema_pedido PRIMARY KEY (id_problema),
    CONSTRAINT fk_problema_pedido FOREIGN KEY (id_pedido) REFERENCES pedido (id_pedido),
    CONSTRAINT fk_problema_item FOREIGN KEY (id_item_pedido) REFERENCES item_pedido (id_item_pedido),
    CONSTRAINT ck_problema_tipo CHECK (tipo_problema IN ('PECA_ERRADA', 'DEFEITO', 'AVARIA',
        'NAO_ENTREGUE', 'OUTRO')),
    CONSTRAINT ck_problema_status CHECK (status IN ('ABERTO', 'EM_ANALISE', 'RESOLVIDO'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE anexo_problema (
    id_anexo    BIGINT       NOT NULL AUTO_INCREMENT,
    id_problema BIGINT       NOT NULL,
    url_arquivo VARCHAR(500) NOT NULL,
    CONSTRAINT pk_anexo_problema PRIMARY KEY (id_anexo),
    CONSTRAINT fk_anexo_problema FOREIGN KEY (id_problema)
        REFERENCES problema_pedido (id_problema) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE devolucao (
    id_devolucao     BIGINT       NOT NULL AUTO_INCREMENT,
    id_pedido        BIGINT       NOT NULL,
    id_problema      BIGINT,
    motivo           VARCHAR(255) NOT NULL,
    status           VARCHAR(20)  NOT NULL DEFAULT 'SOLICITADA',
    data_solicitacao DATETIME     NOT NULL,
    data_resolucao   DATETIME,
    CONSTRAINT pk_devolucao PRIMARY KEY (id_devolucao),
    CONSTRAINT fk_devolucao_pedido FOREIGN KEY (id_pedido) REFERENCES pedido (id_pedido),
    CONSTRAINT fk_devolucao_problema FOREIGN KEY (id_problema) REFERENCES problema_pedido (id_problema),
    CONSTRAINT ck_devolucao_status CHECK (status IN ('SOLICITADA', 'APROVADA', 'RECUSADA', 'CONCLUIDA'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE disputa (
    id_disputa          BIGINT      NOT NULL AUTO_INCREMENT,
    id_pedido           BIGINT      NOT NULL,
    id_problema         BIGINT,
    id_usuario_abertura BIGINT      NOT NULL,
    id_moderador        BIGINT,
    status              VARCHAR(20) NOT NULL DEFAULT 'ABERTA',
    decisao             TEXT,
    data_abertura       DATETIME    NOT NULL,
    data_fechamento     DATETIME,
    CONSTRAINT pk_disputa PRIMARY KEY (id_disputa),
    CONSTRAINT fk_disputa_pedido FOREIGN KEY (id_pedido) REFERENCES pedido (id_pedido),
    CONSTRAINT fk_disputa_problema FOREIGN KEY (id_problema) REFERENCES problema_pedido (id_problema),
    CONSTRAINT fk_disputa_usuario FOREIGN KEY (id_usuario_abertura) REFERENCES usuario (id_usuario),
    CONSTRAINT fk_disputa_moderador FOREIGN KEY (id_moderador) REFERENCES usuario (id_usuario),
    CONSTRAINT ck_disputa_status CHECK (status IN ('ABERTA', 'EM_MEDIACAO', 'ENCERRADA'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
