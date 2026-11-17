-- V13: histórico de preço com a linha da publicação (BP1-53)
-- A V12 deixou peca.preco opcional (o rascunho pode não ter preço). Ao publicar, a primeira
-- linha do histórico não tem preço anterior: o front (obterHistoricoPreco) espera
-- precoAnterior nulo nessa linha. As mudanças seguintes gravam anterior e novo.
-- Esta migração só mexe em historico_preco (V4) e não depende das colunas da V12.

ALTER TABLE historico_preco MODIFY COLUMN preco_anterior DECIMAL(10,2) NULL;

-- Último preço e lista do histórico, da mais recente para a mais antiga.
-- Começa por id_peca, então também atende a FK; o MySQL apaga sozinho o índice
-- que tinha criado para fk_historico_preco_peca.
CREATE INDEX ix_historico_preco_peca_data ON historico_preco (id_peca, data_alteracao);
