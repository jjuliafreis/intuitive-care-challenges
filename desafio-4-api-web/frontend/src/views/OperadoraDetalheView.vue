<template>
  <div class="operadora-detalhe">
    <!-- Navegação -->
    <div class="mb-3">
      <router-link to="/operadoras" class="btn btn-secondary">
        ← Voltar para lista
      </router-link>
    </div>

    <!-- Loading -->
    <div v-if="store.loading" class="loading">
      <div class="spinner"></div>
    </div>

    <!-- Error -->
    <div v-else-if="store.error" class="alert alert-error">
      {{ store.error }}
    </div>

    <!-- Conteúdo -->
    <template v-else-if="store.operadoraAtual">
      <!-- Cabeçalho -->
      <div class="card mb-3">
        <div class="card-header">
          <h1 class="card-title">{{ store.operadoraAtual.razao_social }}</h1>
          <span v-if="store.operadoraAtual.uf" class="badge badge-primary" style="font-size: 1rem">
            {{ store.operadoraAtual.uf }}
          </span>
        </div>

        <div class="grid grid-3 mt-2">
          <div>
            <strong>CNPJ:</strong>
            <p>{{ formatCNPJ(store.operadoraAtual.cnpj) }}</p>
          </div>
          <div>
            <strong>Registro ANS:</strong>
            <p>{{ store.operadoraAtual.registro_ans || 'Não informado' }}</p>
          </div>
          <div>
            <strong>Modalidade:</strong>
            <p>{{ store.operadoraAtual.modalidade || 'Não informada' }}</p>
          </div>
        </div>
      </div>

      <!-- Resumo -->
      <div class="grid grid-2 mb-3">
        <div class="card stat-card">
          <div class="stat-value">{{ formatCurrency(store.operadoraAtual.total_despesas) }}</div>
          <div class="stat-label">Total de Despesas</div>
        </div>
        <div class="card stat-card">
          <div class="stat-value">{{ store.operadoraAtual.quantidade_trimestres }}</div>
          <div class="stat-label">Trimestres com Dados</div>
        </div>
      </div>

      <!-- Gráfico de Evolução -->
      <div class="card mb-3" v-if="store.despesasOperadora.length > 0">
        <div class="card-header">
          <h2 class="card-title">Evolução de Despesas</h2>
        </div>
        <div style="height: 300px;">
          <Line :data="chartData" :options="chartOptions" />
        </div>
      </div>

      <!-- Tabela de Histórico -->
      <div class="card">
        <div class="card-header">
          <h2 class="card-title">Histórico de Despesas</h2>
        </div>

        <div v-if="store.despesasOperadora.length === 0" class="empty-state">
          <p>Nenhum histórico de despesas disponível</p>
        </div>

        <div v-else class="table-container">
          <table>
            <thead>
              <tr>
                <th>Ano</th>
                <th>Trimestre</th>
                <th class="text-right">Valor</th>
                <th class="text-right">Variação</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(despesa, index) in store.despesasOperadora" :key="`${despesa.ano}-${despesa.trimestre}`">
                <td>{{ despesa.ano }}</td>
                <td>{{ despesa.trimestre }}</td>
                <td class="text-right">{{ formatCurrency(despesa.valor_despesas) }}</td>
                <td class="text-right">
                  <span v-if="index === 0">-</span>
                  <span v-else :class="getVariacaoClass(index)">
                    {{ calcularVariacao(index) }}
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>

    <!-- Not Found -->
    <div v-else class="card empty-state">
      <div class="empty-state-icon">🔍</div>
      <p>Operadora não encontrada</p>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Line } from 'vue-chartjs'
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend
} from 'chart.js'
import { useOperadorasStore } from '../stores/operadoras'

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Title, Tooltip, Legend)

const props = defineProps({
  cnpj: {
    type: String,
    required: true
  }
})

const route = useRoute()
const store = useOperadorasStore()

// Carregar dados
const loadData = async () => {
  const cnpj = props.cnpj || route.params.cnpj
  await store.fetchOperadora(cnpj)
  await store.fetchDespesasOperadora(cnpj)
}

onMounted(loadData)

// Recarregar se CNPJ mudar
watch(() => route.params.cnpj, loadData)

// Dados do gráfico
const chartData = computed(() => {
  if (!store.despesasOperadora.length) return null

  return {
    labels: store.despesasOperadora.map(d => `${d.trimestre}/${d.ano}`),
    datasets: [{
      label: 'Despesas',
      data: store.despesasOperadora.map(d => d.valor_despesas),
      borderColor: 'rgba(37, 99, 235, 1)',
      backgroundColor: 'rgba(37, 99, 235, 0.1)',
      fill: true,
      tension: 0.3
    }]
  }
})

const chartOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: {
      display: false
    }
  },
  scales: {
    y: {
      beginAtZero: true,
      ticks: {
        callback: (value) => formatCurrencyShort(value)
      }
    }
  }
}

// Formatadores
const formatCurrency = (value) => {
  if (!value && value !== 0) return '-'
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL'
  }).format(value)
}

const formatCurrencyShort = (value) => {
  if (value >= 1000000) return `R$ ${(value / 1000000).toFixed(1)}M`
  if (value >= 1000) return `R$ ${(value / 1000).toFixed(1)}K`
  return `R$ ${value}`
}

const formatCNPJ = (cnpj) => {
  if (!cnpj || cnpj.length !== 14) return cnpj
  return cnpj.replace(/^(\d{2})(\d{3})(\d{3})(\d{4})(\d{2})$/, '$1.$2.$3/$4-$5')
}

// Calcular variação percentual
const calcularVariacao = (index) => {
  if (index === 0) return '-'
  
  const atual = store.despesasOperadora[index].valor_despesas
  const anterior = store.despesasOperadora[index - 1].valor_despesas
  
  if (!anterior || anterior === 0) return 'N/A'
  
  const variacao = ((atual - anterior) / anterior) * 100
  const sinal = variacao >= 0 ? '+' : ''
  
  return `${sinal}${variacao.toFixed(1)}%`
}

const getVariacaoClass = (index) => {
  if (index === 0) return ''
  
  const atual = store.despesasOperadora[index].valor_despesas
  const anterior = store.despesasOperadora[index - 1].valor_despesas
  
  if (!anterior || anterior === 0) return ''
  
  return atual >= anterior ? 'text-success' : 'text-error'
}
</script>

<style scoped>
.text-success {
  color: var(--success-color);
  font-weight: 500;
}

.text-error {
  color: var(--error-color);
  font-weight: 500;
}
</style>
