/**
 * Store Pinia para gerenciamento de estado das operadoras
 */

import { defineStore } from 'pinia'
import api from '../services/api'

export const useOperadorasStore = defineStore('operadoras', {
  state: () => ({
    operadoras: [],
    operadoraAtual: null,
    despesasOperadora: [],
    estatisticas: null,
    loading: false,
    error: null,
    pagination: {
      page: 1,
      limit: 10,
      total: 0,
      pages: 0
    },
    filtros: {
      q: '',
      uf: ''
    }
  }),

  getters: {
    hasOperadoras: (state) => state.operadoras.length > 0,
    isLoading: (state) => state.loading,
    hasError: (state) => !!state.error,
    totalPages: (state) => state.pagination.pages
  },

  actions: {
    /**
     * Busca lista de operadoras com paginação e filtros
     */
    async fetchOperadoras() {
      this.loading = true
      this.error = null

      try {
        const params = {
          page: this.pagination.page,
          limit: this.pagination.limit
        }

        if (this.filtros.q) params.q = this.filtros.q
        if (this.filtros.uf) params.uf = this.filtros.uf

        const response = await api.get('/operadoras', { params })

        this.operadoras = response.data.data
        this.pagination = {
          ...this.pagination,
          total: response.data.meta.total,
          pages: response.data.meta.pages
        }
      } catch (error) {
        this.error = error.response?.data?.detail || 'Erro ao carregar operadoras'
        console.error('Erro ao buscar operadoras:', error)
      } finally {
        this.loading = false
      }
    },

    /**
     * Busca detalhes de uma operadora específica
     */
    async fetchOperadora(cnpj) {
      this.loading = true
      this.error = null

      try {
        const response = await api.get(`/operadoras/${cnpj}`)
        this.operadoraAtual = response.data
      } catch (error) {
        this.error = error.response?.data?.detail || 'Operadora não encontrada'
        this.operadoraAtual = null
      } finally {
        this.loading = false
      }
    },

    /**
     * Busca histórico de despesas de uma operadora
     */
    async fetchDespesasOperadora(cnpj) {
      this.loading = true
      this.error = null

      try {
        const response = await api.get(`/operadoras/${cnpj}/despesas`)
        this.despesasOperadora = response.data
      } catch (error) {
        this.error = error.response?.data?.detail || 'Erro ao carregar despesas'
        this.despesasOperadora = []
      } finally {
        this.loading = false
      }
    },

    /**
     * Busca estatísticas gerais
     */
    async fetchEstatisticas() {
      this.loading = true
      this.error = null

      try {
        const response = await api.get('/estatisticas')
        this.estatisticas = response.data
      } catch (error) {
        this.error = error.response?.data?.detail || 'Erro ao carregar estatísticas'
      } finally {
        this.loading = false
      }
    },

    /**
     * Atualiza filtros e recarrega dados
     */
    setFiltros(filtros) {
      this.filtros = { ...this.filtros, ...filtros }
      this.pagination.page = 1
      this.fetchOperadoras()
    },

    /**
     * Muda de página
     */
    setPage(page) {
      this.pagination.page = page
      this.fetchOperadoras()
    },

    /**
     * Limpa filtros
     */
    clearFiltros() {
      this.filtros = { q: '', uf: '' }
      this.pagination.page = 1
      this.fetchOperadoras()
    }
  }
})
