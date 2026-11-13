-- Dados de teste: roda SOMENTE no perfil dev (pasta db/dev), nunca em producao.
-- Numero alto (V1000) para nao colidir com as migracoes reais.

INSERT INTO veiculo (marca, modelo, ano, versao) VALUES
    ('Volkswagen', 'Gol', 2015, '1.0 MPI'),
    ('Volkswagen', 'Gol', 2019, '1.6 MSI'),
    ('Volkswagen', 'Polo', 2021, '200 TSI'),
    ('Fiat', 'Uno', 2016, '1.0 Fire'),
    ('Fiat', 'Strada', 2022, '1.3 Firefly'),
    ('Chevrolet', 'Onix', 2020, '1.0 Turbo'),
    ('Chevrolet', 'Prisma', 2018, '1.4'),
    ('Ford', 'Ka', 2019, '1.5 SE'),
    ('Toyota', 'Corolla', 2020, '2.0 XEi'),
    ('Honda', 'Civic', 2018, '2.0 EXL'),
    ('Hyundai', 'HB20', 2021, '1.0 Comfort'),
    ('Renault', 'Sandero', 2019, '1.6 Expression');

INSERT INTO tag (nome) VALUES
    ('importada'),
    ('com garantia'),
    ('nota fiscal'),
    ('kit completo'),
    ('linha pesada');
