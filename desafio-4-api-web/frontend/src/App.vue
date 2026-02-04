<template>
  <div id="app">
    <header class="header">
      <div class="container header-content">
        <router-link to="/" class="logo">
          📊 Operadoras de Saúde
        </router-link>
        <nav class="nav">
          <router-link to="/" class="nav-link">Dashboard</router-link>
          <router-link to="/operadoras" class="nav-link">Operadoras</router-link>
        </nav>
      </div>
    </header>

    <main class="main-content">
      <div class="container">
        <router-view />
      </div>
    </main>

    <footer style="text-align: center; padding: 1rem; color: var(--text-secondary);">
      <p>Desafio 4 - Intuitive Care © 2026 | Júlia Reis</p>
    </footer>
  </div>
</template>

<script setup>
// App.vue - Componente raiz da aplicação
</script>
