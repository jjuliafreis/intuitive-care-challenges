<template>
  <div class="dashboard">
    <h1 class="mb-3">Dashboard - Operadoras de Saúde</h1>

    <!-- Loading State -->
    <div v-if="store.loading" class="loading">
      <div class="spinner"></div>
    </div>

    <!-- Error State -->
    <div v-else-if="store.error" class="alert alert-error">
      {{ store.error }}
      <button class="btn btn-secondary" @click="loadData" style="margin-left: 1rem">
        Tentar novamente
      </button>
    </div>

    <!-- Content -->
    <template v-else-if="store.estatisticas">
      <!-- Stats Cards -->
      <div class="grid grid-4 mb-3">
        <div class="card stat-card">
          <div class="stat-value">{{ formatCurrency(store.estatisticas.total_despesas) }}</div>
          <div class="stat-label">Total de Despesas</div>
        </div>
        <div class="card stat-card">
          <div class="stat-value">{{ formatCurrency(store.estatisticas.media_despesas) }}</div>
          <div class="stat-label">Média de Despesas</div>
        </div>
        <div class="card stat-card">
          <div class="stat-value">{{ store.estatisticas.total_operadoras }}</div>
          <div class="stat-label">Total de Operadoras</div>
        </div>
        <div class="card stat-card">
          <div class="stat-value">{{ store.estatisticas.distribuicao_por_uf?.length || 0 }}</div>
          <div class="stat-label">Estados com Operadoras</div>
        </div>
      </div>

      <!-- Charts Row -->
      <div class="grid grid-2 mb-3">
        <!-- Gráfico de Distribuição por UF -->
        <div class="card">
          <div class="card-header">
            <h2 class="card-title">Distribuição de Despesas por UF</h2>
          </div>
          <div style="height: 300px;">
            <Bar v-if="chartDataUF" :data="chartDataUF" :options="chartOptions" />
          </div>
        </div>

        <!-- Gráfico de Pizza -->
        <div class="card">
          <div class="card-header">
            <h2 class="card-title">Participação por UF (%)</h2>
          </div>
          <div style="height: 300px;">
            <Pie v-if="chartDataPie" :data="chartDataPie" :options="pieOptions" />
          </div>
        </div>
      </div>

      <!-- Top 5 Operadoras -->
      <div class="card">
        <div class="card-header">
          <h2 class="card-title">Top 5 Operadoras por Despesas</h2>
        </div>
        <div class="table-container">
          <table>
            <thead>
              <tr>
                <th>Posição</th>
                <th>Razão Social</th>
                <th>CNPJ</th>
                <th>UF</th>
                <th class="text-right">Total Despesas</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(op, index) in store.estatisticas.top_5_operadoras" :key="op.cnpj">
                <td>
                  <span class="badge badge-primary">{{ index + 1 }}º</span>
                </td>
                <td>{{ op.razao_social }}</td>
                <td>{{ formatCNPJ(op.cnpj) }}</td>
                <td>{{ op.uf || '-' }}</td>
                <td class="text-right">{{ formatCurrency(op.total_despesas) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>

    <!-- Empty State -->
    <div v-else class="empty-state">
      <div class="empty-state-icon">📊</div>
      <p>Nenhum dado disponível</p>
      <button class="btn btn-primary mt-2" @click="loadData">Carregar Dados</button>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { Bar, Pie } from 'vue-chartjs'
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  BarElement,
  ArcElement,
  Title,
  Tooltip,
  Legend
} from 'chart.js'
import { useOperadorasStore } from '../stores/operadoras'

// Registrar componentes do Chart.js
ChartJS.register(CategoryScale, LinearScale, BarElement, ArcElement, Title, Tooltip, Legend)

const store = useOperadorasStore()

// Carregar dados ao montar
onMounted(() => {
  loadData()
})

const loadData = () => {
  store.fetchEstatisticas()
}

// Dados do gráfico de barras
const chartDataUF = computed(() => {
  if (!store.estatisticas?.distribuicao_por_uf) return null

  const top10 = store.estatisticas.distribuicao_por_uf.slice(0, 10)

  return {
    labels: top10.map(d => d.uf),
    datasets: [{
      label: 'Total de Despesas',
      data: top10.map(d => d.total),
      backgroundColor: 'rgba(37, 99, 235, 0.8)',
      borderColor: 'rgba(37, 99, 235, 1)',
      borderWidth: 1
    }]
  }
})

// Dados do gráfico de pizza
const chartDataPie = computed(() => {
  if (!store.estatisticas?.distribuicao_por_uf) return null

  const top5 = store.estatisticas.distribuicao_por_uf.slice(0, 5)

  return {
    labels: top5.map(d => d.uf),
    datasets: [{
      data: top5.map(d => d.percentual),
      backgroundColor: [
        'rgba(37, 99, 235, 0.8)',
        'rgba(34, 197, 94, 0.8)',
        'rgba(245, 158, 11, 0.8)',
        'rgba(239, 68, 68, 0.8)',
        'rgba(139, 92, 246, 0.8)'
      ]
    }]
  }
})

// Opções do gráfico
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

const pieOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: {
      position: 'right'
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
  if (value >= 1000000000) {
    return `R$ ${(value / 1000000000).toFixed(1)}B`
  }
  if (value >= 1000000) {
    return `R$ ${(value / 1000000).toFixed(1)}M`
  }
  if (value >= 1000) {
    return `R$ ${(value / 1000).toFixed(1)}K`
  }
  return `R$ ${value}`
}

const formatCNPJ = (cnpj) => {
  if (!cnpj || cnpj.length !== 14) return cnpj
  return cnpj.replace(/^(\d{2})(\d{3})(\d{3})(\d{4})(\d{2})$/, '$1.$2.$3/$4-$5')
}
</script>
