-- Modelo de peça usado pelo front (form-peca.js), decidido em 16/11/2026:
-- origem NACIONAL ou IMPORTADA, status no lugar de anuncio_ativo, marca e características.
-- O rascunho só exige o título, então categoria, código OEM e preço ficam opcionais aqui;
-- a API exige esses campos para publicar.

ALTER TABLE peca DROP CHECK ck_peca_origem;

UPDATE peca SET origem = IF(origem = 'ORIGINAL', 'NACIONAL', 'IMPORTADA');

ALTER TABLE peca
    MODIFY COLUMN id_categoria INT NULL,
    MODIFY COLUMN codigo_oem VARCHAR(50) NULL,
    MODIFY COLUMN preco DECIMAL(10,2) NULL,
    ADD COLUMN marca_peca VARCHAR(60) NULL AFTER descricao,
    ADD COLUMN caracteristicas VARCHAR(255) NULL AFTER marca_peca,
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'RASCUNHO' AFTER prazo_expedicao_dias,
    ADD CONSTRAINT ck_peca_origem CHECK (origem IN ('NACIONAL', 'IMPORTADA')),
    ADD CONSTRAINT ck_peca_status CHECK (status IN ('RASCUNHO', 'ATIVO', 'PAUSADO', 'EXCLUIDO'));

UPDATE peca SET status = IF(anuncio_ativo, 'ATIVO', 'PAUSADO');

-- O índice novo vem antes de apagar o antigo: o MySQL usa o antigo para a FK de categoria
CREATE INDEX ix_peca_categoria_status ON peca (id_categoria, status);
DROP INDEX ix_peca_categoria_ativo ON peca;

ALTER TABLE peca DROP COLUMN anuncio_ativo;
