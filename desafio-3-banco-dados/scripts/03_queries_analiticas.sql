-- ============================================================================
-- DESAFIO 3 - BANCO DE DADOS E ANÁLISE
-- Queries Analíticas
-- Compatível com PostgreSQL 10+ e MySQL 8.0+
-- ============================================================================

-- ============================================================================
-- QUERY 1: Top 5 operadoras com maior crescimento percentual de despesas
-- entre o primeiro e o último trimestre analisado
-- ============================================================================

/*
ANÁLISE CRÍTICA: Operadoras sem dados em todos os trimestres

DECISÃO: Incluir operadoras que tenham dados em pelo menos 2 trimestres
(primeiro e último que aparecem para cada operadora)

JUSTIFICATIVA:
- Excluir completamente penalizaria operadoras novas ou com dados parciais
- Usar apenas o primeiro e último trimestre disponível para cada operadora
  permite calcular crescimento mesmo com gaps
- Operadoras com apenas 1 trimestre são excluídas (não há como calcular crescimento)

ALTERNATIVAS CONSIDERADAS:
- Exigir todos os 3 trimestres: Muito restritivo, perderia dados relevantes
- Preencher trimestres faltantes com zero: Distorceria a análise de crescimento
- Interpolar valores: Adiciona complexidade e pode mascarar realidade
*/

WITH trimestres_ordenados AS (
    -- Ordenar trimestres cronologicamente
    SELECT 
        cnpj,
        razao_social,
        ano,
        trimestre,
        valor_despesas,
        -- Criar ordem numérica para trimestres
        (ano * 10 + 
            CASE trimestre 
                WHEN 'Q1' THEN 1 
                WHEN 'Q2' THEN 2 
                WHEN 'Q3' THEN 3 
                WHEN 'Q4' THEN 4 
            END
        ) AS ordem_trimestre
    FROM despesas_consolidadas
    WHERE valor_despesas > 0
), -- trimestres_ordenados

operadoras_primeiro_ultimo AS (
    -- Identificar primeiro e último trimestre de cada operadora
    SELECT 
        cnpj,
        razao_social,
        MIN(ordem_trimestre) AS primeiro_trimestre,
        MAX(ordem_trimestre) AS ultimo_trimestre
    FROM trimestres_ordenados
    GROUP BY cnpj, razao_social
    HAVING COUNT(DISTINCT ordem_trimestre) >= 2  -- Pelo menos 2 trimestres
), -- operadoras_primeiro_ultimo

valores_primeiro_ultimo AS (
    -- Obter valores do primeiro e último trimestre
    SELECT 
        o.cnpj,
        o.razao_social,
        t1.valor_despesas AS valor_primeiro,
        t1.ano AS ano_primeiro,
        t1.trimestre AS trimestre_primeiro,
        t2.valor_despesas AS valor_ultimo,
        t2.ano AS ano_ultimo,
        t2.trimestre AS trimestre_ultimo
    FROM operadoras_primeiro_ultimo o
    JOIN trimestres_ordenados t1 
        ON o.cnpj = t1.cnpj 
        AND o.primeiro_trimestre = t1.ordem_trimestre
    JOIN trimestres_ordenados t2 
        ON o.cnpj = t2.cnpj 
        AND o.ultimo_trimestre = t2.ordem_trimestre
    WHERE t1.valor_despesas > 0  -- Evitar divisão por zero
) -- valores_primeiro_ultimo

SELECT 
    cnpj,
    razao_social,
    valor_primeiro,
    CONCAT(trimestre_primeiro, '/', ano_primeiro) AS periodo_inicial,
    valor_ultimo,
    CONCAT(trimestre_ultimo, '/', ano_ultimo) AS periodo_final,
    ROUND(
        ((valor_ultimo - valor_primeiro) / valor_primeiro) * 100, 
        2
    ) AS crescimento_percentual
FROM valores_primeiro_ultimo
WHERE valor_ultimo > valor_primeiro  -- Apenas crescimento positivo
ORDER BY crescimento_percentual DESC
LIMIT 5;
-- QUERY 1


-- ============================================================================
-- QUERY 2: Distribuição de despesas por UF - Top 5 estados
-- com média de despesas por operadora
-- ============================================================================

/*
DESAFIO ADICIONAL: Calcular média de despesas por operadora em cada UF
(não apenas o total)
*/

SELECT 
    uf,
    COUNT(DISTINCT cnpj) AS quantidade_operadoras,
    SUM(valor_despesas) AS total_despesas,
    ROUND(AVG(valor_despesas), 2) AS media_por_registro,
    ROUND(
        SUM(valor_despesas) / NULLIF(COUNT(DISTINCT cnpj), 0), 
        2
    ) AS media_por_operadora,
    MIN(valor_despesas) AS menor_despesa,
    MAX(valor_despesas) AS maior_despesa
FROM despesas_consolidadas
WHERE uf IS NOT NULL 
  AND uf != '' 
  AND uf != 'N/A'
GROUP BY uf
ORDER BY total_despesas DESC
LIMIT 5;
-- QUERY 2


-- ============================================================================
-- QUERY 3: Operadoras com despesas acima da média em pelo menos 2 dos 3 trimestres
-- ============================================================================

/*
TRADE-OFF TÉCNICO: Abordagem escolhida - CTEs com agregação condicional

JUSTIFICATIVA:
- PERFORMANCE: CTEs materializam resultados intermediários, evitando
  recálculo da média geral múltiplas vezes
- MANUTENIBILIDADE: Código modular e fácil de entender/modificar
- LEGIBILIDADE: Cada CTE tem propósito claro e documentado

ALTERNATIVAS CONSIDERADAS:
- Subqueries aninhadas: Menos legível, potencialmente recomputa média
- Joins múltiplos: Mais verboso, mesmo resultado
- Window Functions: Possível, mas CTEs são mais claras para este caso
*/

WITH media_geral AS (
    -- Calcular média geral de despesas
    SELECT AVG(valor_despesas) AS valor_media
    FROM despesas_consolidadas
    WHERE valor_despesas > 0
), -- media_geral

trimestres_acima_media AS (
    -- Identificar em quais trimestres cada operadora ficou acima da média
    SELECT 
        d.cnpj,
        d.razao_social,
        d.trimestre,
        d.ano,
        d.valor_despesas,
        m.valor_media,
        CASE 
            WHEN d.valor_despesas > m.valor_media THEN 1 
            ELSE 0 
        END AS acima_media
    FROM despesas_consolidadas d
    CROSS JOIN media_geral m
    WHERE d.valor_despesas > 0
), -- trimestres_acima_media

operadoras_contagem AS (
    -- Contar quantos trimestres cada operadora ficou acima da média
    SELECT 
        cnpj,
        razao_social,
        COUNT(DISTINCT CONCAT(ano, '-', trimestre)) AS total_trimestres,
        SUM(acima_media) AS trimestres_acima_media,
        ROUND(AVG(valor_despesas), 2) AS media_despesas_operadora,
        (SELECT valor_media FROM media_geral) AS media_geral
    FROM trimestres_acima_media
    GROUP BY cnpj, razao_social
) -- operadoras_contagem

-- Resultado final: contar operadoras com >= 2 trimestres acima da média
SELECT 
    COUNT(*) AS quantidade_operadoras,
    'Operadoras com despesas acima da média em >= 2 trimestres' AS descricao
FROM operadoras_contagem
WHERE trimestres_acima_media >= 2;
-- QUERY 3

-- Detalhamento das operadoras (query auxiliar)
SELECT 
    cnpj,
    razao_social,
    total_trimestres,
    trimestres_acima_media,
    media_despesas_operadora,
    ROUND(media_geral, 2) AS media_geral_referencia
FROM (
    SELECT 
        d.cnpj,
        d.razao_social,
        COUNT(DISTINCT CONCAT(d.ano, '-', d.trimestre)) AS total_trimestres,
        SUM(CASE WHEN d.valor_despesas > m.valor_media THEN 1 ELSE 0 END) AS trimestres_acima_media,
        ROUND(AVG(d.valor_despesas), 2) AS media_despesas_operadora,
        m.valor_media AS media_geral
    FROM despesas_consolidadas d
    CROSS JOIN (SELECT AVG(valor_despesas) AS valor_media FROM despesas_consolidadas WHERE valor_despesas > 0) m
    WHERE d.valor_despesas > 0
    GROUP BY d.cnpj, d.razao_social, m.valor_media
) sub
WHERE trimestres_acima_media >= 2
ORDER BY media_despesas_operadora DESC
LIMIT 20;
-- QUERY 3 Detalhada


-- ============================================================================
-- QUERIES ADICIONAIS ÚTEIS
-- ============================================================================

-- Query auxiliar: Resumo geral dos dados
SELECT 
    'Despesas Consolidadas' AS tabela,
    COUNT(*) AS total_registros,
    COUNT(DISTINCT cnpj) AS operadoras_unicas,
    SUM(valor_despesas) AS soma_total,
    ROUND(AVG(valor_despesas), 2) AS media
FROM despesas_consolidadas
UNION ALL
SELECT 
    'Operadoras Cadastro' AS tabela,
    COUNT(*) AS total_registros,
    COUNT(DISTINCT cnpj) AS operadoras_unicas,
    NULL AS soma_total,
    NULL AS media
FROM operadoras_cadastro
UNION ALL
SELECT 
    'Despesas Agregadas' AS tabela,
    COUNT(*) AS total_registros,
    COUNT(DISTINCT razao_social) AS operadoras_unicas,
    SUM(total_despesas) AS soma_total,
    ROUND(AVG(total_despesas), 2) AS media
FROM despesas_agregadas;
-- Query resumo

-- Query auxiliar: Evolução trimestral
SELECT 
    ano,
    trimestre,
    COUNT(DISTINCT cnpj) AS operadoras,
    SUM(valor_despesas) AS total_despesas,
    ROUND(AVG(valor_despesas), 2) AS media_despesas
FROM despesas_consolidadas
GROUP BY ano, trimestre
ORDER BY ano, 
    CASE trimestre 
        WHEN 'Q1' THEN 1 
        WHEN 'Q2' THEN 2 
        WHEN 'Q3' THEN 3 
        WHEN 'Q4' THEN 4 
    END;
-- Query evolução
