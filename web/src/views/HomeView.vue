<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ROUTES } from '@/router/routes'

const router = useRouter()
</script>

<template>
  <div class="home-container">
    <a-card title="Bem-vindo ao Sistema de Gerenciamento Xiaozhi ESP32" :bordered="false">
      <p>Esta é a página inicial, onde você pode visualizar uma visão geral do sistema.</p>
      <a-space>
        <a-button type="primary" @click="router.push(ROUTES.DASHBOARD)">Ir para o Painel</a-button>
        <a-button @click="router.push(ROUTES.ABOUT)">Sobre o Sistema</a-button>
      </a-space>
    </a-card>
  </div>
</template>

<style scoped>
.home-container {
  padding: 24px;
}
</style>