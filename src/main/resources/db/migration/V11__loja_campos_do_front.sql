-- V11: campos da loja que as telas criar-loja e configuracoes usam
-- BP1-219 (back-end da BP1-137, criar loja e habilitar papel de vendedor)

ALTER TABLE loja
    ADD COLUMN telefone                   VARCHAR(20)   NULL AFTER descricao,
    ADD COLUMN horario_retirada           VARCHAR(100)  NULL AFTER retirada_balcao,
    ADD COLUMN entrega_combinar           BOOLEAN       NOT NULL DEFAULT TRUE AFTER entrega_rapida,
    ADD COLUMN motoboy_proprio            BOOLEAN       NOT NULL DEFAULT FALSE AFTER entrega_rapida,
    ADD COLUMN pedido_minimo_frete_gratis DECIMAL(10,2) NULL AFTER frete_gratis_raio_km,
    ADD CONSTRAINT ck_loja_pedido_minimo CHECK (pedido_minimo_frete_gratis IS NULL OR pedido_minimo_frete_gratis >= 0);