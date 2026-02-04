/**
 * Configuração do cliente Axios para chamadas à API
 * 
 * TRADE-OFF TÉCNICO: TRATAMENTO DE ERROS E LOADING
 * 
 * DECISÃO: Mensagens de erro específicas quando possível
 * 
 * JUSTIFICATIVA:
 * - Melhor UX: Usuário entende o que aconteceu
 * - Debugging: Facilita identificação de problemas
 * - Fallback: Mensagem genérica quando específica não disponível
 * 
 * IMPLEMENTAÇÃO:
 * - Erros de rede: "Erro de conexão. Verifique sua internet."
 * - 404: "Recurso não encontrado"
 * - 500: "Erro interno do servidor"
 * - Outros: Mensagem do backend ou genérica
 */

import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// Interceptor de resposta para tratamento de erros
api.interceptors.response.use(
  (response) => response,
  (error) => {
    // Erro de rede
    if (!error.response) {
      error.message = 'Erro de conexão. Verifique sua internet.'
      return Promise.reject(error)
    }

    // Tratar códigos de status específicos
    switch (error.response.status) {
      case 400:
        error.message = error.response.data?.detail || 'Requisição inválida'
        break
      case 401:
        error.message = 'Não autorizado'
        break
      case 403:
        error.message = 'Acesso negado'
        break
      case 404:
        error.message = error.response.data?.detail || 'Recurso não encontrado'
        break
      case 500:
        error.message = 'Erro interno do servidor. Tente novamente mais tarde.'
        break
      default:
        error.message = error.response.data?.detail || 'Ocorreu um erro inesperado'
    }

    return Promise.reject(error)
  }
)

export default api
