# Teste de Estágio - Intuitive Care

Este repositório contém a solução completa para o teste de estágio da Intuitive Care, abrangendo 4 desafios de programação focados em integração de dados, transformação, banco de dados e desenvolvimento web.

## 📋 Índice

- [Visão Geral](#visão-geral)
- [Estrutura do Projeto](#estrutura-do-projeto)
- [Pré-requisitos](#pré-requisitos)
- [Desafio 1 - Integração com API ANS](#desafio-1---integração-com-api-ans)
- [Desafio 2 - Transformação de Dados](#desafio-2---transformação-de-dados)
- [Desafio 3 - Banco de Dados e SQL](#desafio-3---banco-de-dados-e-sql)
- [Desafio 4 - API e Interface Web](#desafio-4---api-e-interface-web)
- [Decisões Técnicas](#decisões-técnicas)
- [Autor](#autor)

---

## Visão Geral

O projeto foi desenvolvido seguindo as melhores práticas de arquitetura de software, com foco em:

- **Clean Code**: Código legível e bem documentado
- **Escalabilidade**: Padrões que permitem crescimento
- **Modularidade**: Componentes desacoplados e testáveis
- **Performance**: Otimizações para grandes volumes de dados

## Estrutura do Projeto

```
intuitive-care/
├── desafio-1-integracao-api/    # Java - Download e consolidação ANS
├── desafio-2-transformacao/      # Java - Validação e enriquecimento
├── desafio-3-banco-dados/        # SQL - DDL, Import e Queries
├── desafio-4-api-web/            # Python + Vue.js - API e Frontend
│   ├── backend/                  # FastAPI
│   └── frontend/                 # Vue 3
└── README.md                     # Este arquivo
```

## Pré-requisitos

### Para Desafios 1 e 2 (Java)
- Java 17+
- Maven 3.8+

### Para Desafio 3 (SQL)
- PostgreSQL 13+ ou MySQL 8+

### Para Desafio 4 (Python + Vue.js)
- Python 3.10+
- Node.js 18+
- npm ou yarn

---

## Desafio 1 - Integração com API ANS

### Descrição
Aplicação Java que integra com a API pública da ANS para baixar e consolidar demonstrativos contábeis dos últimos 3 trimestres.

### Funcionalidades
- ✅ Identificação automática dos 3 últimos trimestres disponíveis
- ✅ Download de arquivos ZIP com demonstrativos
- ✅ Extração e processamento de arquivos CSV
- ✅ Consolidação em arquivo único
- ✅ Relatório de inconsistências

### Como Executar

```bash
# Navegar para o diretório
cd desafio-1-integracao-api

# Compilar
mvn clean package

# Executar
java -jar target/integracao-api-1.0.0.jar
```

### Saída
- `output/demonstrativos_consolidados.csv` - Arquivo consolidado
- `output/relatorio_inconsistencias.txt` - Relatório de erros
- `logs/` - Arquivos de log

---

## Desafio 2 - Transformação de Dados

### Descrição
Aplicação Java para validar, enriquecer e agregar os dados consolidados do Desafio 1.

### Funcionalidades
- ✅ Validação completa de CNPJ (com dígitos verificadores)
- ✅ Enriquecimento com dados cadastrais das operadoras
- ✅ Agregação estatística por UF
- ✅ Cálculo de média, mediana, desvio padrão

### Como Executar

```bash
# Navegar para o diretório
cd desafio-2-transformacao

# Compilar
mvn clean package

# Executar
java -jar target/transformacao-1.0.0.jar
```

### Parâmetros Opcionais
```bash
# Especificar arquivos de entrada
java -jar target/transformacao-1.0.0.jar \
  --consolidado=caminho/para/consolidado.csv \
  --cadastro=caminho/para/cadastro.csv
```

### Saída
- `output/dados_enriquecidos.csv` - Dados com informações cadastrais
- `output/agregacao_por_uf.csv` - Estatísticas agregadas por UF
- `output/validacao_report.txt` - Relatório de validação

---

## Desafio 3 - Banco de Dados e SQL

### Descrição
Scripts SQL para criação de estrutura de banco de dados, importação de dados e queries analíticas.

### Scripts Disponíveis

1. **`01_ddl_criar_tabelas.sql`** - Criação das tabelas
2. **`02_importacao_dados.sql`** - Importação de CSVs
3. **`03_queries_analiticas.sql`** - Consultas analíticas

### Como Executar

#### PostgreSQL
```bash
# Criar banco
createdb intuitive_care

# Executar scripts em ordem
psql -d intuitive_care -f desafio-3-banco-dados/scripts/01_ddl_criar_tabelas.sql
psql -d intuitive_care -f desafio-3-banco-dados/scripts/02_importacao_dados.sql
psql -d intuitive_care -f desafio-3-banco-dados/scripts/03_queries_analiticas.sql
```

#### MySQL
```bash
# Criar banco
mysql -u root -p -e "CREATE DATABASE intuitive_care;"

# Executar scripts
mysql -u root -p intuitive_care < desafio-3-banco-dados/scripts/01_ddl_criar_tabelas.sql
mysql -u root -p intuitive_care < desafio-3-banco-dados/scripts/02_importacao_dados.sql
mysql -u root -p intuitive_care < desafio-3-banco-dados/scripts/03_queries_analiticas.sql
```

### Queries Analíticas Incluídas
1. Top 10 operadoras por crescimento trimestral
2. Distribuição de despesas por UF
3. Operadoras com despesas acima da média
4. Evolução temporal de despesas
5. Ranking de operadoras por modalidade

---

## Desafio 4 - API e Interface Web

### Descrição
API REST em Python (FastAPI) com interface web em Vue.js 3 para visualização de dados de operadoras.

### Backend (FastAPI)

#### Funcionalidades
- ✅ Endpoints RESTful para operadoras
- ✅ Paginação server-side
- ✅ Busca por razão social e CNPJ
- ✅ Filtro por UF
- ✅ Estatísticas agregadas
- ✅ Cache com TTL

#### Como Executar

```bash
# Navegar para o diretório
cd desafio-4-api-web/backend

# Criar ambiente virtual
python -m venv venv

# Ativar ambiente (Windows)
.\venv\Scripts\activate

# Ativar ambiente (Linux/Mac)
source venv/bin/activate

# Instalar dependências
pip install -r requirements.txt

# Executar em modo desenvolvimento
uvicorn main:app --reload --port 8000
```

#### Endpoints Disponíveis

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| GET | `/api/operadoras` | Lista operadoras (paginado) |
| GET | `/api/operadoras/{cnpj}` | Detalhes de uma operadora |
| GET | `/api/operadoras/{cnpj}/despesas` | Histórico de despesas |
| GET | `/api/estatisticas` | Estatísticas gerais |
| GET | `/api/estatisticas/por-uf` | Despesas agrupadas por UF |
| GET | `/api/estatisticas/top-operadoras` | Top N operadoras |

#### Documentação da API
Após iniciar o servidor, acesse:
- Swagger UI: http://localhost:8000/docs
- ReDoc: http://localhost:8000/redoc

### Frontend (Vue.js)

#### Funcionalidades
- ✅ Dashboard com estatísticas e gráficos
- ✅ Lista paginada de operadoras
- ✅ Busca em tempo real
- ✅ Filtro por UF
- ✅ Página de detalhes com histórico
- ✅ Gráficos interativos (Chart.js)
- ✅ Design responsivo

#### Como Executar

```bash
# Navegar para o diretório
cd desafio-4-api-web/frontend

# Instalar dependências
npm install

# Executar em modo desenvolvimento
npm run dev

# Build para produção
npm run build
```

#### Acessar
Após iniciar, acesse: http://localhost:5173

### Executar Backend e Frontend Juntos

Terminal 1 (Backend):
```bash
cd desafio-4-api-web/backend
.\venv\Scripts\activate
uvicorn main:app --reload --port 8000
```

Terminal 2 (Frontend):
```bash
cd desafio-4-api-web/frontend
npm run dev
```

---

## Decisões Técnicas

### Trade-offs Documentados

#### Desafio 1 - Processamento de Dados
- **Processamento Incremental vs In-Memory**: Escolhi processamento incremental para suportar arquivos maiores sem estouro de memória

#### Desafio 2 - Validação de CNPJ
- **Flag vs Reject**: Optei por marcar registros inválidos com flag ao invés de excluí-los, preservando dados para análise posterior

#### Desafio 3 - Modelagem
- **Normalizado vs Denormalizado**: Escolhi modelo normalizado para integridade referencial, com views denormalizadas para consultas
- **DECIMAL vs FLOAT**: Uso de DECIMAL para valores monetários garantindo precisão

#### Desafio 4 - API
- **FastAPI vs Flask**: FastAPI escolhido por validação automática, documentação integrada e performance superior
- **Cache com TTL**: Implementado para evitar processamento repetitivo do CSV

---

## Tecnologias Utilizadas

### Backend
- **Java 17**: Desafios 1 e 2
- **Maven**: Gerenciamento de dependências
- **JSoup**: Parsing HTML
- **Apache Commons CSV**: Leitura/escrita CSV
- **Apache POI**: Manipulação Excel
- **FastAPI**: API REST (Desafio 4)
- **Pydantic**: Validação de dados

### Frontend
- **Vue.js 3**: Framework reativo
- **Pinia**: Gerenciamento de estado
- **Vue Router**: Navegação SPA
- **Chart.js**: Gráficos interativos
- **Axios**: Cliente HTTP

### Banco de Dados
- **PostgreSQL/MySQL**: SGBD relacional

---

## Autor

**Júlia Reis** - Desenvolvido como parte do processo seletivo para estágio na Intuitive Care.

📅 Fevereiro de 2026

---

## Licença

Este projeto é parte de um teste técnico e não possui licença para uso comercial.
