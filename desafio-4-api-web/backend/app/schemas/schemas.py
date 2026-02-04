"""
Schemas Pydantic para validação e serialização
"""

from pydantic import BaseModel, Field
from typing import Optional, List
from decimal import Decimal

# ============================================================================
# Schemas de Operadora
# ============================================================================

class OperadoraBase(BaseModel):
    """Schema base para operadora"""
    cnpj: str
    razao_social: str
    registro_ans: Optional[str] = None
    modalidade: Optional[str] = None
    uf: Optional[str] = None
# OperadoraBase

class OperadoraResponse(OperadoraBase):
    """Schema de resposta para operadora"""
    id: int
    total_despesas: Optional[float] = None
    
    class Config:
        from_attributes = True
    # Config
# OperadoraResponse

class OperadoraDetalhe(OperadoraBase):
    """Schema com detalhes completos da operadora"""
    id: Optional[int] = None
    total_despesas: Optional[float] = None
    quantidade_trimestres: Optional[int] = None
    
    class Config:
        from_attributes = True
    # Config
# OperadoraDetalhe

# ============================================================================
# Schemas de Despesa
# ============================================================================

class DespesaBase(BaseModel):
    """Schema base para despesa"""
    cnpj: str
    razao_social: str
    trimestre: str
    ano: int
    valor_despesas: Decimal
# DespesaBase

class DespesaResponse(DespesaBase):
    """Schema de resposta para despesa"""
    id: int
    registro_ans: Optional[str] = None
    modalidade: Optional[str] = None
    uf: Optional[str] = None
    cnpj_valido: bool = True
    valor_valido: bool = True
    
    class Config:
        from_attributes = True
    # Config
# DespesaResponse

class DespesaHistorico(BaseModel):
    """Schema para histórico de despesas"""
    trimestre: str
    ano: int
    valor_despesas: Decimal
# DespesaHistorico

# ============================================================================
# Schemas de Agregação
# ============================================================================

class DespesaAgregadaResponse(BaseModel):
    """Schema de resposta para despesa agregada"""
    id: int
    razao_social: str
    uf: Optional[str] = None
    total_despesas: Decimal
    media_por_trimestre: Decimal
    desvio_padrao: Decimal
    quantidade_trimestres: int
    
    class Config:
        from_attributes = True
    # Config
# DespesaAgregadaResponse

# ============================================================================
# Schemas de Paginação
# ============================================================================

"""
TRADE-OFF TÉCNICO: ESTRATÉGIA DE PAGINAÇÃO

DECISÃO: Offset-based pagination

JUSTIFICATIVA:
- Simplicidade: Fácil de implementar e entender
- Compatibilidade: Funciona com qualquer banco de dados
- Flexibilidade: Permite navegação para qualquer página

CONTRAS:
- Performance degrada para offsets grandes (milhões de registros)
- Inconsistência se dados são inseridos/deletados durante navegação

ALTERNATIVAS CONSIDERADAS:
- Cursor-based: Melhor para dados em tempo real, mais complexo
- Keyset: Melhor performance, mas requer ordenação estável

Para o volume esperado (~1000-5000 operadoras), offset-based é adequado.

TRADE-OFF TÉCNICO: ESTRUTURA DE RESPOSTA

DECISÃO: Opção B - Dados + metadados

JUSTIFICATIVA:
- Frontend precisa saber total de páginas para navegação
- Informações de paginação facilitam implementação do cliente
- Padrão comum em APIs REST modernas
"""

class PaginationMeta(BaseModel):
    """Metadados de paginação"""
    total: int = Field(description="Total de registros")
    page: int = Field(description="Página atual")
    limit: int = Field(description="Registros por página")
    pages: int = Field(description="Total de páginas")
    has_next: bool = Field(description="Existe próxima página")
    has_prev: bool = Field(description="Existe página anterior")
# PaginationMeta

class PaginatedResponse(BaseModel):
    """Resposta paginada genérica"""
    data: List
    meta: PaginationMeta
# PaginatedResponse

class OperadorasPaginadas(BaseModel):
    """Resposta paginada de operadoras"""
    data: List[OperadoraResponse]
    meta: PaginationMeta
# OperadorasPaginadas

# ============================================================================
# Schemas de Estatísticas
# ============================================================================

class TopOperadora(BaseModel):
    """Operadora no ranking"""
    cnpj: str
    razao_social: str
    total_despesas: Decimal
    uf: Optional[str] = None
# TopOperadora

class DistribuicaoUF(BaseModel):
    """Distribuição de despesas por UF"""
    uf: str
    total: Decimal
    quantidade_operadoras: int
    percentual: float
# DistribuicaoUF

class EstatisticasResponse(BaseModel):
    """Resposta de estatísticas gerais"""
    total_despesas: Decimal
    media_despesas: Decimal
    total_operadoras: int
    top_5_operadoras: List[TopOperadora]
    distribuicao_por_uf: List[DistribuicaoUF]
# EstatisticasResponse

# ============================================================================
# Schemas de Busca
# ============================================================================

class BuscaParams(BaseModel):
    """Parâmetros de busca"""
    q: Optional[str] = Field(None, description="Termo de busca (razão social ou CNPJ)")
    uf: Optional[str] = Field(None, description="Filtrar por UF")
    modalidade: Optional[str] = Field(None, description="Filtrar por modalidade")
# BuscaParams
