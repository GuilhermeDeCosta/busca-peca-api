-- V6: buscas salvas, favoritos e avisos de disponibilidade
-- Documentacao do Banco de Dados, Modulo 3

CREATE TABLE busca_salva (
    id_busca      BIGINT      NOT NULL AUTO_INCREMENT,
    id_usuario    BIGINT      NOT NULL,
    id_veiculo    BIGINT,
    nome          VARCHAR(80) NOT NULL,
    tipo          VARCHAR(20) NOT NULL,
    termos_filtro JSON,
    data_criacao  DATETIME    NOT NULL,
    CONSTRAINT pk_busca_salva PRIMARY KEY (id_busca),
    CONSTRAINT fk_busca_salva_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario),
    CONSTRAINT fk_busca_salva_veiculo FOREIGN KEY (id_veiculo) REFERENCES veiculo (id_veiculo),
    CONSTRAINT ck_busca_salva_tipo CHECK (tipo IN ('BUSCA', 'VEICULO'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE favorito (
    id_favorito   BIGINT   NOT NULL AUTO_INCREMENT,
    id_usuario    BIGINT   NOT NULL,
    id_peca       BIGINT   NOT NULL,
    data_favorito DATETIME NOT NULL,
    CONSTRAINT pk_favorito PRIMARY KEY (id_favorito),
    CONSTRAINT uk_favorito_usuario_peca UNIQUE (id_usuario, id_peca),
    CONSTRAINT fk_favorito_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario),
    CONSTRAINT fk_favorito_peca FOREIGN KEY (id_peca) REFERENCES peca (id_peca)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE aviso_disponibilidade (
    id_aviso     BIGINT       NOT NULL AUTO_INCREMENT,
    id_usuario   BIGINT       NOT NULL,
    id_peca      BIGINT,
    codigo_oem   VARCHAR(50),
    termo        VARCHAR(150),
    notificado   BOOLEAN      NOT NULL DEFAULT FALSE,
    data_criacao DATETIME     NOT NULL,
    CONSTRAINT pk_aviso_disponibilidade PRIMARY KEY (id_aviso),
    CONSTRAINT fk_aviso_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario),
    CONSTRAINT fk_aviso_peca FOREIGN KEY (id_peca) REFERENCES peca (id_peca),
    CONSTRAINT ck_aviso_alvo CHECK (id_peca IS NOT NULL OR codigo_oem IS NOT NULL OR termo IS NOT NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
