/**
 * Configuração do Vue Router
 */

import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Dashboard',
    component: () => import('../views/DashboardView.vue')
  },
  {
    path: '/operadoras',
    name: 'Operadoras',
    component: () => import('../views/OperadorasView.vue')
  },
  {
    path: '/operadoras/:cnpj',
    name: 'OperadoraDetalhe',
    component: () => import('../views/OperadoraDetalheView.vue'),
    props: true
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
