-- V5: indices de busca (ADR-011)

CREATE FULLTEXT INDEX ft_peca_titulo_descricao ON peca (titulo, descricao);
CREATE INDEX ix_peca_codigo_oem ON peca (codigo_oem);
CREATE INDEX ix_peca_codigo_ean ON peca (codigo_ean);
CREATE INDEX ix_peca_codigo_sku ON peca (codigo_sku);
CREATE INDEX ix_peca_categoria_ativo ON peca (id_categoria, anuncio_ativo);
CREATE INDEX ix_veiculo_marca_modelo_ano ON veiculo (marca, modelo, ano);
