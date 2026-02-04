/**
 * Ponto de entrada da aplicação Vue.js
 * 
 * TRADE-OFF TÉCNICO: GERENCIAMENTO DE ESTADO
 * 
 * DECISÃO: Pinia (Vue 3 state management)
 * 
 * JUSTIFICATIVA:
 * - Oficial: Recomendado pelo time do Vue para Vue 3
 * - TypeScript: Melhor suporte a tipos que Vuex
 * - Simples: API mais intuitiva e menos boilerplate
 * - Composables: Integra naturalmente com Composition API
 * 
 * ALTERNATIVAS CONSIDERADAS:
 * - Props/Events: Muito simples, dificulta compartilhamento entre componentes distantes
 * - Vuex: Mais verboso, menos adequado para Vue 3
 * - Composables puros: Possível, mas Pinia oferece DevTools
 */

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import './styles/main.css'

const app = createApp(App)

app.use(createPinia())
app.use(router)

app.mount('#app')
