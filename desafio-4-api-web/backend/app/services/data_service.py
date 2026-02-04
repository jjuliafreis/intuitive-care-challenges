"""
Serviço de dados - abstrai acesso a banco ou CSV
Permite funcionar com ou sem banco de dados
"""

import pandas as pd
from typing import List, Optional, Tuple
from decimal import Decimal
import os
from functools import lru_cache
from cachetools import TTLCache

from app.core.config import settings

# Cache com TTL para estatísticas
stats_cache = TTLCache(maxsize=100, ttl=settings.CACHE_TTL_SECONDS)

class DataService:
    """
    Serviço de acesso a dados
    Pode usar CSV diretamente ou banco de dados
    """
    
    def __init__(self):
        self._df_despesas: Optional[pd.DataFrame] = None
        self._df_agregadas: Optional[pd.DataFrame] = None
        self._df_operadoras: Optional[pd.DataFrame] = None
    # __init__
    
    def _load_despesas(self) -> pd.DataFrame:
        """Carrega dados de despesas do CSV"""
        if self._df_despesas is None:
            if os.path.exists(settings.CSV_DESPESAS):
                self._df_despesas = pd.read_csv(
                    settings.CSV_DESPESAS, 
                    sep=';', 
                    encoding='utf-8',
                    dtype={'CNPJ': str, 'Ano': int}
                )
                # Normalizar nomes de colunas
                self._df_despesas.columns = [c.lower().replace(' ', '_') for c in self._df_despesas.columns]
            else:
                self._df_despesas = pd.DataFrame()
        return self._df_despesas
    # _load_despesas
    
    def _load_agregadas(self) -> pd.DataFrame:
        """Carrega dados agregados do CSV"""
        if self._df_agregadas is None:
            if os.path.exists(settings.CSV_AGREGADAS):
                self._df_agregadas = pd.read_csv(
                    settings.CSV_AGREGADAS, 
                    sep=';', 
                    encoding='utf-8'
                )
                self._df_agregadas.columns = [c.lower().replace(' ', '_') for c in self._df_agregadas.columns]
            else:
                self._df_agregadas = pd.DataFrame()
        return self._df_agregadas
    # _load_agregadas
    
    def get_operadoras(
        self, 
        page: int = 1, 
        limit: int = 10,
        search: Optional[str] = None,
        uf: Optional[str] = None
    ) -> Tuple[List[dict], int]:
        """
        Lista operadoras com paginação e filtros
        
        TRADE-OFF: BUSCA NO SERVIDOR
        
        DECISÃO: Busca no servidor (não no cliente)
        
        JUSTIFICATIVA:
        - Volume de dados pode ser grande (>1000 operadoras)
        - Evita transferir todos os dados para o cliente
        - Permite filtros complexos no futuro
        - Melhor para conexões lentas
        
        CONTRAS:
        - Latência de rede para cada busca
        - Mais requisições ao servidor
        """
        df = self._load_despesas()
        
        if df.empty:
            return [], 0
        
        # Agrupar por CNPJ para obter operadoras únicas
        operadoras = df.groupby('cnpj').agg({
            'razaosocial': 'first',
            'registroans': 'first',
            'modalidade': 'first',
            'uf': 'first',
            'valordespesas': 'sum'
        }).reset_index()
        
        operadoras.columns = ['cnpj', 'razao_social', 'registro_ans', 'modalidade', 'uf', 'total_despesas']
        
        # Aplicar filtros
        if search:
            search_lower = search.lower()
            mask = (
                operadoras['razao_social'].str.lower().str.contains(search_lower, na=False) |
                operadoras['cnpj'].str.contains(search, na=False)
            )
            operadoras = operadoras[mask]
        
        if uf:
            operadoras = operadoras[operadoras['uf'].str.upper() == uf.upper()]
        
        total = len(operadoras)
        
        # Ordenar por total de despesas
        operadoras = operadoras.sort_values('total_despesas', ascending=False)
        
        # Aplicar paginação
        start = (page - 1) * limit
        end = start + limit
        page_data = operadoras.iloc[start:end]
        
        # Converter para lista de dicts
        result = []
        for idx, (_, row) in enumerate(page_data.iterrows()):
            result.append({
                'id': start + idx + 1,
                'cnpj': str(row['cnpj']),
                'razao_social': str(row['razao_social']),
                'registro_ans': str(int(row['registro_ans'])) if pd.notna(row['registro_ans']) else None,
                'modalidade': str(row['modalidade']) if pd.notna(row['modalidade']) else None,
                'uf': str(row['uf']) if pd.notna(row['uf']) else None,
                'total_despesas': float(row['total_despesas'])
            })
        
        return result, total
    # get_operadoras
    
    def get_operadora_by_cnpj(self, cnpj: str) -> Optional[dict]:
        """Obtém detalhes de uma operadora pelo CNPJ"""
        df = self._load_despesas()
        
        if df.empty:
            return None
        
        # Normalizar CNPJ
        cnpj_norm = cnpj.replace('.', '').replace('/', '').replace('-', '')
        
        # Filtrar por CNPJ
        op_data = df[df['cnpj'] == cnpj_norm]
        
        if op_data.empty:
            return None
        
        first_row = op_data.iloc[0]
        
        return {
            'id': 1,
            'cnpj': str(cnpj_norm),
            'razao_social': str(first_row.get('razaosocial', '')),
            'registro_ans': str(int(first_row.get('registroans'))) if pd.notna(first_row.get('registroans')) else None,
            'modalidade': str(first_row.get('modalidade')) if pd.notna(first_row.get('modalidade')) else None,
            'uf': str(first_row.get('uf')) if pd.notna(first_row.get('uf')) else None,
            'total_despesas': float(op_data['valordespesas'].sum()),
            'quantidade_trimestres': len(op_data)
        }
    # get_operadora_by_cnpj
    
    def get_despesas_operadora(self, cnpj: str) -> List[dict]:
        """Obtém histórico de despesas de uma operadora"""
        df = self._load_despesas()
        
        if df.empty:
            return []
        
        cnpj_norm = cnpj.replace('.', '').replace('/', '').replace('-', '')
        despesas = df[df['cnpj'] == cnpj_norm]
        
        result = []
        for _, row in despesas.iterrows():
            result.append({
                'trimestre': row['trimestre'],
                'ano': int(row['ano']),
                'valor_despesas': float(row['valordespesas'])
            })
        
        # Ordenar por ano e trimestre
        result.sort(key=lambda x: (x['ano'], x['trimestre']))
        
        return result
    # get_despesas_operadora
    
    def get_estatisticas(self) -> dict:
        """
        Obtém estatísticas gerais
        
        TRADE-OFF: CACHE VS QUERIES DIRETAS
        
        DECISÃO: Cachear resultado por 5 minutos (TTL Cache)
        
        JUSTIFICATIVA:
        - Dados de despesas são atualizados trimestralmente
        - Estatísticas não precisam ser em tempo real
        - Reduz carga no servidor/banco
        - TTL de 5 minutos equilibra performance e frescor
        
        CONTRAS:
        - Pode mostrar dados desatualizados por até 5 minutos
        - Usa memória para cache
        """
        cache_key = 'estatisticas'
        
        if cache_key in stats_cache:
            return stats_cache[cache_key]
        
        df = self._load_despesas()
        
        if df.empty:
            return {
                'total_despesas': 0,
                'media_despesas': 0,
                'total_operadoras': 0,
                'top_5_operadoras': [],
                'distribuicao_por_uf': []
            }
        
        # Calcular estatísticas
        total_despesas = float(df['valordespesas'].sum())
        media_despesas = float(df['valordespesas'].mean())
        total_operadoras = df['cnpj'].nunique()
        
        # Top 5 operadoras
        top_ops = df.groupby(['cnpj', 'razaosocial', 'uf']).agg({
            'valordespesas': 'sum'
        }).reset_index()
        top_ops = top_ops.nlargest(5, 'valordespesas')
        
        top_5 = []
        for _, row in top_ops.iterrows():
            top_5.append({
                'cnpj': row['cnpj'],
                'razao_social': row['razaosocial'],
                'total_despesas': float(row['valordespesas']),
                'uf': row['uf'] if pd.notna(row['uf']) else None
            })
        
        # Distribuição por UF
        dist_uf = df.groupby('uf').agg({
            'valordespesas': 'sum',
            'cnpj': 'nunique'
        }).reset_index()
        dist_uf.columns = ['uf', 'total', 'quantidade_operadoras']
        dist_uf = dist_uf[dist_uf['uf'].notna() & (dist_uf['uf'] != '')]
        dist_uf = dist_uf.sort_values('total', ascending=False)
        
        distribuicao = []
        for _, row in dist_uf.iterrows():
            distribuicao.append({
                'uf': row['uf'],
                'total': float(row['total']),
                'quantidade_operadoras': int(row['quantidade_operadoras']),
                'percentual': round(float(row['total']) / total_despesas * 100, 2) if total_despesas > 0 else 0
            })
        
        result = {
            'total_despesas': total_despesas,
            'media_despesas': media_despesas,
            'total_operadoras': total_operadoras,
            'top_5_operadoras': top_5,
            'distribuicao_por_uf': distribuicao
        }
        
        stats_cache[cache_key] = result
        return result
    # get_estatisticas
# DataService

# Singleton do serviço
data_service = DataService()
