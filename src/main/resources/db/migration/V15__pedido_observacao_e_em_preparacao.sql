-- V15: recado do comprador no pedido e status EM_PREPARACAO (BP1-234)
-- O checkout do front manda uma observacao por loja; o Fluxo C (Minha Loja) usa o
-- status EM_PREPARACAO entre ACEITO e PRONTO_RETIRADA / EM_ENTREGA.

ALTER TABLE pedido ADD COLUMN observacao VARCHAR(500) NULL AFTER modalidade_entrega;

ALTER TABLE pedido DROP CHECK ck_pedido_status;

ALTER TABLE pedido ADD CONSTRAINT ck_pedido_status CHECK (status IN ('AGUARDANDO_ACEITE', 'ACEITO',
    'RECUSADO', 'EM_PREPARACAO', 'PRONTO_RETIRADA', 'EM_ENTREGA', 'CONCLUIDO', 'CANCELADO'));
