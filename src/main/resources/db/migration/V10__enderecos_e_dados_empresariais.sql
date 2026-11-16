-- V10: apelido do endereco e dados empresariais do perfil
-- BP1-134 (Meu perfil, dados empresariais e enderecos)
-- Campos que o perfil.js do front ja envia

ALTER TABLE endereco
    ADD COLUMN identificacao VARCHAR(40) NULL AFTER id_usuario;

ALTER TABLE usuario
    ADD COLUMN razao_social       VARCHAR(150) NULL AFTER cnpj_cpf,
    ADD COLUMN inscricao_estadual VARCHAR(20)  NULL AFTER razao_social;