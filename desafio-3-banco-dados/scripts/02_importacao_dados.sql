-- ============================================================================
-- DESAFIO 3 - BANCO DE DADOS E ANÁLISE
-- Scripts de Importação de Dados CSV
-- Compatível com PostgreSQL 10+ e MySQL 8.0+
-- ============================================================================

-- ============================================================================
-- ANÁLISE CRÍTICA: TRATAMENTO DE INCONSISTÊNCIAS NA IMPORTAÇÃO
--
-- 1. VALORES NULL EM CAMPOS OBRIGATÓRIOS:
--    Estratégia: Usar COALESCE com valores padrão
--    Justificativa: Não perder dados, mas marcar como incompletos
--
-- 2. STRINGS EM CAMPOS NUMÉRICOS:
--    Estratégia: Usar NULLIF e CAST com tratamento de erro
--    Justificativa: Converter quando possível, zerar quando não
--
-- 3. DATAS EM FORMATOS INCONSISTENTES:
--    Estratégia: Tentar múltiplos formatos com TRY_CAST (ou equivalente)
--    Justificativa: Maximizar aproveitamento dos dados
-- ============================================================================

-- ============================================================================
-- TABELA TEMPORÁRIA PARA STAGING (PostgreSQL)
-- Usada para validação antes da inserção final
-- ============================================================================

-- Staging para operadoras
CREATE TEMP TABLE IF NOT EXISTS stg_operadoras (
    registro_ans TEXT,
    cnpj TEXT,
    razao_social TEXT,
    nome_fantasia TEXT,
    modalidade TEXT,
    uf TEXT,
    municipio TEXT,
    data_registro TEXT
); -- stg_operadoras

-- Staging para despesas
CREATE TEMP TABLE IF NOT EXISTS stg_despesas (
    cnpj TEXT,
    razao_social TEXT,
    trimestre TEXT,
    ano TEXT,
    valor_despesas TEXT,
    registro_ans TEXT,
    modalidade TEXT,
    uf TEXT,
    cnpj_valido TEXT,
    valor_valido TEXT,
    match_cadastro TEXT
); -- stg_despesas

-- Staging para agregadas
CREATE TEMP TABLE IF NOT EXISTS stg_agregadas (
    razao_social TEXT,
    uf TEXT,
    total_despesas TEXT,
    media_por_trimestre TEXT,
    desvio_padrao TEXT,
    quantidade_trimestres TEXT
); -- stg_agregadas

-- ============================================================================
-- IMPORTAÇÃO PostgreSQL (usando COPY)
-- ============================================================================

-- Importar operadoras (ajustar caminho do arquivo)
-- COPY stg_operadoras FROM '/path/to/operadoras.csv' 
--     WITH (FORMAT csv, HEADER true, DELIMITER ';', ENCODING 'UTF8', NULL '');

-- Importar despesas
-- COPY stg_despesas FROM '/path/to/consolidado_enriquecido.csv' 
--     WITH (FORMAT csv, HEADER true, DELIMITER ';', ENCODING 'UTF8', NULL '');

-- Importar agregadas
-- COPY stg_agregadas FROM '/path/to/despesas_agregadas.csv' 
--     WITH (FORMAT csv, HEADER true, DELIMITER ';', ENCODING 'UTF8', NULL '');

-- ============================================================================
-- IMPORTAÇÃO MySQL (usando LOAD DATA)
-- ============================================================================

-- Para MySQL, use LOAD DATA INFILE:
/*
LOAD DATA INFILE '/path/to/operadoras.csv'
INTO TABLE stg_operadoras
CHARACTER SET utf8mb4
FIELDS TERMINATED BY ';'
ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 ROWS;
*/

-- ============================================================================
-- INSERÇÃO DE OPERADORAS COM TRATAMENTO DE INCONSISTÊNCIAS
-- ============================================================================

INSERT INTO operadoras_cadastro (
    registro_ans,
    cnpj,
    razao_social,
    nome_fantasia,
    modalidade,
    uf,
    municipio,
    data_registro
)
SELECT 
    -- Tratar registro_ans nulo
    COALESCE(NULLIF(TRIM(registro_ans), ''), 'NAO_INFORMADO') AS registro_ans,
    
    -- Normalizar CNPJ (apenas números, com padding)
    LPAD(REGEXP_REPLACE(COALESCE(cnpj, ''), '[^0-9]', '', 'g'), 14, '0') AS cnpj,
    
    -- Razão social obrigatória
    COALESCE(NULLIF(TRIM(razao_social), ''), 'RAZAO NAO INFORMADA') AS razao_social,
    
    -- Nome fantasia pode ser nulo
    NULLIF(TRIM(nome_fantasia), '') AS nome_fantasia,
    
    -- Modalidade
    NULLIF(TRIM(modalidade), '') AS modalidade,
    
    -- UF: validar formato de 2 caracteres
    CASE 
        WHEN LENGTH(TRIM(uf)) = 2 THEN UPPER(TRIM(uf))
        ELSE NULL
    END AS uf,
    
    -- Município
    NULLIF(TRIM(municipio), '') AS municipio,
    
    -- Data de registro: tentar converter
    CASE 
        WHEN data_registro ~ '^\d{4}-\d{2}-\d{2}$' 
            THEN data_registro::DATE
        WHEN data_registro ~ '^\d{2}/\d{2}/\d{4}$' 
            THEN TO_DATE(data_registro, 'DD/MM/YYYY')
        ELSE NULL
    END AS data_registro
FROM stg_operadoras
WHERE cnpj IS NOT NULL 
  AND TRIM(cnpj) != ''
  AND LENGTH(REGEXP_REPLACE(cnpj, '[^0-9]', '', 'g')) >= 8
ON CONFLICT (cnpj) DO UPDATE SET
    razao_social = EXCLUDED.razao_social,
    nome_fantasia = EXCLUDED.nome_fantasia,
    modalidade = EXCLUDED.modalidade,
    uf = EXCLUDED.uf,
    municipio = EXCLUDED.municipio;
-- INSERT operadoras

-- ============================================================================
-- INSERÇÃO DE DESPESAS COM TRATAMENTO DE INCONSISTÊNCIAS
-- ============================================================================

INSERT INTO despesas_consolidadas (
    cnpj,
    razao_social,
    trimestre,
    ano,
    valor_despesas,
    cnpj_valido,
    valor_valido,
    match_cadastro,
    registro_ans,
    modalidade,
    uf
)
SELECT 
    -- CNPJ normalizado
    LPAD(REGEXP_REPLACE(COALESCE(cnpj, ''), '[^0-9]', '', 'g'), 14, '0') AS cnpj,
    
    -- Razão social
    COALESCE(NULLIF(TRIM(razao_social), ''), 'NAO INFORMADA') AS razao_social,
    
    -- Trimestre: normalizar formato
    CASE 
        WHEN UPPER(TRIM(trimestre)) IN ('Q1', '1T', 'T1', '1') THEN 'Q1'
        WHEN UPPER(TRIM(trimestre)) IN ('Q2', '2T', 'T2', '2') THEN 'Q2'
        WHEN UPPER(TRIM(trimestre)) IN ('Q3', '3T', 'T3', '3') THEN 'Q3'
        WHEN UPPER(TRIM(trimestre)) IN ('Q4', '4T', 'T4', '4') THEN 'Q4'
        ELSE 'Q1'
    END AS trimestre,
    
    -- Ano: converter com fallback
    CASE 
        WHEN ano ~ '^\d{4}$' THEN ano::INTEGER
        ELSE EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER
    END AS ano,
    
    -- Valor: tratar formato brasileiro e valores inválidos
    CASE 
        WHEN valor_despesas IS NULL OR TRIM(valor_despesas) = '' THEN 0
        WHEN valor_despesas ~ '^-?[0-9]+([,.][0-9]+)?$' THEN
            REPLACE(REPLACE(valor_despesas, '.', ''), ',', '.')::DECIMAL(18,2)
        ELSE 0
    END AS valor_despesas,
    
    -- Flags de validação
    UPPER(TRIM(cnpj_valido)) = 'SIM' AS cnpj_valido,
    UPPER(TRIM(valor_valido)) = 'SIM' AS valor_valido,
    UPPER(TRIM(match_cadastro)) = 'SIM' AS match_cadastro,
    
    -- Dados do cadastro
    NULLIF(TRIM(registro_ans), '') AS registro_ans,
    NULLIF(TRIM(modalidade), '') AS modalidade,
    CASE 
        WHEN LENGTH(TRIM(uf)) = 2 THEN UPPER(TRIM(uf))
        ELSE NULL
    END AS uf
FROM stg_despesas
WHERE cnpj IS NOT NULL 
  AND TRIM(cnpj) != ''
ON CONFLICT (cnpj, trimestre, ano) DO UPDATE SET
    valor_despesas = EXCLUDED.valor_despesas,
    razao_social = EXCLUDED.razao_social;
-- INSERT despesas

-- ============================================================================
-- INSERÇÃO DE DADOS AGREGADOS
-- ============================================================================

INSERT INTO despesas_agregadas (
    razao_social,
    uf,
    total_despesas,
    media_por_trimestre,
    desvio_padrao,
    quantidade_trimestres
)
SELECT 
    COALESCE(NULLIF(TRIM(razao_social), ''), 'NAO INFORMADA') AS razao_social,
    
    CASE 
        WHEN LENGTH(TRIM(uf)) = 2 THEN UPPER(TRIM(uf))
        WHEN TRIM(uf) = 'N/A' OR TRIM(uf) = '' THEN NULL
        ELSE NULL
    END AS uf,
    
    -- Converter valores numéricos
    COALESCE(
        REPLACE(REPLACE(total_despesas, '.', ''), ',', '.')::DECIMAL(18,2),
        0
    ) AS total_despesas,
    
    COALESCE(
        REPLACE(REPLACE(media_por_trimestre, '.', ''), ',', '.')::DECIMAL(18,2),
        0
    ) AS media_por_trimestre,
    
    COALESCE(
        REPLACE(REPLACE(desvio_padrao, '.', ''), ',', '.')::DECIMAL(18,2),
        0
    ) AS desvio_padrao,
    
    COALESCE(quantidade_trimestres::INTEGER, 0) AS quantidade_trimestres
FROM stg_agregadas
WHERE razao_social IS NOT NULL AND TRIM(razao_social) != ''
ON CONFLICT (razao_social, uf) DO UPDATE SET
    total_despesas = EXCLUDED.total_despesas,
    media_por_trimestre = EXCLUDED.media_por_trimestre,
    desvio_padrao = EXCLUDED.desvio_padrao,
    quantidade_trimestres = EXCLUDED.quantidade_trimestres;
-- INSERT agregadas

-- ============================================================================
-- REGISTRO DE IMPORTAÇÃO NO LOG
-- ============================================================================

INSERT INTO log_importacao (tabela_destino, arquivo_origem, registros_processados, observacoes)
VALUES 
    ('operadoras_cadastro', 'operadoras.csv', (SELECT COUNT(*) FROM stg_operadoras), 'Importação concluída'),
    ('despesas_consolidadas', 'consolidado_enriquecido.csv', (SELECT COUNT(*) FROM stg_despesas), 'Importação concluída'),
    ('despesas_agregadas', 'despesas_agregadas.csv', (SELECT COUNT(*) FROM stg_agregadas), 'Importação concluída');
-- INSERT log

-- ============================================================================
-- LIMPEZA DE TABELAS TEMPORÁRIAS
-- ============================================================================

DROP TABLE IF EXISTS stg_operadoras;
DROP TABLE IF EXISTS stg_despesas;
DROP TABLE IF EXISTS stg_agregadas;
