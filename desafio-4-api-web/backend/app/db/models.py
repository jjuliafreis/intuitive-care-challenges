"""
Modelos SQLAlchemy para o banco de dados
"""

from sqlalchemy import Column, Integer, String, Numeric, Boolean, DateTime, func
from app.db.database import Base

class Operadora(Base):
    """Modelo para dados cadastrais de operadoras"""
    __tablename__ = "operadoras_cadastro"
    
    id = Column(Integer, primary_key=True, index=True)
    registro_ans = Column(String(20), unique=True, index=True)
    cnpj = Column(String(14), unique=True, index=True, nullable=False)
    razao_social = Column(String(255), nullable=False, index=True)
    nome_fantasia = Column(String(255))
    modalidade = Column(String(100))
    uf = Column(String(2), index=True)
    municipio = Column(String(150))
    data_importacao = Column(DateTime, server_default=func.now())
# Operadora

class DespesaConsolidada(Base):
    """Modelo para despesas consolidadas"""
    __tablename__ = "despesas_consolidadas"
    
    id = Column(Integer, primary_key=True, index=True)
    cnpj = Column(String(14), nullable=False, index=True)
    razao_social = Column(String(255), nullable=False, index=True)
    trimestre = Column(String(5), nullable=False)
    ano = Column(Integer, nullable=False)
    valor_despesas = Column(Numeric(18, 2), nullable=False, default=0)
    cnpj_valido = Column(Boolean, default=True)
    valor_valido = Column(Boolean, default=True)
    match_cadastro = Column(Boolean, default=False)
    registro_ans = Column(String(20))
    modalidade = Column(String(100))
    uf = Column(String(2), index=True)
    data_importacao = Column(DateTime, server_default=func.now())
# DespesaConsolidada

class DespesaAgregada(Base):
    """Modelo para despesas agregadas por operadora/UF"""
    __tablename__ = "despesas_agregadas"
    
    id = Column(Integer, primary_key=True, index=True)
    razao_social = Column(String(255), nullable=False, index=True)
    uf = Column(String(2), index=True)
    total_despesas = Column(Numeric(18, 2), nullable=False, default=0)
    media_por_trimestre = Column(Numeric(18, 2), nullable=False, default=0)
    desvio_padrao = Column(Numeric(18, 2), nullable=False, default=0)
    quantidade_trimestres = Column(Integer, nullable=False, default=0)
    data_importacao = Column(DateTime, server_default=func.now())
# DespesaAgregada
