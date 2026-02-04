"""
Rotas da API para Operadoras
"""

from fastapi import APIRouter, Query, HTTPException, Path
from typing import Optional
import math

from app.schemas.schemas import (
    OperadorasPaginadas, 
    OperadoraDetalhe, 
    DespesaHistorico,
    PaginationMeta
)
from app.services.data_service import data_service
from app.core.config import settings

router = APIRouter()

@router.get("/operadoras", response_model=OperadorasPaginadas)
async def listar_operadoras(
    page: int = Query(1, ge=1, description="Número da página"),
    limit: int = Query(10, ge=1, le=100, description="Registros por página"),
    q: Optional[str] = Query(None, description="Buscar por razão social ou CNPJ"),
    uf: Optional[str] = Query(None, max_length=2, description="Filtrar por UF")
):
    """
    Lista todas as operadoras com paginação
    
    - **page**: Número da página (começa em 1)
    - **limit**: Quantidade de registros por página (máximo 100)
    - **q**: Termo de busca (razão social ou CNPJ)
    - **uf**: Filtrar por UF (ex: SP, RJ)
    """
    operadoras, total = data_service.get_operadoras(
        page=page,
        limit=limit,
        search=q,
        uf=uf
    )
    
    total_pages = math.ceil(total / limit) if total > 0 else 1
    
    return {
        "data": operadoras,
        "meta": {
            "total": total,
            "page": page,
            "limit": limit,
            "pages": total_pages,
            "has_next": page < total_pages,
            "has_prev": page > 1
        }
    }
# listar_operadoras

@router.get("/operadoras/{cnpj}", response_model=OperadoraDetalhe)
async def obter_operadora(
    cnpj: str = Path(..., description="CNPJ da operadora (com ou sem formatação)")
):
    """
    Retorna detalhes de uma operadora específica
    
    - **cnpj**: CNPJ da operadora (aceita com ou sem pontuação)
    """
    operadora = data_service.get_operadora_by_cnpj(cnpj)
    
    if not operadora:
        raise HTTPException(
            status_code=404, 
            detail=f"Operadora com CNPJ {cnpj} não encontrada"
        )
    
    return operadora
# obter_operadora

@router.get("/operadoras/{cnpj}/despesas", response_model=list[DespesaHistorico])
async def obter_despesas_operadora(
    cnpj: str = Path(..., description="CNPJ da operadora")
):
    """
    Retorna histórico de despesas de uma operadora
    
    - **cnpj**: CNPJ da operadora
    
    Retorna lista de despesas ordenadas cronologicamente
    """
    # Verificar se operadora existe
    operadora = data_service.get_operadora_by_cnpj(cnpj)
    
    if not operadora:
        raise HTTPException(
            status_code=404, 
            detail=f"Operadora com CNPJ {cnpj} não encontrada"
        )
    
    despesas = data_service.get_despesas_operadora(cnpj)
    
    return despesas
# obter_despesas_operadora
