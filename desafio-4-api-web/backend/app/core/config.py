"""
Configurações da aplicação
Utiliza Pydantic Settings para validação e carregamento de variáveis de ambiente
"""

from pydantic_settings import BaseSettings
from typing import List
import os
from pathlib import Path

# Diretório base do projeto
BASE_DIR = Path(__file__).resolve().parent.parent.parent.parent.parent
DESAFIO_2_OUTPUT = BASE_DIR / "desafio-2-transformacao" / "output"

class Settings(BaseSettings):
    """Configurações da aplicação"""
    
    # Banco de dados
    DATABASE_URL: str = "sqlite:///./operadoras.db"
    
    # Alternativas para produção:
    # PostgreSQL: "postgresql://user:password@localhost/dbname"
    # MySQL: "mysql+mysqlconnector://user:password@localhost/dbname"
    
    # API
    API_V1_PREFIX: str = "/api"
    
    # CORS
    CORS_ORIGINS: List[str] = ["http://localhost:3000", "http://localhost:5173", "http://127.0.0.1:5173"]
    
    # Paginação
    DEFAULT_PAGE_SIZE: int = 10
    MAX_PAGE_SIZE: int = 100
    
    # Cache
    CACHE_TTL_SECONDS: int = 300  # 5 minutos
    
    # Caminhos de arquivos CSV (para modo sem banco)
    CSV_DESPESAS: str = str(DESAFIO_2_OUTPUT / "consolidado_enriquecido.csv")
    CSV_AGREGADAS: str = str(DESAFIO_2_OUTPUT / "despesas_agregadas.csv")
    CSV_OPERADORAS: str = str(BASE_DIR / "desafio-2-transformacao" / "temp" / "Relatorio_cadop.csv")
    
    class Config:
        env_file = ".env"
        case_sensitive = True
    # Config
# Settings

settings = Settings()
