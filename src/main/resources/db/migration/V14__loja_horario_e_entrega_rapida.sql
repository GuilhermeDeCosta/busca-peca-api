-- V14: dois ajustes nas colunas de loja criadas na V11.
--
-- 1. horario_retirada: a tela de configuracoes aceita ate 120 caracteres
--    (maxlength="120"); com 100, um texto maior dava erro 500 ao salvar.
ALTER TABLE loja MODIFY COLUMN horario_retirada VARCHAR(120) NULL;

-- 2. motoboy_proprio e entrega_rapida sao a mesma opcao da tela: "Motoboy proprio"
--    em criar-loja e "Motoboy disponivel para entrega rapida" em configuracoes.
--    Fica so entrega_rapida (da V4). A API continua devolvendo os dois nomes no JSON
--    (motoboyProprio e entregaRapida) com o mesmo valor, para nao quebrar o front.
UPDATE loja SET entrega_rapida = TRUE WHERE motoboy_proprio = TRUE;

ALTER TABLE loja DROP COLUMN motoboy_proprio;
