"""
Rotas da API para Estatísticas
"""

from fastapi import APIRouter

from app.schemas.schemas import EstatisticasResponse
from app.services.data_service import data_service

router = APIRouter()

@router.get("/estatisticas", response_model=EstatisticasResponse)
async def obter_estatisticas():
    """
    Retorna estatísticas agregadas dos dados
    
    Inclui:
    - Total de despesas
    - Média de despesas
    - Total de operadoras
    - Top 5 operadoras por despesas
    - Distribuição de despesas por UF
    
    **Nota**: Os dados são cacheados por 5 minutos para melhor performance.
    """
    return data_service.get_estatisticas()
# obter_estatisticas
