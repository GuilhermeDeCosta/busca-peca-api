-- V9: dados iniciais que o sistema precisa desde o primeiro acesso
-- Documentacao do Banco de Dados, "Dados iniciais"
--
-- O administrador inicial e criado com o e-mail do placeholder ${admin_email}
-- (variavel ADMIN_EMAIL) e SEM senha utilizavel. A senha e definida na primeira
-- subida da aplicacao pela variavel ADMIN_SENHA (classe InicializadorAdmin).
-- Nenhuma senha fica no repositorio.

-- Papeis (ADR-007)
INSERT INTO papel (nome, descricao, interno) VALUES
    ('COMPRADOR', 'Toda conta nasce com este papel', FALSE),
    ('VENDEDOR', 'Ativado ao criar a loja (BP1-67)', FALSE),
    ('ADMINISTRADOR', 'Papel interno, atribuido so por outro administrador', TRUE);

-- Permissoes administrativas (ADR-006)
INSERT INTO permissao (codigo, descricao) VALUES
    ('GERENCIAR_USUARIOS', 'Listar contas e alterar o status (BP1-49)'),
    ('PUBLICAR_TERMOS', 'Criar e publicar versoes dos termos (BP1-54)'),
    ('ENVIAR_NOTIFICACOES', 'Enviar notificacoes a grupos de usuarios (BP1-51)'),
    ('GERENCIAR_PERMISSOES', 'Atribuir papeis e permissoes; consultar auditoria (BP1-56)'),
    ('MODERAR_DISPUTA', 'Mediar disputas entre comprador e loja (BP1-35)');

INSERT INTO papel_permissao (id_papel, id_permissao)
SELECT p.id_papel, pe.id_permissao
FROM papel p CROSS JOIN permissao pe
WHERE p.nome = 'ADMINISTRADOR';

-- Administrador inicial (senha definida pela aplicacao a partir de ADMIN_SENHA)
INSERT INTO usuario (nome, nome_exibicao, email, senha_hash, tipo_pessoa, status, data_cadastro)
VALUES ('Administrador Busca Peca', 'Admin', '${admin_email}', '{bloqueada}', 'PJ', 'ATIVO', NOW());

INSERT INTO usuario_papel (id_usuario, id_papel, data_atribuicao)
SELECT u.id_usuario, p.id_papel, NOW()
FROM usuario u CROSS JOIN papel p
WHERE u.email = '${admin_email}' AND p.nome IN ('ADMINISTRADOR', 'COMPRADOR');

-- Categorias principais
INSERT INTO categoria (id_categoria_pai, nome) VALUES
    (NULL, 'Motor'),
    (NULL, 'Freios'),
    (NULL, 'Suspensão'),
    (NULL, 'Elétrica'),
    (NULL, 'Arrefecimento'),
    (NULL, 'Transmissão'),
    (NULL, 'Carroceria'),
    (NULL, 'Filtros');

-- Subcategorias
INSERT INTO categoria (id_categoria_pai, nome)
SELECT c.id_categoria, s.nome
FROM categoria c
JOIN (
    SELECT 'Motor' AS pai, 'Correias e tensores' AS nome UNION ALL
    SELECT 'Motor', 'Juntas e retentores' UNION ALL
    SELECT 'Motor', 'Velas e cabos' UNION ALL
    SELECT 'Motor', 'Bombas de óleo' UNION ALL
    SELECT 'Freios', 'Pastilhas' UNION ALL
    SELECT 'Freios', 'Discos' UNION ALL
    SELECT 'Freios', 'Lonas e tambores' UNION ALL
    SELECT 'Freios', 'Cilindros e pinças' UNION ALL
    SELECT 'Suspensão', 'Amortecedores' UNION ALL
    SELECT 'Suspensão', 'Molas' UNION ALL
    SELECT 'Suspensão', 'Bandejas e pivôs' UNION ALL
    SELECT 'Suspensão', 'Buchas' UNION ALL
    SELECT 'Elétrica', 'Baterias' UNION ALL
    SELECT 'Elétrica', 'Alternadores' UNION ALL
    SELECT 'Elétrica', 'Motores de partida' UNION ALL
    SELECT 'Elétrica', 'Lâmpadas e faróis' UNION ALL
    SELECT 'Arrefecimento', 'Radiadores' UNION ALL
    SELECT 'Arrefecimento', 'Bombas d''água' UNION ALL
    SELECT 'Arrefecimento', 'Válvulas termostáticas' UNION ALL
    SELECT 'Transmissão', 'Embreagem' UNION ALL
    SELECT 'Transmissão', 'Câmbio' UNION ALL
    SELECT 'Transmissão', 'Semieixos e homocinéticas' UNION ALL
    SELECT 'Carroceria', 'Para-choques' UNION ALL
    SELECT 'Carroceria', 'Retrovisores' UNION ALL
    SELECT 'Carroceria', 'Maçanetas e fechaduras' UNION ALL
    SELECT 'Filtros', 'Filtro de óleo' UNION ALL
    SELECT 'Filtros', 'Filtro de ar' UNION ALL
    SELECT 'Filtros', 'Filtro de combustível' UNION ALL
    SELECT 'Filtros', 'Filtro de cabine'
) s ON s.pai = c.nome
WHERE c.id_categoria_pai IS NULL;

-- Termos de Uso e Politica de Privacidade 1.0 (ADR-013)
-- Texto provisorio: substituir pelo texto aprovado antes da producao,
-- publicando uma nova versao pela area administrativa.
INSERT INTO termo_uso (id_admin_publicador, tipo, versao, conteudo, data_publicacao, vigente)
SELECT u.id_usuario, 'TERMOS_USO', '1.0',
       'Termos de Uso do Busca Peça - versão 1.0 (texto provisório). A plataforma conecta compradores e lojas de autopeças. No MVP, pagamento, frete e retirada são combinados diretamente entre as partes, fora da plataforma.',
       NOW(), TRUE
FROM usuario u WHERE u.email = '${admin_email}';

INSERT INTO termo_uso (id_admin_publicador, tipo, versao, conteudo, data_publicacao, vigente)
SELECT u.id_usuario, 'POLITICA_PRIVACIDADE', '1.0',
       'Política de Privacidade do Busca Peça - versão 1.0 (texto provisório). Coletamos apenas os dados necessários para cada funcionalidade, não comercializamos dados pessoais e o titular pode consultar, corrigir, exportar ou excluir sua conta pelo perfil (LGPD).',
       NOW(), TRUE
FROM usuario u WHERE u.email = '${admin_email}';
