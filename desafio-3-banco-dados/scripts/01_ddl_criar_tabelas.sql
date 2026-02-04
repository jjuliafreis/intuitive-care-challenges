-- ============================================================================
-- DESAFIO 3 - BANCO DE DADOS E ANÁLISE
-- Script DDL para criação das tabelas
-- Compatível com PostgreSQL 10+ e MySQL 8.0+
-- ============================================================================

-- ============================================================================
-- TRADE-OFF TÉCNICO: NORMALIZAÇÃO
-- 
-- DECISÃO: Opção B - Tabelas normalizadas separadas
--
-- JUSTIFICATIVA:
-- 1. Volume de dados: Estimamos ~50.000-100.000 registros de despesas e 
--    ~1.500 operadoras ativas. Este volume é gerenciável com tabelas normalizadas.
--
-- 2. Frequência de atualizações: Os dados de operadoras são atualizados 
--    esporadicamente, enquanto despesas são inseridas trimestralmente.
--    Normalização evita redundância e facilita atualizações.
--
-- 3. Complexidade das queries: Embora JOINs adicionem overhead, o volume
--    esperado não impacta significativamente. Índices adequados garantem
--    performance aceitável.
--
-- 4. Integridade referencial: Tabelas separadas permitem constraints de FK,
--    garantindo consistência dos dados.
--
-- TRADE-OFF TÉCNICO: TIPOS DE DADOS
--
-- DECIMAL para valores monetários:
-- - Precisão exata (evita erros de arredondamento de FLOAT)
-- - DECIMAL(18,2) suporta valores até 9.999.999.999.999.999,99
--
-- DATE para datas:
-- - Mais eficiente que VARCHAR para comparações
-- - Permite funções de data nativas
-- ============================================================================

-- ============================================================================
-- TABELA: operadoras_cadastro
-- Dados cadastrais das operadoras de planos de saúde
-- ============================================================================
CREATE TABLE IF NOT EXISTS operadoras_cadastro (
    id SERIAL PRIMARY KEY,
    registro_ans VARCHAR(20) NOT NULL,
    cnpj VARCHAR(14) NOT NULL,
    razao_social VARCHAR(255) NOT NULL,
    nome_fantasia VARCHAR(255),
    modalidade VARCHAR(100),
    uf CHAR(2),
    municipio VARCHAR(150),
    data_registro DATE,
    data_importacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT uk_operadora_cnpj UNIQUE (cnpj),
    CONSTRAINT uk_operadora_registro_ans UNIQUE (registro_ans),
    CONSTRAINT ck_operadora_cnpj_length CHECK (LENGTH(cnpj) = 14),
    CONSTRAINT ck_operadora_uf_length CHECK (uf IS NULL OR LENGTH(uf) = 2)
); -- operadoras_cadastro

-- Índices para otimização de queries
CREATE INDEX idx_operadora_cnpj ON operadoras_cadastro(cnpj);
CREATE INDEX idx_operadora_uf ON operadoras_cadastro(uf);
CREATE INDEX idx_operadora_modalidade ON operadoras_cadastro(modalidade);
CREATE INDEX idx_operadora_razao_social ON operadoras_cadastro(razao_social);

-- ============================================================================
-- TABELA: despesas_consolidadas
-- Dados consolidados de despesas por trimestre
-- ============================================================================
CREATE TABLE IF NOT EXISTS despesas_consolidadas (
    id SERIAL PRIMARY KEY,
    cnpj VARCHAR(14) NOT NULL,
    razao_social VARCHAR(255) NOT NULL,
    trimestre VARCHAR(5) NOT NULL,  -- Formato: Q1, Q2, Q3, Q4
    ano INTEGER NOT NULL,
    valor_despesas DECIMAL(18,2) NOT NULL DEFAULT 0,
    cnpj_valido BOOLEAN DEFAULT TRUE,
    valor_valido BOOLEAN DEFAULT TRUE,
    match_cadastro BOOLEAN DEFAULT FALSE,
    registro_ans VARCHAR(20),
    modalidade VARCHAR(100),
    uf CHAR(2),
    data_importacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT uk_despesa_cnpj_trimestre_ano UNIQUE (cnpj, trimestre, ano),
    CONSTRAINT ck_despesa_trimestre CHECK (trimestre IN ('Q1', 'Q2', 'Q3', 'Q4')),
    CONSTRAINT ck_despesa_ano CHECK (ano >= 2000 AND ano <= 2100)
); -- despesas_consolidadas

-- Índices para otimização de queries
CREATE INDEX idx_despesa_cnpj ON despesas_consolidadas(cnpj);
CREATE INDEX idx_despesa_ano_trimestre ON despesas_consolidadas(ano, trimestre);
CREATE INDEX idx_despesa_uf ON despesas_consolidadas(uf);
CREATE INDEX idx_despesa_razao_social ON despesas_consolidadas(razao_social);
CREATE INDEX idx_despesa_valor ON despesas_consolidadas(valor_despesas);

-- ============================================================================
-- TABELA: despesas_agregadas
-- Dados agregados por operadora e UF
-- ============================================================================
CREATE TABLE IF NOT EXISTS despesas_agregadas (
    id SERIAL PRIMARY KEY,
    razao_social VARCHAR(255) NOT NULL,
    uf CHAR(2),
    total_despesas DECIMAL(18,2) NOT NULL DEFAULT 0,
    media_por_trimestre DECIMAL(18,2) NOT NULL DEFAULT 0,
    desvio_padrao DECIMAL(18,2) NOT NULL DEFAULT 0,
    quantidade_trimestres INTEGER NOT NULL DEFAULT 0,
    data_importacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT uk_agregada_razao_uf UNIQUE (razao_social, uf)
); -- despesas_agregadas

-- Índices para otimização
CREATE INDEX idx_agregada_razao_social ON despesas_agregadas(razao_social);
CREATE INDEX idx_agregada_uf ON despesas_agregadas(uf);
CREATE INDEX idx_agregada_total ON despesas_agregadas(total_despesas DESC);

-- ============================================================================
-- TABELA: log_importacao
-- Registro de importações para auditoria
-- ============================================================================
CREATE TABLE IF NOT EXISTS log_importacao (
    id SERIAL PRIMARY KEY,
    tabela_destino VARCHAR(100) NOT NULL,
    arquivo_origem VARCHAR(255),
    registros_processados INTEGER DEFAULT 0,
    registros_importados INTEGER DEFAULT 0,
    registros_rejeitados INTEGER DEFAULT 0,
    observacoes TEXT,
    data_importacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP
); -- log_importacao

-- ============================================================================
-- VIEW: vw_despesas_completas
-- View para facilitar consultas com dados de operadora
-- ============================================================================
CREATE OR REPLACE VIEW vw_despesas_completas AS
SELECT 
    d.id,
    d.cnpj,
    d.razao_social,
    d.trimestre,
    d.ano,
    d.valor_despesas,
    d.cnpj_valido,
    d.valor_valido,
    COALESCE(d.registro_ans, o.registro_ans) AS registro_ans,
    COALESCE(d.modalidade, o.modalidade) AS modalidade,
    COALESCE(d.uf, o.uf) AS uf,
    o.nome_fantasia,
    o.municipio
FROM despesas_consolidadas d
LEFT JOIN operadoras_cadastro o ON d.cnpj = o.cnpj;
-- vw_despesas_completas

-- ============================================================================
-- VIEW: vw_resumo_por_uf
-- Resumo de despesas por UF
-- ============================================================================
CREATE OR REPLACE VIEW vw_resumo_por_uf AS
SELECT 
    uf,
    COUNT(DISTINCT cnpj) AS quantidade_operadoras,
    SUM(valor_despesas) AS total_despesas,
    AVG(valor_despesas) AS media_despesas,
    MIN(valor_despesas) AS menor_despesa,
    MAX(valor_despesas) AS maior_despesa
FROM despesas_consolidadas
WHERE uf IS NOT NULL AND uf != ''
GROUP BY uf
ORDER BY total_despesas DESC;
-- vw_resumo_por_uf

COMMENT ON TABLE operadoras_cadastro IS 'Dados cadastrais das operadoras de planos de saúde ativas';
COMMENT ON TABLE despesas_consolidadas IS 'Despesas consolidadas por operadora/trimestre';
COMMENT ON TABLE despesas_agregadas IS 'Dados agregados por operadora e UF';
COMMENT ON TABLE log_importacao IS 'Log de importações para auditoria';
