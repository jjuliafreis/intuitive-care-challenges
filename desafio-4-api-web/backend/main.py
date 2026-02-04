"""
Desafio 4 - API e Interface Web
Aplicação principal FastAPI

TRADE-OFF TÉCNICO: ESCOLHA DO FRAMEWORK

DECISÃO: FastAPI (em vez de Flask)

JUSTIFICATIVA:
- Performance: FastAPI é baseado em Starlette e é uma das frameworks mais rápidas
- Documentação automática: Swagger/OpenAPI gerado automaticamente
- Validação: Pydantic integrado para validação de tipos
- Async: Suporte nativo a operações assíncronas
- Type hints: Melhor experiência de desenvolvimento e menos bugs

CONTRAS CONSIDERADOS:
- Flask tem ecossistema maior e mais maduro
- Flask é mais simples para projetos pequenos
- Mais tutoriais disponíveis para Flask

Para este projeto, os benefícios de FastAPI (documentação automática, validação)
superam a simplicidade do Flask.
"""

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from contextlib import asynccontextmanager

from app.api import operadoras, estatisticas
from app.core.config import settings
from app.db.database import engine, Base

# Criar tabelas no startup
@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup
    Base.metadata.create_all(bind=engine)
    yield
    # Shutdown
    pass
# lifespan

app = FastAPI(
    title="API Operadoras de Saúde",
    description="API para consulta de operadoras de planos de saúde e suas despesas",
    version="1.0.0",
    lifespan=lifespan
)

# Configurar CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.CORS_ORIGINS,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Registrar rotas
app.include_router(operadoras.router, prefix="/api", tags=["Operadoras"])
app.include_router(estatisticas.router, prefix="/api", tags=["Estatísticas"])

@app.get("/", tags=["Health"])
async def root():
    """Health check endpoint"""
    return {
        "status": "healthy",
        "message": "API Operadoras de Saúde",
        "version": "1.0.0"
    }
# root

@app.get("/health", tags=["Health"])
async def health_check():
    """Verificação de saúde da API"""
    return {"status": "ok"}
# health_check
