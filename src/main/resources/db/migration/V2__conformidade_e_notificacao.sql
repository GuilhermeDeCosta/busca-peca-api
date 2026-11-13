-- V2: termos de uso, aceite, log de auditoria e notificacoes
-- Documentacao do Banco de Dados, Modulo 1 (ADR-013)

CREATE TABLE termo_uso (
    id_termo            INT          NOT NULL AUTO_INCREMENT,
    id_admin_publicador BIGINT       NOT NULL,
    tipo                VARCHAR(30)  NOT NULL,
    versao              VARCHAR(10)  NOT NULL,
    conteudo            MEDIUMTEXT   NOT NULL,
    data_publicacao     DATETIME     NOT NULL,
    vigente             BOOLEAN      NOT NULL,
    CONSTRAINT pk_termo_uso PRIMARY KEY (id_termo),
    CONSTRAINT uk_termo_uso_tipo_versao UNIQUE (tipo, versao),
    CONSTRAINT fk_termo_uso_admin FOREIGN KEY (id_admin_publicador)
        REFERENCES usuario (id_usuario),
    CONSTRAINT ck_termo_uso_tipo CHECK (tipo IN ('TERMOS_USO', 'POLITICA_PRIVACIDADE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE aceite_termo (
    id_aceite   BIGINT   NOT NULL AUTO_INCREMENT,
    id_usuario  BIGINT   NOT NULL,
    id_termo    INT      NOT NULL,
    data_aceite DATETIME NOT NULL,
    CONSTRAINT pk_aceite_termo PRIMARY KEY (id_aceite),
    CONSTRAINT uk_aceite_termo_usuario_termo UNIQUE (id_usuario, id_termo),
    CONSTRAINT fk_aceite_termo_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuario (id_usuario),
    CONSTRAINT fk_aceite_termo_termo FOREIGN KEY (id_termo)
        REFERENCES termo_uso (id_termo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE log_auditoria (
    id_log                   BIGINT      NOT NULL AUTO_INCREMENT,
    id_usuario               BIGINT,
    acao                     VARCHAR(50) NOT NULL,
    entidade                 VARCHAR(50) NOT NULL,
    parametros_anonimizados  JSON,
    data_registro            DATETIME    NOT NULL,
    CONSTRAINT pk_log_auditoria PRIMARY KEY (id_log),
    CONSTRAINT fk_log_auditoria_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuario (id_usuario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE notificacao (
    id_notificacao  BIGINT       NOT NULL AUTO_INCREMENT,
    id_usuario      BIGINT       NOT NULL,
    id_admin_origem BIGINT,
    tipo            VARCHAR(30)  NOT NULL,
    canal           VARCHAR(20)  NOT NULL,
    mensagem        VARCHAR(500) NOT NULL,
    lida            BOOLEAN      NOT NULL DEFAULT FALSE,
    data_envio      DATETIME     NOT NULL,
    CONSTRAINT pk_notificacao PRIMARY KEY (id_notificacao),
    CONSTRAINT fk_notificacao_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuario (id_usuario),
    CONSTRAINT fk_notificacao_admin FOREIGN KEY (id_admin_origem)
        REFERENCES usuario (id_usuario),
    CONSTRAINT ck_notificacao_tipo CHECK (tipo IN ('NOVO_PEDIDO', 'STATUS_PEDIDO', 'ESTOQUE_BAIXO',
        'PECA_DISPONIVEL', 'NOVA_MENSAGEM', 'AVISO_ADMIN')),
    CONSTRAINT ck_notificacao_canal CHECK (canal IN ('SISTEMA', 'EMAIL'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
