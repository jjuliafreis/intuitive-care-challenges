<template>
  <div class="operadoras">
    <h1 class="mb-3">Operadoras de Planos de Saúde</h1>

    <!-- Filtros -->
    <!--
      TRADE-OFF TÉCNICO: ESTRATÉGIA DE BUSCA/FILTRO
      
      DECISÃO: Busca no servidor
      
      JUSTIFICATIVA:
      - Volume potencialmente grande de dados
      - Evita transferir todos os registros para o cliente
      - Permite filtros mais complexos no futuro
      - Melhor para dispositivos com pouca memória
      
      IMPLEMENTAÇÃO:
      - Debounce de 300ms para evitar muitas requisições
      - Busca enviada ao servidor via query params
    -->
    <div class="card mb-3">
      <div class="flex gap-4 items-center">
        <div style="flex: 2">
          <input
            v-model="searchQuery"
            type="text"
            class="input"
            placeholder="Buscar por razão social ou CNPJ..."
            @input="debouncedSearch"
          />
        </div>
        <div style="flex: 1">
          <select v-model="selectedUF" class="input" @change="applyFilters">
            <option value="">Todas as UFs</option>
            <option v-for="uf in ufs" :key="uf" :value="uf">{{ uf }}</option>
          </select>
        </div>
        <button class="btn btn-secondary" @click="clearFilters">
          Limpar
        </button>
      </div>
    </div>

    <!-- Loading -->
    <div v-if="store.loading" class="loading">
      <div class="spinner"></div>
    </div>

    <!-- Error -->
    <div v-else-if="store.error" class="alert alert-error">
      {{ store.error }}
      <button class="btn btn-secondary" @click="loadOperadoras" style="margin-left: 1rem">
        Tentar novamente
      </button>
    </div>

    <!-- Tabela de Operadoras -->
    <!--
      TRADE-OFF TÉCNICO: PERFORMANCE DA TABELA
      
      DECISÃO: Tabela HTML padrão com paginação server-side
      
      JUSTIFICATIVA:
      - Paginação limita registros renderizados (10-20 por página)
      - Não há necessidade de virtualização para este volume
      - Mais simples de implementar e manter
      - Acessível (funciona com leitores de tela)
      
      ALTERNATIVAS CONSIDERADAS:
      - Virtual Scrolling: Overhead para poucas centenas de itens
      - Infinite Scroll: Menos controle, confuso para navegação
    -->
    <div v-else-if="store.hasOperadoras" class="card">
      <div class="table-container">
        <table>
          <thead>
            <tr>
              <th>CNPJ</th>
              <th>Razão Social</th>
              <th>Registro ANS</th>
              <th>Modalidade</th>
              <th>UF</th>
              <th class="text-right">Ações</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="op in store.operadoras" :key="op.cnpj">
              <td>{{ formatCNPJ(op.cnpj) }}</td>
              <td>{{ op.razao_social }}</td>
              <td>{{ op.registro_ans || '-' }}</td>
              <td>{{ op.modalidade || '-' }}</td>
              <td>
                <span v-if="op.uf" class="badge badge-primary">{{ op.uf }}</span>
                <span v-else>-</span>
              </td>
              <td class="text-right">
                <router-link
                  :to="`/operadoras/${op.cnpj}`"
                  class="btn btn-primary"
                >
                  Ver Detalhes
                </router-link>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Paginação -->
      <div class="pagination">
        <button
          class="btn btn-secondary"
          :disabled="store.pagination.page === 1"
          @click="prevPage"
        >
          ← Anterior
        </button>
        
        <span class="page-info">
          Página {{ store.pagination.page }} de {{ store.pagination.pages }}
          ({{ store.pagination.total }} registros)
        </span>
        
        <button
          class="btn btn-secondary"
          :disabled="store.pagination.page >= store.pagination.pages"
          @click="nextPage"
        >
          Próxima →
        </button>
      </div>
    </div>

    <!-- Empty State -->
    <div v-else class="card empty-state">
      <div class="empty-state-icon">🔍</div>
      <p>Nenhuma operadora encontrada</p>
      <p class="text-muted" v-if="searchQuery || selectedUF">
        Tente ajustar os filtros de busca
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useOperadorasStore } from '../stores/operadoras'

const store = useOperadorasStore()

const searchQuery = ref('')
const selectedUF = ref('')

// Lista de UFs
const ufs = [
  'AC', 'AL', 'AP', 'AM', 'BA', 'CE', 'DF', 'ES', 'GO', 'MA',
  'MT', 'MS', 'MG', 'PA', 'PB', 'PR', 'PE', 'PI', 'RJ', 'RN',
  'RS', 'RO', 'RR', 'SC', 'SP', 'SE', 'TO'
]

// Debounce para busca
let debounceTimer = null
const debouncedSearch = () => {
  clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => {
    applyFilters()
  }, 300)
}

const applyFilters = () => {
  store.setFiltros({
    q: searchQuery.value,
    uf: selectedUF.value
  })
}

const clearFilters = () => {
  searchQuery.value = ''
  selectedUF.value = ''
  store.clearFiltros()
}

const prevPage = () => {
  if (store.pagination.page > 1) {
    store.setPage(store.pagination.page - 1)
  }
}

const nextPage = () => {
  if (store.pagination.page < store.pagination.pages) {
    store.setPage(store.pagination.page + 1)
  }
}

const loadOperadoras = () => {
  store.fetchOperadoras()
}

const formatCNPJ = (cnpj) => {
  if (!cnpj || cnpj.length !== 14) return cnpj
  return cnpj.replace(/^(\d{2})(\d{3})(\d{3})(\d{4})(\d{2})$/, '$1.$2.$3/$4-$5')
}

onMounted(() => {
  loadOperadoras()
})
</script>
