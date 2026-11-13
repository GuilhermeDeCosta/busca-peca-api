-- V3: catalogo tecnico (categorias, veiculos e tags)
-- Documentacao do Banco de Dados, Modulo 3

CREATE TABLE categoria (
    id_categoria     INT         NOT NULL AUTO_INCREMENT,
    id_categoria_pai INT,
    nome             VARCHAR(80) NOT NULL,
    CONSTRAINT pk_categoria PRIMARY KEY (id_categoria),
    CONSTRAINT uk_categoria_pai_nome UNIQUE (id_categoria_pai, nome),
    CONSTRAINT fk_categoria_pai FOREIGN KEY (id_categoria_pai)
        REFERENCES categoria (id_categoria)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE veiculo (
    id_veiculo BIGINT      NOT NULL AUTO_INCREMENT,
    marca      VARCHAR(50) NOT NULL,
    modelo     VARCHAR(80) NOT NULL,
    ano        SMALLINT    NOT NULL,
    versao     VARCHAR(80),
    CONSTRAINT pk_veiculo PRIMARY KEY (id_veiculo),
    CONSTRAINT uk_veiculo UNIQUE (marca, modelo, ano, versao),
    CONSTRAINT ck_veiculo_ano CHECK (ano BETWEEN 1900 AND 2100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE tag (
    id_tag INT         NOT NULL AUTO_INCREMENT,
    nome   VARCHAR(50) NOT NULL,
    CONSTRAINT pk_tag PRIMARY KEY (id_tag),
    CONSTRAINT uk_tag_nome UNIQUE (nome)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
